package ch.newsriver.test.json;

import ch.newsriver.data.metadata.ReadTime;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by eliapalme on 07/06/16.
 */
public class TestTypeAnnotationRetreival {

    @Test
    public void TestTypeAnnotation(){

        ReadTime readTime = new ReadTime();
        assertTrue(readTime.key().equals("readTime"));

    }
}
