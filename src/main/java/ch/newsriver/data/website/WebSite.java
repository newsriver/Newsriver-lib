package ch.newsriver.data.website;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by eliapalme on 03/04/16.
 */
public class WebSite {

    String  name;
    String  hostName;
    String  domainName;
    boolean ssl;
    int     port;
    Locale  locale;
    Long    rankingGlobal;
    Long    rankingCountry;
    List<String> feeds = new LinkedList<>();
    String  description;
    String  iconURL;
    String  lastUpdate;
    String  canonicalURL;
    List<String> alternativeURLs = new LinkedList<>();

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

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
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
}
