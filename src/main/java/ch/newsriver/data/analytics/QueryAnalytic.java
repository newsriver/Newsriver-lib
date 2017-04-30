package ch.newsriver.data.analytics;

/**
 * Created by eliapalme on 29.04.17.
 */
public class QueryAnalytic {


    private String lastExecution;
    private String query;
    private long executions;
    private long results;

    public String getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(String lastExecution) {
        this.lastExecution = lastExecution;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getExecutions() {
        return executions;
    }

    public void setExecutions(long executions) {
        this.executions = executions;
    }

    public long getResults() {
        return results;
    }

    public void setResults(long results) {
        this.results = results;
    }
}
