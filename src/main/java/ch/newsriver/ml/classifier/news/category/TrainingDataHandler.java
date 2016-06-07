package ch.newsriver.ml.classifier.news.category;

import ch.newsriver.dao.JDBCPoolUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by eliapalme on 14/05/16.
 */
public class TrainingDataHandler {


    private static String location = "/Users/eliapalme/Newsriver/ML/CategoryClassification/data/";
    private static int limit = 5000;
    private static final ObjectMapper mapper = new ObjectMapper();


    public Map<Integer,Map<Integer,ArticleTrainingSet>> loadData() {

        Map<Integer,Map<Integer,ArticleTrainingSet>> trainingSet = new HashMap<>();

        File dir = new File(location);

        Collection<File> files = FileUtils.listFiles(
                dir,
                new RegexFileFilter("^(articles.*.json)"),
                DirectoryFileFilter.DIRECTORY
        );

        for(File file : files){
            try {
                ArticleTrainingSet set = mapper.readValue(file, ArticleTrainingSet.class);
                Map<Integer, ArticleTrainingSet> langSet = trainingSet.get(set.getLanguageId());
                if (langSet == null) {
                    langSet = new HashMap<>();
                }
                langSet.put(set.getCategoryId(), set);
                trainingSet.put(set.getLanguageId(), langSet);
            }catch (Exception e){};

        }
        return trainingSet;
    }


    public void downloadNewDataSet() throws URISyntaxException, Exception {

        Map<String,Integer> languages = getLanguages();

        for(String language : languages.keySet()) {
            int languageID = languages.get(language);
            Map<String, Integer> categories = getCategories(languageID);
            for (String category : categories.keySet()) {

                int categoryID = categories.get(category);
                ArticleTrainingSet set = new ArticleTrainingSet();

                set.setCategory(category);
                set.setCategoryId(categoryID);
                set.setLanguageId(languageID);
                set.setLanguage(language);

                List<ArticleTrainingSet.Article> articles = downloadSet(languageID, categoryID, limit);
                set.setSize(articles.size());
                set.setArticles(articles);

                PrintWriter out = new PrintWriter(location+"articles.lang_"+languageID+".cat_" + categoryID + ".s_" + articles.size() + ".json");
                out.write(mapper.writeValueAsString(set));
                out.close();
            }
        }
    }



    private Map<String,Integer> getLanguages() {


        HashMap<String,Integer> data = new HashMap<>();


        String sql = "SELECT * FROM NewscronConfiguration.language";
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql);) {
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    data.put(rs.getString("name"),rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return  data;

    }


    private Map<String,Integer> getCategories(int laguageID) {


        HashMap<String,Integer> data = new HashMap<>();


        String sql = "SELECT distinct(categoryID) as id, name FROM(select categoryID FROM NewscronArchive.article where languageId=? limit 10000) as F\n" +
                "JOIN NewscronConfiguration.category AS C ON C.id=F.categoryID";
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql);) {
            stmt.setInt(1,laguageID);
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    data.put(rs.getString("name"),rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return  data;

    }


    private List<ArticleTrainingSet.Article> downloadSet(int laguageID, int categoryID, int limit){

        List<ArticleTrainingSet.Article> data = new ArrayList<>();


        String sql = "Select title,snippet,URL from NewscronContent.article where status=5 and CategoryID=? and LanguageID=? order by id desc limit ? ";
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql);) {
            stmt.setInt(1,categoryID);
            stmt.setInt(2,laguageID);
            stmt.setInt(3,limit);
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    ArticleTrainingSet.Article a = new ArticleTrainingSet.Article();
                    a.setText(rs.getString("snippet"));
                    a.setTitle(rs.getString("title"));
                    a.setUrl(rs.getString("URL"));
                    data.add(a);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return  data;
    }



}
