/*
 * Copyright 2019 Paralelni Polis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.paralelnipolis.obcanka.core;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.ConnectException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

public class HttpUtils {

    public static TrustManager[] getTrustManagerWithTrustAllCertificates() {
        return new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @SuppressWarnings("all")
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @SuppressWarnings("all")
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
    }

    public static HostnameVerifier getHostnameVerifierWithAllHostAreValid() {
        return new HostnameVerifier() {
            @SuppressWarnings("all")
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }

    public static PostResponse httpsPost(String url, String body,Map<String, String> headers) throws ConnectException {
        PostResponse result = null;
        try {
            URL urlObj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) urlObj.openConnection();
            result = httpsPost(conn, body, headers);
        } catch (ConnectException e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return result;
    }

    public static PostResponse httpsPostIgnoreNotCertifiedConnection(String url, String body,Map<String, String> headers) throws ConnectException {
        PostResponse result = null;
        try {
            URL urlObj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) urlObj.openConnection();
            conn.setHostnameVerifier(getHostnameVerifierWithAllHostAreValid());
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, getTrustManagerWithTrustAllCertificates(), new SecureRandom());
            conn.setSSLSocketFactory(sslcontext.getSocketFactory());
            result = httpsPost(conn, body,headers);
        } catch (ConnectException e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return result;
    }

    private static PostResponse httpsPost(HttpsURLConnection conn, String body, Map<String, String> headers) throws ConnectException {
        StringBuilder sbResult = new StringBuilder();
        int statusCode = 0;
        boolean networkError = false;
        try {
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", "application/json");
            if (headers != null) {
                Set<Map.Entry<String, String>> entries = headers.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    conn.setRequestProperty(entry.getKey(),entry.getValue());
                }
            }
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            DataOutputStream wr = null;
            try {
                wr = new DataOutputStream(conn.getOutputStream());
                wr.write(body.getBytes());
                wr.flush();
            } catch (ConnectException e) {
                networkError = true;
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (wr != null) {
                    try {
                        wr.close();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
            if (!networkError) {
                statusCode = conn.getResponseCode();
                if (statusCode != 400 && statusCode != 404 && statusCode != 408 && statusCode != 410) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        sbResult.append(line).append("\n");
                    }
                    rd.close();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        if (networkError) {
            throw new ConnectException("httpsPost");
        }
        return new PostResponse(statusCode,sbResult.toString());
    }

    public static String downloadData(String url) {
        return (String) download(url, null, 1);
    }

    public static class PostResponse {
        private int statusCode;
        private String body;

        public PostResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        @Override
        public String toString() {
            return "PostResponse{" +
                    "statusCode=" + statusCode +
                    ", body='" + body + '\'' +
                    '}';
        }
    }

    private static Object download(String url, String filename, int attemps)  {
        if (attemps < 1) {
            System.out.println("downloadFile - stop download, attempts = " + attemps);
            return null;
        }
        InputStream content = null;
        try {
            // connect
            System.out.println("downloadFile - connecting");
            HttpGet httpget = new HttpGet(url);
            httpget.setProtocolVersion(HttpVersion.HTTP_1_0);
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(httpget);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                System.out.println("downloadFile - statusCode = " + statusCode);
                return null;
            }
            // download
            System.out.println("downloadFile - downloading");
            HttpEntity entity = response.getEntity();

            if (filename == null) {
                return EntityUtils.toString(entity);
            } else {
                File file = new File(filename);
                try {
                    if (!file.exists()) {
                        content = entity.getContent();
                        FileOutputStream fos = new FileOutputStream(file);
                        IOUtils.copy(content, fos);
                        fos.close();
                    }else {
                        System.out.println("downloadFile - deleting");
                        // delete and retry
                        if (file.delete()) {
                            System.out.println("downloadFile - deleted");
                            return download(url, filename, --attemps);
                        } else {
                            System.out.println("downloadFile - File \"" + filename + "\" cannot be deleted!");
                            return null;
                        }
                    }
                } finally {
                    downloadClose(content);
                }
                System.out.println("downloadFile - finished");
                return file;
            }

        } catch (Throwable e) {
            e.printStackTrace();

        } finally {
            downloadClose(content);
        }
        return null;
    }

    private static void downloadClose(InputStream content) {
        if (content != null) {
            try {
                content.close();
                System.out.println("downloadClose - closed");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
