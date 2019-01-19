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

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x500.style.IETFUtils;



import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

public class Certificate {

    public enum CertificateType {
        AUTHORIZATION,
        IDENTIFICATION
    }

    private byte[] data;
    private CertificateType type;
    protected X509Certificate parsedCertificate;
    protected String dn;
    protected RDN[] rdns;


    public Certificate(byte[] data, CertificateType type) {
        this.data = data;
        this.type = type;
        parse();
    }

    private void parse() {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection c = cf.generateCertificates(new ByteArrayInputStream(getData()));
            Iterator i = c.iterator();
            while (i.hasNext()) {
                X509Certificate cert = (X509Certificate)i.next();
                parsedCertificate = cert;
                dn = parsedCertificate.getSubjectX500Principal().getName("RFC1779");
                rdns = IETFUtils.rDNsFromString(dn, BCStyle.INSTANCE);
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        }
    }

    public static Certificate parse(byte[] berData, CertificateType type) {
        if (berData == null) {
            return null;
        }
        if (type == CertificateType.IDENTIFICATION) {
            return new IdentificationCertificate(berData, type);
        }else if (type == CertificateType.AUTHORIZATION){
            return new AuthorizationCertificate(berData,type);
        }
        return null;
    }

    protected String extractName(String name) {
        for (RDN rdn : rdns) {
            ASN1ObjectIdentifier oid = rdn.getFirst().getType();
            if (name.equalsIgnoreCase(oid.getId()) || name.equalsIgnoreCase(BCStyle.INSTANCE.oidToDisplayName(oid))) {
                return rdn.getFirst().getValue().toString();
            }
        }
        return null;
    }

    public byte[] getData() {
        return data;
    }
}
