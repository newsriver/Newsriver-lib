package ch.newsriver.performance;

import ch.newsriver.executable.Main;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Properties;

/**
 * Created by eliapalme on 14/04/16.
 */
public class MetricsLogger {

    private static final Logger logger = LogManager.getLogger(MetricsLogger.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private String className;
    private String instance;
    private DatagramSocket socket;

    private InetAddress hostAddress;
    private int port;

    private MetricsLogger(Class loggerClass, String instanceName) {
        className = loggerClass.getCanonicalName();
        instance = instanceName;
        String propFileName = "metricsLogger.properties";
        try (InputStream inputStream = MetricsLogger.class.getClassLoader().getResourceAsStream(propFileName)) {
            Properties prop = new Properties();
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            hostAddress = InetAddress.getByName(prop.getProperty("hostname"));
            port = Integer.parseInt(prop.getProperty("port"));
        } catch (Exception e) {
            logger.error("Unable to read current version number", e);
        }
        openSocket();
    }

    public static MetricsLogger getLogger(Class loggerClass,String instanceName){
        return new MetricsLogger(loggerClass,instanceName);
    }


    public void logMetric(String name) {
        logMetric(name, 1);
    }

    public void logMetric(String name, int count) {
        try {
            Metric metric = new Metric(name, className, instance, count);
            byte[] message = mapper.writeValueAsString(metric).getBytes("utf-8");
            DatagramPacket out = new DatagramPacket(message, message.length, hostAddress, port);

            socket.send(out);
        } catch (IOException e) {
        }
    }

    private void openSocket() {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
        }
    }


}
