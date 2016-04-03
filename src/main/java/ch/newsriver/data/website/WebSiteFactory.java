package ch.newsriver.data.website;

import ch.newsriver.dao.ElasticsearchPoolUtil;
import ch.newsriver.data.publisher.Publisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import java.io.IOException;

/**
 * Created by eliapalme on 03/04/16.
 */
public class WebSiteFactory {

    private static final Logger logger = LogManager.getLogger(WebSiteFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static  WebSiteFactory instance;

    private WebSiteFactory(){

    }

    static  public synchronized  WebSiteFactory getInstance(){

        if(instance == null){
            instance = new WebSiteFactory();
        }
        return instance;
    }



    public WebSite getWebsite(String host){

        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        WebSite webSite = null;
        try {
            GetResponse response = client.prepareGet("newsriver-website", "website", host).execute().actionGet();
            if (response.isExists()) {
                try {
                    webSite = mapper.readValue(response.getSourceAsString(),WebSite.class);
                } catch (IOException e) {
                    logger.fatal("Unable to deserialize website", e);
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Unable to get publisher from elasticsearch", e);
        } finally {
        }

        return webSite;
    }


}
