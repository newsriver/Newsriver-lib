package ch.newsriver.data.content;

import ch.newsriver.data.publisher.Publisher;
import ch.newsriver.data.url.BaseURL;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by eliapalme on 18/03/16.
 */
public class Article {

    String          id;
    String          publishDate;
    String          discoverDate;
    String          title;
    String          language;
    String          html;
    String          text;
    String          url;
    List<Element>   elements = new LinkedList<>();
    Publisher publisher;
    List<BaseURL>   referrals = new LinkedList<>();


    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public String getDiscoverDate() {
        return discoverDate;
    }

    public void setDiscoverDate(String discoverDate) {
        this.discoverDate = discoverDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    public String getId() {return id;}

    public void setId(String id) {this.id = id;}

    public Publisher getPublisher() {return publisher;}

    public void setPublisher(Publisher publisher) {this.publisher = publisher;}

    public List<BaseURL> getReferrals() {return referrals;}

    public void setReferrals(List<BaseURL> referrals) {this.referrals = referrals;}
}
