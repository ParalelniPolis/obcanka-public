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
import cz.paralelnipolis.obcanka.core.communication.CardException;
import cz.paralelnipolis.obcanka.core.communication.ICardInterface;
import cz.paralelnipolis.obcanka.core.communication.ICommandAPDU;
import cz.paralelnipolis.obcanka.core.communication.IResponseAPDU;
import org.spongycastle.asn1.*;

import java.io.IOException;
import java.util.Arrays;

import static cz.paralelnipolis.obcanka.core.encryption.EncryptionUtils.PADDING_ISO7816_4;
import static cz.paralelnipolis.obcanka.core.encryption.EncryptionUtils.incrementCounter;


public class EncryptionToken {
    public static byte[] AES_ENC_KEY = new byte[]
            {
                    (byte) 0x69,
                    (byte) 0x92,
                    (byte) 0x8A,
                    (byte) 0xC3,
                    (byte) 0x9B,
                    (byte) 0xD1,
                    (byte) 0xC3,
                    (byte) 0x0F,
                    (byte) 0x13,
                    (byte) 0xF7,
                    (byte) 0x47,
                    (byte) 0x53,
                    (byte) 0xB4,
                    (byte) 0xBF,
                    (byte) 0x38,
                    (byte) 0x9A
            };

    public static byte[] MAC_KEY = new byte[]
            {
                    (byte) 0x66,
                    (byte) 0x5d,
                    (byte) 0x2A,
                    (byte) 0x6a,
                    (byte) 0x99,
                    (byte) 0x20,
                    (byte) 0x4e,
                    (byte) 0xab,
                    (byte) 0x9d,
                    (byte) 0x7b,
                    (byte) 0x55,
                    (byte) 0xae,
                    (byte) 0xb9,
                    (byte) 0x77,
                    (byte) 0xb5,
                    (byte) 0x0f
            };

    byte[] skEnc;
    byte[] skMac;
    byte[] counter;

    enum ApduCase
    {
        Unknown,
        Case1,
        Case2,
        Case3,
        Case4,
    }

    public EncryptionToken(byte[] skEnc, byte[] skMac, byte[] counter) {
        this.skEnc = skEnc;
        this.skMac = skMac;
        this.counter = counter;
    }

    @Override
    public String toString() {
        return "EncryptionToken{" +
                "skEnc=" +     HexUtils.bytesToHexString(skEnc) +
                ", skMac=" +   HexUtils.bytesToHexString(skMac) +
                ", counter=" + HexUtils.bytesToHexString(counter) +
                '}';
    }

    public IResponseAPDU transmit(ICardInterface ci, ICommandAPDU command) throws CardException {
        ICommandAPDU wrappedCommand = ci.createCommand(wrapCommand(command));

        IResponseAPDU response = ci.transmit(wrappedCommand);
        return createResponse(unwrapResponse(response));
    }

    private static IResponseAPDU createResponse(final byte[] receiveBuffer) {
        int bytesReceived = receiveBuffer.length;

        final byte[] data = new byte[bytesReceived - 2];
        System.arraycopy(receiveBuffer, 0, data, 0, data.length);
        final byte[] fullResponse = new byte[bytesReceived];
        System.arraycopy(receiveBuffer, 0, fullResponse, 0, fullResponse.length);
        final int sw1 = (receiveBuffer[bytesReceived - 2]) & 0xFF;
        final int sw2 = receiveBuffer[bytesReceived - 1] & 0xFF;
        final int sw = sw1 << 8  | sw2;

        return new IResponseAPDU() {
            @Override
            public byte[] getBytes() {
                return fullResponse;
            }

            @Override
            public byte[] getData() {
                return data;
            }

            @Override
            public int getSW() {
                return sw;
            }

            @Override
            public int getSW1() {
                return sw1;
            }

            @Override
            public int getSW2() {
                return sw2;
            }
        };
    }

