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
package cz.paralelnipolis.obcanka.core.scripting.cmd;

import cz.paralelnipolis.obcanka.core.HexUtils;

import cz.paralelnipolis.obcanka.core.communication.CardException;
import cz.paralelnipolis.obcanka.core.communication.ICardInterface;
import cz.paralelnipolis.obcanka.core.communication.ICommandAPDU;
import cz.paralelnipolis.obcanka.core.communication.IResponseAPDU;
import cz.paralelnipolis.obcanka.core.scripting.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdAPDU extends AbstractCommand {
    public static final int ID = 65;

    private byte[] request;
    private List<Response> responses;
    private int errorFlags;
    private Label errorLabel;

    public CmdAPDU(Label label, byte[] request, List<Response> responses, int errorFlags, Label errorLabel) {
        super(ID, label);
        this.request = request;
        this.responses = responses;
        this.errorFlags = errorFlags;
        this.errorLabel = errorLabel;
    }


    public static ICommand parse(byte[] labelId, byte[] commandPayload) {
        List<Response> responses = new ArrayList<>();
        int errorFlags = 0;
        Label errorLabel = null;

        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(commandPayload));
            int requestSize = dis.readInt();
            byte[] request = new byte[requestSize];
            dis.read(request);
            while (dis.available() > 0) {
                int flags = HexUtils.byteToInt(dis.readByte());
                int responseSize = dis.readInt();
                if (doesntHaveResponseFlags(flags, ResponseFlag.Error.getId())) {
                    byte[] response = new byte[responseSize - 4];
                    dis.read(response);
                    byte[] lab = new byte[4];
                    dis.read(lab);
                    responses.add(new Response(response,flags,new Label(lab)));
                }else{
                    byte[] label = new byte[4];
                    dis.read(label);
                    errorLabel = new Label(label);
                    errorFlags = flags;
                }
            }
            dis.close();
            return new CmdAPDU(new Label(labelId), request, responses, errorFlags, errorLabel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getLabel() +": " + getClass().getSimpleName() + " {\n");
        sb.append("      request: " + HexUtils.bytesToHexStringWithSpaces(request) + "\n");
        if (errorFlags != 0) {
            sb.append("      errorFlags: " + errorFlags + " ");
            sb.append("      errorLabel: " + errorLabel + "\n");
        }
        for (int i = 0; i < responses.size(); i++) {
            Response response = responses.get(i);
            sb.append("      response: " + HexUtils.bytesToHexStringWithSpaces(response.getResponse()) + " (" + response.getFlags() + ") => " + response.getLabel()+"\n");
        }
        sb.append(" }");
        return sb.toString();
    }

    @Override
    public CommandResult execute(ScriptExecutor scriptExecutor) {
        ICardInterface ci = scriptExecutor.getCardInterface();
        ICommandAPDU c = ci.createCommand(request);
        IResponseAPDU r = null;
        try {
            r = ci.transmit(c);
            if (responses != null) {
                for (int i = 0; i < responses.size(); i++) {
                    Response condition = responses.get(i);
                    int flags = condition.getFlags();
                    boolean conditionResult = false;
                    byte[] pR = condition.getResponse();
                    if (doesntHaveResponseFlags(flags,ResponseFlag.CheckResponseData.getId())) {
                        if (doesntHaveResponseFlags(flags,ResponseFlag.MaskedCheck.getId())) {
                            if (pR.length == 2) {
                                //Only compare SW on equals
                                conditionResult = Arrays.equals(new byte[]{(byte) r.getSW1(), (byte) r.getSW2()}, pR);
                            }
                        }else if (pR.length == 4) {
                            final int m1 = pR[0] & 0xFF;
                            final int m2 = pR[1] & 0xFF;
                            final int mask = m1 << 8 | m2;

                            final int t1 = pR[3] & 0xFF;
                            final int t2 = pR[4] & 0xFF;
                            final int test = t1 << 8 | t2;

                            conditionResult = ((r.getSW() & mask)) == test;
                        }
                    }else{
                        if (doesntHaveResponseFlags(flags,ResponseFlag.MaskedCheck.getId())) {
                            conditionResult = Arrays.equals(r.getData(),condition.getResponse());
                        }else if ((condition.getResponse().length % 2 == 0) &&
                                (condition.getResponse().length / 2 == r.getData().length)) {
                            byte[] mask = new byte[condition.getResponse().length /2];
                            byte[] test = new byte[condition.getResponse().length /2];
                            byte[] testResult = new byte[condition.getResponse().length /2];
                            System.arraycopy(condition.getResponse(),0,mask,0,mask.length);
                            System.arraycopy(condition.getResponse(),mask.length,test,0,test.length);
                            for (int j=0;j<test.length;j++) {
                                testResult[j] = (byte) ((HexUtils.byteToInt(mask[j]) & HexUtils.byteToInt(condition.getResponse()[j])));
                            }
                            conditionResult = Arrays.equals(testResult,test);
                        }
                    }
                    if (conditionResult) {
                        if (hasResponseFlags(condition.getFlags(),ResponseFlag.StoreResponse.getId())) {
                            return new CommandResult(r.getBytes(),condition.getLabel());
                        }else{
                            return new CommandResult(null,condition.getLabel());
                        }
                    }
                }
                if (hasResponseFlags(errorFlags,ResponseFlag.StoreResponse.getId())) {
                    return new CommandResult(r.getBytes(),errorLabel);
                }else{
                    return new CommandResult(null,errorLabel);
                }
            }
        } catch (CardException e) {
            e.printStackTrace();
        }
        if (r != null) {
            return new CommandResult(r.getBytes(), null);
        }else{
            return new CommandResult(null, null);
        }
    }

}
