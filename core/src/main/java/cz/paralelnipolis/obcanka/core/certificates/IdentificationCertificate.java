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
package cz.paralelnipolis.obcanka.core.certificates;

public class IdentificationCertificate extends Certificate {
    public IdentificationCertificate(byte[] data, CertificateType type) {
        super(data, type);
    }

    public String getGivenName() {
        return extractName("2.5.4.42");
    }


    public String getSurname() {
        return extractName("2.5.4.4");
    }

    public String getCountryName() {
        return extractName("1.2.203.7064.1.1.11.7");
    }

    public String getCountryCode() {
        return extractName("C");
    }

    public String getSerialNumber() {
        return extractName("2.5.4.5");
    }

    public String getOrganization() {
        return extractName("O");
    }

    public String getMarriageStatus() {
        return extractName("1.2.203.7064.1.1.11.6");
    }

    public String getBirthNumber() {
        return extractName("1.2.203.7064.1.1.11.5");
    }


    public String getSex() {
        return extractName("1.2.203.7064.1.1.11.2");
    }


    public String getBirthCity() {
        return extractName("1.2.203.7064.1.1.11.4");
    }

    public String getBirthDate() {
        return extractName("1.2.203.7064.1.1.11.1");
    }

    public String getLocality() {
        return extractName("L");
    }

    public String getCity() {
        return extractName("ST");
    }

    public String getStreet() {
        return extractName("STREET");
    }

    public String getDocumentNumber() {
        return new String(parsedCertificate.getSerialNumber().toByteArray());
    }

    @Override
    public String toString() {
        return "IdentificationCertificate{" +
                "serialNumber: " + getSerialNumber() + "\n" +
                "documentNumber: " + getDocumentNumber() + "\n" +
                getGivenName() + " " + getSurname() +"\n" +
                "Marriage:" + getMarriageStatus() + " sex: " + getSex() +"\n" +
                getStreet() +"\n" +
                getCity() +"\n" +
                getLocality() +"\n" +
                getCountryName() +"\n" +
                getCountryCode() +"\n" +

                "Birth number: " + getBirthNumber() +"\n" +
                "Birth date: " + getBirthDate() +"\n" +
                "City: " + getBirthCity() +"\n" +

                "Organization: " + getOrganization() + "\n" +

                '}';
    }
}
