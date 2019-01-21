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
import cz.paralelnipolis.obcanka.core.scripting.ScriptExecutor;

public class CmdUnknown extends AbstractCommand{
    private byte[] payload;

    public CmdUnknown(int id, Label label, byte[] payload) {
        super(id, label);
        this.payload = payload;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return getLabel() +": " + getClass().getSimpleName() + " " + getId() + " (" + HexUtils.bytesToHexStringWithSpaces(payload) + ")";
    }

    @Override
    public CommandResult execute(ScriptExecutor scriptExecutor) {
        return new CommandResult(new byte[4],null);
    }
}
