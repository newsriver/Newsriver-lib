package ch.newsriver.data.publisher;

import ch.newsriver.dao.ElasticsearchPoolUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;

/**
 * Created by eliapalme on 22/03/16.
 */
public class PublisherFactory {


    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LogManager.getLogger(PublisherFactory.class);
    private static  PublisherFactory instance;

    private PublisherFactory(){

    }

    static  public synchronized  PublisherFactory getInstance(){

        if(instance == null){
            instance = new PublisherFactory();
        }
        return instance;
    }



    public Publisher getPublisher(String domain){

        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        Publisher publisher = null;
        try {
            GetResponse response = client.prepareGet("newsriver-publisher", "publisher", domain).execute().actionGet();
            if (response.isExists()) {
                try {
                    publisher = mapper.readValue(response.getSourceAsString(),Publisher.class);
                } catch (IOException e) {
                    logger.fatal("Unable to deserialize publisher", e);
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Unable to get publisher from elasticsearch", e);
        } finally {
        }

        return publisher;
    }

}
