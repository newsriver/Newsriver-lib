package ch.newsriver.ml.classifier.news.category;

import ch.newsriver.dao.JDBCPoolUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by eliapalme on 14/05/16.
 */
public class TrainingDataHandler {


    private static final ObjectMapper mapper = new ObjectMapper();
    private static String location = "/Users/eliapalme/Newsriver/ML/CategoryClassification/data/";

    public Map<Integer, Map<Integer, ArticleTrainingSet>> loadData() {
        return loadData(null);

    }

    public Map<Integer, Map<Integer, ArticleTrainingSet>> loadData(Integer languageId) {

        Map<Integer, Map<Integer, ArticleTrainingSet>> trainingSet = new HashMap<>();

        File dir = new File(location);

        String regex = "^(articles.*.json)";
        if (languageId != null) {
            regex = "^(articles.lang_" + languageId + ".*.json)";
        }

        Collection<File> files = FileUtils.listFiles(
                dir,
                new RegexFileFilter(regex),
                DirectoryFileFilter.DIRECTORY
        );

        for (File file : files) {
            try {
                ArticleTrainingSet set = mapper.readValue(file, ArticleTrainingSet.class);
                Map<Integer, ArticleTrainingSet> langSet = trainingSet.get(set.getLanguageId());
                if (langSet == null) {
                    langSet = new HashMap<>();
                }

                if (langSet.get(set.getCategoryId()) == null || langSet.get(set.getCategoryId()).getArticles().size() < set.getArticles().size()) {
                    langSet.put(set.getCategoryId(), set);
                }

                trainingSet.put(set.getLanguageId(), langSet);
            } catch (Exception e) {
            }
            ;

        }
        return trainingSet;
    }

    public void downloadNewDataSet(int limit) {
        downloadNewDataSet(limit, null);
    }

    public void downloadNewDataSet(int limit, Integer specificLang) {

        Map<String, Integer> languages = getLanguages(specificLang);

        for (String language : languages.keySet()) {
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
                try {
                    PrintWriter out = new PrintWriter(location + "articles.lang_" + languageID + ".cat_" + categoryID + "." + category + ".s_" + articles.size() + ".json");
                    out.write(mapper.writeValueAsString(set));
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }


    private Map<String, Integer> getLanguages(Integer specificLang) {


        HashMap<String, Integer> data = new HashMap<>();


        String sql = "SELECT * FROM NewscronConfiguration.language";
        if (specificLang != null) {
            sql = "SELECT * FROM NewscronConfiguration.language where id=" + specificLang.toString();
        }


        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.NewscronArchive); PreparedStatement stmt = conn.prepareStatement(sql);) {
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    data.put(rs.getString("name"), rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return data;

    }


    private Map<String, Integer> getCategories(int laguageID) {


        HashMap<String, Integer> data = new HashMap<>();


        String sql = "SELECT C.name,C.id FROM NewscronArchive.article AS A join NewscronConfiguration.category AS C ON C.id=A.categoryID where A.languageID=? group by categoryID";
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.NewscronArchive); PreparedStatement stmt = conn.prepareStatement(sql);) {
            stmt.setInt(1, laguageID);
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    data.put(rs.getString("name"), rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return data;

    }

    private List<ArticleTrainingSet.Article> downloadSet(int laguageID, int categoryID, int limit) {

        return downloadSet(laguageID, categoryID, limit, false);
    }

    private List<ArticleTrainingSet.Article> downloadSet(int laguageID, int categoryID, int limit, boolean useLive) {

        List<ArticleTrainingSet.Article> data = new ArrayList<>();

        String dbName = "NewscronArchive.article";
        if (useLive) {
            dbName = "NewscronContent.article";
        }

        String sql = "Select title,snippet,URL from " + dbName + " where status=5 and CategoryID=? and LanguageID=? order by id desc limit ? ";
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.NewscronArchive); PreparedStatement stmt = conn.prepareStatement(sql);) {
            stmt.setInt(1, categoryID);
            stmt.setInt(2, laguageID);
            stmt.setInt(3, limit);
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

        if (data.size() < limit && useLive == false) {
            data.addAll(downloadSet(laguageID, categoryID, limit - data.size(), true));
        }


        return data;
    }


}
