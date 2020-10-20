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

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;

import java.math.BigInteger;
import java.security.SecureRandom;

/* IMPLEMENTATION ************************************************************/

/**
 * Wrapper class for a secp256k1 elliptic curve key pair.
 */
public class EllipticCurveKeyPair
{
    // Static Fields
    private static final SecureRandom s_secureRandom = new SecureRandom();

    // Instance fields.
    private final BigInteger m_private;
    private final byte[]     m_public;

    /**
     * Generates a fresh elliptic curve key pair.
     */
    public EllipticCurveKeyPair()
    {
        ECKeyPairGenerator        generator    = new ECKeyPairGenerator();
        ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(EllipticCurveProvider.getDomain(), s_secureRandom);

        generator.init(keygenParams);

        AsymmetricCipherKeyPair keypair       = generator.generateKeyPair();
        ECPrivateKeyParameters  privateParams = (ECPrivateKeyParameters) keypair.getPrivate();
        ECPublicKeyParameters   publicParams  = (ECPublicKeyParameters) keypair.getPublic();

        m_private = privateParams.getD();
        m_public  = publicParams.getQ().getEncoded(true);
    }

    /**
     * Generates an elliptic curve key pair from the private key.
     *
     * @param key The private key.
     */
    public EllipticCurveKeyPair(BigInteger key)
    {
        m_private = key;
        m_public  = derivePublicKey(key);
    }

    /**
     * Gets the public key component.
     *
     * @return The public key.
     */
    public byte[] getPublicKey()
    {
        return m_public;
    }

    /**
     * Gets the private key component.
     *
     * @return The private key.
     */
    public BigInteger getPrivateKey()
    {
        return m_private;
    }

    /**
     * Derives a public key from the given private key.
     *
     * @param key The private key.
     *
     * @return The public key.
     */
    private static byte[] derivePublicKey(BigInteger key)
    {
        return EllipticCurveProvider.getDomain().getG().multiply(key).getEncoded(true);
    }
}