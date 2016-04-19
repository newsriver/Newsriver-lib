package ch.newsriver.data.url;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by eliapalme on 11/03/16.
 */

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value=FeedURL.class, name="FeedURL"),
        @JsonSubTypes.Type(value=SourceRSSURL.class, name="SourceRSSURL")
})


public abstract class BaseURL {

    private String referralURL;
    private String rawURL;
    private String ulr;
    private String discoverDate;

    public String getRawURL() {
        return rawURL;
    }

    public void setRawURL(String rawURL) {this.rawURL = rawURL;}

    public String getDiscoverDate() {return discoverDate;}

    public void setDiscoverDate(String discoverDate) {this.discoverDate = discoverDate;}

    public String getUlr() {return ulr;}

    public void setUlr(String ulr) {this.ulr = ulr;}

    public String getReferralURL() {return referralURL;}

    public void setReferralURL(String referralURL) {this.referralURL = referralURL;}
}