    private ApduCase detectApduCommandCase(ICommandAPDU command)
    {
        ApduCase apduCase = ApduCase.Unknown;
        byte[] commandData = command.getData();
        if (commandData.length == 4) {
            apduCase = ApduCase.Case1;
        } else if (commandData.length == 5) {
            apduCase = ApduCase.Case2;
        } else if (commandData.length > 5 && commandData[4] > (byte) 0){
            if (commandData.length == 5 + (int) commandData[4]) {
                apduCase = ApduCase.Case3;
            } else if (commandData.length == 5 + (int) commandData[4] + 1) {
                apduCase = ApduCase.Case4;
            }
        }
        return apduCase;
    }

    private byte[] wrapCommand(ICommandAPDU command) throws CardException {
        ApduCase apduCase = detectApduCommandCase(command);
        if (apduCase == ApduCase.Unknown)
            return null;

        byte[] commandData = command.getData();
        byte[] commandHeader = Arrays.copyOfRange(commandData,0,4);
        byte[] tagLe = null;
        byte[] data = null;
        if (apduCase == ApduCase.Case2) {
            tagLe = new byte[]{
                    (byte) 151,
                    (byte) 0x01,
                    commandData[commandData.length - 1]
            };
        } else if (apduCase == ApduCase.Case3 || apduCase == ApduCase.Case4) {
            data =  Arrays.copyOfRange(commandData,5,5 + commandData[4]);
        }

        counter = EncryptionUtils.incrementCounter(counter);
        byte[] encryptedCommand = encryptCommand(commandHeader, data);
        if (encryptedCommand != null && encryptedCommand.length > 231) {
            throw new CardException("Command is too long to be wrapped.");
        }
        byte[] mac = calculateMAC(commandHeader, tagLe, encryptedCommand);
        return composeCommand(commandHeader, tagLe, encryptedCommand, mac);
    }

    private static byte[] composeCommand(byte[] commandHeader, byte[] lenExpected, byte[] encryptedCommand, byte[] mac) {
        byte[] message = HexUtils.concatArrays (commandHeader, new byte[1]);
        int size = 0;
        if (lenExpected != null) {
            message = HexUtils.concatArrays(message, lenExpected);
            size += lenExpected.length;
        }
        if (encryptedCommand != null) {
            message = HexUtils.concatArrays(message, encryptedCommand);
            size += encryptedCommand.length;
        }

        message = HexUtils.concatArrays(message,mac);
        size = size + mac.length;
        byte[] result = HexUtils.concatArrays(message, new byte[1]);
        result[0] |= 0x0C; //set class
        result[4] = size < 256 ? (byte) size : 0; //set length
        return result;
    }

