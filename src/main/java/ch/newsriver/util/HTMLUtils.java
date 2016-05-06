package ch.newsriver.util;

import ch.newsriver.util.http.HttpClientPool;
import ch.newsriver.util.normalization.text.InterruptibleCharSequence;
import com.google.common.base.Function;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eliapalme on 14/03/16.
 */
public class HTMLUtils {

    private static final Logger logger = LogManager.getLogger(HTMLUtils.class);


    private static final Pattern encoding_detector = Pattern.compile(".*encoding=\"([^\"]*)\".*");
    private static final Pattern meta_encoding_detector = Pattern.compile(".*charset=\"?([^\"'; ]+).*");
    private static final Pattern http_encoding_detector = Pattern.compile(".*charset=\"?([^\\\"'; ]+).*");




    public static String getHTML(String url, boolean simulateMobile) throws IOException {



        String userAgent;

        if (simulateMobile) {
            userAgent = "Mozilla/5.0 (iPhone; U; CPU iPhone OS 3_0 like Mac OS X; en-us) AppleWebKit/528.18 (KHTML, like Gecko) Version/4.0 Mobile/7A341 Safari/528.16";
        } else {
            userAgent = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
        }

        HttpGet httpGetRequest = null;

        try {

            HttpContext context = new BasicHttpContext();
            httpGetRequest = new HttpGet(url);
            httpGetRequest.addHeader("User-Agent", userAgent);


            HttpResponse resp = HttpClientPool.getHttpClientInstance().execute(httpGetRequest, context);
            HttpEntity entity = resp.getEntity();

            Matcher http_ecoding_found = null;
            if (resp.getFirstHeader("Content-Type") != null) {
                http_ecoding_found = http_encoding_detector.matcher(new InterruptibleCharSequence(resp.getFirstHeader("Content-Type").getValue()));
            }


            String html;



            if (entity.getContentEncoding() != null) {

                html = EntityUtils.toString(entity);

            } else if (http_ecoding_found != null && http_ecoding_found.find() && http_ecoding_found.group(1) != null) {

                html = EntityUtils.toString(entity, http_ecoding_found.group(1).toLowerCase());

            } else {

                byte[] rawHTML = EntityUtils.toByteArray(entity);
                html = new String(rawHTML, "utf-8");

                boolean econdingFound = false;

                String[] metas = StringUtils.substringsBetween(html, "<meta", ">");
                Queue<String> metaQueue = null;
                if(metas != null){
                    metaQueue =  new LinkedList<>(Arrays.asList(metas));
                }



                while(metaQueue!=null && !metaQueue.isEmpty()){
                    Matcher meta_ecoding_found = meta_encoding_detector.matcher(new InterruptibleCharSequence(metaQueue.poll()));
                    if (meta_ecoding_found.find() && meta_ecoding_found.group(1) != null) {
                        html = new String(rawHTML, meta_ecoding_found.group(1).toLowerCase());
                        metaQueue.clear();
                        econdingFound=true;
                    }
                }


                Queue<String> xmlsQueue = null;
                if(!econdingFound){
                    String[] xmls = StringUtils.substringsBetween(html, "<?xml", ">");
                    if(xmls != null){
                        xmlsQueue =  new  LinkedList<>(Arrays.asList(xmls));
                    }
                }


                while(xmlsQueue!=null && !xmlsQueue.isEmpty()){
                    Matcher html_ecoding_found = encoding_detector.matcher(new InterruptibleCharSequence(xmlsQueue.poll()));
                    if (html_ecoding_found.find() && html_ecoding_found.group(1) != null) {
                        html = new String(rawHTML, html_ecoding_found.group(1).toLowerCase());
                        xmlsQueue.clear();
                        econdingFound=true;
                    }
                }

                if(!econdingFound){
                    CharsetDetector detector = new CharsetDetector();
                    detector.setText(html.getBytes());
                    CharsetMatch match = detector.detect();
                    html = new String(rawHTML, match.getName());
                }

            }
            EntityUtils.consumeQuietly(entity);

            return html;

        } catch (SocketTimeoutException ex) {
            throw new IOException(ex);
        } finally {
            httpGetRequest.releaseConnection();
        }

    }

    public static String getAjaxBasedHTML(String url) throws IOException {
        String html = null;
        ChromeDriver driver = new ChromeDriver();
        try {
            driver.navigate().to(url);
            Wait<WebDriver> wait = new WebDriverWait(driver, 30);
            wait.until(_driver -> String.valueOf(((JavascriptExecutor) _driver).executeScript("return document.readyState")).equals("complete"));
            html = driver.getPageSource();
        } finally {
            driver.quit();
        }
        return html;
    }



}
