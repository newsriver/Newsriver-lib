package ch.newsriver.data.metadata;

/**
 * Created by eliapalme on 28/08/16.
 */
public class Category extends MetaData {

    private String country;
    private String region;
    private String category;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
