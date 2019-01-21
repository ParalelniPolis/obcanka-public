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
import cz.paralelnipolis.obcanka.core.scripting.CommandResult;
import cz.paralelnipolis.obcanka.core.scripting.Label;
import cz.paralelnipolis.obcanka.core.scripting.ICommand;
import cz.paralelnipolis.obcanka.core.scripting.ScriptExecutor;

public class CmdReturn extends AbstractCommand{
    public static final int ID = 69;

    private byte[] value; //4 bytes

    public CmdReturn(Label label, byte[] value) {
        super(ID, label);
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    public static ICommand parse(byte[] labelId, byte[] commandPayload) {
        return new CmdReturn(new Label(labelId), commandPayload);
    }

    @Override
    public String toString() {
        return getLabel() +": " + getClass().getSimpleName() + "(" + HexUtils.bytesToHexStringWithSpaces(value) + ")";
    }

    @Override
    public CommandResult execute(ScriptExecutor scriptExecutor) {
        return new CommandResult(value,Label.RETURN);
    }

}
