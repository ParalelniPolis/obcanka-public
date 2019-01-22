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
package cz.paralelnipolis.obcanka.core.debug.interpretations;

import cz.paralelnipolis.obcanka.core.HexUtils;
import cz.paralelnipolis.obcanka.core.communication.ICommandAPDU;
import cz.paralelnipolis.obcanka.core.communication.IResponseAPDU;
import cz.paralelnipolis.obcanka.core.debug.CommandDescription;

import java.util.Arrays;

public class ISO7816 implements IInterpreter{
    public void processInterpretationPackages(StringBuilder sb, CommandDescription cd, ICommandAPDU commandAPDU, IResponseAPDU responseAPDU) {
        //ISO 7816-4 Section 6 - Basic Interindustry Commands

        if (cd.ins == 0xB0) {
            String p = "";
            //If bit8=1 in P1, then bit7-6 are set to 0. bit3-1 of P1 are a short EF (Elementary File) identifier and P2 is the offset of the first byte to be read in date units from the beginning of the file.
            if ((cd.p1 & 0x80) == 0x80) {
                //bit7-6 are set to 0
                //bit3-1 = short EF (Elementary File) identifier
                p += "bit8=1. bit3-1 of P1 = short EF (Elementary File) identifier. P2(" + HexUtils.intToHexAndDec(cd.p2) + ") is the offset of the first byte to be read in date units from the beginning of the file.";

            }else{
                //If bit8=0 in P1, then P1||P2 is the offset of the first byte to be read in data units from the beginning of the file.
                p += "bit8=0. Offset = " + (cd.p1 << 8 | cd.p2);
            }

            if (responseAPDU.getSW1() == 0x62) {
                if (responseAPDU.getSW2() == 0x81) {
                    p += " Response: Part of returned data may be corrupted";
                } else if (responseAPDU.getSW2() == 0x82) {
                    p += " Response: End of file reached before reading Le bytes";
                }
            }else if (responseAPDU.getSW1() == 0x67 && responseAPDU.getSW2() == 0x00) {
                p+= " Response:  Wrong length (wrong Le field)";
            }else if (responseAPDU.getSW1() == 0x69) {
                if (responseAPDU.getSW2() == 0x81) {
                    p += " Response: Command incompatible with file structure";
                } else if (responseAPDU.getSW2() == 0x82) {
                    p += " Response: Security status not satisfied";
                } else if (responseAPDU.getSW2() == 0x86) {
                    p += " Response: Command not allowed (no current EF)";
                }
            }else if (responseAPDU.getSW1() == 0x6A) {
                if (responseAPDU.getSW2() == 0x81) {
                    p += " Response: Function not supported";
                } else if (responseAPDU.getSW2() == 0x82) {
                    p += " Response: File not found";
                }
            }else if (responseAPDU.getSW1() == 0x6B) {
                p += "Response: Wrong parameters (offset outside the EF)";
            }else if (responseAPDU.getSW1() == 0x6C) {
                p += "Response: Wrong length (wrong Le field: ‘XX’ indicates the exact length) = " + responseAPDU.getSW2();
            }
            sb.append("READ BINARY " + p);
        }else if (cd.ins == 0xD0){
            sb.append("WRITE BINARY");
        }else if (cd.ins == 0xD6){
            sb.append("UPDATE BINARY");
        }else if (cd.ins == 0x0E){
            sb.append("ERASE BINARY");
        }else if (cd.ins == 0xB2){
            sb.append("READ RECORD");
        }else if (cd.ins == 0xD2){
            sb.append("WRITE RECORD");
        }else if (cd.ins == 0xE2){
            sb.append("APPEND RECORD");
        }else if (cd.ins == 0xDC){
            sb.append("UPDATE RECORD");
        }else if (cd.ins == 0xCA){
            int params = cd.p1 << 8 | cd.p2;
            String p = "";
            if (params >=0x0000 && params <= 0x003F){
                p = "RFU";
            }else if (params >=0x0040 && params <= 0x00FF){
                p = "BER-TLV tag (1 byte) in P2(" + HexUtils.intToHexAndDec(cd.p2) +")";
            }else if (params >=0x0100 && params <= 0x01FF){
                p = "Application data (proprietary coding)";
            }else if (params >=0x0200 && params <= 0x02FF){
                p = "SIMPLE-TLV tag in P2(" + HexUtils.intToHexAndDec(cd.p2) +")";
            }else if (params >=0x4000 && params <= 0xFFFF){
                p = "BER-TLV tag (2 bytes) in P1-P2("+HexUtils.bytesToHexStringWithSpaces(new byte[]{(byte) cd.p1, (byte) cd.p2})+")";
            }

            if (responseAPDU.getSW1() == 0x62 && responseAPDU.getSW2() == 0x81) {
                p += " Response: Part of returned data may be corrupted";
            }else if (responseAPDU.getSW1() == 0x67 && responseAPDU.getSW2() == 0x00) {
                p+= " Response:  Wrong length (wrong Le field)";
            }else if (responseAPDU.getSW1() == 0x69) {
                if (responseAPDU.getSW2() == 0x82) {
                    p += " Response: Security status not satisfied";
                } else if (responseAPDU.getSW2() == 0x85) {
                    p += " Response: Conditions of use not satisfied";
                }
            }else if (responseAPDU.getSW1() == 0x6A) {
                if (responseAPDU.getSW2() == 0x81) {
                    p += " Response: Function not supported";
                } else if (responseAPDU.getSW2() == 0x88) {
                    p += " Response: Referenced data (data objects) not found";
                }
            }else if (responseAPDU.getSW1() == 0x6C) {
                p += "Response: Wrong length (wrong Le field: ‘XX’ indicates the exact length) = " + responseAPDU.getSW2();
            }
            sb.append("GET DATA " + p);
        }else if (cd.ins == 0xDA ){
            sb.append("PUT DATA");
        }else if (cd.ins == 0xA4){
            String p = "RFU";

            if ((cd.p1 & 0xFC) == 0x00) { // 0 0 0 0 0 0 x x Selection by file identifier
                p = "Selection by file identifier";
                if (cd.p1 == 0x00) { //Select MF, DF or EF (data field=identifier or empty)
                    p +=", Select MF, DF or EF (data field=identifier or empty)";
                }else if (cd.p1 == 0x01) { // Select child DF (data field=DF identifier)
                    p +=", Select child DF (data field=DF identifier)";
                }else if (cd.p2 == 0x02) { // Select EF under current DF (data field=EF identifier)
                    p +=", Select EF under current DF (data field=EF identifier)";
                }else if (cd.p2 == 0x03) { // Select parent DF of the current DF (empty data field)
                    p +=", Select parent DF of the current DF (empty data field)";
                }
            }else if ((cd.p1 & 0xFC) == 0x4) { // 0 0 0 0 0 1 x x Selection by DF name
                p = "Selection by DF name";
                if (cd.p1 == 0x4) { // Direct selection by DF name (data field=DF name)
                    p +=", Direct selection by DF name (data field=DF name)";
                }
            }else if ((cd.p1 & 0xF8) == 0x8) { // 0 0 0 0 0 1 x x Selection by path (see 5.1.2)
                p +=", Selection by path";
                if (cd.p1 == 0x8) { // Select from MF (data field=path without the identifier of the MF)
                    p +=", Select from MF (data field=path without the identifier of the MF)";
                }else if (cd.p1 == 0x9) { // Select from current DF (data field=path without the identifier of the current DF)
                    p +=", Select from current DF (data field=path without the identifier of the current DF)";
                }
            }
            sb.append("SELECT FILE " + p);

            if (cd.p1 == 0x00 && cd.p2 == 0x00) {
                if (cd.requestDataLen == 0 || Arrays.equals(cd.requestData, new byte[]{0x3f,0x00})){ //If P1-P2=’0000′ and if the data field is empty or equal to ‘3F00’, then select the MF.
                    sb.append(" = select MF");
                }
            }
        }else if (cd.ins == 0x20){
            sb.append("VERIFY");
        }else if (cd.ins == 0x88){
            sb.append("INTERNAL AUTHENTICATE");
        }else if (cd.ins == 0xB2){
            sb.append("EXTERNAL AUTHENTICATE");
        }else if (cd.ins == 0xB4 ){
            sb.append("GET CHALLENGE");
        }else if (cd.ins == 0x70){
            sb.append("MANAGE CHANNEL");
        }else if (cd.ins == 0xC0){
            sb.append("GET RESPONSE");
        }else if (cd.ins == 0xC2){
            sb.append("ENVELOPE");
        }
    }
}
