package ch.newsriver.data.user;

import ch.newsriver.dao.JDBCPoolUtil;
import ch.newsriver.data.user.User;
import ch.newsriver.data.user.river.RiverBase;
import ch.newsriver.data.user.token.TokenFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.*;

/**
 * Created by eliapalme on 17/07/16.
 */
public class UserFactory {

    private static final Logger log = LogManager.getLogger(TokenFactory.class);
    private static ObjectMapper mapper = new ObjectMapper();


    public User getUser(long userId){

        String sql = "SELECT U.id,U.name,U.email,R.id,R.value FROM user AS U LEFT JOIN riverSetting as R ON R.userId=U.id WHERE id = ?";
        User user = new User();
        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {

            stmt.setLong(1,userId);
            try (ResultSet resultSet = stmt.executeQuery()) {

                if (resultSet.next()) {
                    user.setEmail(resultSet.getString("U.email"));
                    user.setName(resultSet.getString("U.name"));
                    do{
                        try{
                            RiverBase river = mapper.readValue(resultSet.getString("R.value"),RiverBase.class);
                            user.getRivers().put(resultSet.getLong("R.id"),river);
                        }catch (IOException e){
                            log.fatal("Unable to deserialize river",e);
                        }

                    }while (resultSet.next());
                }
            }

        }catch (SQLException e){
            log.fatal("Unable to generate token",e);
            return null;
        }

        return user;
    }


}