    private byte[] encryptCommand(byte[] header, byte[] data) {
        byte[] result = null;
        if (data != null && data.length != 0)
        {
            byte[] counterWithData = HexUtils.concatArrays(counter, data);
            byte[] encryptedCounterWithData = EncryptionUtils.encryptWithAESCBCNone(skEnc, EncryptionUtils.padDataWithISO7816_4(counterWithData));
            byte[] encryptedDataWithout16bytes = Arrays.copyOfRange(encryptedCounterWithData,16,encryptedCounterWithData.length - 1);
            boolean flag = header[1] % 2 != 0;
            if (!flag) {
                encryptedDataWithout16bytes = HexUtils.concatArrays(new byte[]{1}, encryptedDataWithout16bytes);
            }
            try {
                result = new DERTaggedObject(false, flag ? 5 : 7,  new DEROctetString(encryptedDataWithout16bytes)).getEncoded();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private byte[] calculateMAC(byte[] header, byte[] tagLe, byte[] encryptedData) {
        byte[] toBeMaced = HexUtils.concatArrays(counter, header, Arrays.copyOf(EncryptionUtils.PADDING_ISO7816_4,12));
        if (tagLe != null) {
            toBeMaced = HexUtils.concatArrays(toBeMaced, tagLe);
        }

        if (encryptedData != null) {
            toBeMaced = HexUtils.concatArrays(toBeMaced, encryptedData);
        }

        byte[] paddedDataToBeMaced = EncryptionUtils.padDataWithISO7816_4(toBeMaced);
        paddedDataToBeMaced[16] |= (byte) 12;
        try {
            return new DERTaggedObject(false, 14, new DEROctetString(EncryptionUtils.calculateAESCMAC(skMac, paddedDataToBeMaced))).getEncoded();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] unwrapResponse(IResponseAPDU response) throws CardException {
        byte[] numArray = null;
        byte[] responseBytes  = response.getBytes();
        if (responseBytes.length == 2) {
            numArray = responseBytes;
        } else {
            incrementCounter(counter);

            try {
                ASN1EncodableVector asn1Objects = responseToASN1(responseBytes);
                ASN1Object macOBj = getAsn1Object(asn1Objects, 0x8e);
                ASN1Object swObj = getAsn1Object(asn1Objects, 0x99); //SW result
                ASN1Object encryptedDataA = getAsn1Object(asn1Objects, 0x85);
                ASN1Object encryptedDataB = getAsn1Object(asn1Objects, 0x87);
                byte[] mac = ((ASN1OctetString) ((ASN1TaggedObject) macOBj).getObject()).getOctets();
                if (!verifyMAC(swObj.getEncoded(), encryptedDataA != null ? encryptedDataA.getEncoded() : encryptedDataB.getEncoded(), mac)) {
                    throw new CardException("MAC doesn't match");
                }
                numArray = ((ASN1OctetString) ((ASN1TaggedObject) swObj).getObject()).getOctets();
                if (!Arrays.equals(numArray, Arrays.copyOfRange(responseBytes, responseBytes.length - 2, responseBytes.length))) {
                    throw new CardException("SW does not match.");
                }
                byte[] encData = null;
                if (encryptedDataA != null) {
                    encData = ((ASN1OctetString) ((ASN1TaggedObject) encryptedDataA).getObject()).getOctets();
                }
                if (encryptedDataB != null) {
                    byte[] octets = ((ASN1OctetString) ((ASN1TaggedObject) encryptedDataB).getObject()).getOctets();
                    encData = Arrays.copyOfRange(octets, 1, octets.length); //skip first byte
                }
                if (encData != null) {
                    numArray = HexUtils.concatArrays(decryptData(encData), numArray);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return numArray;
    }

    private ASN1Object getAsn1Object(ASN1EncodableVector vector, int tagId) {
        for (int i = 0; i < vector.size(); i++) {
            ASN1Encodable asn1Encodable = vector.get(i);
            ASN1Primitive p = asn1Encodable.toASN1Primitive();
            if (p instanceof ASN1TaggedObject) {
                int tagNo = ((ASN1TaggedObject) p).getTagNo();
                if (tagNo == tagId) {
                    return p;
                }
            }
        }
        return null;
    }

    private boolean verifyMAC(byte[] sw, byte[] encryptedData, byte[] testMAC) {
        byte[] content = counter;
        if (encryptedData != null) {
            content = HexUtils.concatArrays(content,encryptedData);
        }
        byte[] paddedContent = EncryptionUtils.padDataWithISO7816_4(HexUtils.concatArrays(content, sw));
        byte[] calculatedMAC = EncryptionUtils.calculateAESCMAC(skMac, paddedContent);
        return Arrays.equals(testMAC,calculatedMAC);
    }

    private byte[] decryptData(byte[] encData) {
         return EncryptionUtils.decryptWithAESCBCNone(skEnc, HexUtils.concatArrays(EncryptionUtils.encryptWithAESCBCNone(skEnc, counter),encData));
         //TODO: Remove padding
          //.ToArray<byte>())).Skip<byte>(16).ToArray<byte>()).Reverse<byte>().SkipWhile<byte>(AesSecureMessaging.\u003C\u003Ec.\u003C\u003E9__16_0 ?? (AesSecureMessaging.\u003C\u003Ec.\u003C\u003E9__16_0 = new Func<byte, bool>((object) AesSecureMessaging.\u003C\u003Ec.\u003C\u003E9, __methodptr(\u003CDecryptData\u003Eb__16_0)))).Skip<byte>(1).Reverse<byte>().ToArray<byte>();
    }

    private static ASN1EncodableVector responseToASN1(byte[] buffer) throws IOException {
        ASN1EncodableVector result = new ASN1EncodableVector();
        ASN1InputStream asn1Is = new ASN1InputStream(Arrays.copyOfRange(buffer, 0, buffer.length - 2), false);
        ASN1Object asnObj;
        while ((asnObj = asn1Is.readObject()) != null) {
            result.add(asnObj);
        }
        return result;
    }


}
