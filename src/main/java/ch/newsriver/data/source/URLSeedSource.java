package ch.newsriver.data.source;

import java.util.Map;

/**
 * Created by eliapalme on 27/05/16.
 */
public class URLSeedSource extends  BaseSource{

    //Is this a root seed URL or temporary used to traverse the website.
    private boolean permanent;

    //distance from the root seed URL
    private int depth;

    private String referralURL;


    public boolean isPermanent() {
        return permanent;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getReferralURL() {
        return referralURL;
    }

    public void setReferralURL(String referralURL) {
        this.referralURL = referralURL;
    }
}
