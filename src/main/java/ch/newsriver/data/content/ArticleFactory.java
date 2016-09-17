package ch.newsriver.data.content;

import ch.newsriver.dao.ElasticsearchUtil;
import ch.newsriver.data.website.WebSite;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.xml.FilterBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

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
        LinkedList<HighlightedArticle> articles = new LinkedList<>();
        try {
            QueryBuilder qb = QueryBuilders.queryStringQuery(searchRequest.getQuery());

            FilterBuilder filter = null;

            FieldSortBuilder sortBuilder = SortBuilders.fieldSort("discoverDate").order(SortOrder.DESC).sortMode("max");


            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver")
                    .setTypes("article")
                    .addHighlightedField("title")
                    .addHighlightedField("text")
                    .setHighlighterPreTags("<highlighted>")
                    .setHighlighterPostTags("</highlighted>")
                    .setHighlighterRequireFieldMatch(false)
                    .setHighlighterNumOfFragments(1)
                    .setSize(searchRequest.getLimit())
                    .addSort(sortBuilder)
                    .addSort(SortBuilders.scoreSort())
                    .setQuery(qb);

            if (searchRequest.getId() != null) {
                searchRequestBuilder.setPostFilter(QueryBuilders.termQuery("_id", searchRequest.getId()));
            }

            //if(searchRequest.getFields()!=null && !searchRequest.getFields().isEmpty()){
            //    searchRequestBuilder.addFields(searchRequest.getFields().toArray(new String[0]));
            //}

            SearchResponse response = searchRequestBuilder.execute().actionGet();
            for (SearchHit hit : response.getHits()) {


                try {
                    HighlightedArticle article = mapper.readValue(hit.getSourceAsString(), HighlightedArticle.class);
                    article.setId(hit.getId());
                    article.setScore(hit.getScore());

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

    public IndexResponse saveArticle(Article article, String urlHash){
        Client client = ElasticsearchUtil.getInstance().getClient();
        try {

            IndexRequest indexRequest = new IndexRequest("newsriver", "article", urlHash);

            indexRequest.source(mapper.writeValueAsString(article));
            return  client.index(indexRequest).actionGet();

        } catch (Exception e) {
            logger.error("Unable to save article in elasticsearch", e);
            return null;
        }
    }


    public boolean updateArticle(Article article) {

        Client client = ElasticsearchUtil.getInstance().getClient();
        try {

            IndexRequest indexRequest = new IndexRequest("newsriver", "article", article.getId());
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

    public Article getArticle(String id) {

        Client client = ElasticsearchUtil.getInstance().getClient();


        try {

            GetResponse response = client.prepareGet("newsriver", "article", id).execute().actionGet();
            if (response.isExists()) {
                Article article = mapper.readValue(response.getSourceAsString(), Article.class);
                return article;
            }

        } catch (IOException e) {
            logger.fatal("Unable to deserialize article", e);
            return null;
        } catch (Exception e) {
            logger.error("Unable to get article from elasticsearch", e);
            return null;
        }
        return null;
    }


    //Special view to save only a subset of the website object as nested object of the article
    private interface ElasticSearchJSONView extends Article.JSONViews.Internal, WebSite.JSONViews.ArticleNested {
    }

}
