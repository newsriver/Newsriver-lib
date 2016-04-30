package ch.newsriver.data.content;

import ch.newsriver.dao.ElasticsearchPoolUtil;
import ch.newsriver.data.publisher.Publisher;
import ch.newsriver.data.publisher.PublisherFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.xml.FilterBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliapalme on 26/04/16.
 */
public class ArticleFactory {




    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LogManager.getLogger(ArticleFactory.class);
    private static  ArticleFactory instance;

    private ArticleFactory(){

    }

    static  public synchronized  ArticleFactory getInstance(){

        if(instance == null){
            instance = new ArticleFactory();
        }
        return instance;
    }



    public List<Article> searchArticles(ArticleRequest searchRequest){

        Client client = null;
        client = ElasticsearchPoolUtil.getInstance().getClient();
        LinkedList<Article> articles = new LinkedList<>();
        try {
            QueryBuilder qb = QueryBuilders.queryStringQuery(searchRequest.getQuery());

            FilterBuilder filter = null;


            SearchRequestBuilder searchRequestBuilder = client.prepareSearch()
                    .setIndices("newsriver")
                    .setTypes("article")
                    .setHighlighterPreTags("@newsriver-highlighted-field@")
                    .setHighlighterPostTags("@/newsriver-highlighted-field@")
                    .setSize(searchRequest.getLimit())
                    .setQuery(qb);

            if(searchRequest.getId()!=null){
                searchRequestBuilder.setPostFilter(QueryBuilders.termQuery("_id", searchRequest.getId()));
            }

            //if(searchRequest.getFields()!=null && !searchRequest.getFields().isEmpty()){
            //    searchRequestBuilder.addFields(searchRequest.getFields().toArray(new String[0]));
            //}

            SearchResponse response =  searchRequestBuilder.execute().actionGet();
            for (SearchHit hit : response.getHits()) {
                try {
                Article article =  mapper.readValue(hit.getSourceAsString(),Article.class);
                    article.setId(hit.getId());
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



}
