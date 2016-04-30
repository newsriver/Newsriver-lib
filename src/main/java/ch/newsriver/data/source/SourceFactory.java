package ch.newsriver.data.source;

import ch.newsriver.dao.ElasticsearchPoolUtil;
import ch.newsriver.data.content.Article;
import ch.newsriver.data.publisher.Publisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.exec.ExecuteException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.xml.FilterBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.engine.DocumentAlreadyExistsException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by eliapalme on 22/03/16.
 */
public class SourceFactory {


    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LogManager.getLogger(SourceFactory.class);
    private static SourceFactory instance;
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private SourceFactory(){

    }

    static  public synchronized SourceFactory getInstance(){

        if(instance == null){
            instance = new SourceFactory();
        }
        return instance;
    }


    public long updateLastVisit(String id){

        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index("newsriver-source");
            updateRequest.type("source");
            updateRequest.id(id);
            updateRequest.doc(jsonBuilder()
                    .startObject()
                    .field("lastVisit", dateFormatter.format(new Date()))
                    .endObject());

            return client.update(updateRequest).get().getVersion();
        }catch (IOException e ){
            logger.error("Unable to update source", e);
        }catch (ExecutionException e ){
            logger.error("Unable to update source", e);
        }catch (InterruptedException e ){
            logger.error("Unable to update source", e);
        }
        return -1;
    }

    public Set<String> nextToVisits(int count){
        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        HashSet<String> sources = new HashSet<>();
        try {
            //QueryBuilder qb = QueryBuilders.queryStringQuery("httpStatus IS NULL OR httpStatus:200");

            QueryBuilder qb = QueryBuilders.queryStringQuery("*");

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-source")
                    .setTypes("source")
                    .setSize(count*100)
                    .addSort("lastVisit", SortOrder.DESC)
                    .addFields("_id","lastVisit")
                    .setQuery(qb);


            SearchResponse response =  searchRequestBuilder.execute().actionGet();
            LinkedList<String> candidate = new LinkedList<>();
            for(SearchHit i : response.getHits()){

                i.getFields().get("lastVisit");
            }
            response.getHits().forEach(hit -> candidate.add(hit.getId()));
            Collections.shuffle(candidate);

            do {
                String sourceId = candidate.pollFirst();

                //Here check in Redis if this candidate has been recently checked and in case put back

                sources.add(sourceId);
            }while(sources.size() < count && !candidate.isEmpty());



        } catch (Exception e) {
            logger.error("Unable to get articles from elasticsearch", e);
        } finally {
        }

        return sources;
    }


    public  boolean setSource(BaseSource source, boolean updateIfExists){

        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        String urlHash= getURLHash(source.getUrl());
        try {
            IndexRequest indexRequest = new IndexRequest("newsriver-source", "source", urlHash);
            indexRequest.source(mapper.writeValueAsString(source));
            indexRequest.create(!updateIfExists);
            IndexResponse response = client.index(indexRequest).actionGet();
            return (response != null && response.getId() != null && !response.getId().isEmpty());
        } catch (DocumentAlreadyExistsException e){
            logger.warn("Unable to save source, the document already exists");
        } catch (Exception e) {
            logger.error("Unable to save source", e);
        } finally {
        }
        return false;
    }

    public BaseSource getSource(String id){

        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        BaseSource source = null;
        try {
            GetResponse response = client.prepareGet("newsriver-source", "source", id).execute().actionGet();
            if (response.isExists()) {
                try {
                    source = mapper.readValue(response.getSourceAsString(),BaseSource.class);
                } catch (IOException e) {
                    logger.fatal("Unable to deserialize publisher", e);
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Unable to get publisher from elasticsearch", e);
        } finally {
        }

        return source;
    }

    private String getURLHash(String url){

        String urlHash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            urlHash = Base64.encodeBase64URLSafeString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Unable to compute URL hash", e);
            return null;
        }
        return  urlHash;

    }







}
