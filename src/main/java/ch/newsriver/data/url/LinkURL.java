package ch.newsriver.data.url;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by eliapalme on 31/05/16.
 */
//TODO: remove this once we discontinued sources and integrated them into the website
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkURL extends BaseURL {


    private String country;
    private String region;
    private String category;


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
