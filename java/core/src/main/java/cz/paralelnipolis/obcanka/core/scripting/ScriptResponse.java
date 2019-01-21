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

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScriptResponse {
    private Algorithm algorithm;
    private int keyId; //used only when  Algorithm.RSASignatureSHA256 is used
    private List<ResponseBlock> responseBlocks;
    private byte[] hashOrSignature;
    private byte[] returnCode;

    private ScriptResponse(Algorithm algorithm, int keyId, byte[] returnCode, List<ResponseBlock> responseBlocks, byte[] hashOrSignature) {
        this.algorithm = algorithm;
        this.keyId = keyId;
        this.responseBlocks = responseBlocks;
        this.hashOrSignature = hashOrSignature;
        this.returnCode = returnCode;
    }

    public static ScriptResponse build(Algorithm algorithm, int keyId, byte[] returnCode, List<ResponseBlock> responseBlocks) {
        ScriptResponse response = new ScriptResponse(algorithm,keyId,returnCode,responseBlocks,null);
        return response;
    }

    private byte[] calculateHash() {
        if (algorithm == Algorithm.SHA256) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return digest.digest(toByteArray(returnCode,responseBlocks));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte((byte)algorithm.getId());
            dos.writeByte((byte)keyId);
            byte[] blocks = toByteArray(returnCode, responseBlocks);
            dos.writeInt(blocks.length);
            dos.write(blocks);
            if (algorithm == Algorithm.SHA256) {
                byte[] hash = calculateHash();
                dos.writeInt(hash.length);
                dos.write(hash);
            }
            dos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] toByteArray(byte[] returnCode, List<ResponseBlock> responseBlocks) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            for (int i = 0; i < responseBlocks.size(); i++) {
                ResponseBlock block = responseBlocks.get(i);
                dos.write(block.getLabel().getValue());
                dos.writeInt(block.getResponse().length);
                dos.write(block.getResponse());
            }
            dos.close();
            byte[] blocks = bos.toByteArray();
            bos = new ByteArrayOutputStream();
            dos = new DataOutputStream(bos);
            dos.write(returnCode);
            dos.writeInt(blocks.length);
            dos.write(blocks);
            dos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ScriptResponse parse(byte[] data) {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        try {
            Algorithm algorithm = Algorithm.fromId(HexUtils.byteToInt(dis.readByte()));
            int keyId = HexUtils.byteToInt(dis.readByte());

            int responseBlocksLen = dis.readInt();
            byte[] responseBlocks = new byte[responseBlocksLen];
            dis.read(responseBlocks);
            byte[] hashOrSignature = null;
            if (Algorithm.None != algorithm) {
                int hashOrSignatureLen = dis.readInt();
                hashOrSignature = new byte[hashOrSignatureLen];
                dis.read(hashOrSignature);

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] calculatedHash = digest.digest(responseBlocks);
                if (!Arrays.equals(calculatedHash, hashOrSignature)){
                    System.out.println("WARNING : Script hash does not match.");
                }
            }
            dis.close();
            byte[] returnCode = new byte[4];
            List<ResponseBlock> blocks = new ArrayList<>();
            try {
                dis = new DataInputStream(new ByteArrayInputStream(responseBlocks));
                dis.read(returnCode);
                dis.readInt(); //read total size of all blocks
                do {
                    //parse command
                    byte[] labelId = new byte[4];
                    dis.read(labelId);
                    int responseDataSize = dis.readInt();
                    byte[] responseData = new byte[responseDataSize];
                    dis.read(responseData);

                    blocks.add(new ResponseBlock(new Label(labelId),responseData));
                }while (dis.available() > 0);
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new ScriptResponse(algorithm,keyId,returnCode,blocks,hashOrSignature);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScriptResponse{" +
                "algorithm=" + algorithm +
                ", keyId=" + keyId +
                ", returnCode=" + HexUtils.bytesToHexString(returnCode) +
                ", responseBlocks=\n" );
        for (ResponseBlock block : responseBlocks) {
            sb.append("  " +block.getLabel() +": " + HexUtils.bytesToHexStringWithSpaces(block.getResponse()) +"\n");
        }
        sb.append(
                ", hashOrSignature=" + HexUtils.bytesToHexStringWithSpaces(hashOrSignature) +
                '}');
        return sb.toString();
    }
}
