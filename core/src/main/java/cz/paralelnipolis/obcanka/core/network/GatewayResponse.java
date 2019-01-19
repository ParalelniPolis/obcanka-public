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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GatewayResponse {
    private byte[] authScript;
    private WysiwyS wysiwys;
    private String expiration;
    private boolean isFinished;
    private long authenticationResult;

    public class WysiwyS {
        String title;
        List<Detail> details = new ArrayList<>();

        public String getTitle() {
            return title;
        }

        public List<Detail> getDetails() {
            return details;
        }

        @Override
        public String toString() {
            return "WysiwyS{" +
                    "title='" + title + '\'' +
                    ", details=" + details +
                    '}';
        }
    }

    public class Detail {
        String label;
        String value;

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Detail{" +
                    "label='" + label + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    public WysiwyS buildWysiwyS(String title) {
        wysiwys = new WysiwyS();
        wysiwys.title = title;
        return wysiwys;
    }
    public Detail addDetail(String label, String value) {
        Detail detail = new Detail();
        detail.label = label;
        detail.value = value;
        if (wysiwys == null) {
            wysiwys = new WysiwyS();
        }
        wysiwys.details.add(detail);
        return detail;
    }

    public byte[] getAuthScript() {
        return authScript;
    }

    public void setAuthScript(byte[] authScript) {
        this.authScript = authScript;
    }

    public WysiwyS getWysiwys() {
        return wysiwys;
    }

    public void setWysiwys(WysiwyS wysiwys) {
        this.wysiwys = wysiwys;
    }

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public long getAuthenticationResult() {
        return authenticationResult;
    }

    public void setAuthenticationResult(long authenticationResult) {
        this.authenticationResult = authenticationResult;
    }

    @Override
    public String toString() {
        return "GatewayResponse{" +
                "authScript=" + Arrays.toString(authScript) +
                ", wysiwys=" + wysiwys +
                ", expiration='" + expiration + '\'' +
                ", isFinished=" + isFinished +
                ", authenticationResult=" + authenticationResult +
                '}';
    }
}
