package com.whatsapp.s40.net;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class HttpHelper {

    public static String get(String url) throws Exception {
        int maxRedirects = 3;
        while (maxRedirects-- > 0) {
            HttpConnection conn = null;
            InputStream is = null;
            try {
                conn = (HttpConnection) Connector.open(url);
                conn.setRequestMethod(HttpConnection.GET);
                conn.setRequestProperty("User-Agent", "Nokia-S40-WhatsApp/1.0");
                conn.setRequestProperty("Bypass-Tunnel-Reminder", "true");

                int rc = conn.getResponseCode();
                if (rc == HttpConnection.HTTP_MOVED_TEMP || rc == HttpConnection.HTTP_MOVED_PERM || rc == HttpConnection.HTTP_SEE_OTHER || rc == 307 || rc == 308) {
                    String loc = conn.getHeaderField("Location");
                    if (loc != null && loc.length() > 0) {
                        url = loc;
                        continue;
                    }
                }
                if (rc != HttpConnection.HTTP_OK) {
                    throw new Exception("HTTP " + rc);
                }

                is = conn.openInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[256];
                int read = 0;
                while ((read = is.read(buf)) != -1) {
                    bos.write(buf, 0, read);
                }
                return new String(bos.toByteArray(), "UTF-8");
            } finally {
                if (is != null) {
                    try { is.close(); } catch (Exception e) {}
                }
                if (conn != null) {
                    try { conn.close(); } catch (Exception e) {}
                }
            }
        }
        throw new Exception("Too many redirects");
    }

    public static String post(String url, String jsonBody) throws Exception {
        int maxRedirects = 3;
        while (maxRedirects-- > 0) {
            HttpConnection conn = null;
            OutputStream os = null;
            InputStream is = null;
            try {
                conn = (HttpConnection) Connector.open(url);
                conn.setRequestMethod(HttpConnection.POST);
                conn.setRequestProperty("User-Agent", "Nokia-S40-WhatsApp/1.0");
                conn.setRequestProperty("Bypass-Tunnel-Reminder", "true");
                conn.setRequestProperty("Content-Type", "application/json");

                byte[] bytes = jsonBody.getBytes("UTF-8");
                conn.setRequestProperty("Content-Length", Integer.toString(bytes.length));

                os = conn.openOutputStream();
                os.write(bytes);
                os.flush();

                int rc = conn.getResponseCode();
                if (rc == HttpConnection.HTTP_MOVED_TEMP || rc == HttpConnection.HTTP_MOVED_PERM || rc == HttpConnection.HTTP_SEE_OTHER || rc == 307 || rc == 308) {
                    String loc = conn.getHeaderField("Location");
                    if (loc != null && loc.length() > 0) {
                        url = loc;
                        continue;
                    }
                }
                if (rc != HttpConnection.HTTP_OK && rc != HttpConnection.HTTP_CREATED) {
                    throw new Exception("HTTP " + rc);
                }

                is = conn.openInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[256];
                int read = 0;
                while ((read = is.read(buf)) != -1) {
                    bos.write(buf, 0, read);
                }
                return new String(bos.toByteArray(), "UTF-8");
            } finally {
                if (os != null) {
                    try { os.close(); } catch (Exception e) {}
                }
                if (is != null) {
                    try { is.close(); } catch (Exception e) {}
                }
                if (conn != null) {
                    try { conn.close(); } catch (Exception e) {}
                }
            }
        }
        throw new Exception("Too many redirects");
    }
}
