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
package cz.paralelnipolis.obcanka.desktop.app;

import cz.paralelnipolis.obcanka.core.HexUtils;
import cz.paralelnipolis.obcanka.core.card.Card;
import cz.paralelnipolis.obcanka.core.certificates.Certificate;
import cz.paralelnipolis.obcanka.core.communication.CardException;
import cz.paralelnipolis.obcanka.core.communication.ICardInterface;
import cz.paralelnipolis.obcanka.desktop.lib.DesktopCardInterface;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This is example desktop command line application that reads certificates from card and writes them on the filesystem.
 */
public class HelloWorld {
    public static void main(String[] args) {
        DesktopCardInterface ci = DesktopCardInterface.create();
        try {
            downloadCertificates(ci);
        } catch (CardException e) {
            e.printStackTrace();
        }
    }

    private static void downloadCertificates(ICardInterface ci) throws CardException {
        Card cm = new Card(ci);
        String cardID = cm.getCardNumber();
        System.out.println("cardID = " + cardID);
        byte[] serialNumber = cm.getSerialNumber();
        System.out.println("serialNumber = " + HexUtils.bytesToHexStringWithSpacesAndAscii(serialNumber));
        System.out.println("dokState = " + cm.getDokState());
        System.out.println("DokTryLimit() = " + cm.getDokTryLimit());
        System.out.println("DokMaxTryLimit = " + cm.getDokMaxTryLimit());

        System.out.println("iokState = " + cm.getIokState());
        System.out.println("cm.getIokMaxTryLimit() = " + cm.getIokMaxTryLimit());
        System.out.println("cm.getIokTryLimit() = " + cm.getIokTryLimit());

        Certificate longCert = cm.getCertificate(Certificate.CertificateType.IDENTIFICATION);
        System.out.println("longCert = " + longCert);

        Certificate shortCert = cm.getCertificate(Certificate.CertificateType.AUTHORIZATION);
        System.out.println("shortCert = " + longCert);

        try {
            FileOutputStream fos = new FileOutputStream("long.crt");
            fos.write(longCert.getData());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream fos = new FileOutputStream("short.crt");
            fos.write(shortCert.getData());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        cm.unblockIOK("xxx", "xxx");
//        System.out.println("certificate - Given Name = " + certificate.getGivenName());
//        System.out.println("certificate - Surname = " + certificate.getSurname());
    }
}
