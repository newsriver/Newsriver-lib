package ch.newsriver.data.website.alexa;

import ch.newsriver.dao.RedisPoolUtil;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import redis.clients.jedis.Jedis;
import sun.misc.BASE64Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by eliapalme on 03/04/16.
 */
public class AlexaClient {

    private static final Logger logger = LogManager.getLogger(AlexaClient.class);


    private static final String ACTION_NAME = "UrlInfo";
    private static final String RESPONSE_GROUP_NAME = "RelatedLinks,Categories,Rank,RankByCountry,UsageStats,ContactInfo,AdultContent,Speed,Language,OwnedDomains,LinksInCount,SiteData,Related,TrafficData,ContentData";
    private static final String SERVICE_HOST = "awis.amazonaws.com";
    private static final String AWS_BASE_URL = "http://" + SERVICE_HOST + "/?";
    private static final String HASH_ALGORITHM = "HmacSHA256";

    private static final String DATEFORMAT_AWS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private String accessKeyId="AKIAI3EISEPCGM7GPHXQ";
    private String secretAccessKey="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";



    public AlexaSiteInfo getSiteInfo(String domain){

        try {
            String query = this.buildQuery(domain);
            String toSign = "GET\n" + SERVICE_HOST + "\n/\n" + query;
            String signature = this.generateSignature(toSign);
            String uri = AWS_BASE_URL + query + "&Signature=" + URLEncoder.encode(signature, "UTF-8");


            Document document =  makeRequest(uri);

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xp = xpf.newXPath();


            AlexaSiteInfo alexaSiteInfo = new AlexaSiteInfo();
            alexaSiteInfo.setOwner(xp.evaluate("//Alexa/ContactInfo/OwnerName/text()",document.getDocumentElement()));
            alexaSiteInfo.setCountry(xp.evaluate("//Alexa/ContactInfo/PhysicalAddress/Country/text()",document.getDocumentElement()));
            alexaSiteInfo.setTitle(xp.evaluate("//Alexa/ContentData/SiteData/Title/text()",document.getDocumentElement()));
            alexaSiteInfo.setDescription(xp.evaluate("//Alexa/ContentData/SiteData/Description/text()",document.getDocumentElement()));
            alexaSiteInfo.setOnlineSince(xp.evaluate("//Alexa/ContentData/SiteData/OnlineSince/text()",document.getDocumentElement()));
            String loadTime = xp.evaluate("//Alexa/ContentData/Speed/MedianLoadTime/text()",document.getDocumentElement());
            if(loadTime !=null && !loadTime.isEmpty()){
                alexaSiteInfo.setLoadTime(Long.parseLong(loadTime));
            }
            alexaSiteInfo.setLanguage(xp.evaluate("//Alexa/ContentData/Language/Locale/text()",document.getDocumentElement()));

            String rank = xp.evaluate("//Alexa/TrafficData/Rank[1]/text()",document.getDocumentElement());
            if(rank !=null && !rank.isEmpty()){
                alexaSiteInfo.setGlobalRank(Long.parseLong(rank));
            }
            NodeList list= (NodeList) xp.evaluate("//Alexa/TrafficData/RankByCountry/Country",document.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                String country = xp.evaluate("@Code",node);
                String countryRank = xp.evaluate("Rank/text()",node);
                if(countryRank !=null && !countryRank.isEmpty()){
                    alexaSiteInfo.getCountryRank().put(country,Long.parseLong(countryRank));
                }
            }

            return alexaSiteInfo;

        }catch (IOException e){
            logger.error("Unable to fetch publisher site info",e);
            return null;
        }catch (SignatureException e){
            logger.error("Unable to fetch publisher site info",e);
            return null;
        }catch (ParserConfigurationException e){
            logger.error("Unable to fetch publisher site info",e);
            return null;
        }catch (SAXException e){
            logger.error("Unable to fetch publisher site info",e);
            return null;
        }catch (XPathExpressionException e){
            logger.error("Unable to fetch publisher site info",e);
            return null;
        }

    }

    public Document makeRequest(String requestUrl) throws IOException, ParserConfigurationException, SAXException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        String xml = null;
        xml = getAlexaCache(requestUrl);
        if(xml!=null){
            return builder.parse(xml);
        }

        URL url = new URL(requestUrl);
        URLConnection conn = url.openConnection();
        try(InputStream in = conn.getInputStream()){
            StringWriter writer = new StringWriter();
            IOUtils.copy(in, writer, "utf-8");
            xml = writer.toString();
            Document document = builder.parse(xml);
            setAlexaCache(requestUrl,xml);
            return document;
        }
    }


    /**
     * Builds the query string
     */
    protected String buildQuery(String site)
            throws UnsupportedEncodingException {
        String timestamp = getTimestampFromLocalTime(Calendar.getInstance().getTime());

        Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("Action", ACTION_NAME);
        queryParams.put("ResponseGroup", RESPONSE_GROUP_NAME);
        queryParams.put("AWSAccessKeyId", accessKeyId);
        queryParams.put("Timestamp", timestamp);
        queryParams.put("Url", site);
        queryParams.put("SignatureVersion", "2");
        queryParams.put("SignatureMethod", HASH_ALGORITHM);

        String query = "";
        boolean first = true;
        for (String name : queryParams.keySet()) {
            if (first)
                first = false;
            else
                query += "&";

            query += name + "=" + URLEncoder.encode(queryParams.get(name), "UTF-8");
        }

        return query;
    }
    protected static String getTimestampFromLocalTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(DATEFORMAT_AWS);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }


    protected String generateSignature(String data)
            throws SignatureException {
        String result;
        try {
            // get a hash key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(
                    secretAccessKey.getBytes(), HASH_ALGORITHM);

            // get a hasher instance and initialize with the signing key
            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // base64-encode the hmac
            // result = Encoding.EncodeBase64(rawHmac);
            result = new BASE64Encoder().encode(rawHmac);

        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : "
                    + e.getMessage());
        }
        return result;
    }




    private final static String REDIS_KEY_PREFIX     = "alexaStite";
    private final static String REDIS_KEY_VERSION     = "1";

    private String getKey(String url){
        StringBuilder builder = new StringBuilder();
        return builder.append(REDIS_KEY_PREFIX).append(":")
                .append(REDIS_KEY_VERSION).append(":")
                .append(url).toString();
    }

    public String getAlexaCache(String url){
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.ALEXA_CACHE)) {
            return  jedis.get(getKey(url));
        }
    }

    public void setAlexaCache(String url,String html){
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.ALEXA_CACHE)) {
            jedis.set(getKey(url),html);
        }
    }

}
