package ch.newsriver.test.executable;

import ch.newsriver.executable.poolExecution.InterruptibleWithinExecutorPool;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * Created by eliapalme on 14/03/16.
 */
public class TestExecutionInterruption{




    @Test
    public void runSingleTask() {

        InterruptibleWithinExecutorPool pool = new TestExecutionInterruptionPool(1,Duration.ofMillis(100));
        CompletableFuture<String> feature = new CompletableFuture();

        InterruptibleWithinExecutorPool.supplyAsyncInterruptExecutionWithin(() -> {
            return "ok";
        }, pool)
                .thenAcceptAsync(string -> {
                    assertEquals(string, "ok");
                }, pool)
                .exceptionally(throwable -> {
                    fail("Unrecoverable error");
                    return null;
                });

    }



    @Test
    public void doNotKillQueuedTasks() throws ExecutionException,InterruptedException {


        int milliSleep = 100;
        int milliSleepMargin = 10;

        //This pool has 1 thread and waits 1 seconds before killing
        InterruptibleWithinExecutorPool pool = new TestExecutionInterruptionPool(1,Duration.ofMillis(milliSleep));

        Semaphore semaphore = new Semaphore(0);


        int taskNum = 2;
        for(int i=0;i<taskNum;i++) {
            CompletableFuture feature = InterruptibleWithinExecutorPool.supplyAsyncInterruptExecutionWithin(() -> {
                try {
                    Thread.sleep(milliSleep-milliSleepMargin);
                    semaphore.release();
                } catch (InterruptedException e) {
                    assertNull(e);
                }
                return null;
            }, pool);
        }
        assertTrue(semaphore.tryAcquire(taskNum,taskNum*milliSleep,TimeUnit.MILLISECONDS));

    }

    @Test
    public void killQueuedTasks() throws ExecutionException,InterruptedException {


        int milliSleep = 100;
        int milliSleepMargin = 10;

        //This pool has 1 thread and waits 1 seconds before killing
        InterruptibleWithinExecutorPool pool = new TestExecutionInterruptionPool(1,Duration.ofMillis(milliSleep));

        Semaphore semaphore = new Semaphore(0);
        int taskNum = 2;
        for(int i=0;i<taskNum;i++) {
            CompletableFuture feature = InterruptibleWithinExecutorPool.supplyAsyncInterruptExecutionWithin(() -> {
                try {
                    Thread.sleep(milliSleep + milliSleepMargin);
                } catch (InterruptedException e) {
                    semaphore.release();
                }
                return null;
            }, pool);
        }

        assertTrue(semaphore.tryAcquire(taskNum,taskNum*(milliSleep+milliSleepMargin),TimeUnit.MILLISECONDS));


    }


    public static  class  TestExecutionInterruptionPool extends InterruptibleWithinExecutorPool{
        public TestExecutionInterruptionPool(int threads, Duration duration){
            super(threads,duration);

        }

    }

}
