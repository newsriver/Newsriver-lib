package ch.newsriver.data.content;

import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.website.WebSite;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliapalme on 18/03/16.
 */




public class Article {

    static public class  ArticleViews {
        static public class PublicView { }
        static class APIView extends PublicView { }
        static class Internal extends APIView { }
    }

    @JsonView(ArticleViews.APIView.class)
    String          id;
    @JsonView(ArticleViews.APIView.class)
    String          publishDate;
    @JsonView(ArticleViews.PublicView.class)
    String          discoverDate;
    @JsonView(ArticleViews.PublicView.class)
    String          title;
    @JsonView(ArticleViews.PublicView.class)
    String          language;
    @JsonView(ArticleViews.PublicView.class)
    String          text;
    @JsonView(ArticleViews.PublicView.class)
    String          url;
    @JsonView(ArticleViews.PublicView.class)
    List<Element>   elements = new LinkedList<>();
    @JsonView(ArticleViews.APIView.class)
    WebSite         website;
    @JsonView(ArticleViews.Internal.class)
    List<BaseURL>   referrals = new LinkedList<>();


    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public String getDiscoverDate() {
        return discoverDate;
    }

    public void setDiscoverDate(String discoverDate) {
        this.discoverDate = discoverDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    public String getId() {return id;}

    public void setId(String id) {this.id = id;}

    public List<BaseURL> getReferrals() {return referrals;}

    public void setReferrals(List<BaseURL> referrals) {this.referrals = referrals;}

    public WebSite getWebsite() {return website;}

    public void setWebsite(WebSite website) {this.website = website;}


}


