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

import cz.paralelnipolis.obcanka.core.HexUtils;

import java.util.Arrays;

public class Label {
    public static final Label RETURN = new Label(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF});

    private String name;
    private byte[] value;//4 bytes

    public Label(byte[] value) {
        this.name = new String(value);
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + "(" + HexUtils.bytesToHexStringWithSpaces(value) +")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label = (Label) o;
        return Arrays.equals(value, label.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

}
