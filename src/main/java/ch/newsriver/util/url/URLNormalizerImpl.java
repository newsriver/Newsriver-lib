package ch.newsriver.util.url;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.net.*;
import java.util.LinkedList;

/**
 * Created by eliapalme on 11/03/16.
 */

/**
 * URL normalizer
 * <p>
 * http://en.wikipedia.org/wiki/URL_normalization
 */
public class URLNormalizerImpl {

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

            final LinkedList<Pair<String, String>> params = createParameterMap(canonicalURL.getQuery());
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
    private static LinkedList<Pair<String, String>> createParameterMap(final String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }

        final LinkedList<Pair<String, String>> params = new LinkedList();


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
     *               order.
     * @return Canonical form of query string.
     */
    private static String canonicalize(final LinkedList<Pair<String, String>> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        final StringBuffer sb = new StringBuffer(100);
        for (Pair<String, String> pair : params) {
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
