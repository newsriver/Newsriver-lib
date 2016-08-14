package ch.newsriver.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by eliapalme on 11/03/16.
 */
public class JDBCPoolUtil {

    private static final Logger logger = LogManager.getLogger(JDBCPoolUtil.class);

    public enum DATABASES {

        Sources {
            @Override
            public String toString() {
                return "/sourceDatabase.properties";
            }
        },
        NewscronArchive {
            @Override
            public String toString() {
                return "/newscronArchiveDatabase.properties";
            }
        };
    }

    private static DataSource dsSources = null;
    private static DataSource dsNArchive = null;

    private static JDBCPoolUtil instance = null;

    public static synchronized JDBCPoolUtil getInstance() {

        if (instance == null) {
            instance = new JDBCPoolUtil();
        }

        return instance;
    }


    public void shutdown() {


        if (dsSources != null) {
            ((HikariDataSource) dsSources).close();
        }
        if (dsNArchive != null) {
            ((HikariDataSource) dsNArchive).close();
        }


    }



    public Connection getConnection(DATABASES database) {

        DataSource ds = null;

        switch (database) {
            case Sources:
                ds = dsSources;
                break;
            case NewscronArchive:
                ds = dsNArchive;
                break;
        }

        if (ds == null) {
                HikariConfig config = new HikariConfig(database.toString());
                ds = new HikariDataSource(config);
                switch (database) {

                    case Sources:
                        dsSources = ds;
                        break;
                    case NewscronArchive:
                        dsNArchive = ds;
                        break;
                }
        }


        if (ds != null) {
            try {
                return ds.getConnection();
            } catch (SQLException ex) {
                logger.fatal("Unable to retreive connection", ex);
            }
        }
        return null;


    }

}
