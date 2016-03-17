package ch.newsriver.test.executable;

import ch.newsriver.data.url.FeedURL;
import ch.newsriver.executable.InterruptibleWithinExecutorPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Created by eliapalme on 14/03/16.
 */
public class TestExecutionInterruption extends InterruptibleWithinExecutorPool {

    public TestExecutionInterruption(){
        super(10);
    }

    /*
    @Test
    public void runSingleTask() {

        CompletableFuture<String> feature = new CompletableFuture();

        feature.supplyAsync(() -> {
            return "ok";
        }, this.getScheduledExecutorPool())
                .thenAcceptAsync(string -> {
                    assertEquals(string, "ok");
                }, this.getScheduledExecutorPool())
                .exceptionally(throwable -> {
                    fail("Unrecoverable error");
                    return null;
                });

    }

    @Test
    public void runMultiTasks() {

        Semaphore semaphore = new Semaphore(0);
        CompletableFuture<String> feature = new CompletableFuture();

        for (int i = 1; i <= this.getMaxQueueSize() + this.getPoolSize(); i++) {
            feature.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    fail("InterruptedException");
                }
                return "ok";
            }, this.getScheduledExecutorPool())
                    .thenAcceptAsync(string -> {
                        assertEquals(string, "ok");
                        semaphore.release();
                    }, this.getScheduledExecutorPool())
                    .exceptionally(throwable -> {
                        assertTrue(false);
                        return null;
                    });

            //Need to sleep to give time to the executor to start running the tasks
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                fail("InterruptedException");
            }

            if (i > this.getPoolSize()) {
                assertTrue(this.getAvailability() == this.getMaxQueueSize() - (i - this.getPoolSize()));
            } else {
                assertTrue(this.getAvailability() == this.getMaxQueueSize());
            }
        }
        assertFalse(this.hasAvailability());
        semaphore.release(50);

        try {
            assertTrue(semaphore.tryAcquire(50, 1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail("InterruptedException");
        }

    }
*/


    @Test
    public void killCompletableFutureTask() throws ExecutionException,InterruptedException {
        Semaphore semaphore = new Semaphore(0);



        CompletableFuture feature = supplyAsyncInterruptWithin(assertInterruption(() -> {
            semaphore.acquire();
            return "not interrupted";
        }), Duration.ofMillis(10), this);

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
        }

        assertTrue(feature.isCompletedExceptionally());
        ExecutionException ex = null;
        try{
            feature.get();
        }catch (ExecutionException e){
            ex = e;
        }
        assertNotNull(ex);
        assertTrue(ex.getCause() instanceof InterruptedException);

    }


    static public <T> Supplier<T> assertInterruption(Callable<T> callable) {
        return () -> {
            boolean wasInterrupted = false;
            try {
                return callable.call();
            } catch (Exception e) {
                wasInterrupted = true;
            }
            assertTrue(wasInterrupted);
            return null;
        };
    }



}
