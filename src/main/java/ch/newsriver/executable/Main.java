package ch.newsriver.executable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by eliapalme on 11/03/16.
 */
public abstract class Main {

    private static final int DEFAUTL_PORT = 9098;

    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final SimpleDateFormat fmt = new SimpleDateFormat("'Current time: ' yyyy-MM-dd HH:mm:ssZ");
    protected static HashMap<Metric, SortedMap<Long, Long>> metrics = new HashMap<Metric, SortedMap<Long, Long>>();
    protected static HashMap<Metric, Check> checks = new HashMap<Metric, Check>();

    static Console webConsole;
    private static Main instance;
    private CommandLine cmd = null;
    private String pidFile;
    private String instanceName = "stand-alone";
    private int port;


    public Main(String[] args, boolean runConsole) {

        this(args, null, runConsole);
    }

    public Main(String[] args, List<Option> addOptions, boolean runConsole) {

        Options options = new Options();
        options.addOption("f", "pidfile", true, "pid file location");
        options.addOption("n", "name", true, "instance name");
        options.addOption(Option.builder("p").longOpt("port").hasArg().type(Number.class).desc("port number").build());

        if (addOptions != null) {
            addOptions.stream().forEach(options1 -> options.addOption(options1));
        }

        instance = this;

        CommandLineParser parser = new DefaultParser();

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("options:", options);
            return;
        }


        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Shutting down!");
                if (webConsole != null) webConsole.stop();
                instance.shutdown();
            }
        });


        readParameters();

        String pid = getPid();
        try {
            File file = new File(pidFile);
            file.createNewFile();
            FileWriter fstream = new FileWriter(file, false);
            try (BufferedWriter out = new BufferedWriter(fstream)) {
                out.write(pid);
            } catch (Exception e) {
                logger.error("Unable to save process pid to file", e);
            }
        } catch (IOException e) {
            logger.error("Unable to save process pid to file", e);
        }

        System.out.print(getManifest());

        if (runConsole) {
            try {
                webConsole = new Console(port, metrics);
                webConsole.start();
            } catch (IOException ex) {
                logger.fatal("Unable to bind http port:" + port + "\n" + ex.getLocalizedMessage());
                return;
            }
        }

        instance.start();

    }

    public static Main getInstance() {
        return instance;
    }

    private static String getPid() {

        String processName = ManagementFactory.getRuntimeMXBean().getName();
        if (processName.indexOf("@") > -1) {
            return processName.split("@")[0];
        }
        return null;
    }

    private static String getVersion() {
        InputStream inputStream = null;
        try {
            Properties prop = new Properties();
            String propFileName = "version.properties";
            inputStream = Main.class.getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            return prop.getProperty("version");

        } catch (Exception e) {
            logger.error("Unable to read current version number", e);
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
            }
        }
        return null;
    }

    private static String getWelcome() {
        try {
            InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("welcome.txt");
            return IOUtils.toString(inputStream, "utf-8");
        } catch (Exception ex) {
            return "";
        }
    }

    private static String getHostName() {

        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            hostname = "Unknown Host";
            logger.error("Unable to retrieve host name", ex);
        }
        return hostname;
    }

    public static String getManifest() {

        StringBuilder result = new StringBuilder();
        result.append(getWelcome()).append("\n");
        result.append("Version: ").append(getVersion()).append("\n");
        result.append("Hostname: ").append(getHostName()).append("\n");
        result.append("Instance Name: ").append(instance.getInstanceName()).append("\n");
        result.append("PORT: ").append(instance.getPort()).append("\n");
        result.append("PID: ").append(getPid()).append("\n");
        result.append("Max Heap: ").append(new Float((float) Runtime.getRuntime().maxMemory() / 1048576f).intValue()).append(" MB\n");
        result.append(fmt.format(new Date())).append("\n");

        return result.toString();
    }


    public static synchronized void addMetric(String metricName, int count) {
        addMetric(metricName, count, ChronoUnit.SECONDS);
    }

    public static synchronized void addMetric(String metricName, int count, ChronoUnit timeUnit) {

        Metric metric = new Metric(metricName, timeUnit);
        long unit = 0;
        if (timeUnit == ChronoUnit.SECONDS) {
            unit = Duration.ofNanos(System.nanoTime()).getSeconds();
        } else if (timeUnit == ChronoUnit.MINUTES) {
            unit = Duration.ofNanos(System.nanoTime()).toMinutes();
        }


        SortedMap<Long, Long> units = metrics.get(metric);
        if (units == null) {
            units = new TreeMap<Long, Long>();
        }

        units.put(unit, units.getOrDefault(unit, 0l) + count);
        metrics.put(metric, units.tailMap(unit - 60));


    }

    public static synchronized void addCheck(String metricName, ChronoUnit timeUnit, int expcted, long toCompare) {
        Check check = new Check();
        check.setExpected(expcted);
        check.setToComapre(toCompare);
        Metric metric = new Metric(metricName, timeUnit);
        checks.put(metric, check);
    }

    public static HashMap<Metric, Check> getChecks() {
        return checks;
    }

    public abstract void shutdown();

    public abstract void start();

    public int getDefaultPort() {
        return DEFAUTL_PORT;
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    protected int getPort() {
        return this.port;
    }

    public CommandLine getCmd() {
        return this.cmd;
    }

    protected void readParameters() {

        //GET PID
        BufferedWriter out = null;

        this.pidFile = "/var/run/Newsriver-pid.pid";
        if (cmd.hasOption("pidfile")) {
            this.pidFile = cmd.getOptionValue("pidfile");
        }


        this.port = getDefaultPort();
        try {
            if (cmd.hasOption("p")) {
                this.port = ((Number) cmd.getParsedOptionValue("p")).intValue();
            }

            Map<String, String> env = System.getenv();
            if (env.containsKey("PORT")) {
                this.port = Integer.parseInt(env.get("PORT"));
            }
        } catch (ParseException e) {
            logger.fatal("Unable to parse port number:" + cmd.getOptionValue("p"));
        }


        this.instanceName = "stand-alone";
        try {
            if (cmd.hasOption("name")) {
                this.instanceName = cmd.getOptionValue("name");
            }

            Map<String, String> env = System.getenv();
            if (env.containsKey("MESOS_TASK_ID")) {
                this.instanceName = env.get("MESOS_TASK_ID");
            }
        } catch (Exception e) {
            logger.fatal("Unable to retreive instanceId", e);
        }

    }


    public static class Check {
        private int expected;
        private Long toComapre;

        public int getExpected() {
            return expected;
        }

        public void setExpected(int expected) {
            this.expected = expected;
        }

        public Long getToComapre() {
            return toComapre;
        }

        public void setToComapre(Long toComapre) {
            this.toComapre = toComapre;
        }
    }


    public static class Metric {

        private String name;
        private ChronoUnit unit;

        public Metric(String name, ChronoUnit unit) {
            this.name = name;
            this.unit = unit;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ChronoUnit getUnit() {
            return unit;
        }

        public void setUnit(ChronoUnit unit) {
            this.unit = unit;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 31).
                    append(name).
                    append(unit).
                    toHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Metric) {
                final Metric other = (Metric) obj;
                return new EqualsBuilder()
                        .append(name, other.name)
                        .append(unit, other.unit)
                        .isEquals();
            } else {
                return false;
            }
        }
    }

}
