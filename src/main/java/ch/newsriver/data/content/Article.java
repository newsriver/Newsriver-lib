package ch.newsriver.data.content;

import ch.newsriver.data.metadata.MetaData;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.website.WebSite;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliapalme on 18/03/16.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Article {

    @JsonView(JSONViews.API.class)
    String id;
    @JsonView(JSONViews.API.class)
    String publishDate;
    @JsonView(JSONViews.Public.class)
    String discoverDate;
    @JsonView(JSONViews.Public.class)
    String title;
    @JsonView(JSONViews.Public.class)
    String language;
    @JsonView(JSONViews.Public.class)
    String text;
    @JsonView(JSONViews.Public.class)
    String url;
    @JsonView(JSONViews.Public.class)
    @JsonDeserialize(as = LinkedList.class)
    List<Element> elements = new LinkedList<>();
    @JsonView(JSONViews.Public.class)
    WebSite website;
    @JsonView(JSONViews.Internal.class)
    @JsonDeserialize(as = LinkedList.class)
    List<BaseURL> referrals = new LinkedList<>();
    @JsonView(JSONViews.Public.class)
    HashMap<String, MetaData> metadata = new HashMap<>();

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<BaseURL> getReferrals() {
        return referrals;
    }

    public void setReferrals(List<BaseURL> referrals) {
        this.referrals = referrals;
    }

    public WebSite getWebsite() {
        return website;
    }

    public void setWebsite(WebSite website) {
        this.website = website;
    }

    public HashMap<String, MetaData> getMetadata() {
        return metadata;
    }

    public void setMetadata(HashMap<String, MetaData> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(MetaData metadata) {
        this.metadata.put(metadata.key(), metadata);
    }

    static public class JSONViews {
        static public interface Public {
        }

        static public interface API extends Public {
        }

        static public interface Internal extends API {
        }
    }

}


