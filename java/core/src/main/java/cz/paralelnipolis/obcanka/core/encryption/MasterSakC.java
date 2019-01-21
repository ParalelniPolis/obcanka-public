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
package cz.paralelnipolis.obcanka.core.encryption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MasterSakC {

    public MasterSakC(byte[] key) {
        this.key = key;
    }

    private byte[] key;

    public SakC deriveEncryptionKey(byte[] serialNumber, byte[] cardNumber) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(new byte[] {(byte)0x86, (byte)0xC5});
            bos.write(serialNumber);
            bos.write(cardNumber);
            bos.write(serialNumber);
            bos.write(new byte[] {(byte)0x45, (byte)0x4e, (byte)0x43, (byte)0x00, (byte)0xD3});
            return new SakC(deriveKey(bos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] deriveKey(byte[] input) {
        return EncryptionUtils.encryptWithAESCBCNone(key,input);
    }
}
