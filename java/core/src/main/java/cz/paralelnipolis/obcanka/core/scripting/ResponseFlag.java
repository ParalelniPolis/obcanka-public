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

public enum ResponseFlag {
    StoreResponse(1),
    Error(2),
    CheckResponseData(4),
    MaskedCheck(8),
    Cancel(16),
    Reserved1(32),
    Reserved2(64),
    Reserved3(128),
    AllFlags(ResponseFlag.StoreResponse.getId() | ResponseFlag.Error.getId() | ResponseFlag.CheckResponseData.getId() | ResponseFlag.MaskedCheck.getId() | ResponseFlag.Cancel.getId() | ResponseFlag.Reserved1.getId() | ResponseFlag.Reserved2.getId() | ResponseFlag.Reserved3.getId());

    private int id;

    ResponseFlag(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ResponseFlag fromId(int id) {
        for (ResponseFlag f : values()) {
            if (f.getId() == id) {
                return f;
            }
        }
        return null;
    }
}
