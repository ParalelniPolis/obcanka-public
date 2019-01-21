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
package cz.paralelnipolis.obcanka.android.lib;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;
import cz.paralelnipolis.obcanka.core.HexUtils;
import cz.paralelnipolis.obcanka.core.communication.CardException;
import cz.paralelnipolis.obcanka.core.communication.ICardInterface;
import cz.paralelnipolis.obcanka.core.communication.ICommandAPDU;
import cz.paralelnipolis.obcanka.core.communication.IResponseAPDU;

public class AndroidCardInterface implements ICardInterface {
    private Reader reader;
    private int slotNumber;
    private byte[] atr;

    public AndroidCardInterface(Reader reader, int slotNumber) {
        this.reader = reader;
        this.slotNumber = slotNumber;
    }

    public static AndroidCardInterface create(Reader reader, int slotNumber) {
        return new AndroidCardInterface(reader,slotNumber);
    }

    @Override
    public IResponseAPDU transmit(ICommandAPDU command) throws CardException {
        try {
            if (reader.isOpened()) {
                if (reader.getState(slotNumber) == Reader.CARD_ABSENT) {
                    throw new CardException("Cannot transmit to card when is not present.");
                }else if (reader.getState(slotNumber) == Reader.CARD_PRESENT) {
                    System.out.println("state before power = " + reader.getState(slotNumber));
                    reader.power(slotNumber,Reader.CARD_WARM_RESET);
                    System.out.println("state after power = " + reader.getState(slotNumber));
                }

                if (reader.getState(slotNumber) == Reader.CARD_NEGOTIABLE) {
                    System.out.println("state before protocol = " + reader.getState(slotNumber));
                    int protocolResult = reader.setProtocol(slotNumber, Reader.PROTOCOL_T0);
                    System.out.println("protocolResult = " + protocolResult);
                    System.out.println("state after protocol = " + reader.getState(slotNumber));
                    atr = reader.getAtr(slotNumber);
                }
            }
        } catch (ReaderException e) {
            throw new CardException(e);
        }

        byte[] receiveBuffer = new byte[300];
        try {
            System.out.println("state before transmit = " + reader.getState(slotNumber));
            System.out.println(">>>>>Command: " + HexUtils.bytesToHexString(command.getData()));
            int bytesReceived = reader.transmit(slotNumber, command.getData(), command.getData().length, receiveBuffer, receiveBuffer.length);
            System.out.println("bytes Received = " + bytesReceived + " " + HexUtils.bytesToHexString(receiveBuffer));

            if (bytesReceived > 0) {
                final byte[] data = new byte[bytesReceived - 2];
                System.arraycopy(receiveBuffer, 0, data, 0, data.length);
                final byte[] fullResponse = new byte[bytesReceived];
                System.arraycopy(receiveBuffer, 0, fullResponse, 0, fullResponse.length);
                final int sw1 = (receiveBuffer[bytesReceived - 2]) & 0xFF;
                final int sw2 = receiveBuffer[bytesReceived - 1] & 0xFF;
                final int sw = sw1 << 8  | sw2;

                System.out.println(">>>>Response sw = " + Integer.toHexString(sw) + " data: " +  HexUtils.bytesToHexString(data));
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
            throw new CardException("No response.");
        } catch (ReaderException e) {
            throw new CardException(e);
        }

    }

    @Override
    public ICommandAPDU createCommand(final byte[] commandData) {
        return new ICommandAPDU() {
            @Override
            public byte[] getData() {
                return commandData;
            }
        };
    }

    @Override
    public byte[] getATR() {
        return atr;
    }
}
