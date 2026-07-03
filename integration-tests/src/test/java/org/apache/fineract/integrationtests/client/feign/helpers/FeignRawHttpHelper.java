/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.integrationtests.client.feign.helpers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.fineract.integrationtests.ConfigProperties;

public final class FeignRawHttpHelper {

    private static final String BASE_URL = ConfigProperties.Backend.PROTOCOL + "://" + ConfigProperties.Backend.HOST + ":"
            + ConfigProperties.Backend.PORT + "/fineract-provider/api/v1";
    private static final String AUTH_HEADER = "Basic " + Base64.getEncoder()
            .encodeToString((ConfigProperties.Backend.USERNAME + ":" + ConfigProperties.Backend.PASSWORD).getBytes(StandardCharsets.UTF_8));
    private static final String TENANT_ID = "default";

    private FeignRawHttpHelper() {}

    public static String put(String path, String jsonBody) {
        return execute("PUT", path, jsonBody);
    }

    public static String post(String path, String jsonBody) {
        return execute("POST", path, jsonBody);
    }

    private static String execute(String method, String path, String jsonBody) {
        try {
            URI uri = URI.create(BASE_URL + path);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();

            if (conn instanceof HttpsURLConnection httpsConn) {
                disableSslVerification(httpsConn);
            }

            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", AUTH_HEADER);
            conn.setRequestProperty("Fineract-Platform-TenantId", TENANT_ID);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String response = new String((status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream()).readAllBytes(),
                    StandardCharsets.UTF_8);

            if (status < 200 || status >= 300) {
                throw new RuntimeException("HTTP " + status + " " + method + " " + path + ": " + response);
            }

            return response;
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute " + method + " " + path, e);
        }
    }

    private static void disableSslVerification(HttpsURLConnection conn) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[] { new X509TrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new SecureRandom());
            conn.setSSLSocketFactory(sc.getSocketFactory());
            conn.setHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to disable SSL verification", e);
        }
    }
}
