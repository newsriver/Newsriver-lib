package ch.newsriver.util;

import ch.newsriver.util.http.HttpClientPool;
import ch.newsriver.util.normalization.text.InterruptibleCharSequence;
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
public class ImageUtils {


    public static BufferedImage downloadImage(String url) throws IOException {

        HttpGet httpGetRequest = null;
        try {
            HttpContext context = new BasicHttpContext();
            httpGetRequest = new HttpGet(url);

            HttpEntity entity = HttpClientPool.execute(httpGetRequest, context).getEntity();
            BufferedImage image = ImageIO.read(entity.getContent());

            return image;

        } finally {
            httpGetRequest.releaseConnection();
        }
    }

}
