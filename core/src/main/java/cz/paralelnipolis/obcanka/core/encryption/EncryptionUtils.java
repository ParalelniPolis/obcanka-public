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
import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.macs.CMac;
import org.spongycastle.crypto.params.KeyParameter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EncryptionUtils {

    public static byte[] PADDING_ISO7816_4 = new byte[]
            {
                    (byte) 128,   //80 // -1
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0,
                    (byte) 0
            };


    private static Map<byte[],byte[]> keysByKeyChecksumValue = new HashMap<>();
    static {
        addKey(new byte[]
                        {
                                (byte) 65,
                                (byte) 66,
                                (byte) 67,
                                (byte) 68,
                                (byte) 69,
                                (byte) 70,
                                (byte) 71,
                                (byte) 72,
                                (byte) 49,
                                (byte) 50,
                                (byte) 51,
                                (byte) 52,
                                (byte) 53,
                                (byte) 54,
                                (byte) 55,
                                (byte) 56,
                                (byte) 33,
                                (byte) 34,
                                (byte) 35,
                                (byte) 36,
                                (byte) 37,
                                (byte) 38,
                                (byte) 39,
                                (byte) 40,
                                (byte) 17,
                                (byte) 18,
                                (byte) 19,
                                (byte) 20,
                                (byte) 21,
                                (byte) 22,
                                (byte) 23,
                                (byte) 24
                        }
        );

        addKey(new byte[]
                        {
                                (byte) 164,
                                (byte) 113,
                                (byte) 106,
                                (byte) 111,
                                (byte) 209,
                                (byte) 87,
                                (byte) 9,
                                (byte) 67,
                                (byte) 147,
                                (byte) 208,
                                (byte) 155,
                                (byte) 155,
                                (byte) 249,
                                (byte) 9,
                                (byte) 162,
                                (byte) 180,
                                (byte) 147,
                                (byte) 181,
                                (byte) 156,
                                (byte) 173,
                                (byte) 226,
                                (byte) 227,
                                (byte) 55,
                                (byte) 211,
                                (byte) 179,
                                (byte) 3,
                                (byte) 100,
                                (byte) 182,
                                (byte) 43,
                                (byte) 17,
                                (byte) 15,
                                (byte) 219
                        }
        );


        addKey(new byte[]{
                        (byte) 130,
                        (byte) 103,
                        (byte) 34,
                        (byte) 113,
                        (byte) 141,
                        (byte) 150,
                        (byte) 195,
                        (byte) 161,
                        (byte) 58,
                        (byte) 64,
                        (byte) 13,
                        (byte) 173,
                        (byte) 60,
                        (byte) 18,
                        (byte) 71,
                        (byte) 95,
                        (byte) 189,
                        (byte) 202,
                        (byte) 55,
                        (byte) 187,
                        (byte) 62,
                        (byte) 89,
                        (byte) 13,
                        (byte) 187,
                        (byte) 255,
                        (byte) 170,
                        (byte) 27,
                        (byte) 208,
                        (byte) 78,
                        (byte) 121,
                        (byte) 205,
                        (byte) 73
                }
        );

        addKey(new byte[]{
                        (byte) 227,
                        (byte) 101,
                        (byte) 90,
                        (byte) 171,
                        (byte) 103,
                        (byte) 127,
                        (byte) 198,
                        (byte) 88,
                        (byte) 25,
                        (byte) 227,
                        (byte) 134,
                        (byte) 75,
                        (byte) 93,
                        (byte) 101,
                        (byte) 76,
                        (byte) 139,
                        (byte) 145,
                        (byte) 88,
                        (byte) 187,
                        (byte) 3,
                        (byte) 90,
                        (byte) 18,
                        (byte) 244,
                        (byte) 98,
                        (byte) 192,
                        (byte) 32,
                        (byte) 147,
                        (byte) 253,
                        (byte) 83,
                        (byte) 44,
                        (byte) 164,
                        (byte) 13
                }
        );
    }

    private static void addKey(byte[] key) {
        keysByKeyChecksumValue.put(calculateKeyChecksumValue(key),key);
    }

    public static byte[] getKeyByKeyChecksumValue(byte[] keyChecksumValue) {
        if (keyChecksumValue != null) {
            Set<Map.Entry<byte[], byte[]>> entries = keysByKeyChecksumValue.entrySet();
            for (Map.Entry<byte[], byte[]> entry : entries) {
                if (Arrays.equals(entry.getKey(), keyChecksumValue)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public static byte[] calculateKeyChecksumValue(byte[] key) {
        byte[] KCVlong = encryptWithAESCBCNone(key,new byte[16]);
        return new byte[]{KCVlong[0], KCVlong[1], KCVlong[2]};
    }

    public static byte[] encryptWithAESCBCNone(byte[] key, byte[] data) {
        try {
            Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec skeySpec = new SecretKeySpec(key,"AES");
            c.init(Cipher.ENCRYPT_MODE,skeySpec, new IvParameterSpec(new byte[16]));
            return c.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] decryptWithAESCBCNone(byte[] key, byte[] data) {
        try {
            Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec skeySpec = new SecretKeySpec(key,"AES");
            c.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            return c.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] calculateAESCMAC(byte[] key, byte[] data) {
        byte[] result = new byte[16];
        CipherParameters params = new KeyParameter(key);
        BlockCipher aes = new AESEngine();
        CMac mac = new CMac(aes);
        mac.init(params);
        mac.update(data, 0, data.length);
        mac.doFinal(result, 0);
        return result;
    }

    public static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] incrementCounter(byte[] counter) {
        BigInteger b = new BigInteger(counter);
        byte[] r = b.add(BigInteger.ONE).toByteArray();
        if (r.length > counter.length) {
            r = new byte[]{0};
        }
        byte[] padding = new byte[counter.length-r.length];
        return HexUtils.concatArrays(padding,r);

    }

    public static byte[] padDataWithISO7816_4(byte[] data) {
        return HexUtils.concatArrays(data, Arrays.copyOf(PADDING_ISO7816_4, PADDING_ISO7816_4.length - (data.length % 16)));
    }



}
