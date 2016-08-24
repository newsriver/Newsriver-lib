package ch.newsriver.test.json;

import ch.newsriver.data.content.Article;
import ch.newsriver.data.metadata.FinancialSentiment;
import ch.newsriver.data.metadata.ReadTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by eliapalme on 07/06/16.
 */
public class TestTypeAnnotation {

    protected ObjectMapper mapper;
    private String articleJSON;
    private Article articleObject;


    @Before
    public void setUp() throws IOException {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);


        articleJSON = IOUtils.toString(this.getClass().getResourceAsStream("/ch/newsriver/test/json/testTypeAnnotation.json"), "UTF-8");

        articleObject = new Article();
        articleObject.setId("13");
        ReadTime readTime = new ReadTime();
        readTime.setSeconds(60);
        articleObject.addMetadata(readTime);
        FinancialSentiment financialSentiment = new FinancialSentiment();
        financialSentiment.setSentiment(1);
        articleObject.addMetadata(financialSentiment);
    }


    @Test
    public void TestTypeAnnotationKey() {

        ReadTime readTime = new ReadTime();
        assertTrue(readTime.key().equals("readTime"));

    }

    @Test
    public void TestMetadataSerialization() throws IOException {

        String articleStr = mapper.writeValueAsString(articleObject);
        assertNotNull(articleStr);
        assertTrue(articleStr.equals(articleJSON));

    }


    @Test
    public void TestMetadataDeserialisation() throws IOException {

        Article article = mapper.readValue(articleJSON, Article.class);
        assertNotNull(article);
        //TODO: here we should test that both objects are equal
        //The issue is that reflectionEquals is not recoursive and will fail when comparing nested objects
        //Assert.assertTrue(EqualsBuilder.reflectionEquals(articleObject, article));


    }


}
