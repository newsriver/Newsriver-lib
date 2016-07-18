package ch.newsriver.data.html;

import java.util.Set;

/**
 * Created by eliapalme on 20/06/16.
 */




public class AjaxHTML extends HTML {

    Set<String> dynamicURLs;

    public Set<String> getDynamicURLs() {
        return dynamicURLs;
    }

    public void setDynamicURLs(Set<String> dynamicURLs) {
        this.dynamicURLs = dynamicURLs;
    }
}
