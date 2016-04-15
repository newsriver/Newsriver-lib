package ch.newsriver.test.dao;

import ch.newsriver.dao.ElasticsearchPoolUtil;
import org.junit.Test;

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
