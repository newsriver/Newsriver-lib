package ch.newsriver.data.analytics;

import ch.newsriver.dao.JDBCPoolUtil;
import ch.newsriver.data.content.ArticleFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by eliapalme on 25.04.17.
 */
public class AnalyticsFactory {

    private static final Logger logger = LogManager.getLogger(ArticleFactory.class);
    private static final SimpleDateFormat dateFormatterSQL = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static AnalyticsFactory instance;


    private AnalyticsFactory() {

    }

    static public synchronized AnalyticsFactory getInstance() {

        if (instance == null) {
            instance = new AnalyticsFactory();
        }
        return instance;
    }

    public UserUsage getUsage(long userId, Period period) {

        UserUsage userUsage = new UserUsage();
        userUsage.setApiCalls(getAPICalls(userId, period));
        userUsage.setDataPoints(getDataPoints(userId, period));
        userUsage.setQueriesHistory(getQueriesHistory(userId, period));
        return userUsage;
    }


    public MetricAnalytic getAPICalls(long userId, Period period) {


        return getAggregatedCounts(userId, period, "logAPIcalls");
    }


    public MetricAnalytic getDataPoints(long userId, Period period) {


        return getAggregatedCounts(userId, period, "logDataPoint");
    }


    private MetricAnalytic getAggregatedCounts(long userId, Period period, String table) {

        MetricAnalytic metrics = new MetricAnalytic();
        long total = 0;
        Map<String, Integer> aggregatedCounts = new LinkedHashMap<String, Integer>();
        LocalDate now = java.time.LocalDate.now();
        for (int i = 0; i <= period.getDays(); i++) {
            aggregatedCounts.put(dateFormatter.format(now.minusDays(i)), 0);
        }

        String sql = "SELECT count,day FROM Newsriver." + table + " WHERE userId=? AND day>DATE_SUB(NOW(), INTERVAL 30 DAY)";
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    aggregatedCounts.put(dateFormatterSQL.format(resultSet.getDate("day")), resultSet.getInt("count"));
                    total += resultSet.getInt("count");
                }
            }
        } catch (SQLException e) {
            logger.fatal("Unable to retreive API calls analytics", e);
            return null;
        }
        metrics.setAggregatedMetric(aggregatedCounts);
        metrics.setTotal(total);
        return metrics;
    }


    private LinkedList<QueryAnalytic> getQueriesHistory(long userId, Period period) {

        LinkedList<QueryAnalytic> queries = new LinkedList<QueryAnalytic>();

        String sql = "SELECT * FROM Newsriver.logQuery where userid=? order by lastExecution desc limit 10";
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    QueryAnalytic query = new QueryAnalytic();
                    query.setExecutions(resultSet.getLong("count"));
                    query.setResults(resultSet.getLong("cumulatedResults"));
                    query.setQuery(resultSet.getString("query"));
                    query.setLastExecution(dateFormatterSQL.format(resultSet.getDate("lastExecution")));
                    queries.add(query);
                }
            }
        } catch (SQLException e) {
            logger.fatal("Unable to retreive API calls analytics", e);
            return null;
        }

        return queries;
    }


}
