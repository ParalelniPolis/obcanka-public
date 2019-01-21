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

import cz.paralelnipolis.obcanka.core.scripting.ICommand;
import cz.paralelnipolis.obcanka.core.scripting.Label;
import cz.paralelnipolis.obcanka.core.scripting.ResponseFlag;
import cz.paralelnipolis.obcanka.core.scripting.IUIEntryProvider;

public abstract class AbstractCommand implements ICommand {
    private int id;
    private Label label;
    private IUIEntryProvider entryProvider;

    public AbstractCommand(int id, Label label) {
        this.id = id;
        this.label = label;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Label getLabel() {
        return label;
    }

    static boolean doesntHaveResponseFlags(int flags, int checkFlags) {
        return (flags & checkFlags) == (~(ResponseFlag.AllFlags.getId()) & 0xFF);
    }
    static boolean hasResponseFlags(int flags, int checkFlags) {
        return !doesntHaveResponseFlags(flags,checkFlags);
    }

    public IUIEntryProvider getEntryProvider() {
        return entryProvider;
    }

    @Override
    public void setEntryProvider(IUIEntryProvider entryProvider) {
        this.entryProvider = entryProvider;
    }
}
