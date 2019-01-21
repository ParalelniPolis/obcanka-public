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

import cz.paralelnipolis.obcanka.core.communication.ICommandAPDU;
import cz.paralelnipolis.obcanka.core.communication.IResponseAPDU;
import cz.paralelnipolis.obcanka.core.debug.CommandDescription;

public class ISO7816 implements IInterpreter{
    public void processInterpretationPackages(StringBuilder sb, CommandDescription cd, ICommandAPDU commandAPDU, IResponseAPDU responseAPDU) {
        //ISO 7816-4 Section 6 - Basic Interindustry Commands

        if (cd.ins == 0xB0) {
            sb.append("READ BINARY");
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
            sb.append("GET DATA");
        }else if (cd.ins == 0xDA ){
            sb.append("PUT DATA");
        }else if (cd.ins == 0xA4){
            sb.append("SELECT FILE");
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
