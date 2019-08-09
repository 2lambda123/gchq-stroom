package stroom.proxy.app;


import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendSampleProxyDataZip {
    private SendSampleProxyDataZip() {
        // Private constructor.
    }

    public static void main(final String[] args) {
        try {
            String urlS = "http://localhost:8980/stroom-proxy/datafeed";

            URL url = new URL(urlS);

            for (int i = 0; i < 1; i++) {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setHostnameVerifier((arg0, arg1) -> true);
                }

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/audit");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setChunkedStreamingMode(100);
                connection.addRequestProperty("Feed", "VERY_SIMPLE_DATA_SPLITTER-EVENTS");
                connection.addRequestProperty("Compression", "zip");

                connection.connect();
                OutputStream out = connection.getOutputStream();
                out.close();

                int response = connection.getResponseCode();
                String msg = connection.getResponseMessage();

                connection.disconnect();

                System.out.println("Client Got Response " + response);
                if (msg != null && !msg.isEmpty()) {
                    System.out.println(msg);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
