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

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
     * Generates a signature for the given input data.
     *
     * @param data The data to be signed.
     *
     * @return The DER-encoded signature.
     */
    public static byte[] sign(byte[] data, BigInteger privateKey)
    {
        ECDSASigner signer     = new ECDSASigner();
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKey, s_domain);

        signer.init(true, privateKeyParameters);

        BigInteger[] signature = signer.generateSignature(data);
        return encodeToDER(signature[0], signature[1]);
    }

    /**
     * Verifies given signature against a hash using the public key.
     *
     * @param data      Hash of the data to verify.
     * @param signature The DER-encoded signature.
     * @param publicKey The public key bytes to use.
     */
    public static boolean verify(byte[] data, byte[] signature, byte[] publicKey)
    {
        BigInteger[] decodedSignature = decodeFromDer(signature);

        ECDSASigner           signer = new ECDSASigner();
        ECPublicKeyParameters params = new ECPublicKeyParameters(s_domain.getCurve().decodePoint(publicKey), s_domain);
        signer.init(false, params);

        BigInteger r = decodedSignature[0];
        BigInteger s = decodedSignature[1];

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

    /**
     * Creates a digital signature from the DER-encoded values
     *
     * @param encodedSignature DER-encoded value.
     */
    private static BigInteger[] decodeFromDer(byte[] encodedSignature)
    {
        BigInteger[] signature = new BigInteger[2];

        try
        {
            try (ASN1InputStream decoder = new ASN1InputStream(encodedSignature))
            {
                DLSequence seq = (DLSequence)decoder.readObject();
                signature[0] = ((ASN1Integer)seq.getObjectAt(0)).getPositiveValue();
                signature[1] = ((ASN1Integer)seq.getObjectAt(1)).getPositiveValue();
            }

        }
        catch (ClassCastException | IOException exc)
        {
            throw new RuntimeException("Unable to decode signature", exc);
        }

        return signature;
    }


    /**
     * Encodes R and S as a DER-encoded byte array
     *
     * @return A byte array with the DER-encoded signature.
     */
    private static byte[] encodeToDER(BigInteger r, BigInteger s)
    {
        byte[] encodedBytes = null;

        try
        {
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream())
            {
                DERSequenceGenerator seq = new DERSequenceGenerator(outStream);
                seq.addObject(new ASN1Integer(r));
                seq.addObject(new ASN1Integer(s));
                seq.close();
                encodedBytes = outStream.toByteArray();
            }
        }
        catch (IOException exc)
        {
            throw new IllegalStateException("Unexpected IOException", exc);
        }

        return encodedBytes;
    }
}
