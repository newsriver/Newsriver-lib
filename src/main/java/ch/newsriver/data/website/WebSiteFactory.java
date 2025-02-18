package ch.newsriver.data.website;

import ch.newsriver.dao.ElasticsearchUtil;
import ch.newsriver.dao.RedisPoolUtil;
import ch.newsriver.data.website.source.BaseSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.search.MatchQuery;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * Created by eliapalme on 03/04/16.
 */
public class WebSiteFactory {

    private final static int SOURCE_TO_FETCH = 100; //fetch 100 sources at a time
    private final static int SOURCE_TO_FETCH_X = 10;  //fecth from Elastic X times more source and than randobly chose up to SOURCE_TO_FETCH. //IF there are too many conflicts increase this.
    private final static String REDIS_KEY_PREFIX = "visSource";
    private final static String REDIS_KEY_VERSION = "4";
    private final static Long GRACETIME_MILLISECONDS = 60l * 5l * 1000l; //about 5 min
    private final static int TROTTLE_TIMOUT = 2; //two seconds;
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final Logger logger = LogManager.getLogger(WebSiteFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String visitedScript = "local current\n" +
            "current = redis.call(\"incr\",KEYS[1])\n" +
            "if tonumber(current) == 1 then\n" +
            "    redis.call(\"expire\",KEYS[1]," + TROTTLE_TIMOUT + ")\n" +
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


    public String addWebsite(WebSite webSite ) {

        Client client = ElasticsearchUtil.getInstance().getClient();
        try {

            IndexResponse response = client.prepareIndex("newsriver-website", "website", webSite.getHostName().toLowerCase().trim())
                    .setSource(mapper.writeValueAsString(webSite))
                    .setOpType(DocWriteRequest.OpType.CREATE)
                    .get();

            if(response.getResult() != DocWriteResponse.Result.CREATED ){
                return null;
            }else{
                return response.getId();
            }

        } catch (IOException e) {
            logger.fatal("Unable to serialize website", e);
            return null;
        } catch (Exception e) {
            logger.error("Unable to update website", e);
            return null;
        }

    }

    public WebSite getWebsite(String host) {
        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
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

    public List<WebSite> searchWebsitesWithName(String name, Long ownerId) {

        return searchWebsitesWithQuery(name,ownerId, 20, "domainName", "hostName", "name");
    }

    public List<WebSite> searchWebsitesWithQuery(String query) {
        return searchWebsitesWithQuery(query,null, -1, null);
    }

    public List<WebSite> searchWebsitesWithQuery(String query,Long ownerId, int limit, String... fields) {
        Client client;
        client = ElasticsearchUtil.getInstance().getClient();
        LinkedList<WebSite> websites = new LinkedList<>();

        try {
            QueryBuilder qb;

            //I don't like this, ideally we should have a queryString that is equivalent to a searchPrasePrefix
            if (fields == null) {
                qb = QueryBuilders.queryStringQuery(query);
            } else {
                qb = QueryBuilders.multiMatchQuery(query, fields).type(MatchQuery.Type.PHRASE_PREFIX).maxExpansions(200);
            }

            if(ownerId!=null){
                //start quering by name if at least one char is provided chars are provided
                if(query.length() > 0){
                    qb = QueryBuilders.boolQuery().must(qb).filter(termQuery("ownerId", ownerId));
                }else{
                    qb = termQuery("ownerId", ownerId);
                }
            }

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-website")
                    .setTypes("website")
                    .setQuery(qb);


            if (limit > 0) {
                searchRequestBuilder = searchRequestBuilder.setSize(limit);
            }

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

        initializeSources(webSite);

        Client client = ElasticsearchUtil.getInstance().getClient();
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

    public HashMap<String, BaseSource> nextWebsiteSourcesToVisits() {
        // example with query return nextWebsiteSourcesToVisits("\"corriere del ticino\"");
        return nextWebsiteSourcesToVisits(null);
    }


    private void initializeSources(WebSite webSite) {

        for (BaseSource source : webSite.getSources()) {
            if (source.getLastVisit() == null) {
                source.setLastVisit(dateFormatter.format(new Date()));
            }
            if (source.getHttpStatus() == null) {
                source.setHttpStatus("200");
            }
        }
    }


    /*
    The search next website to visit is based on the following query
    Note that nested filtering is required in order to sort for the lowest visited source date.

    GET /newsriver-website/_search/
        {
        "query": {

            "bool": {
                "must": {
                    "term": {
                        "satus": "active"
                    }
                },
                "must": {
                    "nested": {
                        "path": "sources",
                        "score_mode": "min",
                        "query": {
                            "range": {
                                "sources.lastVisit": {
                                    "lte": "now"
                                }
                            }
                        }
                    }
                }
            }
        },

        "sort": {
            "sources.lastVisit": {
                "order": "asc",
                "nested_path": "sources",
                "nested_filter": {
                    "range": {
                        "sources.lastVisit": {
                            "lte": "now"
                        }
                    }
                }
            }
        }
    }
     */
    public HashMap<String, BaseSource> nextWebsiteSourcesToVisits(String query) {

        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
        HashMap<String, BaseSource> selectedSources = new HashMap<>();
        try {


            NestedQueryBuilder qnt = QueryBuilders.nestedQuery("sources", QueryBuilders.rangeQuery("sources.lastVisit").lt(new Date().getTime() - GRACETIME_MILLISECONDS), ScoreMode.Min);
            qnt.innerHit(new InnerHitBuilder().setName("source"));

            BoolQueryBuilder qb = null;
            qb = QueryBuilders.boolQuery().must(termQuery("status", "active")).must(qnt);

            if (query != null) {
                qb.must(QueryBuilders.queryStringQuery(query));
            }


            FieldSortBuilder sort = SortBuilders.fieldSort("sources.lastVisit")
                    .setNestedPath("sources")
                    .setNestedFilter(QueryBuilders.rangeQuery("sources.lastVisit").lt(new Date().getTime() - GRACETIME_MILLISECONDS))
                    .order(SortOrder.ASC);

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver-website")
                    .setTypes("website")
                    .addSort(sort)
                    .setSize(SOURCE_TO_FETCH * SOURCE_TO_FETCH_X)
                    .addStoredField("_id")
                    .setFetchSource(false) //TODO: this can may be removed.
                    .setQuery(qb);


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


    private String getKey(String websiteHost) {
        StringBuilder builder = new StringBuilder();
        return builder.append(REDIS_KEY_PREFIX).append(":")
                .append(REDIS_KEY_VERSION).append(":")
                .append(websiteHost).toString();
    }


    public boolean setVisited(String websiteHost) {
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.VISITED_URLS)) {
            Long counter = (Long) jedis.eval(visitedScript, 1, getKey(websiteHost));
            return counter == 1;
        }
    }

    //TODO: eventually we could set lastUpdate as the oldest lastVisit among the items
    public long updateSourceLastVisit(String hostname, BaseSource source) {

        String lastVisit = dateFormatter.format(new Date());
        //This script iterates trough all sources of a website and
        //updates the lastVisit field of the specific source.

        Map<String, Object> params = new HashMap<>();
        params.put("lastVisit", lastVisit);
        params.put("url", source.getUrl());


        final String updateScritp = "ctx._source['lastUpdate']=lastVisit \n " +
                "for (item in ctx._source.sources) {" +
                "   if (item['url'] == url) { " +
                "      item['lastVisit'] =  lastVisit " +
                "   }" +
                "}";


        Client client = ElasticsearchUtil.getInstance().getClient();
        try {

            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index("newsriver-website")
                    .type("website")
                    .id(hostname)
                    .script(new Script(ScriptType.INLINE, "groovy", updateScritp, params))
                    .retryOnConflict(3);

            return client.update(updateRequest).get().getVersion();

        } catch (ExecutionException e) {
            logger.error("Unable to update website source", e);
        } catch (InterruptedException e) {
            logger.error("Unable to update website source", e);
        }
        return -1;
    }
}
