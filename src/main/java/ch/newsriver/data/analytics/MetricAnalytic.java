package ch.newsriver.data.analytics;

import java.util.Map;

/**
 * Created by eliapalme on 25.04.17.
 */
public class MetricAnalytic {

    private long total;
    private Map<String, Integer> aggregatedMetric;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public Map<String, Integer> getAggregatedMetric() {
        return aggregatedMetric;
    }

    public void setAggregatedMetric(Map<String, Integer> aggregatedMetric) {
        this.aggregatedMetric = aggregatedMetric;
    }
}
