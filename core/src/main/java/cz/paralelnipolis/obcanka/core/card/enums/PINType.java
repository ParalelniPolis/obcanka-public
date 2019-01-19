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
package cz.paralelnipolis.obcanka.core.card.enums;

public enum PINType {
    Unknown(0),
    PIN(1),
    PIN2(2),
    PIN3(3),
    PIN4(4),
    PIN5(5),
    PIN6(6),
    PIN7(7),
    AdminKey(101),
    PUK(102),
    QPIN(103),
    IOK(104),
    DOK(105);

    private int id;

    PINType(int id) {
        this.id = id;
    }

}
