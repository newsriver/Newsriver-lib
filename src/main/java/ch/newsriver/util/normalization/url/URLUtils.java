package ch.newsriver.util.normalization.url;


import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by eliapalme on 11/03/16.
 */
public class URLUtils {

    private static final Logger logger = LogManager.getLogger(URLUtils.class);

    static final HashSet<String> FILTER_QUERY_PARAMS = new HashSet(Arrays.asList("utm_campaign", "utm_medium", "utm_source","mod","from","ref","utm_cid","utm_content","utm_hp_ref","ncid","feedName","feedType","track","source","localLinksEnabled","cmp","ir","rss","cmpid","m","rssfeed","wprss"));
    static final String GOOGLE_BOT_FRIEND_PARAM = "_escaped_fragment_";
    static final String GOOGLE_BOT_FRIEND_REF = "!";

    /*
     * !!! DON'T NORMALIZE THE SAME URL MORE THAN ONE TIME !!!
     */

    public static String normalizeUrl(String dirtyUrlString, boolean encodeQuery) throws MalformedURLException {
        return normalizeUrl(dirtyUrlString, "", encodeQuery);
    }
    public static String normalizeUrl(String dirtyUrlString) throws MalformedURLException {
        return normalizeUrl(dirtyUrlString, true);
    }

    public static String normalizeUrl(String dirtyUrlString, String baseURLString) throws MalformedURLException  {
        return normalizeUrl(dirtyUrlString, baseURLString, true);
    }

    public static String normalizeUrl(String dirtyUrlString, String baseURLString, boolean encodeQuery) throws MalformedURLException  {


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
        } catch (URISyntaxException ex) { }

        try {
            absoluteURL = makeGooglebotFriendly(absoluteURL);
        } catch (URISyntaxException ex) { }


        URL normalizedURL = URLNormalizerImpl.getNormalizedURL(absoluteURL);
        if (normalizedURL == null) {
            logger.fatal("It is not possible to normalize the url: " + absoluteURL.toString());
            throw new MalformedURLException("The URL string could not be normalized: " + absoluteURL.toString());
        }
        absoluteURL = normalizedURL;

        return StringUtils.strip(absoluteURL.toString(), "#");
    }

    /* In case of problem!
    private final static String[] searchList =      {"\t", "|" ,   " ",  "\u00A0", "[",   "]",   "\u0093", "\u009F", "\u0094", "\"",  "\u2009",    "\u00E2", "\u0060", "\u0081", "\u0085", "\u009D", "\u0080", "\u009C", "\u00C3", "\u00A9", "\u0092"};
    private final static String[] replacementList = {"",   "%7C", "%20", "%C2%A0", "%5B", "%5D", "%C2%93", "%C2%9F", "%C2%94", "%22", "%E2%80%89", "%E2"    ,"%60",    "",       "",       "",        ""   ,    "",      "%C3",    "%A9",    "%92"};
    */

    private final static String[] searchList =      {"\t", "\u0081", "\u0085", "\u009D", "\u0080", "\u009C"};
    private final static String[] replacementList = {"",   "",       "",       "",       ""   ,    "",};

    private static String tryToFixUrlString(String urlString, boolean encodeQuery) {
        if (urlString == null) {
            return null;
        }

        urlString = StringUtils.replaceEach(urlString.trim(), searchList, replacementList);

        if (encodeQuery) {
            try {

                String[] parts = urlString.split("#");
                parts[0] = URIUtil.encodeQuery(parts[0]);

                return StringUtils.join(parts,"#");
            } catch (URIException ex) { }
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
        }
        catch (final MalformedURLException e){ }

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
        } catch (MalformedURLException ex) {}

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

        for(NameValuePair param : params){
            if(FILTER_QUERY_PARAMS.contains(param.getName())) {
                continue;
            }
            //remove all empty params
            if(param.getValue() == null || param.getValue().isEmpty()){
                continue;
            }
            uriBuilder.addParameter(param.getName(), param.getValue());
        }
        return uriBuilder.build().toURL();
    }
}



