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

public class CmdGotoLabel extends AbstractCommand {
    public static final int ID = 76;

    private Label nextLabel;

    public CmdGotoLabel(Label label, Label nextLabel) {
        super(ID, label);
        this.nextLabel = nextLabel;
    }

    public static ICommand parse(byte[] labelId, byte[] commandPayload) {
        return new CmdGotoLabel(new Label(labelId),new Label(commandPayload));
    }

    @Override
    public CommandResult execute(ScriptExecutor scriptExecutor) {
        return new CommandResult(null, nextLabel);
    }
}
