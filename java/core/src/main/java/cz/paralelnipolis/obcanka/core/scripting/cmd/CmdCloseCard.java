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
import cz.paralelnipolis.obcanka.core.scripting.ICommand;
import cz.paralelnipolis.obcanka.core.scripting.Label;
import cz.paralelnipolis.obcanka.core.scripting.ScriptExecutor;


public class CmdCloseCard extends AbstractCommand{
    public static final int ID = 67;

    private Label nextLabel;
    private Change change;

    public CmdCloseCard(Label label, Label nextLabel, Change change) {
        super(ID, label);
        this.nextLabel = nextLabel;
        this.change = change;
    }


    public Label getNextLabel() {
        return nextLabel;
    }

    public Change getChange() {
        return change;
    }

    public enum Change{
        No(0),
        Data(100),
        Profile(112);

        private int id;

        Change(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Change fromId(int id) {
            for (Change type : values()) {
                if (type.getId() == id) {
                    return type;
                }
            }
            return null;
        }
    }

    public static ICommand parse(byte[] labelId, byte[] commandPayload) {
        Change change = null;
        int start = 0;
        if (commandPayload.length > 4) {
            //contains Change
            start = 1;
            change = Change.fromId(HexUtils.byteToInt(commandPayload[0]));
        }
        byte[] nextLabel = new byte[4];
        System.arraycopy(commandPayload,start,nextLabel,0,4);
        return new CmdCloseCard(new Label(labelId),new Label(nextLabel),change);
    }

    @Override
    public CommandResult execute(ScriptExecutor scriptExecutor) {
        return new CommandResult(null, nextLabel);
    }
}
