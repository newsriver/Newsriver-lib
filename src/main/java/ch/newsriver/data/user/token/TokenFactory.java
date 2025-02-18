package ch.newsriver.data.user.token;

import ch.newsriver.dao.JDBCPoolUtil;
import ch.newsriver.dao.RedisPoolUtil;
import ch.newsriver.data.user.User;
import ch.newsriver.data.user.UserFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Created by eliapalme on 16/07/16.
 */
public class TokenFactory {

    private static final Logger log = LogManager.getLogger(TokenFactory.class);
    private static ObjectMapper mapper = new ObjectMapper();

    private static ObjectPool<Cipher> cipherEncryptPool;
    private static ObjectPool<Cipher> cipherDecryptPool;


    static {


        Properties tokenKeyProperty = new Properties();
        InputStream propertiesReader = TokenFactory.class.getResourceAsStream("/ch/newsriver/data/user/token/tokenKey.properties");
        try {

            tokenKeyProperty.load(propertiesReader);
            byte[] tokenKeyByte = tokenKeyProperty.getProperty("key").getBytes("ASCII");
            SecretKeySpec tokenKey = new SecretKeySpec(tokenKeyByte, "AES");

            cipherEncryptPool = new GenericObjectPool<Cipher>(new CipherPoolFactory(Cipher.ENCRYPT_MODE, tokenKey), Integer.parseInt(tokenKeyProperty.getProperty("encryptCipherPoolSize")));
            cipherDecryptPool = new GenericObjectPool<Cipher>(new CipherPoolFactory(Cipher.DECRYPT_MODE, tokenKey), Integer.parseInt(tokenKeyProperty.getProperty("decryptCipherPoolSize")));

        } catch (IOException ex) {
            log.fatal("Unable to read database connection pool properties", ex);
        } catch (Exception ex) {
            log.fatal("Unable to start connection pool", ex);
        } finally {
            try {
                propertiesReader.close();
            } catch (IOException ex) {
                log.fatal(ex);
            }
        }
    }


    public static boolean apiRateLimitExceeded(String token) {
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.API)) {
            Long counter = (Long) jedis.incr("APIRateLimit:v1:"+token);
            //if counter was 0 start a new 15min window
            if(counter<=1){
                jedis.expire("APIRateLimit:v1:"+token,900);
            }
            return counter > 225;
        }
    }


    public String generateTokenAPI(long userId) {

        TokenBase token = new TokenAPI();
        token.setUserId(userId);

        String sql = "INSERT INTO token (userId,type) VALUES (?,'TokenAPI')";

        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {

            stmt.setLong(1, userId);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    token.setTokenId(generatedKeys.getLong(1));
                }
            }

        } catch (SQLException e) {
            log.fatal("Unable to generate token", e);
            return null;
        }

        try {
            return encrypt(mapper.writeValueAsString(token));
        } catch (JsonProcessingException e) {
            log.fatal(e);
            return null;
        }
    }

    ;


    public TokenBase verifyToken(String token) {

        String dectypted;
        try {
            dectypted = decrypt(token);
            if (dectypted != null) {
                return mapper.readValue(dectypted, TokenBase.class);
            }
        } catch (IOException e) {
            log.fatal(e);
        }
        return null;
    }

    public User getTokenUser(String tokenStr) throws TokenVerificationException, UserFactory.UserNotFountException {

        if (tokenStr == null) {
            throw new TokenVerificationException("Authorization token missing");
        }

        TokenFactory tokenFactory = new TokenFactory();
        TokenBase token = tokenFactory.verifyToken(tokenStr);

        if (token == null) {
            throw new TokenVerificationException("Invalid token");
        }
        
        return UserFactory.getInstance().getUser(token.getUserId());
    }


    protected String encrypt(String string) {

        Cipher cipherEncrypt = null;
        try {

            cipherEncrypt = cipherEncryptPool.borrowObject();

            byte[] bytes = string.getBytes();
            bytes = cipherEncrypt.doFinal(bytes);

            return Base64.encodeBase64URLSafeString(bytes);


        } catch (Exception ex) {
            log.fatal("Unable to get chipher from pool", ex);
        } finally {
            try {
                cipherEncryptPool.returnObject(cipherEncrypt);
            } catch (Exception ex) {
                log.fatal("Unable to return cipher object to pool", ex);
            }
        }
        return null;
    }

    protected String decrypt(String encodedString) throws IOException {

        byte[] tokenByte = Base64.decodeBase64(encodedString);

        Cipher cipherDecrypt = null;
        try {

            cipherDecrypt = cipherDecryptPool.borrowObject();

            return new String(cipherDecrypt.doFinal(tokenByte));

        } catch (BadPaddingException ex) {
            log.fatal("Looks like we got an invalid token", ex);
        } catch (IllegalBlockSizeException ex) {
            log.fatal("Looks like we got an invalid token", ex);
        } catch (Exception ex) {
            log.fatal("Unable to get chipher from pool", ex);
        } finally {
            try {
                cipherDecryptPool.returnObject(cipherDecrypt);
            } catch (Exception ex) {
                log.fatal("Unable to return cipher object to pool", ex);
            }
        }
        return null;
    }

    public static class CipherPoolFactory extends BasePoolableObjectFactory<Cipher> {


        private int mode;
        private SecretKeySpec key;

        public CipherPoolFactory(int mode, SecretKeySpec key) {
            super();
            this.mode = mode;
            this.key = key;

        }


        @Override
        public Cipher makeObject() throws Exception {

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(mode, key);
            return cipher;

        }

    }

    public static class TokenVerificationException extends Exception {
        public TokenVerificationException(String message) {
            super(message);
        }
    }

}
