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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ScriptEnvelope {
    private Algorithm algorithm;
    private int keyId; //used only when  Algorithm.RSASignatureSHA256 is used
    private Script script;
    private byte[] hashOrSignature;


    private ScriptEnvelope(Algorithm algorithm, int keyId, Script script, byte[] hashOrSignature) {
        this.algorithm = algorithm;
        this.keyId = keyId;
        this.script = script;
        this.hashOrSignature = hashOrSignature;
    }

    public static ScriptEnvelope parse(byte[] envelope) {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(envelope));
        try {
            Algorithm algorithm = Algorithm.fromId(HexUtils.byteToInt(dis.readByte()));
            int keyId = HexUtils.byteToInt(dis.readByte());

            int scriptLen = dis.readInt();
            byte[] script = new byte[scriptLen];
            dis.read(script);
            byte[] hashOrSignature = null;
            if (Algorithm.None != algorithm) {
                int hashOrSignatureLen = dis.readInt();
                hashOrSignature = new byte[hashOrSignatureLen];
                dis.read(hashOrSignature);

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] calculatedHash = digest.digest(script);
                if (!Arrays.equals(calculatedHash, hashOrSignature)){
                    System.out.println("WARNING : Response hash does not match.");
                }
            }
            dis.close();
            Script s = Script.parse(script);
            return new ScriptEnvelope(algorithm,keyId,s,hashOrSignature);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public int getKeyId() {
        return keyId;
    }

    public Script getScript() {
        return script;
    }

    public byte[] getHashOrSignature() {
        return hashOrSignature;
    }

    @Override
    public String toString() {
        return "ScriptEnvelope{" +
                "algorithm=" + algorithm +
                ", keyId=" + keyId +
                ", script=\n" + script +
                ",\n hashOrSignature=" + HexUtils.bytesToHexStringWithSpaces(hashOrSignature) +
                '}';
    }
}
