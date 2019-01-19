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
