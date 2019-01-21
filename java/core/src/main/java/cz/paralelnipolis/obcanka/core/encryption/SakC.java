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

import cz.paralelnipolis.obcanka.core.HexUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static cz.paralelnipolis.obcanka.core.encryption.EncryptionUtils.PADDING_ISO7816_4;

public class SakC {

    private byte[] key;

    public SakC(byte[] key) {
        this.key = key;
    }

    public byte[] encryptSecureCodeData(byte[] counter, byte[] data) {
        try {
            final Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] c = Arrays.copyOf(counter,counter.length);
            c = EncryptionUtils.incrementCounter(c);
            byte[] k = hmac.doFinal(c);
            return EncryptionUtils.encryptWithAESCBCNone(k, EncryptionUtils.padDataWithISO7816_4(data));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

}
