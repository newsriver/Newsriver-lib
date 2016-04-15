package ch.newsriver.data.html;

import ch.newsriver.data.url.BaseURL;

/**
 * Created by eliapalme on 15/03/16.
 */
public class HTML {

    BaseURL referral;
    String  rawHTML;
    String  title;
    String  encoding;
    String  language;
    String  url;
    boolean alreadyFetched = false;

    public String getRawHTML() {
        return rawHTML;
    }

    public void setRawHTML(String rawHTML) {
        this.rawHTML = rawHTML;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public BaseURL getReferral() {
        return referral;
    }

    public void setReferral(BaseURL referral) {
        this.referral = referral;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isAlreadyFetched() {
        return alreadyFetched;
    }

    public void setAlreadyFetched(boolean alreadyFetched) {
        this.alreadyFetched = alreadyFetched;
    }
}
