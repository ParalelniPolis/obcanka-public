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
package cz.paralelnipolis.obcanka.publictools.crypto;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

import org.web3j.utils.Numeric;

public class ECKeyPair {
    private final BigInteger privateKey;
    private final BigInteger publicKey;
    private PublicKey pubKey = null;
    private PrivateKey prvKey = null;

    public ECKeyPair(BigInteger privateKey, BigInteger publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public ECKeyPair(BigInteger privateKeyValue, BigInteger publicKeyValue, BCECPrivateKey privateKey, BCECPublicKey publicKey) {
        this(privateKeyValue,publicKeyValue);
        this.pubKey = publicKey;
        this.prvKey = privateKey;
    }

    public BigInteger getPrivateKey() {
        return privateKey;
    }

    public BigInteger getPublicKey() {
        return publicKey;
    }


    public ECDSASignature sign(byte[] hash) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA512Digest()));

        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(privateKey, Sign.CURVE);
        signer.init(true, privKey);
        BigInteger[] components = signer.generateSignature(hash);

        return new ECDSASignature(components[0], components[1]).toCanonicalised();
    }

    public static ECKeyPair create(KeyPair keyPair) {
        BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
        BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();

        BigInteger privateKeyValue = privateKey.getD();

        byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);
        BigInteger publicKeyValue =
                new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));

        return new ECKeyPair(privateKeyValue, publicKeyValue, privateKey, publicKey);
    }

    public static ECKeyPair create(BigInteger privateKey) {
        return new ECKeyPair(privateKey, Sign.publicKeyFromPrivate(privateKey));
    }

    public static ECKeyPair create(byte[] privateKey) {
        return create(Numeric.toBigInt(privateKey));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ECKeyPair ecKeyPair = (ECKeyPair) o;
        if (privateKey != null ? !privateKey.equals(ecKeyPair.privateKey) : ecKeyPair.privateKey != null) {
            return false;
        }

        return publicKey != null ? publicKey.equals(ecKeyPair.publicKey) : ecKeyPair.publicKey == null;
    }

    @Override
    public int hashCode() {
        int result = privateKey != null ? privateKey.hashCode() : 0;
        result = 31 * result + (publicKey != null ? publicKey.hashCode() : 0);
        return result;
    }

    public PublicKey getPubKey() {
        return pubKey;
    }

    public PrivateKey getPrvKey() {
        return prvKey;
    }
}

