package ch.newsriver.util;

import ch.newsriver.data.html.AjaxHTML;
import ch.newsriver.data.html.HTML;
import ch.newsriver.util.http.HttpClientPool;
import ch.newsriver.util.text.InterruptibleCharSequence;
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
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.*;
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


    public static HTML getHTML(String url, boolean simulateMobile) throws IOException {

        HTML html = null;

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


            String htmlSrc;


            if (entity.getContentEncoding() != null) {

                htmlSrc = EntityUtils.toString(entity);

            } else if (http_ecoding_found != null && http_ecoding_found.find() && http_ecoding_found.group(1) != null) {

                htmlSrc = EntityUtils.toString(entity, http_ecoding_found.group(1).toLowerCase());

            } else {

                byte[] rawHTML = EntityUtils.toByteArray(entity);
                htmlSrc = new String(rawHTML, "utf-8");

                boolean econdingFound = false;

                String[] metas = StringUtils.substringsBetween(htmlSrc, "<meta", ">");
                Queue<String> metaQueue = null;
                if (metas != null) {
                    metaQueue = new LinkedList<>(Arrays.asList(metas));
                }


                while (metaQueue != null && !metaQueue.isEmpty()) {
                    Matcher meta_ecoding_found = meta_encoding_detector.matcher(new InterruptibleCharSequence(metaQueue.poll()));
                    if (meta_ecoding_found.find() && meta_ecoding_found.group(1) != null) {
                        htmlSrc = new String(rawHTML, meta_ecoding_found.group(1).toLowerCase());
                        metaQueue.clear();
                        econdingFound = true;
                    }
                }


                Queue<String> xmlsQueue = null;
                if (!econdingFound) {
                    String[] xmls = StringUtils.substringsBetween(htmlSrc, "<?xml", ">");
                    if (xmls != null) {
                        xmlsQueue = new LinkedList<>(Arrays.asList(xmls));
                    }
                }


                while (xmlsQueue != null && !xmlsQueue.isEmpty()) {
                    Matcher html_ecoding_found = encoding_detector.matcher(new InterruptibleCharSequence(xmlsQueue.poll()));
                    if (html_ecoding_found.find() && html_ecoding_found.group(1) != null) {
                        htmlSrc = new String(rawHTML, html_ecoding_found.group(1).toLowerCase());
                        xmlsQueue.clear();
                        econdingFound = true;
                    }
                }

                if (!econdingFound) {
                    CharsetDetector detector = new CharsetDetector();
                    detector.setText(htmlSrc.getBytes());
                    CharsetMatch match = detector.detect();
                    htmlSrc = new String(rawHTML, match.getName());
                }

            }
            EntityUtils.consumeQuietly(entity);

            if (htmlSrc != null) {
                html = new HTML();
                html.setRawHTML(htmlSrc);
            }

            return html;

        } catch (SocketTimeoutException ex) {
            throw new IOException(ex);
        } finally {
            httpGetRequest.releaseConnection();
        }

    }

    public static AjaxHTML getAjaxBasedHTML(String url, boolean extractDynamicLinks) throws IOException {
        AjaxHTML html = null;
        /*ChromeDriver driver = new ChromeDriver();
        try {
            driver.navigate().to(url);
            Wait<WebDriver> wait = new WebDriverWait(driver, 30);
            wait.until(_driver -> String.valueOf(((JavascriptExecutor) _driver).executeScript("return document.readyState")).equals("complete"));
            html = driver.getPageSource();
        } finally {
            driver.quit();
        }*/
        Set<String> urls = new HashSet<>();
        String htlmSrc = null;
        WebDriver driver = new RemoteWebDriver(new URL("http://46.4.71.105:31555"), DesiredCapabilities.phantomjs());
        try {

            driver.navigate().to(url);
            Wait<WebDriver> wait = new WebDriverWait(driver, 30);
            wait.until(_driver -> String.valueOf(((JavascriptExecutor) _driver).executeScript("return document.readyState")).equals("complete"));
            htlmSrc = driver.getPageSource();

            if (extractDynamicLinks) {

                String mailURL = driver.getCurrentUrl();
                int divFound = driver.findElements(By.tagName("div")).size();

                for (int pos = 0; pos < divFound; pos++) {
                    WebElement element = driver.findElements(By.tagName("div")).get(pos);
                    if (!element.isDisplayed() || !element.isEnabled()) {
                        continue;
                    }
                    element.click();
                    //TODO: instead of waiting for the new url to load would be good to intercept the URL change, block it, save it, and continue the iteration
                    wait.until(_driver -> String.valueOf(((JavascriptExecutor) _driver).executeScript("return document.readyState")).equals("complete"));

                    if (!driver.getCurrentUrl().equals(mailURL)) {
                        urls.add(driver.getCurrentUrl());
                        driver.navigate().back();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error scanning ajax website", e);
        } finally {
            driver.quit();
        }

        if (htlmSrc != null) {
            html = new AjaxHTML();
            html.setRawHTML(htlmSrc);
            html.setDynamicURLs(urls);
        }


        return html;
    }


}
