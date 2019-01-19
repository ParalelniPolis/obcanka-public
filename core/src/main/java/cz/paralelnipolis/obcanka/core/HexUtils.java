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
package cz.paralelnipolis.obcanka.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HexUtils {
    public static byte[] hexStringToByteArray(String s) {
        s = s.replace(" ","").replace("\n","");
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    public static String bytesToHexStringWithSpaces(byte[] bytes) {
        char[] chars = bytesToHexString(bytes).toCharArray();
        StringBuilder sb = new StringBuilder();
        hexWithSpaces(chars, sb);
        return sb.toString();
    }

    private static void hexWithSpaces(char[] chars, StringBuilder sb) {
        for (int i = 0; i < chars.length; i+=2) {
            char a = chars[i];
            char b = chars[i+1];
            sb.append(a);
            sb.append(b);
            if (i+1<chars.length-1) {
                sb.append(" ");
            }
        }
    }

    public static String bytesToHexStringWithSpacesAndAscii(byte[] bytes) {
        char[] chars = bytesToHexString(bytes).toCharArray();
        StringBuilder sb = new StringBuilder();
        hexWithSpaces(chars, sb);
        return sb.toString() + " --- " + new String(bytes);
    }

    public static String bytesToHexString(byte[] bytes) {

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static int byteToInt(int val) {
        if (val < 0 ) {
            val = val +256;
        }
        return val;
    }

    public static byte[] concatArrays(byte[] ... arrays ) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < arrays.length; i++) {
            try {
                bos.write(arrays[i]);
            } catch (IOException e) {
            }
        }
        return bos.toByteArray();
    }
}
