package ch.newsriver.processor;

import ch.newsriver.executable.poolExecution.BatchInterruptibleWithinExecutorPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * Created by eliapalme on 04/05/16.
 */
public abstract class Processor<I,O> extends BatchInterruptibleWithinExecutorPool {

    private boolean priority;
    private static final Logger logger = LogManager.getLogger(Processor.class);

    protected Processor(int poolSize, int maxQueueSize,Duration duration,boolean priority){
        super(poolSize,maxQueueSize,duration);
        this.priority=priority;

    }


    public Output<I,O> process (String data){
        Output<I,O> outputFailOver = new  Output<I,O>();
        outputFailOver.setSuccess(false);
        try{
            Output<I,O> output = implProcess(data);
            if(output!=null){
                return output;
            }else{
                return outputFailOver;
            }

        }catch (Exception e){
            logger.error("Error while processing the task", e);
            return outputFailOver;
        }
    }


    protected abstract Output<I,O> implProcess(String data);

    public boolean isPriority() {
        return priority;
    }


}
