package ch.newsriver.data.website.source;

import ch.newsriver.dao.ElasticsearchUtil;
import ch.newsriver.dao.RedisPoolUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.delete.DeleteResponse;
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
import org.elasticsearch.search.sort.SortOrder;
import redis.clients.jedis.Jedis;

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
    private final static int SOURCE_TO_FETCH = 100; //fetch 100 sources at a time
    private final static int SOURCE_TO_FETCH_X = 10;  //fecth from Elastic X times more source and than randobly chose up to SOURCE_TO_FETCH. //IF there are too many conflicts increase this.
    private final static String REDIS_KEY_PREFIX = "visSource";
    private final static String REDIS_KEY_VERSION = "4";
    private final static int GRACETIME_SECONDS = 60 * 5; //about 5 min
    private final static Long GRACETIME_MILLISECONDS = ((long) GRACETIME_SECONDS) * 1000l;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LogManager.getLogger(SourceFactory.class);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static SourceFactory instance;
    private static String visitedScript = "local current\n" +
            "current = redis.call(\"incr\",KEYS[1])\n" +
            "if tonumber(current) == 1 then\n" +
            "    redis.call(\"expire\",KEYS[1]," + GRACETIME_SECONDS + ")\n" +
            "end\n" +
            "return current";

    private SourceFactory() {

    }

    static public synchronized SourceFactory getInstance() {

        if (instance == null) {
            instance = new SourceFactory();
        }
        return instance;
    }

    public long updateLastVisit(String id) {

        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
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
        } catch (IOException e) {
            logger.error("Unable to update source", e);
        } catch (ExecutionException e) {
            logger.error("Unable to update source", e);
        } catch (InterruptedException e) {
            logger.error("Unable to update source", e);
        }
        return -1;
    }

    public Set<String> nextToVisits() {

        //QueryBuilder qb = QueryBuilders.queryStringQuery("httpStatus IS NULL OR httpStatus:200");

        //QueryBuilder qb = QueryBuilders.queryStringQuery("type:URLSeedSource");

        return nextToVisits("*");
    }

    public Set<String> nextToVisits(String query) {

        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
        HashSet<String> sources = new HashSet<>();
        try {

            QueryBuilder qb = QueryBuilders.queryStringQuery(query);

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-source")
                    .setTypes("source")
                    .setSize(SOURCE_TO_FETCH * SOURCE_TO_FETCH_X)
                    .addSort("lastVisit", SortOrder.ASC)
                    .addFields("_id", "lastVisit")
                    .setQuery(qb)
                    .setPostFilter(QueryBuilders.rangeQuery("lastVisit").lt(new Date().getTime() - GRACETIME_MILLISECONDS));


            SearchResponse response = searchRequestBuilder.execute().actionGet();
            LinkedList<String> candidate = new LinkedList<>();
            response.getHits().forEach(hit -> candidate.add(hit.getId()));
            Collections.shuffle(candidate);

            if (!candidate.isEmpty()) {
                do {
                    String sourceId = candidate.pollFirst();

                    if (!setVisited(sourceId)) {
                        logger.warn("Conflict found, the source was recently visited id:" + sourceId);
                        continue;
                    }
                    sources.add(sourceId);
                } while (sources.size() < SOURCE_TO_FETCH && !candidate.isEmpty());
            }


        } catch (Exception e) {
            logger.error("Unable to get articles from elasticsearch", e);
        } finally {
        }

        return sources;
    }

    public boolean setSource(BaseSource source, boolean updateIfExists) {

        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
        String urlHash = getURLHash(source.getUrl());
        try {
            IndexRequest indexRequest = new IndexRequest("newsriver-source", "source", urlHash);
            indexRequest.source(mapper.writeValueAsString(source));
            indexRequest.create(!updateIfExists);
            IndexResponse response = client.index(indexRequest).actionGet();
            return (response != null && response.getId() != null && !response.getId().isEmpty());
        } catch (DocumentAlreadyExistsException e) {
            logger.warn("Unable to save source, the document already exists");
        } catch (Exception e) {
            logger.error("Unable to save source", e);
        } finally {
        }
        return false;
    }

    public boolean removeSource(BaseSource source) {

        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
        String urlHash = getURLHash(source.getUrl());


        try {
            DeleteResponse response = client.prepareDelete("newsriver-source", "source", urlHash)
                    .execute()
                    .actionGet();

            return response.isFound();
        } catch (Exception e) {
            logger.error("Unable to delete source", e);
        }
        return false;
    }

    public BaseSource getSource(String id) {

        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
        BaseSource source = null;
        try {
            GetResponse response = client.prepareGet("newsriver-source", "source", id).execute().actionGet();
            if (response.isExists()) {
                try {
                    source = mapper.readValue(response.getSourceAsString(), BaseSource.class);
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

    private String getURLHash(String url) {

        String urlHash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            urlHash = Base64.encodeBase64URLSafeString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Unable to compute URL hash", e);
            return null;
        }
        return urlHash;

    }

    private String getKey(String id) {
        StringBuilder builder = new StringBuilder();
        return builder.append(REDIS_KEY_PREFIX).append(":")
                .append(REDIS_KEY_VERSION).append(":")
                .append(id).toString();
    }

    public boolean setVisited(String id) {
        boolean newVisit;
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.VISITED_URLS)) {
            Long counter = (Long) jedis.eval(visitedScript, 1, getKey(id));
            return counter == 1;
        }
    }


}
