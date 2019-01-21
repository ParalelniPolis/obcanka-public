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
package cz.paralelnipolis.obcanka.desktop.lib;

import cz.paralelnipolis.obcanka.core.communication.CardException;
import cz.paralelnipolis.obcanka.core.communication.ICardInterface;
import cz.paralelnipolis.obcanka.core.communication.ICommandAPDU;
import cz.paralelnipolis.obcanka.core.communication.IResponseAPDU;

import javax.smartcardio.*;
import java.util.List;

public class DesktopCardInterface implements ICardInterface {
    private Card card;
    private CardChannel channel;
    private byte[] atr;

    private DesktopCardInterface(Card card, CardChannel channel,byte[] atr) {
        this.card = card;
        this.atr = atr;
        this.channel = channel;
    }

    public static CardTerminal getCardTerminalWithCard() throws javax.smartcardio.CardException {

        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminals terminals = factory.terminals();


        List<CardTerminal> allTerminals = terminals.list();
        if (allTerminals.isEmpty()) {
            System.out.println("No CardTerminal seems to be inserted.");
            return null;
        }else {
            System.out.println("List of existing CardTerminals:");
            for (int i = 0; i < allTerminals.size(); i++) {
                CardTerminal cardTerminal = allTerminals.get(i);
                System.out.println("  "+  (i+1) + ". " + cardTerminal);
            }
        }
        List<CardTerminal> terminalsWithCard = terminals.list(CardTerminals.State.CARD_PRESENT);
        if (terminalsWithCard.isEmpty()) {
            System.out.println("Waiting for card to be inserted...");
            while (terminalsWithCard.isEmpty()) {
                terminals.waitForChange(1000);
                terminalsWithCard = terminals.list(CardTerminals.State.CARD_PRESENT);
            }
        }
        CardTerminal cardTerminal = terminalsWithCard.get(0);
        return cardTerminal;
    }

    public static DesktopCardInterface create() {
        try {
            CardTerminal terminal = getCardTerminalWithCard();
            return create(terminal.connect("T=0"));
        } catch (javax.smartcardio.CardException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static DesktopCardInterface create(Card card) {
        try {
            ATR atr = card.getATR();
            card.beginExclusive();
            return new DesktopCardInterface(card,card.getBasicChannel(),atr.getHistoricalBytes());
        } catch (javax.smartcardio.CardException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void destroy (){
        if (card != null) {
            try {
                card.endExclusive();
                card.disconnect(true);
            } catch (javax.smartcardio.CardException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public byte[] getATR() {
        return atr;
    }

    @Override
    public IResponseAPDU transmit(ICommandAPDU command) throws CardException {
        final ResponseAPDU r;
        try {
            r = channel.transmit(new CommandAPDU(command.getData()));
        } catch (javax.smartcardio.CardException e) {
            throw new CardException(e);
        }
        return new IResponseAPDU() {
            @Override
            public byte[] getBytes() {
                return r.getBytes();
            }

            @Override
            public byte[] getData() {
                return r.getData();
            }

            @Override
            public int getSW() {
                return r.getSW();
            }

            @Override
            public int getSW1() {
                return r.getSW1();
            }

            @Override
            public int getSW2() {
                return r.getSW2();
            }
        };
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
}
