package ch.newsriver.executable.poolExecution;

import java.time.Duration;
import java.util.concurrent.Semaphore;

/**
 * Created by eliapalme on 15/03/16.
 */
public class BatchInterruptibleWithinExecutorPool extends InterruptibleWithinExecutorPool {


    private Semaphore availableSlots;
    private int maxQueueSize;
    protected BatchInterruptibleWithinExecutorPool(int poolSize, int maxQueueSize,Duration duration){
        super(poolSize,duration);
        this.maxQueueSize = maxQueueSize;
        availableSlots = new Semaphore(0);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        availableSlots.release();
    }

    public synchronized  void waitFreeBatchExecutors(int batchSize)throws InterruptedException,BatchSizeException{
        if(batchSize > this.maxQueueSize){
            throw new BatchSizeException();
        }

        if(this.getQueue().size() > maxQueueSize) {
            availableSlots.acquire((this.getQueue().size()+batchSize) -  maxQueueSize);
        }
    }

    public  static class BatchSizeException extends Exception {

    }

}
