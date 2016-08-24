package ch.newsriver.util;

import ch.newsriver.util.http.HttpClientPool;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

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
