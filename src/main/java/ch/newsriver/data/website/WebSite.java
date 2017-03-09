package ch.newsriver.data.website;

import ch.newsriver.data.website.source.BaseSource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by eliapalme on 03/04/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSite {

    @JsonView(JSONViews.API.class)
    Status status = Status.ACTIVE;

    ;
    @JsonView(JSONViews.Public.class)
    String name;
    @JsonView(JSONViews.Public.class)
    String hostName;
    @JsonView(JSONViews.Public.class)
    String domainName;
    @JsonView(JSONViews.Public.class)
    String iconURL;
    @JsonView(JSONViews.Public.class)
    Long rankingGlobal;
    @JsonView(JSONViews.ArticleNested.class)
    String countryName;
    @JsonView(JSONViews.ArticleNested.class)
    String countryCode;
    @JsonView(JSONViews.API.class)
    String canonicalURL;
    @JsonView(JSONViews.API.class)
    Long rankingCountry;
    @JsonView(JSONViews.Internal.class)
    String lastUpdate;
    @JsonView(JSONViews.Internal.class)
    String description;
    @JsonView(JSONViews.Internal.class)
    List<String> alternativeURLs = new LinkedList<>();
    @JsonView(JSONViews.Internal.class)
    List<Double> geoLocation;
    @JsonView(JSONViews.Internal.class)
    boolean ssl;
    @JsonView(JSONViews.Internal.class)
    int port;
    @JsonView(JSONViews.Internal.class)
    Set<String> languages = new HashSet<>();
    @JsonView(JSONViews.Internal.class)
    boolean ajaxBased = false;
    @JsonView(JSONViews.API.class)
    List<BaseSource> sources = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Long getRankingGlobal() {
        return rankingGlobal;
    }

    public void setRankingGlobal(Long rankingGlobal) {
        this.rankingGlobal = rankingGlobal;
    }

    public Long getRankingCountry() {
        return rankingCountry;
    }

    public void setRankingCountry(Long rankingCountry) {
        this.rankingCountry = rankingCountry;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconURL() {
        return iconURL;
    }

    public void setIconURL(String iconURL) {
        this.iconURL = iconURL;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getCanonicalURL() {
        return canonicalURL;
    }

    public void setCanonicalURL(String canonicalURL) {
        this.canonicalURL = canonicalURL;
    }

    public List<String> getAlternativeURLs() {
        return alternativeURLs;
    }

    public void setAlternativeURLs(List<String> alternativeURLs) {
        this.alternativeURLs = alternativeURLs;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
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

    public List<Double> getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(List<Double> geoLocation) {
        this.geoLocation = geoLocation;
    }

    public boolean isAjaxBased() {
        return ajaxBased;
    }

    public void setAjaxBased(boolean ajaxBased) {
        this.ajaxBased = ajaxBased;
    }

    public List<BaseSource> getSources() {
        return sources;
    }

    public void setSources(List<BaseSource> sources) {
        this.sources = sources;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {ACTIVE, DISALLOWED, IGNORED}

    public static class JSONViews {

        public static interface Public {
        }

        public static interface ArticleNested extends Public {
        }

        public static interface API extends ArticleNested {
        }

        public static interface Internal extends API {
        }

    }
}
