package ch.newsriver.executable;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

/**
 * Created by eliapalme on 11/03/16.
 */
public abstract class Main {

    private static final int DEFAUTL_PORT = 9098;

    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final SimpleDateFormat fmt = new SimpleDateFormat("'Current time: ' yyyy-MM-dd HH:mm:ssZ");
    private static int port;

    private static Main instance;
    public abstract void shutdown();
    public abstract void start();

    protected static HashMap<String,SortedMap<Long,Long>> metrics = new HashMap<String,SortedMap<Long,Long>>();

    static Console webConsole;

    public int getDefaultPort(){
        return DEFAUTL_PORT;
    }

    protected static int getPort(){
        return port;
    }

    public Main(String[] args, Options options, boolean runConsole){
        instance = this;


        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse( options, args);
        }
        catch( ParseException exp ) {
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "options:", options );
            return;
        }


        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Shutting down!");
                if(webConsole!=null)webConsole.stop();
                instance.shutdown();
            }
        });

        String pid = getPid();

        //GET PID
        BufferedWriter out = null;
        try {

            String pidFile = "/var/run/Newsriver-pid.pid";
            if(cmd.hasOption("pidfile")){
                pidFile = cmd.getOptionValue("pidfile");
            }

            File file = new File(pidFile);
            file.createNewFile();
            FileWriter fstream = new FileWriter(file, false);
            out = new BufferedWriter(fstream);
            out.write(pid);
        } catch (Exception e) {
            logger.error("Unable to save process pid to file", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {};
        }

        port = getDefaultPort();
        try {
            if (cmd.hasOption("p")) {
                port = ((Number) cmd.getParsedOptionValue("p")).intValue();
            }

            Map<String, String> env = System.getenv();
            if(env.containsKey("PORT")){
                port = Integer.parseInt( env.get("PORT"));
            }


        }catch (ParseException e ){
            logger.fatal("Unable to parse port number:" +cmd.getOptionValue("p"));
            return;
        }

        System.out.print(getManifest());

        if(runConsole) {
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

    private static String getPid() {

        String processName = ManagementFactory.getRuntimeMXBean().getName();
        if(processName.indexOf("@")>-1){
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
            logger.error("Unable to read current version number",e);
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) { }
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

    public static String getManifest(){

        StringBuilder result = new StringBuilder();
        result.append(getWelcome()).append("\n");
        result.append("Version: ").append(getVersion()).append("\n");
        result.append("Hostname: ").append(getHostName()).append("\n");
        result.append("PORT: ").append(port).append("\n");
        result.append("PID: ").append(getPid()).append("\n");
        result.append(fmt.format(new Date())).append("\n");

        return result.toString();
    }



    public static synchronized void addMetric(String metricName, int count){

        long second = Duration.ofNanos(System.nanoTime()).getSeconds();

        SortedMap<Long,Long> units = metrics.get(metricName);
        if(units == null){
            units = new  TreeMap<Long,Long>();
        }

        units.put(second,units.getOrDefault(second,0l)+count);
        metrics.put(metricName,units.tailMap(second-60));


    }

}
