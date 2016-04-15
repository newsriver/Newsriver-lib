package ch.newsriver.executable.poolExecution;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;


/**
 * Created by eliapalme on 14/03/16.
 */
public abstract class InterruptibleWithinExecutorPool extends  ScheduledThreadPoolExecutor{


    private  ScheduledExecutorService timeoutExecutor= null;
    private  Duration duration;

    public InterruptibleWithinExecutorPool(int corePoolSize, Duration duration){
        super(corePoolSize);
        timeoutExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
        this.duration = duration;
    }

    public InterruptibleWithinExecutorPool(int corePoolSize,Duration duration, RejectedExecutionHandler handler){
        super(corePoolSize,handler);
        timeoutExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
        this.duration = duration;
    }

    public InterruptibleWithinExecutorPool(int corePoolSize,Duration duration, ThreadFactory threadFactory){
        super(corePoolSize,threadFactory);
        timeoutExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
        this.duration = duration;
    }

    public InterruptibleWithinExecutorPool(int corePoolSize,Duration duration, ThreadFactory threadFactory, RejectedExecutionHandler handler){
        super(corePoolSize,threadFactory,handler);
        timeoutExecutor = new ScheduledThreadPoolExecutor(corePoolSize);
        this.duration = duration;
    }


    public void shutdown(){
        super.shutdown();
        timeoutExecutor.shutdown();
    }


    public ScheduledExecutorService getTimeoutExecutor() {
        return timeoutExecutor;
    }


    public static <T> CompletableFuture<T> supplyAsyncInterruptExecutionWithin(final Supplier<T> supplier, InterruptibleWithinExecutorPool pool) {

        final CompletableFuture<T> cf = new CompletableFuture<T>();


        Future<?> future = pool.submit(() -> {
            try {
                cf.complete(supplier.get());
            } catch (Throwable ex) {
                ex.printStackTrace();
                cf.completeExceptionally(ex);

            }
        });

        return cf;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);

        FutureTask cf = (FutureTask) r;


        this.getTimeoutExecutor().schedule(() -> {
            if (!cf.isDone() && !cf.isCancelled()) {
                //System.out.println("done:"+cf.isDone()+" int:"+cf.isCompletedExceptionally()+" canc:"+cf.isCancelled());
                cf.cancel(true);
            }
        }, duration.toNanos(), TimeUnit.NANOSECONDS);


    }





}
