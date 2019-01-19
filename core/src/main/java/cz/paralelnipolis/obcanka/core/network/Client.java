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
package cz.paralelnipolis.obcanka.core.network;

import com.google.gson.*;
import cz.paralelnipolis.obcanka.core.HttpUtils;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Iterator;

public class Client {

    public static final String SERVICE_PROVIDER_URL = "https://eop.eidentita.cz";
    public static final String GATEWAY_URL = "https://mweop.eidentita.cz";

    enum State {
        I, //Initialized
        O, //Info
        E, //Error
        T, //Time Out
        R, //Refused - most likely wrong IOK
        V, //Canceled
        S, //Processing, please wait
        A, //Processing, please wait
        B, //Processing, please wait
        C  //Processing, please wait
    }


    public State getSessionState(String mwId) {
        try {
            HttpUtils.PostResponse postResponse = HttpUtils.httpsPost(SERVICE_PROVIDER_URL + "/IPSTS.EOP/Login.aspx/GetSessionState", "{mwId: \""+ mwId+"\"}", buildBrowserHeaders());
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(postResponse.getBody());
            if (element.isJsonObject()) {
                JsonObject json = element.getAsJsonObject();
                JsonElement d = json.get("d");
                if (d.isJsonObject()) {
                    String sessionState = d.getAsJsonObject().get("sessionState").getAsString();
                    return State.valueOf(sessionState);
                }
            }
        } catch (ConnectException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getMWID() {
        try {
            HttpUtils.PostResponse postResponse = HttpUtils.httpsPost(SERVICE_PROVIDER_URL + "/IPSTS.EOP/Login.aspx/GetMwId", "{}", buildBrowserHeaders());
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(postResponse.getBody());
            if (element.isJsonObject()) {
                JsonObject json = element.getAsJsonObject();
                JsonElement d = json.get("d");
                if (d.isJsonObject()) {
                    return d.getAsJsonObject().get("mwId").getAsString();
                }
            }
        } catch (ConnectException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static HashMap<String, String> buildBrowserHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("accept","application/json, text/javascript, */*; q=0.01");
        headers.put("content-type","application/json");
        headers.put("user-agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");
        headers.put("x-requested-with","XMLHttpRequest");
        headers.put("origin", SERVICE_PROVIDER_URL);
        return headers;
    }

    private static HashMap<String, String> buildGatewayHeaders(String mwId) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("content-type","application/json; charset=UTF-8");
        headers.put("x-request-id", mwId);
        headers.put("origin", SERVICE_PROVIDER_URL);
        return headers;
    }



    private static String buildDeviceInfo() {
        return "{\n" +
                "  \"deviceInfo\": {\n" +
                "    \"appId\": \"IdIs_App_Win\",\n" +
                "    \"appVersion\": \"3.1.0.18314\",\n" +
                "    \"osVersion\": \"Microsoft Windows NT 10.0.17134.0\",\n" +
                "    \"deviceId\": \"DESKTOP\"\n" +
                "  }\n" +
                "}";
    }

    public GatewayResponse askGateway(String mwId, byte[] authScriptResult) {
        try {
            GatewayResponse response = new GatewayResponse();
            HttpUtils.PostResponse postResponse = HttpUtils.httpsPostIgnoreNotCertifiedConnection(GATEWAY_URL + "/authgtw/authentication/" +mwId + (authScriptResult != null  ? "/process" : "/init"), (authScriptResult != null  ? buildAuthScriptResultJSON(authScriptResult) : buildDeviceInfo()), buildGatewayHeaders(mwId));

            //parse response
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(postResponse.getBody());
            if (element.isJsonObject()) {
                JsonObject json = element.getAsJsonObject();
                JsonElement authScript = json.get("authScript");
                if (authScript != null && !authScript.isJsonNull()) {
                    response.setAuthScript(org.apache.commons.codec.binary.Base64.decodeBase64(authScript.getAsString()));
                }
                JsonElement expiration = json.get("expiration");
                if (expiration != null && !expiration.isJsonNull()) {
                    response.setExpiration(expiration.getAsString());
                }
                JsonElement wysiwys = json.get("wysiwys");
                if (wysiwys != null && !wysiwys.isJsonNull()) {
                    JsonObject w = wysiwys.getAsJsonObject();
                    response.buildWysiwyS(w.get("title").getAsString());

                    JsonElement d = w.get("detail");
                    if (d != null && !d.isJsonNull()) {
                        JsonArray array = d.getAsJsonArray();
                        Iterator<JsonElement> iterator = array.iterator();
                        while (iterator.hasNext()) {
                            JsonElement di = iterator.next();
                            response.addDetail(di.getAsJsonObject().get("label").getAsString(),di.getAsJsonObject().get("value").getAsString());
                        }
                    }
                }
                JsonElement isFinished = json.get("isFinished");
                if (isFinished != null && !isFinished.isJsonNull()) {
                    response.setFinished(isFinished.getAsBoolean());
                }

                JsonElement authenticationResult = json.get("authenticationResult");
                if (authenticationResult != null && !authenticationResult.isJsonNull()) {
                    response.setAuthenticationResult(authenticationResult.getAsLong());
                }
            }
            return response;
        } catch (ConnectException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String buildAuthScriptResultJSON(byte[] authScriptResult) {
        return "{\n" +
                "  \"authScriptResult\": \""+ org.apache.commons.codec.binary.Base64.encodeBase64String(authScriptResult).replace("/","_")+"\"\n" +
                "}";
    }

}
