package ch.newsriver.data.metadata;

/**
 * Created by eliapalme on 07/06/16.
 */
public class FinancialSentiment  extends MetaData{

    float sentiment;

    public float getSentiment() {
        return sentiment;
    }

    public void setSentiment(float sentiment) {
        this.sentiment = sentiment;
    }
}
