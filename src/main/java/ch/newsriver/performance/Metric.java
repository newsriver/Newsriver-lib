package ch.newsriver.performance;

import ch.newsriver.data.url.BaseURL;

import java.util.Map;

/**
 * Created by eliapalme on 14/04/16.
 */
public class Metric {

    int count;
    String message;
    String instance;
    String className;
    BaseURL info;
    Map<String, String> metadata;

    public Metric(String message, BaseURL info, Map<String, String> metadata, String className, String instance, int count) {
        this.count = count;
        this.message = message;
        this.instance = instance;
        this.className = className;
        this.info = info;
        this.metadata = metadata;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }


    public BaseURL getInfo() {
        return info;
    }

    public void setInfo(BaseURL info) {
        this.info = info;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
