package ch.newsriver.executable.poolExecution;

import ch.newsriver.executable.Main;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliapalme on 15/04/16.
 */
public abstract class MainWithPoolExecutorOptions extends Main {


    private static final Logger logger = LogManager.getLogger(MainWithPoolExecutorOptions.class);


    private static final int DEFAUTL_BATCH_SIZE = 5;
    private static final int DEFAUTL_POOL_SIZE = 5;
    private static final int DEFAUTL_QUEUE_SIZE = 10;

    private static final LinkedList<Option> additionalOptions = new LinkedList<>();

    static {
        additionalOptions.add(Option.builder("t").longOpt("threads").hasArg().type(Number.class).desc("Number of executor threads").build());
        additionalOptions.add(Option.builder("b").longOpt("batch").hasArg().type(Number.class).desc("Execution batch size").build());
        additionalOptions.add(Option.builder("q").longOpt("queue").hasArg().type(Number.class).desc("Executable task queue size").build());
        additionalOptions.add(Option.builder("pr").longOpt("priority").desc("Consume and Produce in priority topics").build());
    }

    private int poolSize;
    private int batchSize;
    private int queueSize;
    private boolean priority;


    public MainWithPoolExecutorOptions(String[] args, List<Option> addOptions, boolean runConsole) {
        super(args, combine(additionalOptions, addOptions), runConsole);

    }

    public MainWithPoolExecutorOptions(String[] args, boolean runConsole) {
        super(args, additionalOptions, runConsole);

    }

    private static List<Option> combine(List<Option> lista, List<Option> listb) {
        lista.addAll(listb);
        return lista;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public boolean isPriority() {
        return priority;
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
        this.batchSize = DEFAUTL_BATCH_SIZE;
        try {
            if (this.getCmd().hasOption("b")) {
                this.batchSize = ((Number) this.getCmd().getParsedOptionValue("b")).intValue();
            }
        } catch (ParseException e) {
            logger.fatal("Unable to parse batch size:" + this.getCmd().getOptionValue("b"));
            return;
        }
        this.queueSize = DEFAUTL_QUEUE_SIZE;
        try {
            if (this.getCmd().hasOption("q")) {
                this.queueSize = ((Number) this.getCmd().getParsedOptionValue("q")).intValue();
            }
        } catch (ParseException e) {
            logger.fatal("Unable to parse executor queue size:" + this.getCmd().getOptionValue("q"));
            return;
        }
        this.priority = false;

        if (this.getCmd().hasOption("pr")) {
            this.priority = true;
        }

    }

    protected String getCustomOption(String optionName) {

        if (this.getCmd().hasOption(optionName)) {
            return this.getCmd().getOptionValue(optionName);
        }
        return null;
    }


}
