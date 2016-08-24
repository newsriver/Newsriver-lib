package ch.newsriver.util.url;


import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.util.PublicSuffixList;
import org.apache.http.conn.util.PublicSuffixListParser;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


/**
 * Created by eliapalme on 11/03/16.
 */
public class URLUtils {

    static final HashSet<String> FILTER_QUERY_PARAMS = new HashSet(Arrays.asList("utm_campaign", "utm_medium", "utm_source", "mod", "from", "ref", "utm_cid", "utm_content", "utm_hp_ref", "ncid", "feedName", "feedType", "track", "source", "localLinksEnabled", "cmp", "ir", "rss", "cmpid", "m", "rssfeed", "wprss"));
    static final String GOOGLE_BOT_FRIEND_PARAM = "_escaped_fragment_";
    static final String GOOGLE_BOT_FRIEND_REF = "!";
    private static final Logger logger = LogManager.getLogger(URLUtils.class);

    /*
     * !!! DON'T NORMALIZE THE SAME URL MORE THAN ONE TIME !!!
     */
    private final static String[] searchList = {"\t", "\u0081", "\u0085", "\u009D", "\u0080", "\u009C"};
    private final static String[] replacementList = {"", "", "", "", "", "",};


    static PublicSuffixList suffixList;
    static PublicSuffixMatcher matcher;

    static {

        String listname = "suffixlist.txt";
        try (InputStream inputStream = URLUtils.class.getClassLoader().getResourceAsStream(listname)) {

            //suffix list is optained from https://publicsuffix.org
            if (inputStream != null) {
                suffixList = new PublicSuffixListParser().parse(new InputStreamReader(inputStream, Consts.UTF_8));
            }
        } catch (Exception e) {
            logger.fatal("Unable to load public suffix List", e);
        }

        matcher = new PublicSuffixMatcher(suffixList.getRules(), suffixList.getExceptions());

    }

    public static String normalizeUrl(String dirtyUrlString, boolean encodeQuery) throws MalformedURLException {
        return normalizeUrl(dirtyUrlString, "", encodeQuery);
    }

    public static String normalizeUrl(String dirtyUrlString) throws MalformedURLException {
        return normalizeUrl(dirtyUrlString, true);
    }

    public static String normalizeUrl(String dirtyUrlString, String baseURLString) throws MalformedURLException {
        return normalizeUrl(dirtyUrlString, baseURLString, true);
    }

    /* In case of problem!
    private final static String[] searchList =      {"\t", "|" ,   " ",  "\u00A0", "[",   "]",   "\u0093", "\u009F", "\u0094", "\"",  "\u2009",    "\u00E2", "\u0060", "\u0081", "\u0085", "\u009D", "\u0080", "\u009C", "\u00C3", "\u00A9", "\u0092"};
    private final static String[] replacementList = {"",   "%7C", "%20", "%C2%A0", "%5B", "%5D", "%C2%93", "%C2%9F", "%C2%94", "%22", "%E2%80%89", "%E2"    ,"%60",    "",       "",       "",        ""   ,    "",      "%C3",    "%A9",    "%92"};
    */

    public static String normalizeUrl(String dirtyUrlString, String baseURLString, boolean encodeQuery) throws MalformedURLException {


        //In case the URL is protocol relative add http as default
        if (dirtyUrlString.toLowerCase().matches("^//.*")) {
            dirtyUrlString = "http:" + dirtyUrlString;
        }

        //if URL schema is missing like www.newscron.com instead of http://www.newscron.com
        //manually add the schema, this is most used for curators manual import.
        if (!dirtyUrlString.toLowerCase().matches("^\\w+://.*")) {
            dirtyUrlString = "http://" + dirtyUrlString;
        }

        dirtyUrlString = tryToFixUrlString(dirtyUrlString, encodeQuery); // Temporary fix!!!
        baseURLString = tryToFixUrlString(baseURLString, encodeQuery); // Temporary fix!!!


        if (!isAbsolute(dirtyUrlString, baseURLString)) {
            logger.info("The url is relative: " + dirtyUrlString + " BaseUrl:  " + (baseURLString != null ? baseURLString : "NULL"));
            throw new MalformedURLException("The url is relative: " + dirtyUrlString + " BaseUrl:  " + baseURLString != null ? baseURLString : "NULL");
        }

        URL absoluteURL = null;

        if (baseURLString != null && !baseURLString.isEmpty()) {
            URL baseURL = makeValidURL(baseURLString);
            absoluteURL = new URL(baseURL, dirtyUrlString.trim());
        } else if (dirtyUrlString != null) {
            absoluteURL = makeValidURL(dirtyUrlString.trim());
        }

        // God bless you if somebody changes the order of the following functions
        try {
            absoluteURL = cleanServiceParameters(absoluteURL);
        } catch (URISyntaxException ex) {
        }

        try {
            absoluteURL = makeGooglebotFriendly(absoluteURL);
        } catch (URISyntaxException ex) {
        }


        URL normalizedURL = URLNormalizerImpl.getNormalizedURL(absoluteURL);
        if (normalizedURL == null) {
            logger.fatal("It is not possible to normalize the url: " + absoluteURL.toString());
            throw new MalformedURLException("The URL string could not be normalized: " + absoluteURL.toString());
        }
        absoluteURL = normalizedURL;

        return StringUtils.strip(absoluteURL.toString(), "#");
    }

