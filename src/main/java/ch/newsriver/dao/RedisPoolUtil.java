package ch.newsriver.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;


import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by eliapalme on 11/03/16.
 */
public class RedisPoolUtil {

    public enum DATABASES {

        VISITED_URLS(1),
        RESOLVED_URLS(2),
        ALEXA_CACHE(16);

        DATABASES(int code) {
            this.code = code;
        }
        protected int code;

        public int getCode() {
            return code;
        }

    };

    private static final Logger log = LogManager.getLogger(RedisPoolUtil.class);

    private static RedisPoolUtil instance = null;
    private static JedisSentinelPool pool;

    private boolean connected = false;

    private String ovveride = null;
    private String pass=null;
    private RedisPoolUtil() {
    }



    public static synchronized RedisPoolUtil getInstance() {

        if (instance == null) {
            instance = new RedisPoolUtil();
            instance.connect();
        }

        return instance;
    }

    public void connect() {

        //check if pool is not already been connected
        if (pool != null) {
            return;
        }

        //Initialize redis connection pool
        Properties redisConfig = new Properties();
        InputStream propertiesReaderRedisConfig = this.getClass().getResourceAsStream("/redisConfig.properties");

        try {
            redisConfig.load(propertiesReaderRedisConfig);
        } catch (IOException ex) {
            log.fatal( "Unable to read readis connection pool properties", ex);
        } catch (Exception ex) {
            log.fatal("Unable to start connection pool", ex);
        } finally {
            try {

                propertiesReaderRedisConfig.close();
            } catch (Exception ex) {
                log.fatal("RedisPoolUtil unable to read properties", ex);
            }
        }



        if (redisConfig.containsKey("directMaster")) {
            ovveride = redisConfig.getProperty("directMaster");
            pass= redisConfig.getProperty("password-cluster");
        } else {
            // Create and set a JedisPoolConfig
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            // Maximum active connections to Redis instance
            //poolConfig.setMaxActive(50);

            // and do nothing
            poolConfig.setMaxTotal(250);
            poolConfig.setMaxIdle(210);
            // Minimum number of idle connections to Redis
            // These can be seen as always open and ready to serve
            poolConfig.setMinIdle(50);

            // Idle connection checking period
            poolConfig.setTimeBetweenEvictionRunsMillis(60000);
            // Create the jedisPool

            poolConfig.setTestOnBorrow(true);

            Set<String> sentinels = new HashSet();
            sentinels.addAll(Arrays.asList(redisConfig.getProperty("sentinels").split(",")));

            pool = new JedisSentinelPool(redisConfig.getProperty("masterName"), sentinels, poolConfig, redisConfig.getProperty("password-cluster"));
        }

        connected = true;

    }

    public void destroy() {
        connected = false;
        if (pool != null) {
            pool.destroy();
        }
        instance = null;
    }

    public Jedis getResource(DATABASES database) {

        if (ovveride != null) {

            String[] ovveride_parts = ovveride.split(":");
            Jedis j = new Jedis(ovveride_parts[0], Integer.parseInt(ovveride_parts[1]), 1800);
            j.auth(pass);
            j.select(database.code);
            return j;
        }

        if (!connected) {
            log.fatal("RedisPoolUtil need to be connected before getting resoure!");
        }
        Jedis j = pool.getResource();
        j.select(database.code);
        return j;
    }

}
