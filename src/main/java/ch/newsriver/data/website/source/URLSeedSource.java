package ch.newsriver.data.website.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by eliapalme on 27/05/16.
 */

//TODO: remove this once we discontinued sources and integrated them into the website
@JsonIgnoreProperties(ignoreUnknown = true)
public class URLSeedSource extends BaseSource {

    //Is this a root seed URL or temporary used to traverse the website.
    private boolean permanent;

    private String countryName;
    private String countryCode;
    private String region;
    private String languageCode;
    private String category;
    private String expectedPath;


    public boolean isPermanent() {
        return permanent;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }
    
    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getExpectedPath() {
        return expectedPath;
    }

    public void setExpectedPath(String expectedPath) {
        this.expectedPath = expectedPath;
    }
}
