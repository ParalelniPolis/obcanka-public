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

import cz.paralelnipolis.obcanka.core.scripting.*;

public class CmdOpenCard extends AbstractCommand {
    public static final int ID = 79;

    private cz.paralelnipolis.obcanka.core.scripting.Script script;
    private Label labelOK;
    private Label labelError;

    public CmdOpenCard(Label label, Script script, Label labelOK, Label labelError) {
        super(ID, label);
        this.script = script;
        this.labelOK = labelOK;
        this.labelError = labelError;
    }

    public Script getScript() {
        return script;
    }

    public Label getLabelOK() {
        return labelOK;
    }

    public Label getLabelError() {
        return labelError;
    }

    public static ICommand parse(byte[] labelId, byte[] commandPayload) {
        byte[] script = new byte[commandPayload.length - 8];
        System.arraycopy(commandPayload,0,script,0,script.length);
        byte[] labelOK = new byte[4];
        System.arraycopy(commandPayload,script.length,labelOK,0,4);
        byte[] labelError = new byte[4];
        System.arraycopy(commandPayload,script.length+4,labelError,0,4);
        Script s = Script.parse(script);
        return new CmdOpenCard(new Label(labelId), s,new Label(labelOK),new Label(labelError));
    }

    @Override
    public CommandResult execute(ScriptExecutor scriptExecutor) {
        byte[] atr = scriptExecutor.getCardInterface().getATR();
        if (atr != null) {
            return new CommandResult(null,labelOK);
        }else{
            return new CommandResult(null,labelError);
        }
    }
}
