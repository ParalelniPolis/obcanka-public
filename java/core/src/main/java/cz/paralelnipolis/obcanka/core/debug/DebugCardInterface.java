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
package cz.paralelnipolis.obcanka.core.debug;

import cz.paralelnipolis.obcanka.core.HexUtils;
import cz.paralelnipolis.obcanka.core.communication.CardException;
import cz.paralelnipolis.obcanka.core.communication.ICardInterface;
import cz.paralelnipolis.obcanka.core.communication.ICommandAPDU;
import cz.paralelnipolis.obcanka.core.communication.IResponseAPDU;
import cz.paralelnipolis.obcanka.core.debug.interpretations.Custom;
import cz.paralelnipolis.obcanka.core.debug.interpretations.IInterpreter;
import cz.paralelnipolis.obcanka.core.debug.interpretations.ISO7816;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DebugCardInterface implements ICardInterface {
    private ICardInterface targetCardInterface;
    private boolean logToConsole;
    private OutputStream logToStream;
    private int counter = 0;

    public DebugCardInterface(ICardInterface targetCardInterface, boolean logToConsole, OutputStream logToStream) {
        this.targetCardInterface = targetCardInterface;
        this.logToConsole = logToConsole;
        this.logToStream = logToStream;
    }

    @Override
    public IResponseAPDU transmit(ICommandAPDU command) throws CardException {
        IResponseAPDU response = targetCardInterface.transmit(command);

        if (isLoggingEnabled()) {
            counter++;
            StringBuilder sb = new StringBuilder();
            sb.append("== " + String.format("%d", counter) + " ==================================================================================\n");
            sb.append(getCommandAndResponseAsString(command, response) + "\n");
            sb.append("========================================================================================\n");
            appeendToLog(sb.toString());
        }
        return response;
    }

    private void appeendToLog(String logPart) {
        if (logToConsole) {
            System.out.print(logPart);
        }
        if (logToStream != null) {
            try {
                logToStream.write(logPart.getBytes());
            } catch (IOException e) {
            }
        }
    }

    private String getCommandAndResponseAsString(ICommandAPDU command, IResponseAPDU response) {
        StringBuilder sb = new StringBuilder();
        sb.append("APDU command and response:\n");
        sb.append("               " + drawScale(command.getData().length) + "\n");
        sb.append(" Command Full: " + HexUtils.bytesToHexStringWithSpaces(command.getData()) +"\n");
        CommandDescription commandDescription = processDescription(command, response);
        sb.append(" Command Desc: " + commandDescription.textDescription +"\n");
        sb.append(" Command Interpretation: " + commandDescription.textInterpretation +"\n");
        sb.append("                                                             " + drawScale(response.getBytes().length) + "\n");
        sb.append("                                              Response Full: " +
                HexUtils.bytesToHexStringWithSpaces(response.getBytes()) + "\n");
        sb.append("                                                Response SW: " +
                HexUtils.bytesToHexStringWithSpaces(new byte[]{response.getBytes()[response.getBytes().length -2 ], response.getBytes()[response.getBytes().length -1 ] }) + "\n");
        sb.append("                                              Response Data: ");
        if (response.getData().length == 0 ){
            sb.append("No data.");
        }else{
                sb.append(HexUtils.bytesToHexStringWithSpaces(response.getData()) + " In total: "  + response.getData().length + " bytes.\n");
        }

        return sb.toString();
    }

    private String drawScale(int length) {
        byte[] scale = new byte[length];
        for (int i=0;i<length;i++) {
            scale[i] = (byte)i;
        }
        return HexUtils.bytesToHexStringWithSpaces(scale) + " In total: " + length + " bytes";
    }

    private boolean isLoggingEnabled() {
        return logToConsole || logToStream != null;
    }

    @Override
    public ICommandAPDU createCommand(byte[] commandData) {
        return targetCardInterface.createCommand(commandData);
    }

    @Override
    public byte[] getATR() {
        byte[] atr = targetCardInterface.getATR();
        if (isLoggingEnabled()) {
            counter++;
            StringBuilder sb = new StringBuilder();
            sb.append("== " + String.format("%d", counter) + " ==================================================================================\n");
            sb.append(" ATR: " + HexUtils.bytesToHexStringWithSpaces(atr) +"\n");
            sb.append("========================================================================================\n");
            appeendToLog(sb.toString());
        }
        return atr;
    }

    private CommandDescription processDescription(ICommandAPDU command, IResponseAPDU response) {
        byte[] contains = command.getData();
        CommandDescription cd = new CommandDescription();
        if ( contains != null && contains.length >= 4 ) {
            cd.cla = HexUtils.byteToInt(contains[0]); //class
            cd.ins = HexUtils.byteToInt(contains[1]); //instruction
            cd.p1 = HexUtils.byteToInt(contains[2]); //Parameter 1
            cd.p2 = HexUtils.byteToInt(contains[3]); //Parameter 2
            cd.responseLenExpected = 0;
            cd.requestDataLen = 0;
            cd.requestData = null;

            if (contains.length == 4) {
                //Case 1
                cd.caseNumber = 1;
            }
            if (contains.length == 5) {
                //Case 2
                cd.caseNumber = 2;
                cd.responseLenExpected = HexUtils.byteToInt(contains[4]);
            }
            if (contains.length >= 6) {
                cd.requestDataLen = HexUtils.byteToInt(contains[4]);
                if (cd.requestDataLen + 5 ==  contains.length) {
                    //Case 3
                    cd.caseNumber = 3;
                    cd.requestData = new byte[cd.requestDataLen];
                    System.arraycopy(contains,5, cd.requestData,0,cd.requestDataLen);
                }else if (cd.requestDataLen + 6 ==  contains.length) {
                    //Case 4
                    cd.caseNumber = 4;
                    cd.requestData = new byte[cd.requestDataLen];
                    System.arraycopy(contains,5, cd.requestData,0, cd.requestDataLen);
                    cd.responseLenExpected = HexUtils.byteToInt(contains[contains.length - 1]);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        if (cd.caseNumber == -1) { //invalid command
            sb.append(" Invalid command");
        }

        //Case 1
        sb.append("CASE: " +  cd.caseNumber + " CLA: " + HexUtils.intToHexAndDec(cd.cla) + " INS: " + HexUtils.intToHexAndDec(cd.ins) + " P1: " + HexUtils.intToHexAndDec(cd.p1)+ " P2: " + HexUtils.intToHexAndDec(cd.p2));
        if (cd.caseNumber == 2) {
            //Case 2
            sb.append(" Le: " + HexUtils.intToHexAndDec(cd.responseLenExpected) + " Expected response len is " + cd.responseLenExpected + " bytes.");
        }
        if (cd.caseNumber == 3) {
            sb.append(" Lc: " + HexUtils.intToHexAndDec(cd.requestData.length) + " Command data len is " + cd.requestData.length + " bytes: " + HexUtils.bytesToHexStringWithSpaces(cd.requestData));
        }
        if (cd.caseNumber == 4) {
            sb.append(" Le: " + HexUtils.intToHexAndDec(cd.responseLenExpected) + " Expected response len is " + cd.responseLenExpected + " bytes.");
            sb.append(" Lc: " + HexUtils.intToHexAndDec(cd.requestData.length) + " Command data len is " + cd.requestData.length + " bytes: " + HexUtils.bytesToHexStringWithSpaces(cd.requestData));
        }
        cd.textDescription = sb.toString();
        processInterpretationPackages(cd, command, response);
        return cd;
    }

    private void processInterpretationPackages(CommandDescription cd, ICommandAPDU commandAPDU, IResponseAPDU responseAPDU) {
        StringBuilder sb = new StringBuilder();
        List<IInterpreter> interpreters = new ArrayList<>();
        interpreters.add(new Custom());
        interpreters.add(new ISO7816());
        for (int i = 0; i < interpreters.size(); i++) {
            IInterpreter interpreter = interpreters.get(i);
            interpreter.processInterpretationPackages(sb,cd,commandAPDU,responseAPDU);
        }
        cd.textInterpretation = sb.toString();
    }
}
