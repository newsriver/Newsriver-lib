package ch.newsriver.data.url;

/**
 * Created by eliapalme on 11/03/16.
 */
public class FeedURL extends BaseURL {


    private String title;
    private String publicationDate;
    private String headlines;


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeadlines() {
        return headlines;
    }

    public void setHeadlines(String headlines) {
        this.headlines = headlines;
    }


    public String getPublicationDate() {return publicationDate;}

    public void setPublicationDate(String publicationDate) {this.publicationDate = publicationDate;}
}
