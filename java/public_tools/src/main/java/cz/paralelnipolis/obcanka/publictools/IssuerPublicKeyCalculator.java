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
package cz.paralelnipolis.obcanka.publictools;

import cz.paralelnipolis.obcanka.core.HexUtils;
import cz.paralelnipolis.obcanka.core.card.Card;
import cz.paralelnipolis.obcanka.core.certificates.Certificate;
import cz.paralelnipolis.obcanka.core.communication.CardException;
import cz.paralelnipolis.obcanka.desktop.lib.DesktopCardInterface;
import cz.paralelnipolis.obcanka.publictools.crypto.Hash;
import cz.paralelnipolis.obcanka.publictools.crypto.Sign;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.web3j.utils.Numeric;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

/**
 * This class demonstrates how to read certificate from card and calculate issuer's public key.
 * Calculated issuer's public key is later stored in issuer_of_short.crt
 */
public class IssuerPublicKeyCalculator {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    public static void main(String[] args) throws CardException, CertificateException, IOException, SignatureException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException {
        DesktopCardInterface ci = DesktopCardInterface.create();
        Card c = new Card(ci);


        //1. read certificate from card
        Certificate cardCertificate = c.getCertificate(Certificate.CertificateType.AUTHORIZATION);

        CertificateFactory fac = new CertificateFactory();
        X509Certificate citizenCertificate = (X509Certificate) fac.engineGenerateCertificate(new ByteArrayInputStream(cardCertificate.getData()));

        byte[] signatureData = citizenCertificate.getSignature();
        System.out.println("signature = " + HexUtils.bytesToHexString(signatureData));

        byte[] dataForHashing = citizenCertificate.getTBSCertificate();
        byte[] hashOfCertificate = Hash.sha512(dataForHashing);
        System.out.println("hashOfCertificate(TBS) = " + Hex.toHexString(hashOfCertificate));

        //2. try to calculate public key
        ASN1InputStream asn1 = new ASN1InputStream(signatureData);
        DLSequence seq = (DLSequence) asn1.readObject();
        BigInteger r = ((ASN1Integer) seq.getObjectAt(0)).getPositiveValue();
        BigInteger s = ((ASN1Integer) seq.getObjectAt(1)).getPositiveValue();

        byte[] r1 = Numeric.toBytesPadded(r, 128);
        byte[] s1 = Numeric.toBytesPadded(s, 128);
        for (int recId=0;recId<4;recId++) {
            //itterates over the candidates
            ECPoint pubKeyRecovered = Sign.signedMessageToKey(hashOfCertificate, new Sign.SignatureData((byte) (27 + recId), r1, s1));
            if (pubKeyRecovered != null) {
                System.out.println("pubKeyCandidate = " + pubKeyRecovered);
                System.out.println("x = " + pubKeyRecovered.getXCoord());
                System.out.println("y = " + pubKeyRecovered.getYCoord());
                byte[] encodedDiscoveredPublicKey = pubKeyRecovered.getEncoded(false);
                System.out.println("publicKey compressed = " + Hex.toHexString(encodedDiscoveredPublicKey));
                DLSequence dls = new DLSequence(new ASN1Encodable[]{new DLSequence(new ASN1Encodable[]{
                        new ASN1ObjectIdentifier("1.2.840.10045.2.1"),
                        new ASN1ObjectIdentifier("1.3.132.0.35")
                }), new DLBitString(encodedDiscoveredPublicKey)});

                byte[] encodedPubKey = dls.getEncoded();
                System.out.println("publicKey from encodedPubKey = " + Hex.toHexString(encodedPubKey));
                PublicKey publicKey = BouncyCastleProvider.getPublicKey(SubjectPublicKeyInfo.getInstance(encodedPubKey));

                //Lets try to use calculated public key to verify signature
                boolean verify = Sign.verify(hashOfCertificate, signatureData, publicKey);
                System.out.println("verify = " + verify);
                if (verify) {
                    System.out.println("BINGO!!!!!!!!!!! " + pubKeyRecovered);
                    byte[] issuerCertificateData = generateCertificate(publicKey, citizenCertificate.getIssuerX500Principal(), citizenCertificate.getExtensionValue("2.5.29.35"));
                    FileOutputStream fos = new FileOutputStream("issuer_of_short.crt");
                    fos.write(issuerCertificateData);
                    fos.close();

                    //Double check that certificate can be used for verification
                    FileInputStream fin2 = new FileInputStream("issuer_of_short.crt");
                    X509Certificate certificateX = (X509Certificate) fac.engineGenerateCertificate(fin2);
                    boolean verify2 = Sign.verify(hashOfCertificate, signatureData, certificateX.getPublicKey());
                    if (verify2) {
                        citizenCertificate.verify(certificateX.getPublicKey());
                        System.out.println("All tests done. Key extracted and stored in issuer_of_short.crt");
                    }else{
                        System.out.println("verify2 failed.");
                    }

                    break;
                }
            }
        }
    }

    private static byte[] generateCertificate(PublicKey publicKey, X500Principal dnName, byte[] extensionValue) throws CertificateEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidAlgorithmParameterException {
        ASN1InputStream is = new ASN1InputStream(extensionValue);
        DEROctetString os = (DEROctetString) is.readObject();
        byte[] octets = os.getOctets();
        is = new ASN1InputStream(octets);
        DLSequence asn1Primitive = (DLSequence) is.readObject();
        DERTaggedObject objectAt = (DERTaggedObject) asn1Primitive.getObjectAt(0);
        DEROctetString object = (DEROctetString) objectAt.getObject();

        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp521r1"));
        KeyPair keyPair = generator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        Date validityBeginDate = new Date(System.currentTimeMillis() - 200L * (24L * 60 * 60 * 1000));
        Date validityEndDate = new Date(System.currentTimeMillis() + 100L * (365L * 24 * 60 * 60 * 1000));


        // GENERATE THE X509 CERTIFICATE
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal issuer = new X500Principal("C=CZ,O=Paralelní Polis,CN=Neoficiální certifikační autorita pro identifikační doklady");

        certGen.setSerialNumber(new BigInteger(1,object.getOctets()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(issuer); // use the same
        certGen.setNotBefore(validityBeginDate);
        certGen.setNotAfter(validityEndDate);
        certGen.setPublicKey(publicKey);
        certGen.setSignatureAlgorithm("SHA512withECDSA");

        X509Certificate cert = certGen.generate(privateKey);

        // DUMP CERTIFICATE AND KEY PAIR
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PemWriter pemWriter = new PemWriter(new OutputStreamWriter(bos));
        pemWriter.writeObject(new PemObject("CERTIFICATE",cert.getEncoded()));
        pemWriter.flush();
        return bos.toByteArray();
    }

}
