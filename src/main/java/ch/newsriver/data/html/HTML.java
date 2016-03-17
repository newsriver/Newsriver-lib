package ch.newsriver.data.html;

import ch.newsriver.data.url.BaseURL;

/**
 * Created by eliapalme on 15/03/16.
 */
public class HTML {

    BaseURL refferal;
    String  rawHTML;

    public BaseURL getRefferal() {
        return refferal;
    }

    public void setRefferal(BaseURL refferal) {
        this.refferal = refferal;
    }

    public String getRawHTML() {
        return rawHTML;
    }

    public void setRawHTML(String rawHTML) {
        this.rawHTML = rawHTML;
    }
}