/**
 * URL normalizer
 *
 * http://en.wikipedia.org/wiki/URL_normalization
 *
 *
 */

class URLNormalizerImpl {

    public static URL getNormalizedURL(URL url) {
        try {
            URL canonicalURL = new URL(url.toString());
            String path = canonicalURL.getPath();

            /*
             * Normalize: no empty segments (i.e., "//"), no segments equal to
             * ".", and no segments equal to ".." that are preceded by a segment
             * not equal to "..".
             */
            path = new URI(path).normalize().toString();

            /*
             * Convert '//' -> '/'
             */
            int idx = path.indexOf("//");
            while (idx >= 0) {
                path = path.replace("//", "/");
                idx = path.indexOf("//");
            }

            /*
             * Drop starting '/../'
             */
            while (path.startsWith("/../")) {
                path = path.substring(3);
            }

            /*
             * Trim
             */
            path = path.trim();

            final LinkedList<Pair<String,String>> params = createParameterMap(canonicalURL.getQuery());
            final String queryString;

            if (params != null && params.size() > 0) {
                String canonicalParams = canonicalize(params);
                queryString = (canonicalParams.isEmpty() ? "" : "?" + canonicalParams);
            } else {
                queryString = "";
            }

            /*
             * Add starting slash if needed
             */
            if (path.length() == 0) {
                path = "/" + path;
            }

            /*
             * Drop default port: example.com:80 -> example.com
             */
            int port = canonicalURL.getPort();
            if (port == canonicalURL.getDefaultPort()) {
                port = -1;
            }

            /*
             * Lowercasing protocol and host
             */
            String protocol = canonicalURL.getProtocol().toLowerCase();
            String host = canonicalURL.getHost().toLowerCase();

            /*
             * Add
             */

            String pathAndQueryString = normalizePath(path) + queryString;

            return new URL(protocol, host, port, pathAndQueryString);

        } catch (MalformedURLException ex) {
            return null;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /**
     * Takes a query string, separates the constituent name-value pairs, and
     * stores them in a SortedMap ordered by lexicographical order.
     *
     * @return Null if there is no query string.
     */
    private static LinkedList<Pair<String,String>> createParameterMap(final String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }

        final LinkedList<Pair<String,String>> params = new LinkedList();



        final String[] pairs = queryString.split("&");


        for (final String pair : pairs) {
            if (pair.length() == 0) {
                continue;
            }

            String[] tokens = pair.split("=", 2);
            switch (tokens.length) {
                case 1:
                    if (pair.charAt(0) == '=') {
                        params.add(new ImmutablePair("", tokens[0]));
                    } else {
                        params.add(new ImmutablePair(tokens[0], ""));
                    }
                    break;
                case 2:
                    params.add(new ImmutablePair(tokens[0], tokens[1]));
                    break;
            }
        }
        return params;
    }

    /**
     * Canonicalize the query string.
     *
     * @param params Parameter name-value pairs in lexicographical
     * order.
     * @return Canonical form of query string.
     */
    private static String canonicalize(final LinkedList<Pair<String,String>> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        final StringBuffer sb = new StringBuffer(100);
        for (Pair<String,String> pair : params) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(percentEncodeRfc3986(pair.getKey()));
            if (!pair.getValue().isEmpty()) {
                sb.append('=');

                if (pair.getKey().equals(URLUtils.GOOGLE_BOT_FRIEND_PARAM)) {
                    sb.append(pair.getValue());
                } else {
                    sb.append(percentEncodeRfc3986(pair.getValue()));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Percent-encode values according the RFC 3986. The built-in Java
     * URLEncoder does not encode according to the RFC, so we make the extra
     * replacements.
     *
     * @param string Decoded string.
     * @return Encoded string per RFC 3986.
     */
    private static String percentEncodeRfc3986(String string) {
        try {
            string = string.replace("+", "%2B");
            string = URLDecoder.decode(string, "UTF-8");
            string = URLEncoder.encode(string, "UTF-8");
            return string.replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (Exception e) {
            return string;
        }
    }

    private static String normalizePath(final String path) {
        return path.replace("%7E", "~").replace(" ", "%20");
    }

}
