package ch.newsriver.data.content;

import ch.newsriver.dao.ElasticsearchUtil;
import ch.newsriver.data.website.WebSite;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliapalme on 26/04/16.
 */
public class ArticleFactory {


    private static final ObjectMapper mapper;
    private static final Logger logger = LogManager.getLogger(ArticleFactory.class);
    private static ArticleFactory instance;


    //The article is saved into ElasticSearch with a subset of the Website object fields
    //There is no need to replicate the full object.
    static {
        mapper = new ObjectMapper();
        mapper.setConfig(mapper.getSerializationConfig().withView(ElasticSearchJSONView.class));
    }

    //TODO: one day when the old monolitic index Newsriver will nolonger exsit change the following wildcard to newsriver-data-*
    final GetIndexRequest getNewsriverIndexRequest = new GetIndexRequest().indices("newsriver*").features(GetIndexRequest.Feature.ALIASES);

    private ArticleFactory() {

    }

    static public synchronized ArticleFactory getInstance() {

        if (instance == null) {
            instance = new ArticleFactory();
        }
        return instance;
    }

    public List<HighlightedArticle> searchArticles(ArticleRequest searchRequest) {
        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
        return this.searchArticles(searchRequest, client);
    }

    public List<HighlightedArticle> searchArticles(ArticleRequest searchRequest, Client client) {


        LinkedList<HighlightedArticle> articles = new LinkedList<>();
        try {
            QueryBuilder qb = QueryBuilders.queryStringQuery(searchRequest.getQuery());


            HighlightBuilder hilight = new HighlightBuilder().field("title").field("text").preTags("<highlighted>").postTags("</highlighted>").numOfFragments(1).requireFieldMatch(true);

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver*")
                    .setTypes("article")
                    .highlighter(hilight)
                    .setSize(searchRequest.getLimit())
                    .setQuery(qb);

            if (searchRequest.getId() != null) {
                searchRequestBuilder.setPostFilter(QueryBuilders.termQuery("_id", searchRequest.getId()));
            }

            //Order by field and then by score, or only by score if no field is defined
            if (searchRequest.getSortBy() != null && !searchRequest.getSortBy().equalsIgnoreCase("_score")) {
                FieldSortBuilder sortBuilder = SortBuilders.fieldSort(searchRequest.getSortBy()).order(searchRequest.getSortOrder()).sortMode(SortMode.MAX);
                searchRequestBuilder.addSort(sortBuilder);
            }
            searchRequestBuilder.addSort(SortBuilders.scoreSort());

            //if(searchRequest.getFields()!=null && !searchRequest.getFields().isEmpty()){
            //    searchRequestBuilder.addFields(searchRequest.getFields().toArray(new String[0]));
            //}

            SearchResponse response = searchRequestBuilder.execute().actionGet();
            for (SearchHit hit : response.getHits()) {


                try {
                    HighlightedArticle article = mapper.readValue(hit.getSourceAsString(), HighlightedArticle.class);
                    article.setId(hit.getId());
                    article.setScore(hit.getScore());
                    article.setIndexName(hit.getIndex());

                    for (HighlightField filed : hit.getHighlightFields().values()) {

                        if (filed.getFragments() == null || filed.getFragments().length < 1) continue;

                        Text fragmentText = filed.getFragments()[0];


                        if (filed.getName().equalsIgnoreCase("title")) {
                            article.setHighlight(fragmentText.toString());
                        } else if (article.getHighlight() == null) {
                            article.setHighlight(fragmentText.toString());
                        }
                    }

                    articles.add(article);
                } catch (IOException e) {
                    logger.fatal("Unable to deserialize articles", e);
                }
            }

        } catch (Exception e) {
            logger.error("Unable to get articles from elasticsearch", e);
        } finally {
        }

        return articles;
    }

    public IndexResponse saveArticle(Article article, String urlHash, String index) {
        Client client = null;
        client = ElasticsearchUtil.getInstance().getClient();
        return this.saveArticle(article, urlHash, index, client);
    }

    public IndexResponse saveArticle(Article article, String urlHash, String index, Client client) {
        try {
            IndexRequest indexRequest = new IndexRequest(index, "article", urlHash);
            indexRequest.source(mapper.writeValueAsString(article));
            return client.index(indexRequest).actionGet();
        } catch (Exception e) {
            logger.error("Unable to save article in elasticsearch", e);
            return null;
        }
    }

    public boolean updateArticle(Article article) {
        String index = article.getIndexName();
        Client client = ElasticsearchUtil.getInstance().getClient();
        try {
            if (index == null) {
                logger.warn("Updating article that has no indexName set.");
                index = this.getArticle(article.getId()).getIndexName();
            }
            IndexRequest indexRequest = new IndexRequest(index, "article", article.getId());
            indexRequest.source(mapper.writeValueAsString(article));
            return client.index(indexRequest).actionGet().getId() != null;
        } catch (IOException e) {
            logger.fatal("Unable to serialize article", e);
            return false;
        } catch (Exception e) {
            logger.error("Unable to update article", e);
            return false;
        }
    }

    public Article getArticle(String id) throws Exception {
        Client client = ElasticsearchUtil.getInstance().getClient();
        return getArticle(id, client);
    }

    public Article getArticle(String id, Client client) throws Exception {


        try {
            MultiGetRequestBuilder articleRequest = client.prepareMultiGet();
            for (String indexName : client.admin().indices().getIndex(getNewsriverIndexRequest).actionGet().getIndices()) {
                articleRequest = articleRequest.add(indexName, "article", id);
            }
            MultiGetResponse responses = articleRequest.execute().actionGet();
            for (MultiGetItemResponse response : responses.getResponses()) {
                //// TODO: 06.10.17 Consider checking it the article has been found in multiple indexes and throw an error if so 
                if (response.getResponse().isExists()) {
                    Article article = mapper.readValue(response.getResponse().getSourceAsString(), Article.class);
                    article.setIndexName(response.getResponse().getIndex());
                    return article;
                }
            }
        } catch (IOException e) {
            logger.fatal("Unable to deserialize article", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unable to get article from elasticsearch", e);
            throw e;
        }
        return null;
    }


    //Special view to save only a subset of the website object as nested object of the article
    private interface ElasticSearchJSONView extends Article.JSONViews.Internal, WebSite.JSONViews.ArticleNested {
    }

}
