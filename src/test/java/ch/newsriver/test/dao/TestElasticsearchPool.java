package ch.newsriver.test.dao;

import ch.newsriver.dao.ElasticsearchPoolUtil;
import ch.newsriver.executable.BatchInterruptibleWithinExecutorPool;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.ClusterAdminClient;
import org.junit.Test;

import java.util.concurrent.Semaphore;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by eliapalme on 18/03/16.
 */
public class TestElasticsearchPool {


    @Test
    public void checkClusterHealth() {


       boolean isHealthy =  ElasticsearchPoolUtil.getInstance().checkHealth();
        assertTrue(isHealthy);


    }
}
