package ch.newsriver.data.analytics;

import java.time.Period;
import java.util.LinkedList;

/**
 * Created by eliapalme on 25.04.17.
 */
public class UserUsage {

    private MetricAnalytic apiCalls;
    private MetricAnalytic dataPoints;
    private Period period;
    private LinkedList<QueryAnalytic> queriesHistory;


    public MetricAnalytic getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(MetricAnalytic dataPoints) {
        this.dataPoints = dataPoints;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public MetricAnalytic getApiCalls() {
        return apiCalls;
    }

    public void setApiCalls(MetricAnalytic apiCalls) {
        this.apiCalls = apiCalls;
    }

    public LinkedList<QueryAnalytic> getQueriesHistory() {
        return queriesHistory;
    }

    public void setQueriesHistory(LinkedList<QueryAnalytic> queriesHistory) {
        this.queriesHistory = queriesHistory;
    }
}
