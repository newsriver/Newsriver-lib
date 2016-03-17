package ch.newsriver.executable;

import javax.xml.ws.Response;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Created by eliapalme on 14/03/16.
 */
public abstract class InterruptibleWithinExecutorPool extends  ScheduledThreadPoolExecutor{


    private  ScheduledExecutorService timeoutExecutor= null;


    public InterruptibleWithinExecutorPool(int corePoolSize){
        super(corePoolSize);
        timeoutExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
    }

    public InterruptibleWithinExecutorPool(int corePoolSize, RejectedExecutionHandler handler){
        super(corePoolSize,handler);
        timeoutExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
    }

    public InterruptibleWithinExecutorPool(int corePoolSize, ThreadFactory threadFactory){
        super(corePoolSize,threadFactory);
        timeoutExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
    }

    public InterruptibleWithinExecutorPool(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler){
        super(corePoolSize,threadFactory,handler);
        timeoutExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
    }


    public void shutdown(){
        super.shutdown();
        timeoutExecutor.shutdown();
    }


    public ScheduledExecutorService getTimeoutExecutor() {
        return timeoutExecutor;
    }


    public static <T> CompletableFuture<T> supplyAsyncInterruptWithin(final Supplier<T> supplier, Duration duration,  InterruptibleWithinExecutorPool pool) {

        final CompletableFuture<T> cf = new CompletableFuture<T>();


        Future<?> future = pool.submit(() -> {
            try {
                cf.complete(supplier.get());
            } catch (Throwable ex) {
                ex.printStackTrace();
                cf.completeExceptionally(ex);

            }
        });


        pool.getTimeoutExecutor().schedule(() -> {
            if (!cf.isDone() && !cf.isCompletedExceptionally() && !cf.isCancelled()) {
                System.out.println("done:"+cf.isDone()+" int:"+cf.isCompletedExceptionally()+" canc:"+cf.isCancelled());
                System.out.println("done:"+future.isDone()+" canc:"+future.isCancelled());

                future.cancel(true);
                InterruptedException ex = new InterruptedException("Task did not complete within time.");
                cf.completeExceptionally(ex);

            }
        }, duration.toNanos(), TimeUnit.NANOSECONDS);

        return cf;
    }





}
