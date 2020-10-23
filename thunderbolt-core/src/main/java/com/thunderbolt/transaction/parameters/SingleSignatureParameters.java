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
package com.thunderbolt.transaction.parameters;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.security.Ripemd160Digester;
import com.thunderbolt.security.Sha256Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Helper class to wrap the locking parameters of a single signature transaction.
 */
public class SingleSignatureParameters implements ISerializable
{
    private static final Logger s_logger = LoggerFactory.getLogger(SingleSignatureParameters.class);

    private static final int PUBLIC_KEY_SIZE    = 33;
    private static final int MAX_SIGNATURE_SIZE = 72;

    private byte[] m_publicKey = null;
    private byte[] m_signature = null;

    /**
     * Initializes a new instance of the SingleSignatureParameters class.
     */
    public SingleSignatureParameters()
    {
    }

    /**
     * Initializes a new instance of the SingleSignatureParameters class.
     *
     * @param buffer A buffer contained a serialized SingleSignatureParameters.
     */
    public SingleSignatureParameters(ByteBuffer buffer)
    {
        m_publicKey = new byte[PUBLIC_KEY_SIZE];

        buffer.get(m_publicKey);

        m_signature = new byte[buffer.get() & 0xFF];

        buffer.get(m_signature);
    }

    /**
     * Initializes a new instance of the SingleSignatureParameters class.
     *
     * @param buffer A buffer contained a serialized SingleSignatureParameters.
     */
    public SingleSignatureParameters(byte[] buffer)
    {
        this(ByteBuffer.wrap(buffer));
    }

    /**
     * Initializes a new instance of the SingleSignatureParameters class.
     *
     * @param publicKey The public key.
     * @param signature The signature.
     */
    public SingleSignatureParameters(byte[] publicKey, byte[] signature)
    {
        m_publicKey = publicKey;
        m_signature = signature;
    }

    /**
     * Gets the public keys that signs on this output.
     *
     * @return The public key.
     */
    public byte[] getPublicKey()
    {
        return m_publicKey;
    }

    /**
     * Sets the public key that signs on this output.
     *
     * @param publicKey The public key.
     */
    public void setPublicKey(byte[] publicKey)
    {
        m_publicKey = publicKey;
    }

    /**
     * Gets the signature of the output.
     *
     * @return The signature.
     */
    public byte[] getSignature()
    {
        return m_signature;
    }

    /**
     * Sets the signature of the output.
     *
     * @param signature The signature.
     */
    public void setSignature(byte[] signature)
    {
        m_signature = signature;
    }

    /**
     * Gets the hash of the public key (SHA-256 and the RIPEMD160).
     *
     * @return The hash.
     */
    public byte[] getPublicKeyHash()
    {
        return Ripemd160Digester.digest(m_publicKey);
    }

    /**
     * Serializes an object in raw byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        if (m_publicKey.length != PUBLIC_KEY_SIZE)
            throw new RuntimeException(
                    String.format("Wrong public key size. Expected %s, actual %s",
                            PUBLIC_KEY_SIZE,
                            m_publicKey.length));

        data.writeBytes(m_publicKey);

        if (m_signature.length > MAX_SIGNATURE_SIZE)
            throw new RuntimeException(
                    String.format("Wrong signature key size. Expected less than %s, actual %s",
                            MAX_SIGNATURE_SIZE,
                            m_signature.length));

        data.write((byte)m_signature.length);
        data.writeBytes(m_signature);

        return data.toByteArray();
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format(
                "{                          %n" +
                "  \"publicKey\":   \"%s\", %n" +
                "  \"signature\":   \"%s\"  %n" +
                "}",
                Convert.toHexString(m_publicKey),
                Convert.toHexString(m_signature));
    }
}
