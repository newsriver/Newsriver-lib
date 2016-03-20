package ch.newsriver.data.url;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by eliapalme on 11/03/16.
 */

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value=FeedURL.class, name="FeedURL")
})


public abstract class BaseURL {

    private String referral;
    private String rawURL;
    private String normalizeURL;
    private String discoverDate;

    public String getReferral() {
        return referral;
    }

    public void setReferral(String referral) {
        this.referral = referral;
    }

    public String getRawURL() {
        return rawURL;
    }

    public void setRawURL(String rawURL) {
        this.rawURL = rawURL;
    }

    public String getNormalizeURL() {
        return normalizeURL;
    }

    public void setNormalizeURL(String normalizeURL) {
        this.normalizeURL = normalizeURL;
    }

    public String getDiscoverDate() {return discoverDate;}

    public void setDiscoverDate(String discoverDate) {this.discoverDate = discoverDate;}
}
