package ch.newsriver.executable;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.SortedMap;

/**
 * Created by eliapalme on 10/03/16.
 */
public class Console extends NanoHTTPD {

    static WeakReference<HashMap<Main.Metric, SortedMap<Long, Long>>> metrics;


    public Console(int port, HashMap<Main.Metric, SortedMap<Long, Long>> metrics) throws IOException {
        super(port);
        this.metrics = new WeakReference<HashMap<Main.Metric, SortedMap<Long, Long>>>(metrics);
    }

    @Override
    public Response serve(IHTTPSession session) {

        StringBuilder body = new StringBuilder();
        body.append("<html><head><meta http-equiv=\"refresh\" content=\"1\"></head><body style='font-family: monospace'>");
        body.append(Main.getManifest().replace("\n", "<br/>").replace(" ", "&nbsp;"));
        body.append("<br/><table border='1'>");
        body.append("<tr><td><b>Metric</b></td><td>&nbsp;</td></tr>");

        for (Main.Metric metric : metrics.get().keySet()) {
            SortedMap<Long, Long> units = metrics.get().get(metric);

            int mesurments = 30;
            Long totalCount = 0l;


            long now = 0;
            String unit = "";
            if (metric.getUnit() == ChronoUnit.SECONDS) {
                now = Duration.ofNanos(System.nanoTime()).getSeconds();
                unit = "[u/s]";
            } else if (metric.getUnit() == ChronoUnit.MINUTES) {
                now = Duration.ofNanos(System.nanoTime()).toMinutes();
                unit = "[u/m]";
            }

            for (int i = 1; i <= mesurments; i++) {
                totalCount += units.getOrDefault(now - i, 0l);
            }

            body.append("<tr><td>" + metric.getName() + "</td><td>" + totalCount / mesurments + unit + "</td></tr>");
        }

        body.append("</table>");
        body.append("</body></html>");


        return newFixedLengthResponse(body.toString());

    }
}
