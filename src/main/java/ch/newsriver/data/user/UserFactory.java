package ch.newsriver.data.user;

import ch.newsriver.dao.JDBCPoolUtil;
import ch.newsriver.data.user.token.TokenFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by eliapalme on 17/07/16.
 */
public class UserFactory {

    private static final Logger log = LogManager.getLogger(TokenFactory.class);
    private static ObjectMapper mapper = new ObjectMapper();

    private static UserFactory instance;


    private UserFactory() {

    }

    static public synchronized UserFactory getInstance() {

        if (instance == null) {
            instance = new UserFactory();
        }
        return instance;
    }

    public boolean setSubscription(long userId, User.Subscription subscription) {


        String sql = "UPDATE user SET subscription=? WHERE id = ?";
        User user = new User();
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql)) {


            stmt.setString(1, subscription.name());
            stmt.setLong(2, userId);

            return stmt.executeUpdate() == 1;

        } catch (SQLException e) {
            log.fatal("Unable to update user subscription", e);
        }
        return false;
    }

    public User getUser(long userId) throws UserNotFountException {

        //Old version including saved rivers
        // String sql = "SELECT U.id,U.name,U.email,U.role,R.id,R.value FROM user AS U LEFT JOIN riverSetting as R ON R.userId=U.id WHERE U.id = ?";

        String sql = "SELECT U.id,U.name,U.email,U.role,U.usage,U.subscription FROM user AS U WHERE U.id = ?";
        User user = null;
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            try (ResultSet resultSet = stmt.executeQuery()) {

                if (resultSet.next()) {
                    user = new User();
                    user.setId(resultSet.getLong("U.id"));
                    user.setEmail(resultSet.getString("U.email"));
                    user.setName(resultSet.getString("U.name"));
                    user.setRole(User.Role.valueOf(resultSet.getString("U.role")));
                    user.setSubscription(User.Subscription.valueOf(resultSet.getString("U.subscription")));
                    user.setUsage(User.Usage.valueOf(resultSet.getString("U.usage")));
                    /* Old version including saved rivers
                    do {
                        try {
                            String riverStr = resultSet.getString("R.value");
                            if(!resultSet.wasNull()) {
                                RiverBase river = mapper.readValue(riverStr, RiverBase.class);
                                user.getRivers().put(resultSet.getLong("R.id"), river);
                            }
                        } catch (IOException e) {
                            log.fatal("Unable to deserialize river", e);
                        }

                    } while (resultSet.next());
                    */
                } else {
                    throw new UserNotFountException("Unable to finde user with id:" + userId);
                }
            }

        } catch (SQLException e) {
            log.fatal("Unable to retreive user", e);
            return null;
        } catch (Exception e) {
            log.fatal("Unable to retreive user", e);
            return null;
        }

        return user;
    }


    public static class UserNotFountException extends Exception {
        public UserNotFountException(String message) {
            super(message);
        }
    }


}
