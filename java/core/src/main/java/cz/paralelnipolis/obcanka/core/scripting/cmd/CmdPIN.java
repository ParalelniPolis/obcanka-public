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


public class CmdPIN extends AbstractCommand{
    public static final int ID = 118;

    private byte[] message;
    private SpeData data;

    private List<Response> responses;
    private int errorFlags;
    private Label errorLabel;


    public CmdPIN(Label label, byte[] message, SpeData data, List<Response> responses, int errorFlags, Label errorLabel) {
        super(ID, label);
        this.message = message;
        this.data = data;
        this.responses = responses;
        this.errorFlags = errorFlags;
        this.errorLabel = errorLabel;
    }

    static class SpeSWData {
        private int requestType;
        private int PINMinExtraDigit;
        private int PINMaxExtraDigit;

        public SpeSWData(int requestType, int PINMinExtraDigit, int PINMaxExtraDigit) {
            this.requestType = requestType;
            this.PINMinExtraDigit = PINMinExtraDigit;
            this.PINMaxExtraDigit = PINMaxExtraDigit;
        }
    }

    static class SpeData {
        private byte[] data;
        private int requestType;
        private int timeOut;
        private int timeOut2;
        private int formatString;
        private int PINBlockString;
        private int PINLengthFormat;
        private int insertionOffsetOld;
        private int insertionOffsetNew;
        private int PINMinExtraDigit;
        private int PINMaxExtraDigit;
        private int confirmPIN;
        private int entryValidationCondition;
        private int numberMessage;
        private int langId; // 2 bytes
        private int msgIndex1;
        private int msgIndex2;
        private int msgIndex3;
        private int msgIndex4;
        private byte[] request;


        public SpeData(byte[] data) {
            this.data = data;
            requestType = HexUtils.byteToInt(data[0]);
            timeOut = HexUtils.byteToInt(data[1]);
            timeOut2 = HexUtils.byteToInt(data[2]);
            formatString = HexUtils.byteToInt(data[3]);
            PINBlockString = HexUtils.byteToInt(data[4]);
            PINLengthFormat = HexUtils.byteToInt(data[5]);
            insertionOffsetOld = HexUtils.byteToInt(data[6]);
            insertionOffsetNew = HexUtils.byteToInt(data[7]);
            PINMinExtraDigit = HexUtils.byteToInt(data[8]);
            PINMaxExtraDigit = HexUtils.byteToInt(data[9]);
            confirmPIN = HexUtils.byteToInt(data[10]);
            entryValidationCondition = HexUtils.byteToInt(data[11]);
            numberMessage = HexUtils.byteToInt(data[12]);
            langId = (HexUtils.byteToInt(data[13]) << 8) | HexUtils.byteToInt(data[14]); // 2 bytes
            msgIndex1 = HexUtils.byteToInt(data[15]);
            msgIndex2 = HexUtils.byteToInt(data[16]);
            msgIndex3 = HexUtils.byteToInt(data[17]);
            int position = 18;
            if (requestType == 4) {
                msgIndex4 = HexUtils.byteToInt(data[position]);
            }
            int count =  (HexUtils.byteToInt(data[position]) << 24) | (HexUtils.byteToInt(data[position+1]) << 16) | (HexUtils.byteToInt(data[position+2]) << 8) | HexUtils.byteToInt(data[position+3]);
            position+=4;
            request = new byte[count];
            System.arraycopy(data,position, request,0,request.length);
        }

        public byte[] getData() {
            return data;
        }

        public int getRequestType() {
            return requestType;
        }

        public int getTimeOut() {
            return timeOut;
        }

        public int getTimeOut2() {
            return timeOut2;
        }

        public int getFormatString() {
            return formatString;
        }

        public int getPINBlockString() {
            return PINBlockString;
        }

        public int getPINLengthFormat() {
            return PINLengthFormat;
        }

        public int getInsertionOffsetOld() {
            return insertionOffsetOld;
        }

