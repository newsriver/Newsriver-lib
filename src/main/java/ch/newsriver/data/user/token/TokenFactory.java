package ch.newsriver.data.user.token;

import ch.newsriver.dao.JDBCPoolUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
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
            log.fatal( "Unable to read database connection pool properties", ex);
        } catch (Exception ex) {
            log.fatal( "Unable to start connection pool", ex);
        } finally {
            try {
                propertiesReader.close();
            } catch (IOException ex) {
                log.fatal(ex);
            }
        }
    }


    public String generateTokenAPI(long userId){

        TokenBase token = new TokenAPI();
        token.setTokenId(userId);

        String sql = "INSERT INTO token (userId,type) VALUES (?,'TokenAPI')";

        try (Connection conn = JDBCPoolUtil.getInstance().getConnection(JDBCPoolUtil.DATABASES.Sources); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {

            stmt.setLong(1,userId);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    token.setTokenId(generatedKeys.getLong(1));
                }
            }

        }catch (SQLException e){
            log.fatal("Unable to generate token",e);
            return null;
        }

        try {
            return encrypt(mapper.writeValueAsString(token));
        }catch (JsonProcessingException e){
            log.fatal(e);
            return null;
        }
    };


    public TokenBase verifyToken(String token){

        String dectypted;
        try {
            dectypted =  decrypt(token);
            return mapper.readValue(dectypted, TokenBase.class);
        }catch (IOException e){
            log.fatal(e);
            return null;
        }
    };




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
                log.fatal( "Unable to return cipher object to pool", ex);
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
            log.fatal( "Looks like we got an invalid token", ex);
        } catch (IllegalBlockSizeException ex) {
            log.fatal( "Looks like we got an invalid token", ex);
        } catch (Exception ex) {
            log.fatal( "Unable to get chipher from pool", ex);
        } finally {
            try {
                cipherDecryptPool.returnObject(cipherDecrypt);
            } catch (Exception ex) {
                log.fatal( "Unable to return cipher object to pool", ex);
            }
        }
        return null;
    }

    public static class CipherPoolFactory extends BasePoolableObjectFactory<Cipher> {


        private int mode;
        private SecretKeySpec key;

        public CipherPoolFactory(int mode, SecretKeySpec key){
            super();
            this.mode=mode;
            this.key=key;

        }


        @Override
        public Cipher makeObject() throws Exception {

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(mode, key);
            return cipher;

        }

    }

}
