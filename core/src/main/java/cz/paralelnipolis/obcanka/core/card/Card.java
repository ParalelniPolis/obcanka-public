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
package cz.paralelnipolis.obcanka.core.card;

import cz.paralelnipolis.obcanka.core.HexUtils;
import cz.paralelnipolis.obcanka.core.card.enums.*;
import cz.paralelnipolis.obcanka.core.certificates.Certificate;
import cz.paralelnipolis.obcanka.core.communication.ICommandAPDU;
import cz.paralelnipolis.obcanka.core.encryption.EncryptionToken;
import cz.paralelnipolis.obcanka.core.encryption.EncryptionUtils;
import cz.paralelnipolis.obcanka.core.encryption.MasterSakC;
import cz.paralelnipolis.obcanka.core.encryption.SakC;
import cz.paralelnipolis.obcanka.core.communication.CardException;
import cz.paralelnipolis.obcanka.core.communication.ICardInterface;
import cz.paralelnipolis.obcanka.core.communication.IResponseAPDU;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;

public class Card {

    public static final int TAG_ID_CARD_NUMBER = 1;
    public static final int TAG_ID_CERTIFICATE_SERIAL_NUMBER = 2;
    public static final int TAG_ID_KEY_KCV = 0xC0;
    public static final int TAG_ID_KEY_COUNTER = 0xC1;

    public static final int TAG_ID_DOK_STATE = 0x8B;
    public static final int TAG_ID_DOK_TRY_LIMIT = 0x8C;
    public static final int TAG_ID_DOK_MAX_TRY_LIMIT = 0x8D;

    public static final int TAG_ID_IOK_STATE = 0x82;
    public static final int TAG_ID_IOK_TRY_LIMIT = 0x83;
    public static final int TAG_ID_IOK_MAX_TRY_LIMIT = 0x84;


    private static byte[] APP_ID_CARD_MANAGEMENT = {(byte) 0xD2, 0X03, 0x10, 0x01, 0x00, 0x01, 0x00, 0x02, 0x02};
    private static byte[] APP_ID_FILE_MANAGEMENT = {(byte) 0xD2, 0x03, 0x10, 0x01, 0x00, 0x01, 0x03, 0x02, 0x01, 0x00};

    private static int FILE_ID_CERTIFICATE_AUTHORIZATION = 0x0132;
    private static int FILE_ID_CERTIFICATE_IDENTIFICATION = 0x0001;

    private ICardInterface c;
    private byte[] currentApplication;

    public Card(ICardInterface c) {
        this.c = c;
    }


    public boolean selectApplication(byte[] appId) throws CardException {
        if (currentApplication != null) {
            if (Arrays.equals(currentApplication, appId)) { //Don't select application if is already set
                return true;
            }
        }

        byte[] selectApplet = new byte[]{
                0x00, (byte) 0xA4, 0x04, 0x0C, (byte) appId.length,
        };
        IResponseAPDU r = c.transmit(c.createCommand((HexUtils.concatArrays(selectApplet, appId))));

        if (r.getSW() == 0x9000) {
            currentApplication = appId;
            return true;
        }
        return false;
    }