        public int getInsertionOffsetNew() {
            return insertionOffsetNew;
        }

        public int getPINMinExtraDigit() {
            return PINMinExtraDigit;
        }

        public int getPINMaxExtraDigit() {
            return PINMaxExtraDigit;
        }

        public int getConfirmPIN() {
            return confirmPIN;
        }

        public int getEntryValidationCondition() {
            return entryValidationCondition;
        }

        public int getNumberMessage() {
            return numberMessage;
        }

        public int getLangId() {
            return langId;
        }

        public int getMsgIndex1() {
            return msgIndex1;
        }

        public int getMsgIndex2() {
            return msgIndex2;
        }

        public int getMsgIndex3() {
            return msgIndex3;
        }

        public int getMsgIndex4() {
            return msgIndex4;
        }

        public byte[] getRequest() {
            return request;
        }

        @Override
        public String toString() {
            return "SpeData{" +
                    "data=" + HexUtils.bytesToHexStringWithSpaces(data) +
                    ", requestType=" + requestType +
                    ", timeOut=" + timeOut +
                    ", timeOut2=" + timeOut2 +
                    ", formatString=" + formatString +
                    ", PINBlockString=" + PINBlockString +
                    ", PINLengthFormat=" + PINLengthFormat +
                    ", insertionOffsetOld=" + insertionOffsetOld +
                    ", insertionOffsetNew=" + insertionOffsetNew +
                    ", PINMinExtraDigit=" + PINMinExtraDigit +
                    ", PINMaxExtraDigit=" + PINMaxExtraDigit +
                    ", confirmPIN=" + confirmPIN +
                    ", entryValidationCondition=" + entryValidationCondition +
                    ", numberMessage=" + numberMessage +
                    ", langId=" + langId +
                    ", msgIndex1=" + msgIndex1 +
                    ", msgIndex2=" + msgIndex2 +
                    ", msgIndex3=" + msgIndex3 +
                    ", msgIndex4=" + msgIndex4 +
                    ", request=" + HexUtils.bytesToHexStringWithSpaces(request) +
                    '}';
        }
    }

    enum SpeStatus {
        Timeout(0x6400),
        CancelByUser(0x6401),
        CancelByApplication(0x6480),
        SpeInvalidParameter(0x6B80),
        OK(0x9000);

        private int id;

        SpeStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }


    public static ICommand parse(byte[] labelId, byte[] commandPayload) {
        List<Response> responses = new ArrayList<>();
        int errorFlags = 0;
        Label errorLabel = null;

        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(commandPayload));
            int messageSize = dis.readInt();
            byte[] message = new byte[messageSize];
            dis.read(message);
            int dataSize = dis.readInt();
            byte[] data = new byte[dataSize];
            dis.read(data);
            while (dis.available() > 0) {
                int flags = HexUtils.byteToInt(dis.readByte());
                int responseSize = dis.readInt();
                if (doesntHaveResponseFlags(flags,ResponseFlag.Error.getId())) {
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
            return new CmdPIN(new Label(labelId), message, new SpeData(data), responses, errorFlags, errorLabel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getLabel() +": " + getClass().getSimpleName() + " {\n");
        sb.append("      message: " + HexUtils.bytesToHexStringWithSpaces(message) + "\n");
        sb.append("      data: " + data + "\n");
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
        SpeSWData d = new SpeSWData(data.requestType,data.PINMinExtraDigit,data.PINMaxExtraDigit);
        ICardInterface ci = scriptExecutor.getCardInterface();
        IResponseAPDU r = null;
        try {
            String pin = getEntryProvider().getPIN();
            System.arraycopy(pin.getBytes(),0, data.request,5 + data.insertionOffsetOld, pin.getBytes().length);
            ICommandAPDU c = ci.createCommand(data.request);
            r = ci.transmit(c);
//            EncryptionToken secureToken = card.createEncryptionToken();
//            System.out.println("secureToken = " + secureToken);
//            r = secureToken.transmit(ci,c);
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
