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
        }else if (cd.ins == 0xCB){
            sb.append("GET DATA (TAG or LIST)");
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
            sb.append("VERIFY (PIN)");
        }else if (cd.ins == 0x21){
            sb.append("VERIFY (PIN) with structure");
        }else if (cd.ins == 0x22){
            boolean setOrGetCrt = false;
            sb.append("MANAGE SECURITY ENVIRONMENT -");
            if ((cd.p1 & 0x0F) == 0x01) {
                sb.append(" SET");
                setOrGetCrt = true;
            }else if ((cd.p1 & 0xFF) == 0xF2) {
                sb.append(" STORE");
            }else if ((cd.p1 & 0xFF) == 0xF3) {
                sb.append(" RESTORE");
            }else if ((cd.p1 & 0xFF) == 0xF4) {
                sb.append(" ERASE");
            }else {
                if ((cd.p1 & 0x10) == 0x10) {
                    sb.append(" Secure messaging in command data field");
                }
                if ((cd.p1 & 0x20) == 0x20) {
                    sb.append(" Secure messaging in response data field");
                }
                if ((cd.p1 & 0x40) == 0x40) {
                    sb.append(" Computation, decipherment, internal authentication and key agreement");
                }
                if ((cd.p1 & 0x80) == 0x80) {
                    sb.append(" Verification, encipherment, external authentication and key agreement");
                }
            }
            if (setOrGetCrt) {
                switch (cd.p2) {
                    case 0xA4:
                        sb.append(" Control reference template for authentication (AT)");
                        break;
                    case 0xA6:
                        sb.append(" Control reference template for key agreement (KAT)");
                        break;
                    case 0xAA:
                        sb.append(" Control reference template for hash-code (HT)");
                        break;
                    case 0xB4:
                        sb.append(" Control reference template for cryptographic checksum (CCT)");
                        break;
                    case 0xB6:
                        sb.append(" Control reference template for digital signature (DST)");
                        break;
                    case 0xB8:
                        sb.append(" Control reference template for confidentiality (CT)");
                        break;
                }
            }else{

            }
        }else if (cd.ins == 0x86 || cd.ins == 0x87){
            sb.append("GENERAL AUTHENTICATE");
            if (cd.requestData.length > 2) {
                if (cd.requestData[0] == 0x7c) {
                    sb.append(" Set of dynamic authentication data objects with the following tag: ");
                    switch (cd.requestData[1]) {
                        case (byte)0x80:
                            sb.append("Witness");
                            break;
                        case (byte)0x81:
                            sb.append("Challenge");
                            break;
                        case (byte)0x82:
                            sb.append("Response");
                            break;
                        case (byte)0x83:
                            sb.append("Committed challenge ");
                            break;
                        case (byte)0x84:
                            sb.append("Authentication code");
                            break;
                        case (byte)0x85:
                            sb.append("Exponential");
                            break;
                        case (byte)0xA0:
                            sb.append("Identification data template");
                            break;
                    }
                }
            }
        }else if (cd.ins == 0x88){
            sb.append("INTERNAL AUTHENTICATE");
        }else if (cd.ins == 0xB2){
            sb.append("EXTERNAL AUTHENTICATE");
        }else if (cd.ins == 0xB4 ){
            sb.append("GET CHALLENGE");
        }else if (cd.ins == 0x46 ){
            sb.append("GENERATE ASYMMETRIC KEY PAIR");
        }else if (cd.ins == 0x70){
            sb.append("MANAGE CHANNEL");
        }else if (cd.ins == 0xC0){
            sb.append("GET RESPONSE");
        }else if (cd.ins == 0xC2){
            sb.append("ENVELOPE");
        }else if (cd.ins == 0x2a){
            sb.append("PERFORM SECURITY OPERATION");
            if (cd.p1 == 0x80) {
                sb.append(" - DECIPHER");
            }else if (cd.p1 == 0x82) {
                sb.append(" - ENCIPHER");
            }else if (cd.p1 == 0x8e) {
                sb.append(" - COMPUTE CRYPTOGRAPHIC CHECKSUM");
            }else if (cd.p1 == 0x90) {
                sb.append(" - HASH");
                if (cd.p2 == 0x80) {
                    sb.append(" - data field contains data to be hashed.");
                }else if (cd.p2 == 0xa0) {
                    sb.append(" - data field contains reference to be hashed.");
                }
            }else if (cd.p1 == 0x9e) {
                sb.append(" - COMPUTE DIGITAL SIGNATURE");
                if (cd.p2 == 0x9A) {
                    sb.append(" - data field contains data to be signed.");
                } else if (cd.p2 == 0xac) {
                    sb.append(" - data field contains reference to be signed.");
                } else if (cd.p2 == 0xbc) {
                    sb.append(" - data field contains reference to be signed.");
                }
            }else if (cd.p1 == 0x00) {
                if (cd.p2 == 0xA2) {
                    sb.append(" - VERIFY CRYPTOGRAPHIC CHECKSUM");
                } else if (cd.p2 == 0xA8) {
                    sb.append(" - VERIFY DIGITAL SIGNATURE");
                } else if (cd.p2 == 0x92 || cd.p2 == 0xae || cd.p2 == 0xbe ) {
                    sb.append(" - VERIFY CERTIFICATE");
                }
            }
        }else if (cd.ins == 0x84){
            sb.append("GET CHALLENGE");
        }else if (cd.ins == 0x82){
            sb.append("EXTERNAL (/ MUTUAL) AUTHENTICATE");
        }
    }
}
