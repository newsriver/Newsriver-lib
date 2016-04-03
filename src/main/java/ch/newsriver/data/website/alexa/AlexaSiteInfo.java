package ch.newsriver.data.website.alexa;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by eliapalme on 03/04/16.
 */
public class AlexaSiteInfo {

    Long globalRank;
    Map<String,Long> countryRank = new TreeMap<>();
    String country;
    String owner;
    String title;
    String description;
    Long  loadTime;
    String language;
    String onlineSince;


    public Long getGlobalRank() {
        return globalRank;
    }

    public void setGlobalRank(Long globalRank) {
        this.globalRank = globalRank;
    }

    public Map<String, Long> getCountryRank() {
        return countryRank;
    }

    public void setCountryRank(Map<String, Long> countryRank) {
        this.countryRank = countryRank;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getLoadTime() {
        return loadTime;
    }

    public void setLoadTime(Long loadTime) {
        this.loadTime = loadTime;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }


    public String getOnlineSince() {
        return onlineSince;
    }

    public void setOnlineSince(String onlineSince) {
        this.onlineSince = onlineSince;
    }

}
