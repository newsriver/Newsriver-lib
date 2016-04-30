package ch.newsriver.data.source;

import ch.newsriver.data.website.WebSite;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by eliapalme on 19/04/16.
 */

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value=FeedSource.class, name="FeedSource"),
})

public abstract class BaseSource {

    private String  url;
    private WebSite website;
    private String  lastVisit;
    private String  httpStatus;
    private String  exception;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public WebSite getWebsite() {
        return website;
    }

    public void setWebsite(WebSite website) {
        this.website = website;
    }

    public String getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(String httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getLastVisit() {return lastVisit;}

    public void setLastVisit(String lastVisit) {this.lastVisit = lastVisit;}
}
