package ch.newsriver.data.website;

import ch.newsriver.data.content.Article;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * Created by eliapalme on 03/04/16.
 */
public class WebSite {

    String  name;
    @JsonView(Article.ArticleViews.PublicView.class)
    String  hostName;
    @JsonView(Article.ArticleViews.PublicView.class)
    String  domainName;
    @JsonView(Article.ArticleViews.Internal.class)
    boolean ssl;
    @JsonView(Article.ArticleViews.Internal.class)
    int     port;
    @JsonView(Article.ArticleViews.APIView.class)
    String  countryName;
    @JsonView(Article.ArticleViews.APIView.class)
    String  countryCode;
    @JsonView(Article.ArticleViews.APIView.class)
    Set<String> languages = new HashSet<>();
    @JsonView(Article.ArticleViews.PublicView.class)
    Long    rankingGlobal;
    @JsonView(Article.ArticleViews.APIView.class)
    Long    rankingCountry;
    @JsonView(Article.ArticleViews.Internal.class)
    List<String> feeds = new LinkedList<>();
    @JsonView(Article.ArticleViews.PublicView.class)
    String  description;
    @JsonView(Article.ArticleViews.PublicView.class)
    String  iconURL;
    @JsonView(Article.ArticleViews.Internal.class)
    String  lastUpdate;
    @JsonView(Article.ArticleViews.PublicView.class)
    String  canonicalURL;
    @JsonView(Article.ArticleViews.APIView.class)
    List<String> alternativeURLs = new LinkedList<>();
    @JsonView(Article.ArticleViews.APIView.class)
    List<Double> geoLocation;

    @JsonView(Article.ArticleViews.Internal.class)
    boolean ajaxBased=false;


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

    public List<String> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<String> feeds) {
        this.feeds = feeds;
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
}
