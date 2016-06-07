package ch.newsriver.executable;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

/**
 * Created by eliapalme on 02/06/16.
 */
public abstract class StreamExecutor extends Main {

    private static final Logger logger = LogManager.getLogger(StreamExecutor.class);


    private static final int DEFAUTL_POOL_SIZE = 5;


    private static final LinkedList<Option> additionalOptions = new LinkedList<>();

    private int poolSize;


    public int getPoolSize() {
        return poolSize;
    }


    static {
        additionalOptions.add(Option.builder("t").longOpt("threads").hasArg().type(Number.class).desc("Number of executor threads").build());
    }

    public StreamExecutor(String[] args, boolean runConsole) {
        super(args, additionalOptions, runConsole);

    }


    @Override
    protected void readParameters() {

        super.readParameters();
        this.poolSize = DEFAUTL_POOL_SIZE;
        try {
            if (this.getCmd().hasOption("t")) {
                this.poolSize = ((Number) this.getCmd().getParsedOptionValue("t")).intValue();
            }
        } catch (ParseException e) {
            logger.fatal("Unable to parse thread pool size:" + this.getCmd().getOptionValue("t"));
            return;
        }

    }
}
