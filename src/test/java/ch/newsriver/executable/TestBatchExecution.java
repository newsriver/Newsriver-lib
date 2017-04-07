package ch.newsriver.executable;

import ch.newsriver.executable.poolExecution.BatchInterruptibleWithinExecutorPool;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by eliapalme on 15/03/16.
 */
public class TestBatchExecution {





    /*
    We do test the betch exector as following:

    waitFreeBatchExecutors will block if the queue is bigger than max queue size and will release once the requested number of executors have completed.

    Take a pool of 1 thread and a queue of 10
    This will allow to get 10 tasks to execute in raw and each will release a semaphore. Task till need 1 sec to execute.
    Than we ask from another batch of 10, this should not free before all 10 taks have completed.
    Immediatly after the bactch we try to acqire a semafore of 10 with no wait time, if it fails it means the waitForBatch has filed.
    * */


    @Test
    public void runSingleTask() throws InterruptedException, BatchInterruptibleWithinExecutorPool.BatchSizeException {

        int sleep = 100;
        int sleepMargin = 10;
        int queue = 10;
        int poolSize = 2;
        int overQueue = 10;
        TestBatchInterruptibleWithinExecutorPool pool = new TestBatchInterruptibleWithinExecutorPool(poolSize, queue, Duration.ofMillis(sleep));


        Semaphore semaphore = new Semaphore(0);
        pool.waitFreeBatchExecutors(queue);

        //we need to schedule numberOfThreads + poolSize to make sure the queue is full so the next time we call waitFreeBatchExecutors
        //it will wait till numberOfThreads have finished to execute, this will also ensure the semaphore as numberOfThreads releases.

        for (int i = 0; i < queue + overQueue; i++) {
            CompletableFuture<String> feature = new CompletableFuture();
            feature = feature.supplyAsync(() -> {
                try {
                    Thread.sleep(sleep - sleepMargin);
                } catch (InterruptedException e) {
                    assertNull(e);
                }
                semaphore.release();
                return "ok";
            }, pool);
        }
        //Now we aim to queue another job but since the queue is supposed to be full it will block
        pool.waitFreeBatchExecutors(1);
        //Once the overQueue threads have been consumed the waitFreeBatchExecutors will unlock and we will be able to acquite the semaphore
        //minus poolSize because the queue is unlocked but the threads still need to run
        assertTrue(semaphore.tryAcquire(overQueue-poolSize));

    }

    public static class TestBatchInterruptibleWithinExecutorPool extends BatchInterruptibleWithinExecutorPool {
        public TestBatchInterruptibleWithinExecutorPool(int threads, int batch, Duration duration) {
            super(threads, batch, duration);

        }

    }

}
