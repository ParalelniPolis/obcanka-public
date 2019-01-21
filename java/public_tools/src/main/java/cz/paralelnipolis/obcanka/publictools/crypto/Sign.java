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

import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;

import org.bouncycastle.math.ec.custom.sec.SecP521R1Curve;
import org.web3j.utils.Numeric;

import static org.web3j.utils.Assertions.verifyPrecondition;

public class Sign {

    public static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp521r1");
    public static final ECDomainParameters CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(), CURVE_PARAMS.getH());
    static final BigInteger HALF_CURVE_ORDER = CURVE_PARAMS.getN().shiftRight(1);

    public static SignatureData signMessage(byte[] message, ECKeyPair keyPair) {
        return signMessage(message, keyPair, true);
    }

    public static SignatureData signMessage(byte[] message, ECKeyPair keyPair, boolean isHashed) {
        BigInteger publicKey = keyPair.getPublicKey();
        byte[] messageHash;
        if (isHashed) {
            messageHash = Hash.sha512(message);
        } else {
            messageHash = message;
        }

        ECDSASignature sig = keyPair.sign(messageHash);

        int recId = -1;
        for (int i = 0; i < 4; i++) {
            ECPoint q = recoverFromSignature(i, sig, messageHash);
            if (q != null) {
                byte[] qBytes = q.getEncoded(false);
                BigInteger k =  new BigInteger(1, Arrays.copyOfRange(qBytes, 1, qBytes.length));
                if (k.equals(publicKey)) {
                    recId = i;
                    break;
                }
            }
        }
        if (recId == -1) {
            throw new RuntimeException("Could not construct a recoverable key. This should never happen.");
        }

        int headerByte = recId + 27;

        // 1 header + 32 bytes for R + 32 bytes for S
        byte v = (byte) headerByte;
        byte[] r = Numeric.toBytesPadded(sig.r, 128);
        byte[] s = Numeric.toBytesPadded(sig.s, 128);

        return new SignatureData(v, r, s);
    }

    private static ECPoint recoverFromSignature(int recId, ECDSASignature sig, byte[] message) {
        verifyPrecondition(recId >= 0, "recId must be positive");
        verifyPrecondition(sig.r.signum() >= 0, "r must be positive");
        verifyPrecondition(sig.s.signum() >= 0, "s must be positive");
        verifyPrecondition(message != null, "message cannot be null");

        BigInteger n = CURVE.getN();  // Curve order.
        BigInteger i = BigInteger.valueOf((long) recId / 2);
        BigInteger x = sig.r.add(i.multiply(n));
        BigInteger prime = SecP521R1Curve.q;
        if (x.compareTo(prime) >= 0) {
            return null;
        }
        ECPoint R = decompressKey(x, (recId & 1) == 1);
        if (!R.multiply(n).isInfinity()) {
            return null;
        }
        BigInteger e = new BigInteger(1, message);

        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = sig.r.modInverse(n);
        BigInteger srInv = rInv.multiply(sig.s).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
        ECPoint q = ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, R, srInv);
        return q;
    }

    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
        compEnc[0] = (byte)(yBit ? 0x03 : 0x02);
        return CURVE.getCurve().decodePoint(compEnc);
    }


    public static ECPoint signedMessageToKey(
            byte[] messageHash, SignatureData signatureData) throws SignatureException {

        byte[] r = signatureData.getR();
        byte[] s = signatureData.getS();
        verifyPrecondition(r != null && r.length == 128, "r must be 32 bytes");
        verifyPrecondition(s != null && s.length == 128, "s must be 32 bytes");

        int header = signatureData.getV() & 0xFF;
        // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
        //                  0x1D = second key with even y, 0x1E = second key with odd y
        if (header < 27 || header > 34) {
            throw new SignatureException("Header byte out of range: " + header);
        }

        ECDSASignature sig = new ECDSASignature(
                new BigInteger(1, signatureData.getR()),
                new BigInteger(1, signatureData.getS()));

        int recId = header - 27;
        ECPoint key = recoverFromSignature(recId, sig, messageHash);
        return key;
    }

    public static BigInteger publicKeyFromPrivate(BigInteger privKey) {
        ECPoint point = publicPointFromPrivate(privKey);

        byte[] encoded = point.getEncoded(false);
        return new BigInteger(1, Arrays.copyOfRange(encoded, 1, encoded.length));  // remove prefix
    }

    private static ECPoint publicPointFromPrivate(BigInteger privKey) {
        if (privKey.bitLength() > CURVE.getN().bitLength()) {
            privKey = privKey.mod(CURVE.getN());
        }
        return new FixedPointCombMultiplier().multiply(CURVE.getG(), privKey);
    }

    private static ECPoint publicPoint(BigInteger publicKey) {
        return new FixedPointCombMultiplier().multiply(CURVE.getG(), publicKey);
    }

    public static class SignatureData {
        private final byte v;
        private final byte[] r;
        private final byte[] s;

        public SignatureData(byte v, byte[] r, byte[] s) {
            this.v = v;
            this.r = r;
            this.s = s;
        }

        public byte getV() {
            return v;
        }

        public byte[] getR() {
            return r;
        }

        public byte[] getS() {
            return s;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SignatureData that = (SignatureData) o;

            if (v != that.v) {
                return false;
            }
            if (!Arrays.equals(r, that.r)) {
                return false;
            }
            return Arrays.equals(s, that.s);
        }

        @Override
        public int hashCode() {
            int result = (int) v;
            result = 31 * result + Arrays.hashCode(r);
            result = 31 * result + Arrays.hashCode(s);
            return result;
        }
    }

    public static boolean verify(byte[] hash, byte[] signature, PublicKey publicKey) {
        ASN1InputStream asn1 = new ASN1InputStream(signature);
        try {
            DLSequence seq = (DLSequence) asn1.readObject();
            BigInteger r = ((ASN1Integer) seq.getObjectAt(0)).getPositiveValue();
            BigInteger s = ((ASN1Integer) seq.getObjectAt(1)).getPositiveValue();
            ECDSASigner signer = new ECDSASigner();
            CipherParameters param = ECUtil.generatePublicKeyParameter(publicKey);
            signer.init(false, param);
            return signer.verifySignature(hash, r, s);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                asn1.close();
            } catch (IOException ignored) {
            }
        }
    }

}
