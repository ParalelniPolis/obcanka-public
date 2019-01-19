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

import cz.paralelnipolis.obcanka.core.scripting.CommandResult;
import cz.paralelnipolis.obcanka.core.scripting.ICommand;
import cz.paralelnipolis.obcanka.core.scripting.Label;
import cz.paralelnipolis.obcanka.core.scripting.ScriptExecutor;

public class CmdMessage extends AbstractCommand {
    public static final int ID = 101;

    private Label nextLabel;
    private String text;
    private byte[] value;

    public CmdMessage(Label label, byte[] value,Label nextLabel) {
        super(ID, label);
        this.value = value;
        this.text = new String(value);
        this.nextLabel = nextLabel;
    }

    public Label getNextLabel() {
        return nextLabel;
    }

    public String getText() {
        return text;
    }

    public byte[] getValue() {
        return value;
    }

    public static ICommand parse(byte[] labelId, byte[] commandPayload) {
        byte[] value = new byte[commandPayload.length -4];
        System.arraycopy(commandPayload,0,value,0,value.length);
        byte[] nextLabel = new byte[4];
        System.arraycopy(commandPayload,value.length,nextLabel,0,4);
        return new CmdMessage(new Label(labelId),value,new Label(nextLabel));
    }

    @Override
    public String toString() {
        return getLabel() +": " + getClass().getSimpleName() + "('" + getText() + "') => " + getNextLabel();
    }

    @Override
    public CommandResult execute(ScriptExecutor scriptExecutor) {
        System.out.println("============================== " + text + " ===============================");
        return new CommandResult(value,nextLabel);
    }
}
