package ch.newsriver.util.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Created by eliapalme on 14/03/16.
 *
 * Provides a pool of httpclient to fetch data from internet.
 * In addition thoese client are very "nice" and allow self signed certificates or very unsecure SSL connections
 *
 */
public class HttpClientPool {

    private static final Logger logger = LogManager.getLogger(HttpClientPool.class);

    private static CloseableHttpClient httpClient = null;
    private static PoolingHttpClientConnectionManager connectionManager;

    public static final int MAXIMUM_CONNECTIONS = 3000;
    public static final int MAXIMUM_CONNECTIONS_PER_ROUTE = 25;
    public static final int MAXIMUM_REDIRECTS = 10;
    public static final int HTTP_TIMEOUT = 30000;
    public static final int SOKET_TIMEOUT = 30000;

    private static class TrustAllStrategy  implements TrustStrategy {
        @Override
        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            return true;
        }

    }


    public static void initialize() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {


        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustAllStrategy());
        SSLContext sslcontext = builder.build();
        sslcontext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, new java.security.SecureRandom());


        //Setting the list of algos will force SSL connector to first try with TLSv1.2 as some website no longer support TLSV1
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext,new String[]{"TLSv1.2","TLSv1.1","TLSv1","SSLv3"},null,SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();


        connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(MAXIMUM_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAXIMUM_CONNECTIONS_PER_ROUTE);


        RequestConfig defaultRequestConfig = RequestConfig.custom()
                /*.setCookieSpec(CookieSpecs.BEST_MATCH) due to a bug in version 4.3.1 of file BrowserCompatSpec we cannot use best match, note that bug will be fixed in version 4.3.2  */
                .setCookieSpec(CookieSpecs.STANDARD)
                .setExpectContinueEnabled(true)
                .setStaleConnectionCheckEnabled(true)
                .setConnectTimeout(HTTP_TIMEOUT)
                .setSocketTimeout(new Integer(SOKET_TIMEOUT))
                .setMaxRedirects(10)
                .setCircularRedirectsAllowed(true)
                .build();

        httpClient = HttpClientBuilder.create().setSSLSocketFactory(sslsf).setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).setConnectionManager(connectionManager)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .setDefaultRequestConfig(defaultRequestConfig)
                .build();


    }

    public static HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {

        if (httpClient == null) {
            logger.fatal("The connection pool has not been initialized, please call NewscronHttpClient.initialize(). The pool will now automaticcly be initialized.");
            try {
                initialize();
            } catch (NoSuchAlgorithmException ex) {
                logger.fatal("Unable to initialize httpclient", ex);
            } catch (KeyStoreException ex) {
                logger.fatal("Unable to initialize httpclient", ex);
            } catch (KeyManagementException ex) {
                logger.fatal("Unable to initialize httpclient", ex);
            }
        }

        return httpClient.execute(request, context);
    }

    public static void shutdown() {
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
        try {
            httpClient.close();
        } catch (IOException ex) {
        }
        httpClient = null;
    }

    public static org.apache.http.client.HttpClient getHttpClientInstance() {
        if (httpClient == null) {
            logger.fatal("The connection pool has not been initialized, please call NewscronHttpClient.initialize(). The pool will now automaticcly be initialized.");
        }

        try {
            if (connectionManager.getTotalStats().getLeased() > (connectionManager.getTotalStats().getMax() * 0.9)) {
                connectionManager.closeExpiredConnections();
                connectionManager.closeIdleConnections(10, TimeUnit.SECONDS);
                logger.error("Need to clean HttpClientPool pool available:" + connectionManager.getTotalStats().getAvailable() + " max:" + connectionManager.getTotalStats().getMax());
            }
        } catch (Exception e) {
            logger.error("Unable to clean HttpClientPool connection pool", e);
        }

        return httpClient;
    }

}
