package ch.newsriver.data.publisher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by eliapalme on 18/03/16.
 */
public class Publisher {

    String domainName;
    String country;
    String icon;
    SiteInfo siteInfo;
    Set<String> hosts = new HashSet<>();

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public void setHosts(Set<String> hosts) {
        this.hosts = hosts;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public SiteInfo getSiteInfo() {
        return siteInfo;
    }

    public void setSiteInfo(SiteInfo siteInfo) {
        this.siteInfo = siteInfo;
    }

}