    public String getCardNumber() throws CardException {
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            byte[] data = getData(TAG_ID_CARD_NUMBER, 0);
            if (data != null) {
                return new String(data, Charset.forName("UTF-8"));
            }
        }
        return null;
    }

    public byte[] getSerialNumber() throws CardException {
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            byte[] data = getData(TAG_ID_CERTIFICATE_SERIAL_NUMBER, 0);
            if (data != null) {
                return data;
            }
        }
        return null;
    }


    public byte[] getKeyChecksumValue() throws CardException {
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            byte[] data = getData(TAG_ID_KEY_KCV, 1);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    public byte[] getData(int tagId, int authId) throws CardException {
        authId = authId << 4;
        byte[] request = new byte[]{
                (byte) 0x80, (byte) 0xCA,
                (byte) (authId | 1),
                (byte) (authId | tagId), 0
        };

        IResponseAPDU r = c.transmit(c.createCommand(request));
        if (r.getSW() == 0x9000) {
            return r.getData();
        } else if (r.getSW1() == 0x6c) {
            request[request.length - 1] = (byte) r.getSW2();
            r = c.transmit(c.createCommand(request));
            if (r.getSW() == 0x9000) {
                return r.getData();
            }
        }
        return null;
    }

    public DokState getDokState() throws CardException {
        DokState result = DokState.None;
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            byte[] data = getData(TAG_ID_DOK_STATE, 0);
            if (data != null) {
                int state = HexUtils.byteToInt(data[0]);
                if ((state & 1) == 1) {
                    result = DokState.Normal;
                }
                if ((state & 4) == 4) {
                    result = DokState.Blocked;
                }
            }
        }
        return result;
    }

    public int getDokTryLimit() throws CardException {
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            byte[] data = getData(TAG_ID_DOK_TRY_LIMIT, 0);
            if (data != null) {
                return HexUtils.byteToInt(data[0]);
            }
        }
        return -1;
    }

    public int getDokMaxTryLimit() throws CardException {
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            byte[] data = getData(TAG_ID_DOK_MAX_TRY_LIMIT, 0);
            if (data != null) {
                return HexUtils.byteToInt(data[0]);
            }
        }
        return -1;
    }


    public IokState getIokState() throws CardException {
        IokState result = IokState.None;
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            byte[] data = getData(TAG_ID_IOK_STATE, 0);
            if (data != null) {
                int state = HexUtils.byteToInt(data[0]);
                if ((state & 1) == 1) {
                    result = IokState.Normal;
                }
                if ((state & 4) == 4) {
                    result = IokState.Blocked;
                }
            }
        }
        return result;
    }

    public int getIokTryLimit() throws CardException {
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            byte[] data = getData(TAG_ID_IOK_TRY_LIMIT, 0);
            if (data != null) {
                return HexUtils.byteToInt(data[0]);
            }
        }
        return -1;
    }

    public int getIokMaxTryLimit() throws CardException {
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            byte[] data = getData(TAG_ID_IOK_MAX_TRY_LIMIT, 0);
            if (data != null) {
                return HexUtils.byteToInt(data[0]);
            }
        }
        return -1;
    }

    public CardAuthorizationResult changePIN(PINType pinType, String oldPIN, String newPIN) throws CardException {
        int securityCode = 0;
        if (pinType == PINType.IOK) {
            securityCode = 17; //0x11
        } else if (pinType == PINType.DOK) {
            securityCode = 16; //0x10
        } else {
            throw new IllegalArgumentException("Unsupported PIN.");
        }
        if (oldPIN == null || newPIN == null) {
            throw new IllegalArgumentException("PIN cannot be null.");
        }
        if (oldPIN.length() < 4 || oldPIN.length() > 10) {
            throw new IllegalArgumentException("Invalid length of PIN.");
        }
        if (newPIN.length() < 4 || newPIN.length() > 10) {
            throw new IllegalArgumentException("Invalid length of PIN.");
        }
        return changeOrUnblockPIN(ChangeReason.DifferentValue, securityCode, oldPIN, newPIN);
    }

    public CardAuthorizationResult unblockIOK(String dokPIN, String newIOKPIN) throws CardException {
        int securityCode = 17; // IOK

        if (dokPIN == null || newIOKPIN == null) {
            throw new IllegalArgumentException("PIN cannot be null.");
        }
        if (dokPIN.length() < 4 || dokPIN.length() > 10) {
            throw new IllegalArgumentException("Invalid length of PIN.");
        }
        if (newIOKPIN.length() < 4 || newIOKPIN.length() > 10) {
            throw new IllegalArgumentException("Invalid length of PIN.");
        }
        return changeOrUnblockPIN(ChangeReason.Unblock, securityCode, dokPIN, newIOKPIN);
    }

    private CardAuthorizationResult changeOrUnblockPIN(ChangeReason reason, int securityCode, String pinA, String pinB) throws CardException {
        if (selectApplication(APP_ID_CARD_MANAGEMENT)) {
            CardAuthorizationResult result = CardAuthorizationResult.OK;

            byte[] request = new byte[25];
            Arrays.fill(request, (byte) 0xFF);
            request[0] = (byte) 0;
            request[1] = (byte) 0x24;
            if (reason == ChangeReason.DifferentValue) {
                request[2] = (byte) 0;
            } else if (reason == ChangeReason.Unblock) {
                request[2] = (byte) 1;
            }
            request[3] = (byte) securityCode;
            request[4] = (byte) (request.length - 5);
            System.arraycopy(pinA.getBytes(), 0, request, 5, pinA.getBytes().length);
            System.arraycopy(pinB.getBytes(), 0, request, 15, pinB.getBytes().length);

            IResponseAPDU r = c.transmit(c.createCommand(encryptAPDU(request)));
            int sw = r.getSW();

            if ((sw & 0xFFF0) == 0x63C0 || sw == 0x6983) {
                //some problem
                int attemptsRemaining = sw & 0x0F;
                if (attemptsRemaining == 0) {
                    return CardAuthorizationResult.Blocked;
                }
                result = CardAuthorizationResult.WrongEntry;
                result.setAttemptsRemaining(attemptsRemaining);
                return result;
            } else if (sw != 0x9000) {
                return CardAuthorizationResult.CardCommunicationProblem;
            }
            return result;
        } else {
            return CardAuthorizationResult.CardCommunicationProblem;
        }
    }

    private byte[] encryptAPDU(byte[] apduRequest) throws CardException {
        byte[] cardNumber = getData(TAG_ID_CARD_NUMBER, 0);
        byte[] serialNumber = getSerialNumber();
        byte[] keyChecksumValue = getKeyChecksumValue();
        byte[] keyCounter = getData(TAG_ID_KEY_COUNTER, 1);

        byte[] encKey = EncryptionUtils.getKeyByKeyChecksumValue(keyChecksumValue);
        if (encKey != null) {
            MasterSakC m = new MasterSakC(encKey);
            SakC sakC = m.deriveEncryptionKey(serialNumber, cardNumber);
            byte[] commandHeader = Arrays.copyOf(apduRequest, 5);
            byte[] payload = Arrays.copyOfRange(apduRequest, 5, apduRequest.length);
            byte[] encryptedPayload = sakC.encryptSecureCodeData(keyCounter, payload);

            commandHeader[0] = (byte) 0x8C;
            commandHeader[4] = (byte) encryptedPayload.length;
            return HexUtils.concatArrays(commandHeader, encryptedPayload);
        }

        return null;
    }

    public Certificate getCertificate(Certificate.CertificateType type) throws CardException {
        selectApplication(APP_ID_FILE_MANAGEMENT);
        if (type == Certificate.CertificateType.AUTHORIZATION) {
            return Certificate.parse(readFile(FILE_ID_CERTIFICATE_AUTHORIZATION), type);
        }
        if (type == Certificate.CertificateType.IDENTIFICATION) {
            return Certificate.parse(readFile(FILE_ID_CERTIFICATE_IDENTIFICATION), type);
        } else {
            return null;
        }
    }

    public byte[] readFile(int fileId) throws CardException {
        int h = fileId / 256;
        int l = fileId % 256;

        byte[] fileInfo = new byte[]{0x00, (byte) 0xA4, 0x08, 0x00, 0x02, (byte) h, (byte) l};
        IResponseAPDU r = c.transmit(c.createCommand(fileInfo));
        if (r.getSW1() == 0x61) { //you must call get response
            byte[] getResponse = new byte[]{0x00, (byte) 0xC0, 0, 0, (byte) r.getSW2()};
            r = c.transmit(c.createCommand(getResponse));
        }


        if (r.getSW() == 0x9000) {
            int size = 0xD0;
            //file exists
            int offset = 0;
            byte[] fileInfoData = r.getData();
            int fileSize = HexUtils.byteToInt(fileInfoData[4]) * 256 + HexUtils.byteToInt(fileInfoData[5]);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            do {
                h = offset / 256;
                l = offset % 256;
                if (offset + size > fileSize) {
                    size = fileSize - offset;
                }
                if (size <= 0) {
                    break;
                }
                byte[] readFileRequest = new byte[]{0x00, (byte) 0xB0, (byte) h, (byte) l, (byte) size};
                r = c.transmit(c.createCommand(readFileRequest));
                if (r.getSW() == 0x9000) {
                    try {
                        bos.write(r.getData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    offset += size;
                }
            } while (r.getSW() == 0x9000);
            return bos.toByteArray();
        }

        return null;
    }

    public EncryptionToken createEncryptionToken() throws CardException {
        byte[] request = new byte[]
                {
                        (byte) 0x00,
                        (byte) 0xCA, //get data
                        (byte) 0x9F, //tag id
                        (byte) 0x7F, //tag id
                        (byte) 0x00
                };
        ICommandAPDU command = c.createCommand(request);
        IResponseAPDU r = c.transmit(command);

        if (r.getSW() != 0x9000) {
            throw new CardException("Cold not find ASN1 tag");
        }
        byte[] asn1Tag = r.getBytes();
        System.out.println("ASN1 = " + HexUtils.bytesToHexString(asn1Tag)); //9f7f2a 409078971291615304008017570c1c60a79c129280171293801712948017000000010001183400000000 9000

        request = new byte[] // MANAGE SECURITY ENVIRONMENT, MSE SET
                {
                        (byte) 0x00,
                        (byte) 0x22,
                        (byte) 0xC1,
                        (byte) 0xA4,
                        (byte) 0x09, //8+len(pace_oid)
                        (byte) 0x80,
                        (byte) 0x01, //pace_oid
                        (byte) 0x8C, //pace_oid
                        (byte) 0x83,
                        (byte) 0x01,
                        (byte) 0x01, //pw_ref
                        (byte) 0x95, //pw_ref
                        (byte) 0x01, //pw_ref
                        (byte) 0x80  //pw_ref
                };
        command = c.createCommand(request);
        r = c.transmit(command);

        if (r.getSW() != 0x9000) {
            throw new CardException("Setting security environment failed.");
        }
        System.out.println("Some response = " + HexUtils.bytesToHexString(r.getBytes())); //9000


        request = new byte[]
                {
                        (byte) 0x00,
                        (byte) 0x84, //GET CHALLENGE 6.15
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x08 // 8 bytes of challenge
                };
        command = c.createCommand(request);
        r = c.transmit(command);

        if (r.getSW() != 0x9000) {
            throw new CardException("Failed to obtain challenge.");
        }
        System.out.println("Some response = " + HexUtils.bytesToHexString(r.getBytes())); //6655e688cfb5b31c 9000

        SecureRandom rnd = new SecureRandom();
        byte[] randomBytesA = new byte[8];
        byte[] randomBytesB = new byte[8];
        byte[] randomBytesC = new byte[32];

        rnd.nextBytes(randomBytesA);
        rnd.nextBytes(randomBytesB);
        rnd.nextBytes(randomBytesC);

        byte[] challange = r.getData(); //8 bytes

        byte[] partOfASN1 = Arrays.copyOfRange(asn1Tag, 3 + 10, 3 + 10 + 8); // 8017570c1c60a79c
        byte[] informationWithRandomData = HexUtils.concatArrays(randomBytesA, randomBytesB, challange, partOfASN1, randomBytesC);
        byte[] informationEncryptedWithAES = EncryptionUtils.encryptWithAESCBCNone(EncryptionToken.AES_ENC_KEY, informationWithRandomData);
        byte[] macOfEncryptedInformation = EncryptionUtils.calculateAESCMAC(EncryptionToken.MAC_KEY, HexUtils.concatArrays(new byte[16], informationEncryptedWithAES));
        byte[] array4 = HexUtils.concatArrays(informationEncryptedWithAES, macOfEncryptedInformation);

        request = new byte[]
                {
                        (byte) 0x00,
                        (byte) 0x82, //INS_EXTERNAL_AUTHENTICATE
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) array4.length//len ( should be 80 bytes.
                };
        request = HexUtils.concatArrays(request, array4, new byte[]{0x00});
        System.out.println("Long request = " + request.length + " " + array4.length);
        command = c.createCommand(request);
        r = c.transmit(command);
        byte[] response = r.getBytes();
        System.out.println("response = " + HexUtils.bytesToHexString(response));
        if (r.getSW() != 0x9000) {
            throw new CardException("Failed to authenticate.");
        }

        byte[] testMAC = Arrays.copyOfRange(response, 64, 64 + 16);
        byte[] encryptedData = Arrays.copyOfRange(response, 0, 64);

        byte[] calculatedMAC = EncryptionUtils.calculateAESCMAC(EncryptionToken.MAC_KEY, HexUtils.concatArrays(new byte[16], encryptedData));
        if (!Arrays.equals(testMAC, calculatedMAC)) {
            throw new CardException("MAC did not match.");
        }
        byte[] decryptedData = EncryptionUtils.decryptWithAESCBCNone(EncryptionToken.AES_ENC_KEY, encryptedData);
        byte[] testRandomBytesA = Arrays.copyOfRange(decryptedData, 16, 16 + 8);
        byte[] testRandomBytesB = Arrays.copyOfRange(decryptedData, 24, 24 + 8);
        if (!Arrays.equals(testRandomBytesA, randomBytesA)) {
            throw new CardException("Random values didn't match");
        }

        if (!Arrays.equals(testRandomBytesB, randomBytesB)) {
            throw new CardException("Random values didn't match");
        }

        byte[] xorMask = Arrays.copyOfRange(decryptedData, 32, 32 + 32);
        byte[] inputForKeyDerivation = new byte[32];
        for (int index = 0; index < inputForKeyDerivation.length; index++)
            inputForKeyDerivation[index] = (byte) (randomBytesC[index] ^ xorMask[index]);

        byte[] skEnc = Arrays.copyOfRange(EncryptionUtils.sha256(HexUtils.concatArrays(inputForKeyDerivation,
                new byte[]{
                        (byte) 0,
                        (byte) 0,
                        (byte) 0,
                        (byte) 1
                })), 0, 16);
        byte[] skMac = Arrays.copyOfRange(EncryptionUtils.sha256(HexUtils.concatArrays(inputForKeyDerivation,
                new byte[]{
                        (byte) 0,
                        (byte) 0,
                        (byte) 0,
                        (byte) 2
                })), 0, 16);

        byte[] counter = HexUtils.concatArrays(challange, randomBytesA);
        return new EncryptionToken(skEnc, skMac, counter);
    }
}
