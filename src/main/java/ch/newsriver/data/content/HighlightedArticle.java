package ch.newsriver.data.content;

/**
 * Created by eliapalme on 12/06/16.
 */
public class HighlightedArticle extends Article {



    private String highlight;
    private Float score;

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }
}
