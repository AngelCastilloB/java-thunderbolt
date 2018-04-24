/*
 * MIT License
 *
 * Copyright (c) 2018 Angel Castillo.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.thunderbolt.security;

/* IMPORTS *******************************************************************/

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;

import java.math.BigInteger;

/* IMPLEMENTATION ************************************************************/

/**
 * Elliptic Curve signature provider.
 *
 * Signs and verify signatures using the secp256k1 Elliptic Curve.
 */
public class EllipticCurveProvider
{
    // Static Fields
    private static final X9ECParameters     s_curve  = SECNamedCurves.getByName ("secp256k1");
    private static final ECDomainParameters s_domain = new ECDomainParameters(s_curve.getCurve(),
                                                                              s_curve.getG(),
                                                                              s_curve.getN(),
                                                                              s_curve.getH());

    /**
     * Generates a signature for the given input hash.
     *
     * @param hash The hash to be signed.
     *
     * @return The signature.
     */
    public static BigInteger[] sign(byte[] hash, BigInteger privateKey)
    {
        ECDSASigner signer     = new ECDSASigner();
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKey, s_domain);

        signer.init(true, privateKeyParameters);

        return signer.generateSignature(hash);
    }

    /**
     * Verifies given signature against a hash using the public key.
     *
     * @param data      Hash of the data to verify.
     * @param signature The signature.
     * @param publicKey The public key bytes to use.
     */
    public static boolean verify(byte[] data, BigInteger[] signature, byte[] publicKey)
    {
        ECDSASigner           signer = new ECDSASigner();
        ECPublicKeyParameters params = new ECPublicKeyParameters(s_domain.getCurve().decodePoint(publicKey), s_domain);
        signer.init(false, params);

        BigInteger r = signature[0];
        BigInteger s = signature[1];

        return signer.verifySignature(data, r, s);
    }

    /**
     * Gets the secp256k1 elliptic curve domain parameters.
     *
     * @return The domain parameters.
     */
    static ECDomainParameters getDomain()
    {
        return s_domain;
    }
}
