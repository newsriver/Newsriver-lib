package ch.newsriver.elasticsearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.util.AttributeFactory;
import org.junit.Before;

import java.io.IOException;

/**
 * Created by eliapalme on 23/08/16.
 */
public class URLAnalyzer {

    Analyzer urlAnalyzer;

    @Before
    public void setUp() throws IOException {
        urlAnalyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                UAX29URLEmailTokenizer tokenizer = new UAX29URLEmailTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY);
                tokenizer.setMaxTokenLength(Integer.MAX_VALUE);  // Tokenize arbitrary length URLs
                return new TokenStreamComponents(tokenizer);
            }
        };
    }

    //TODO: this is experimental and proves that UAX29URLEmailTokenizer is not suited to analyse sub components of a URL such as schema, host, path, etc.
    //Instead the UAX29URLEmailTokenizer recognises URLs in text and will threat them like a single token
    /*
    @Test
    public void testURLAnalyzer() throws IOException {


        TokenStream tokenStream = urlAnalyzer.tokenStream("dummy", new StringReader("http://alvinalexander.com/java/jwarehouse/lucene/backwards/src/test-framework/org/apache/lucene/analysis/BaseTokenStreamTestCase.java.shtml"));
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            String term = charTermAttribute.toString();
            System.out.println(term);
        }
    }*/


}
