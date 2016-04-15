package ch.newsriver.performance;

/**
 * Created by eliapalme on 14/04/16.
 */
public class Metric {

    int count;
    String message;
    String instance;
    String className;

    public Metric(String message, String className,String instance,  int count) {
        this.count = count;
        this.message = message;
        this.instance = instance;
        this.className = className;
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
}
