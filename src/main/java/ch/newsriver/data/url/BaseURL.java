package ch.newsriver.data.url;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by eliapalme on 11/03/16.
 */

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value=FeedURL.class, name="FeedURL"),
        @JsonSubTypes.Type(value=SourceRSSURL.class, name="SourceRSSURL"),
        @JsonSubTypes.Type(value=ManualURL.class, name="ManualURL"),
        @JsonSubTypes.Type(value=SeedURL.class, name="SeedURL"),
        @JsonSubTypes.Type(value=LinkURL.class, name="LinkURL")
})

//Has been added due to the ULR property that was wrongly spelled.
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseURL {

    private String referralURL;
    private String rawURL;
    private String url;
    private String discoverDate;

    public String getRawURL() {
        return rawURL;
    }

    public void setRawURL(String rawURL) {this.rawURL = rawURL;}

    public String getDiscoverDate() {return discoverDate;}

    public void setDiscoverDate(String discoverDate) {this.discoverDate = discoverDate;}

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getReferralURL() {return referralURL;}

    public void setReferralURL(String referralURL) {this.referralURL = referralURL;}
}
