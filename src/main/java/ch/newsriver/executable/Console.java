package ch.newsriver.executable;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.HashMap;
import java.util.SortedMap;

/**
 * Created by eliapalme on 10/03/16.
 */
public class Console extends NanoHTTPD {

    static WeakReference<HashMap<String,SortedMap<Long,Long>>> metrics;


    public Console(int port,HashMap<String,SortedMap<Long,Long>> metrics) throws IOException {
        super(port);
        this.metrics = new WeakReference<HashMap<String,SortedMap<Long,Long>>>(metrics);
    }

    @Override
    public Response serve(IHTTPSession session) {

        StringBuilder body = new StringBuilder();
        body.append("<html><head><meta http-equiv=\"refresh\" content=\"1\"></head><body style='font-family: monospace'>");
        body.append(Main.getManifest().replace("\n","<br/>").replace(" ","&nbsp;"));
        body.append("<br/><table border='1'>");
        body.append("<tr><td><b>Metric</b></td><td>&nbsp;</td></tr>");

        for(String metric : metrics.get().keySet()){
            SortedMap<Long,Long> units = metrics.get().get(metric);

            int mesurments =   30;
            Long totalCount = 0l;
            long now = Duration.ofNanos(System.nanoTime()).getSeconds();
            for(int i=1;i<=mesurments;i++){
                totalCount +=  units.getOrDefault(now-i,0l);
            }

            body.append("<tr><td>"+metric+"</td><td>"+totalCount/mesurments+"[u/s]</td></tr>");
        }

        body.append("</table>");
        body.append("</body></html>");


        return newFixedLengthResponse(body.toString());

    }
}
