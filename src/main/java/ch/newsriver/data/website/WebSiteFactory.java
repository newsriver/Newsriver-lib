package ch.newsriver.data.website;

import ch.newsriver.dao.ElasticsearchPoolUtil;
import ch.newsriver.dao.RedisPoolUtil;
import ch.newsriver.data.website.source.BaseSource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.support.QueryInnerHitBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by eliapalme on 03/04/16.
 */
public class WebSiteFactory {

    private final static int SOURCE_TO_FETCH = 100; //fetch 100 sources at a time
    private final static int SOURCE_TO_FETCH_X = 10;  //fecth from Elastic X times more source and than randobly chose up to SOURCE_TO_FETCH. //IF there are too many conflicts increase this.
    private final static String REDIS_KEY_PREFIX = "visSource";
    private final static String REDIS_KEY_VERSION = "4";
    private final static int GRACETIME_SECONDS = 60 * 5; //about 5 min
    private final static Long GRACETIME_MILLISECONDS = ((long) GRACETIME_SECONDS) * 1000l;
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final Logger logger = LogManager.getLogger(WebSiteFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String visitedScript = "local current\n" +
            "current = redis.call(\"incr\",KEYS[1])\n" +
            "if tonumber(current) == 1 then\n" +
            "    redis.call(\"expire\",KEYS[1]," + GRACETIME_SECONDS + ")\n" +
            "end\n" +
            "return current";
    private static WebSiteFactory instance;


    private WebSiteFactory() {

    }

    static public synchronized WebSiteFactory getInstance() {

        if (instance == null) {
            instance = new WebSiteFactory();
        }
        return instance;
    }

    public WebSite getWebsite(String host) {
        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        WebSite webSite = null;
        try {
            GetResponse response = client.prepareGet("newsriver-website", "website", host.toLowerCase().trim()).execute().actionGet();
            if (response.isExists()) {
                try {
                    webSite = mapper.readValue(response.getSourceAsString(), WebSite.class);
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

    public List<WebSite> getWebsitesWithFeed(String feedURL) {
        return searchWebsitesWithQuery("feeds:\"" + feedURL + "\"");
    }

    public List<WebSite> getWebsitesWithDomain(String domain) {
        return searchWebsitesWithQuery("domainName:\"" + domain + "\"");
    }

    private List<WebSite> searchWebsitesWithQuery(String query) {
        Client client;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        LinkedList<WebSite> websites = new LinkedList<>();

        try {
            QueryBuilder qb = QueryBuilders.queryStringQuery(query);

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-website")
                    .setTypes("website")
                    .setQuery(qb);

            SearchResponse response = searchRequestBuilder.execute().actionGet();

            for (SearchHit hit : response.getHits()) {
                websites.add(mapper.readValue(hit.getSourceAsString(), WebSite.class));
            }

        } catch (Exception e) {
            logger.error("Unable to get website from elasticsearch", e);
            return null;
        } finally {
        }
        return websites;
    }

    public boolean updateWebsite(WebSite webSite) {

        Client client = ElasticsearchPoolUtil.getInstance().getClient();
        try {

            IndexRequest indexRequest = new IndexRequest("newsriver-website", "website", webSite.getHostName().toLowerCase().trim());
            indexRequest.source(mapper.writeValueAsString(webSite));
            return client.index(indexRequest).actionGet().getId() != null;


        } catch (IOException e) {
            logger.fatal("Unable to serialize website", e);
            return false;
        } catch (Exception e) {
            logger.error("Unable to update website", e);
            return false;
        }
    }


    //TODO: avoid using this methos as it currently couses ES to go crazy
    //The issue is due to the setSourceVisited method that run update scripts on ES
    public HashMap<String, BaseSource> nextWebsiteSourcesToVisits() {

        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        HashMap<String, BaseSource> selectedSources = new HashMap<>();
        try {


            QueryBuilder qb = QueryBuilders.nestedQuery("sources", QueryBuilders.rangeQuery("sources.lastVisit").lt(new Date().getTime() - GRACETIME_MILLISECONDS)).innerHit(new QueryInnerHitBuilder().setName("source"));
            ;

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-website")
                    .setTypes("website")
                    .setSize(SOURCE_TO_FETCH * SOURCE_TO_FETCH_X)
                    .addSort("sources.lastVisit", SortOrder.ASC)
                    .addFields("_id")
                    .setQuery(qb);

            //.setPostFilter(QueryBuilders.nestedQuery("sources", QueryBuilders.existsQuery("lastVisit")));
            //.setPostFilter(QueryBuilders.rangeQuery("lastVisit").lt(new Date().getTime() - GRACETIME_MILLISECONDS));


            SearchResponse response = searchRequestBuilder.execute().actionGet();
            HashMap<String, BaseSource> candidates = new HashMap<>();
            response.getHits().forEach(hit -> {
                LinkedList<BaseSource> sources = new LinkedList<>();
                hit.getInnerHits().get("source").forEach(innerHit -> {
                    try {
                        BaseSource source = mapper.readValue(innerHit.getSourceAsString(), BaseSource.class);
                        sources.add(source);
                    } catch (Exception e) {
                        logger.error("Unable to deserialise source", e);
                    }
                });
                if (sources.isEmpty()) return;
                //Pick one random source among the available
                Collections.shuffle(sources);
                candidates.put(hit.getId(), sources.getFirst());
            });


            if (!candidates.isEmpty()) {
                LinkedList<String> ids = new LinkedList(candidates.keySet());
                Collections.shuffle(ids);
                do {
                    String sourceId = ids.pollFirst();

                    if (!setVisited(sourceId)) {
                        logger.warn("Conflict found, the source was recently visited id:" + sourceId);
                        continue;
                    }
                    selectedSources.put(sourceId, candidates.get(sourceId));
                } while (selectedSources.size() < SOURCE_TO_FETCH && !ids.isEmpty());
            }


        } catch (Exception e) {
            logger.error("Unable to get articles from elasticsearch", e);
        } finally {
        }

        return selectedSources;
    }


    //TODO: we should find a better soltution to this method something like: nextWebsiteSourcesToVisits that is not get the next sources to crawl and not
    //the next website. This bacause getting the next website means making a lot of requests to the same domain.
    public List<BaseSource> nextWebsiteToVisits() {

        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        List<BaseSource> sources = new LinkedList<>();
        try {

            QueryBuilder qb = QueryBuilders.queryStringQuery("*");

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-website")
                    .setTypes("website")
                    .setSize(SOURCE_TO_FETCH * SOURCE_TO_FETCH_X)
                    .addSort("lastVisit", SortOrder.ASC)
                    .addFields("_id", "lastVisit", "sources")
                    .setQuery(qb)
                    .setPostFilter(QueryBuilders.rangeQuery("lastVisit").lt(new Date().getTime() - GRACETIME_MILLISECONDS));


            SearchResponse response = searchRequestBuilder.execute().actionGet();
            Map<String, List<BaseSource>> candidate = new HashMap<>();
            response.getHits().forEach(hit -> {

                List<BaseSource> webStieSources;
                try {
                    webStieSources = mapper.readValue((String) hit.getSource().get("sources"), new TypeReference<LinkedList<BaseSource>>() {
                    });
                    candidate.put(hit.getId(), webStieSources);
                } catch (Exception e) {
                    logger.error("Unable to deserialise source", e);
                }

            });

            LinkedList<String> websites = new LinkedList<>(candidate.keySet());
            Collections.shuffle(websites);

            if (!candidate.isEmpty()) {
                do {
                    String sourceId = websites.pollFirst();

                    if (!setVisited(sourceId)) {
                        logger.warn("Conflict found, the source was recently visited id:" + sourceId);
                        continue;
                    }
                    sources.addAll(candidate.get(sourceId));
                } while (sources.size() < SOURCE_TO_FETCH && !candidate.isEmpty());
            }


        } catch (Exception e) {
            logger.error("Unable to get articles from elasticsearch", e);
        } finally {
        }
        //shuffle the sources to avoid crawling several sources of a same domain in a row
        Collections.shuffle(sources);
        return sources;
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

    //TODO: eventually we could set lastUpdate as the oldest lastVisit among the items
    public long updateSourceLastVisit(String hostname, BaseSource source) {

        String lastVisit = dateFormatter.format(new Date());
        //This script iterates trough all sources of a website and
        //updates the lastVisit field of the specific source.
        Script updateSource = new Script("ctx._source['lastUpdate']='" + lastVisit + "' \n " +
                "for (item in ctx._source.sources) {" +
                "if (item['url'] == '" + source.getUrl() + "') { " +
                "item['lastVisit'] = '" + lastVisit +
                "'}" +
                "}");


        Client client = ElasticsearchPoolUtil.getInstance().getClient();
        try {

            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index("newsriver-website")
                    .type("website")
                    .id(hostname)
                    .script(updateSource)
                    .retryOnConflict(0);


            return client.update(updateRequest).get().getVersion();

        } catch (ExecutionException e) {
            logger.error("Unable to update website source", e);
        } catch (InterruptedException e) {
            logger.error("Unable to update website source", e);
        }
        return -1;
    }
}
