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

import cz.paralelnipolis.obcanka.core.HexUtils;
import cz.paralelnipolis.obcanka.core.scripting.cmd.*;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;

public class Script {
    private List<ICommand> commands;

    private Script(List<ICommand> commands) {
        this.commands = commands;
    }

    public List<ICommand> getCommands() {
        return commands;
    }

    public static Script parse(byte[] script) {
        List<ICommand> commands = new ArrayList<>();
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(script));
            do {
                //parse command
                byte[] labelId = new byte[4];
                dis.read(labelId);
                int commandId = HexUtils.byteToInt(dis.readByte());
                int commandSize = dis.readInt();
                byte[] commandPayload = new byte[commandSize];
                dis.read(commandPayload);
                ICommand command = parseCommand(commandId,labelId, commandPayload);
                commands.add(command);
            }while (dis.available() > 0);
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Script(commands);
    }

    private static ICommand parseCommand(int commandId, byte[] labelId, byte[] commandPayload) {
        switch (commandId) {
            case CmdGotoLabel.ID:
                return CmdGotoLabel.parse(labelId,commandPayload);
            case CmdReturn.ID:
                return CmdReturn.parse(labelId,commandPayload);
            case CmdMessage.ID:
                return CmdMessage.parse(labelId,commandPayload);
            case CmdOpenCard.ID:
                return CmdOpenCard.parse(labelId,commandPayload);
            case CmdCloseCard.ID:
                return CmdCloseCard.parse(labelId,commandPayload);
            case CmdAPDU.ID:
                return CmdAPDU.parse(labelId,commandPayload);
            case CmdPIN.ID:
                return CmdPIN.parse(labelId,commandPayload);
                //TODO: Add check Atr
            default:
                return new CmdUnknown(commandId,new Label(labelId),commandPayload);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < commands.size(); i++) {
            ICommand command = commands.get(i);
            sb.append(" ");
            sb.append(command);
            sb.append("\n");
        }
        return sb.toString();
    }
}
