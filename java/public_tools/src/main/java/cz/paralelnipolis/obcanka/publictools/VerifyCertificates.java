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

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class VerifyCertificates {
    public static void main(String[] args) {
        if ( args.length !=2 ) {
            System.out.println("Usage: VerifyCertificates issuer_of_short.crt short.crt");
            System.out.println("Example: VerifyCertificates ./public_tools/issuer_of_short.crt ./desktop_app/short.crt");
            System.exit(1);
        } else {
            System.out.println("Current directory is: " + (new File(".").getAbsolutePath()));
            String issuerFilename = args[0];
            String certToTest = args[1];

            try {
                CertificateFactory fac = new CertificateFactory();
                X509Certificate issuer = (X509Certificate) fac.engineGenerateCertificate(new FileInputStream(issuerFilename));
                X509Certificate test = (X509Certificate) fac.engineGenerateCertificate(new FileInputStream(certToTest));
                test.verify(issuer.getPublicKey());
                System.out.println("Success: Certificate signature is correct!");
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (SignatureException e) {
                System.out.println("Error: Invalid signature!");
            }


        }
    }
}
