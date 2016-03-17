package ch.newsriver.test.executable;

import ch.newsriver.executable.BatchInterruptibleWithinExecutorPool;
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
 * Created by eliapalme on 15/03/16.
 */
public class TestBatchExecution extends BatchInterruptibleWithinExecutorPool {

    public TestBatchExecution(){
        super(5,5);
    }


    @Test
    public void runSingleTask() throws InterruptedException,BatchSizeExcpetion{

        Semaphore semaphore = new Semaphore(0);

        this.waitFreeBatchExecutors(10);
        for(int i=0;i<=5;i++){
            CompletableFuture<String> feature = new CompletableFuture();
            feature = feature.supplyAsync(() -> {

                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                }

                return "ok";
            }, this);
        }
        this.waitFreeBatchExecutors(10);
        for(int i=0;i<=5;i++){
            CompletableFuture<String> feature = new CompletableFuture();
            feature = feature.supplyAsync(() -> {return "ok";}, this);
        }

        //assertTrue(feature.isDone());

    }

}
