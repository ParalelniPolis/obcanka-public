/*
 * Copyright 2018
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

import com.google.common.util.concurrent.ListenableFuture;
import org.bitcoinj.core.*;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * For backup purposes issuer's compressed public key is also stored in bitcoin blockchain as OP_RETURN of output 0 of transaction a0549be380a0eb8d623c9e18a072e494952333a96921db393dbb4c5cfddea86c.
 * This class downloads public key from the bitcoin blockchain and writes it into crt file.
 */
public class IssuerPublicKeyDownloader {
    public static final String BLOCK_HASH = "0000000000000000001fc5536e7b21c00b49eb187be394454a2faf8d80b47d2f";
    public static final String TX_HASH_IN_THE_BLOCK = "a0549be380a0eb8d623c9e18a072e494952333a96921db393dbb4c5cfddea86c";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static byte[] concatArrays(byte[] ... arrays ) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < arrays.length; i++) {
            try {
                bos.write(arrays[i]);
            } catch (IOException e) {
            }
        }
        return bos.toByteArray();
    }

    private static  byte[] compressedToUncompressed(byte[] compKey) {
        X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp521r1");

        ECPoint point = CURVE_PARAMS.getCurve().decodePoint(compKey);
        byte[] x = point.getXCoord().getEncoded();
        byte[] y = point.getYCoord().getEncoded();
        // concat 0x04, x, and y, make sure x and y has 32-bytes:
        return concatArrays(new byte[] {0x04}, x, y);
    }

    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateEncodingException, SignatureException, InvalidKeyException, IOException {
        MainNetParams netParams = MainNetParams.get();
        PeerGroup peerGroup = new PeerGroup(netParams);
        peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
        peerGroup.setMaxConnections(20);
        peerGroup.start();
        System.out.println("Connecting to bitcoin network, please wait until peer with correct block is found.");
        Block blockFound = null;
        do {
            List<Peer> connectedPeers = peerGroup.getConnectedPeers();
            for (Peer peer : connectedPeers) {
                System.out.println("Requesting block from " + peer);
                ListenableFuture<Block> futureBlock = peer.getBlock(Sha256Hash.wrap(BLOCK_HASH));
                try {
                    Block block = null;
                    try {
                        block = futureBlock.get(20, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                    }
                    if (block == null) {
                        peer.close();
                    } else {
                        blockFound = block;
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }while (blockFound == null);
        System.out.println("Block found.");
        List<Transaction> transactions = blockFound.getTransactions();
        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            if (transaction.getHash().equals(Sha256Hash.wrap(TX_HASH_IN_THE_BLOCK))) {
                System.out.println("Transaction found.");
                byte[] scriptBytes = transaction.getOutputs().get(0).getScriptBytes();

                byte[] encodedDiscoveredPublicKey = Arrays.copyOfRange(scriptBytes, 2, scriptBytes.length);
                System.out.println("publicKey compressed = " + Hex.toHexString(encodedDiscoveredPublicKey));
                byte[] encodedDiscoveredPublicKeyUncompressed = compressedToUncompressed(encodedDiscoveredPublicKey);
                System.out.println("publicKey uncompressed = " + Hex.toHexString(encodedDiscoveredPublicKeyUncompressed));



                DLSequence dls = new DLSequence(new ASN1Encodable[]{new DLSequence(new ASN1Encodable[]{
                new ASN1ObjectIdentifier("1.2.840.10045.2.1"),
                new ASN1ObjectIdentifier("1.3.132.0.35")
                }), new DLBitString(encodedDiscoveredPublicKeyUncompressed)});

                byte[] encodedPubKey = dls.getEncoded();
                System.out.println("publicKey from encodedPubKey = " + Hex.toHexString(encodedPubKey));
                PublicKey publicKey = BouncyCastleProvider.getPublicKey(SubjectPublicKeyInfo.getInstance(encodedPubKey));
                byte[] issuerCertificateData = generateCertificate(publicKey);
                FileOutputStream fos = new FileOutputStream("issuer_of_short.crt");
                fos.write(issuerCertificateData);
                fos.close();
                System.out.println("Public key downloaded and saved to file issuer_of_short.crt");
                return;
            }
        }
    }

    private static byte[] generateCertificate(PublicKey publicKey) throws CertificateEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidAlgorithmParameterException {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp521r1"));
        KeyPair keyPair = generator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        Date validityBeginDate = new Date(System.currentTimeMillis() - 200L * (24L * 60 * 60 * 1000));
        Date validityEndDate = new Date(System.currentTimeMillis() + 100L * (365L * 24 * 60 * 60 * 1000));


        // GENERATE THE X509 CERTIFICATE
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal issuer = new X500Principal("C=CZ,O=Veřejnost,CN=Neoficiální certifikační autorita pro identifikační doklady");

        certGen.setSerialNumber(new BigInteger("1"));
        certGen.setSubjectDN(issuer);
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
