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

import cz.paralelnipolis.obcanka.core.communication.ICardInterface;

import java.util.ArrayList;
import java.util.List;

public class ScriptExecutor {
    private ICardInterface cardInterface;

    public ScriptExecutor(ICardInterface cardInterface) {
        this.cardInterface = cardInterface;
    }

    public ICardInterface getCardInterface() {
        return cardInterface;
    }

    public ScriptResponse execute(ScriptEnvelope env, IUIEntryProvider ep) {
        Script script = env.getScript();
        List<ICommand> commands = script.getCommands();
        List<ResponseBlock> blocks = new ArrayList<>();
        byte[] returnCode = new byte[]{(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff};

        if (commands.size() > 0) {
            int cmdIndex = 0;

            do {
                ICommand cmd = commands.get(cmdIndex);
                cmd.setEntryProvider(ep);
                CommandResult result = cmd.execute(this);
                if (result.getNextLabel() == null) {
                    //no jump go to next label
                    cmdIndex++;
                }else if (Label.RETURN.equals(result.getNextLabel())) {
                    //end of program called (CmdReturn);
                    returnCode = result.getResponse();
                    break;
                }else {
                    //jump to next label
                    cmdIndex = findCommandIndexByLabel(commands, result.getNextLabel(), cmdIndex);
                }
                if (result.getResponse() != null) {
                    blocks.add(new ResponseBlock(cmd.getLabel(), result.getResponse())); //save command result
                }

            }while (cmdIndex <= commands.size() - 1);
        }
        return ScriptResponse.build(env.getAlgorithm(),env.getKeyId(),returnCode,blocks);
    }

    private static int findCommandIndexByLabel(List<ICommand> commands, Label label, int currentCmdIndex) {
        //try first searching from currentCmdIndex+1 further as there can be multiple commands with same label
        for (int i = currentCmdIndex+1; i < commands.size(); i++) {
            ICommand command = commands.get(i);
            if (command.getLabel().equals(label)) {
                return i;
            }
        }
        //if not found afterwards try searching before current index
        for (int i = 0; i < currentCmdIndex; i++) {
            ICommand command = commands.get(i);
            if (command.getLabel().equals(label)) {
                return i;
            }
        }
        return -1;
    }

}
