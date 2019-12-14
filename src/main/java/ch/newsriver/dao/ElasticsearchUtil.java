package ch.newsriver.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;

/**
 * Created by eliapalme on 18/03/16.
 */
public class ElasticsearchUtil {

    private static final Logger logger = LogManager.getLogger(ElasticsearchUtil.class);
    private static ElasticsearchUtil instance = null;
    private static TransportClient client = null;
    private static String clusterName = "";
    private boolean connected;

    private ElasticsearchUtil() {

        connected = false;
    }


    public static synchronized ElasticsearchUtil getInstance() {

        if (instance == null) {
            instance = new ElasticsearchUtil();
            instance.connect();
        }

        return instance;
    }

    public static boolean hasBeenInstanced() {

        return instance != null;
    }


    private void connect() {


        //check if pool is not already been connected
        if (client != null) {
            return;
        }

        Properties elasitcserachConfig = new Properties();
        InputStream propertiesReaderElastic = this.getClass().getResourceAsStream("/elasticserach.properties");


        try {
            elasitcserachConfig.load(propertiesReaderElastic);
            clusterName = elasitcserachConfig.getProperty("cluster.name");
            Settings settings = Settings.builder().put("cluster.name", clusterName)
                    .put("client.transport.sniff", false)
                    .put("client.transport.ping_timeout", "5s")

                    //Due to a bug in the Elastic search client we have to manualy limit the threadpool size and set the processors
                    //http://elasticsearch-users.115913.n3.nabble.com/TransportClient-behavior-when-the-server-node-is-not-availble-td4045466.html

                    //Looks like it has now been fixed: https://github.com/elastic/elasticsearch/issues/5151
                    //I keep the code here in case we still experience VM crashing because out fo therads all taken by elastic client
                    /*
                                                                   .put("threadpool.search.size","10")
                                                                   .put("threadpool.search.type","fixed")
                                                                   .put("threadpool.precolate.size","10")
                                                                   .put("threadpool.precolate.type","fixed")
                                                                   .put("threadpool.get.size","5")
                                                                   .put("threadpool.get.type","fixed")
                                                                   .put("threadpool.refresh.size","5")
                                                                   .put("threadpool.refresh.type","fixed")
                                                                   .put("threadpool.warmer.size","5")
                                                                   .put("threadpool.warmer.type","fixed")
                                                                   .put("processors","1")
                                                                   */
                    .build();


            client = new PreBuiltTransportClient(settings);

            if (elasitcserachConfig.getProperty("cluster.hostname") != null) {
                for (InetAddress addr : InetAddress.getAllByName(elasitcserachConfig.getProperty("cluster.hostname"))) {
                    client.addTransportAddress(new TransportAddress(addr, Integer.parseInt(elasitcserachConfig.getProperty("cluster.port"))));
                }
            } else {
                if (elasitcserachConfig.getProperty("cluster.address.1") != null) {
                    client.addTransportAddress(new TransportAddress(InetAddress.getByName(elasitcserachConfig.getProperty("cluster.address.1")), Integer.parseInt(elasitcserachConfig.getProperty("cluster.port"))));
                }
                if (elasitcserachConfig.getProperty("cluster.address.2") != null) {
                    client.addTransportAddress(new TransportAddress(InetAddress.getByName(elasitcserachConfig.getProperty("cluster.address.2")), Integer.parseInt(elasitcserachConfig.getProperty("cluster.port"))));
                }
                if (elasitcserachConfig.getProperty("cluster.address.3") != null) {
                    client.addTransportAddress(new TransportAddress(InetAddress.getByName(elasitcserachConfig.getProperty("cluster.address.3")), Integer.parseInt(elasitcserachConfig.getProperty("cluster.port"))));
                }
            }


        } catch (IOException ex) {
            logger.fatal("Unable to read elasticsearch connection pool properties", ex);
        } catch (Exception ex) {
            logger.fatal("Unable to create elasticsearch client", ex);
        } finally {
            try {
                propertiesReaderElastic.close();
            } catch (IOException ex) {
                logger.fatal("Unable to close properties", ex);
            }
        }

        connected = true;

    }


    public void release() {
        connected = false;
        if (client != null) {
            client.close();
        }
        instance = null;
    }


    public Client getClient() {
        if (!connected) {
            logger.fatal("ElasticsearchConnectionFactory need to be connected before asking client!");
        }
        if (client == null) {
            logger.fatal("ElasticsearchConnectionFactory client is null!");
        }
        return client;
    }


    public ClusterHealthStatus checkHealth() {

        final ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest(clusterName).timeout(TimeValue.timeValueSeconds(60)).waitForYellowStatus();
        ClusterHealthResponse clusterHealth = getClient().admin().cluster().health(clusterHealthRequest).actionGet();
        return clusterHealth.getStatus();
    }


}