    private static String tryToFixUrlString(String urlString, boolean encodeQuery) {
        if (urlString == null) {
            return null;
        }

        urlString = StringUtils.replaceEach(urlString.trim(), searchList, replacementList);

        if (encodeQuery) {
            try {

                String[] parts = urlString.split("#");
                parts[0] = URIUtil.encodeQuery(parts[0]);

                return StringUtils.join(parts, "#");
            } catch (URIException ex) {
            }
        }

        return urlString;
    }

    // https://developers.google.com/webmasters/ajax-crawling/docs/getting-started
    private static URL makeGooglebotFriendly(URL url) throws URISyntaxException, MalformedURLException {
        if (url.getRef() != null && url.getRef().startsWith(GOOGLE_BOT_FRIEND_REF)) {
            String ref = url.getRef();
            ref = ref.replaceFirst(GOOGLE_BOT_FRIEND_REF, "");

            String queryString = "";
            if (url.getQuery() != null && !url.getQuery().isEmpty()) {
                queryString = url.getQuery() + "&" + GOOGLE_BOT_FRIEND_PARAM + "=" + ref;
            } else {
                queryString = GOOGLE_BOT_FRIEND_PARAM + "=" + ref;
            }
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), queryString, "").toURL();
        }
        return url;
    }

    private static URL makeValidURL(final String url) throws MalformedURLException {
        try {
            return new URL(url.trim());
        } catch (final MalformedURLException e) {
        }

        // Add by default http protocol
        return new URL("http://" + url.trim());
    }

    private static boolean isAbsolute(String dirtyUrlString, String baseUrlString) {
        if (dirtyUrlString == null) {
            return false;
        }

        String cleanUrlString = dirtyUrlString.trim();
        URL baseUrl = null;

        //in case the feed link is not valid we don't care as the vaseUrl can also be null
        try {
            baseUrl = new URL(baseUrlString);
        } catch (MalformedURLException ex) {
        }

        try {

            //Next two steps are used to escape disalowed carachters
            URL testURL = new URL(baseUrl, cleanUrlString);
            URI testURI = new URI(testURL.getProtocol(), testURL.getUserInfo(), testURL.getHost(), testURL.getPort(), testURL.getPath(), testURL.getQuery(), testURL.getRef());

            if (!testURI.isAbsolute()) {
                logger.fatal("Unable to generate absolute link for: " + cleanUrlString);
                return false;
            }
        } catch (URISyntaxException ex) {
            return false;
        } catch (MalformedURLException ex) {
            return false;
        }
        return true;
    }

    private static URL cleanServiceParameters(URL url) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(url.toURI());
        List<NameValuePair> params = uriBuilder.getQueryParams();
        uriBuilder.removeQuery();

        for (NameValuePair param : params) {
            if (FILTER_QUERY_PARAMS.contains(param.getName())) {
                continue;
            }
            //remove all empty params
            if (param.getValue() == null || param.getValue().isEmpty()) {
                continue;
            }
            uriBuilder.addParameter(param.getName(), param.getValue());
        }
        return uriBuilder.build().toURL();
    }

    public static String getDomainRoot(String url) {
        return matcher.getDomainRoot(url);
    }
}




