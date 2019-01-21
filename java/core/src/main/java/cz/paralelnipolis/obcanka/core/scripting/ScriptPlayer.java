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
package cz.paralelnipolis.obcanka.core.scripting;

import cz.paralelnipolis.obcanka.core.network.Client;
import cz.paralelnipolis.obcanka.core.network.GatewayResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ScriptPlayer {
    private ScriptExecutor executor;
    private Client client;

    public ScriptPlayer(ScriptExecutor executor, Client client) {
        this.executor = executor;
        this.client = client;
    }

    static class UIEntryProvider implements cz.paralelnipolis.obcanka.core.scripting.IUIEntryProvider {
        @Override
        public String getPIN() {
            System.out.print("Please enter PIN[and press ENTER]:");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String pin = null;
            try {
                pin = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return pin;
        }
    };

    public boolean start() {
        UIEntryProvider ep = new UIEntryProvider();
        String mwid = client.getMWID();
        byte[] authScriptResult = null;
        boolean isFinished = false;
        do {
            GatewayResponse response = client.askGateway(mwid, authScriptResult);
            if (response == null) {
                break;
            }

            if (response.isFinished()) {
                long authenticationResult = response.getAuthenticationResult();
                if (authenticationResult == 0) {
                    // do nothing we need to continue
                }else if (authenticationResult == 1) {
                    return true;
                }else{
                    return false;
                }
            }

            // Display stuff
            GatewayResponse.WysiwyS wysiwys = response.getWysiwys();
            if (wysiwys != null) {
                String title = wysiwys.getTitle();
                System.out.println("========================= " + title + " =========================");
                List<GatewayResponse.Detail> details = wysiwys.getDetails();
                if (details != null) {
                    for (int i = 0; i < details.size(); i++) {
                        GatewayResponse.Detail detail = details.get(i);
                        System.out.println("===== " + detail.getLabel() + ":  " + detail.getValue());
                    }
                }
                System.out.println("========================= " + title + " =========================");
            }

            if (response.getAuthScript() != null) {
                ScriptEnvelope envelope = ScriptEnvelope.parse(response.getAuthScript());
                System.out.println("Received script to execute: " + envelope);
                ScriptResponse scriptResponse = executor.execute(envelope, ep);
                if (scriptResponse != null) {
                    authScriptResult = scriptResponse.toByteArray();
                } else {
                    //We didn't receive any response. Something went wrong.
                    return false;
                }

            }

        } while ( !isFinished );

        return false;
    }

}
