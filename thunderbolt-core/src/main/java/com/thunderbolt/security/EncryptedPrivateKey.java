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

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.contracts.ISerializable;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/* IMPLEMENTATION ************************************************************/

/**
 * Wraps an AES encrypted key with all the necessary parameters to decrypt it.
 */
public class EncryptedPrivateKey implements ISerializable
{
    // Static Fields
    private static final SecureRandom s_secureRandom = new SecureRandom();

    // Constants
    private static final int KEY_LENGTH   = 32;
    private static final int BLOCK_LENGTH = 16;
    private static final int IV_LENGTH    = BLOCK_LENGTH;
    private static final int SALT_LENGTH  = KEY_LENGTH;

    // Instance Fields
    private byte[] m_encKeyBytes = null;
    private byte[] m_iv          = new byte[IV_LENGTH];
    private byte[] m_salt        = new byte[SALT_LENGTH];

    /**
     * Creates a new encrypted private key given a private key an a pass phrase.
     *
     * @param  privateKey Private key
     * @param  keyPhrase  Phrase used to derive the encryption key
     *
     * @throws GeneralSecurityException Unable to complete a cryptographic function.
     */
    public EncryptedPrivateKey(BigInteger privateKey, String keyPhrase) throws GeneralSecurityException
    {
        s_secureRandom.nextBytes(m_salt);

        KeyParameter aesKey = deriveKey(keyPhrase, m_salt);

        try
        {
            s_secureRandom.nextBytes(m_iv);

            ParametersWithIV keyWithIV = new ParametersWithIV(aesKey, m_iv);
            CBCBlockCipher blockCipher = new CBCBlockCipher(new AESEngine());
            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(blockCipher);
            cipher.init(true, keyWithIV);

            byte[] privKeyBytes = privateKey.toByteArray();

            int encryptedLength = cipher.getOutputSize(privKeyBytes.length);
            m_encKeyBytes = new byte[encryptedLength];

            int length = cipher.processBytes(privKeyBytes, 0, privKeyBytes.length, m_encKeyBytes, 0);

            cipher.doFinal(m_encKeyBytes, length);
        }
        catch (Exception exception)
        {
            throw new GeneralSecurityException("Unable to encrypt the private key", exception);
        }
    }

    /**
     * Creates a new EncryptedPrivateKey from the serialized data.
     *
     * @param encryptedKey Serialized key
     *
     * @throws RuntimeException End-of-data while processing serialized data
     */
    public EncryptedPrivateKey(ByteBuffer encryptedKey) throws RuntimeException
    {
        encryptedKey.get(m_iv);
        encryptedKey.get(m_salt);

        int encryptedSize = encryptedKey.getInt();
        m_encKeyBytes = new byte[encryptedSize];
        encryptedKey.get(m_encKeyBytes);
    }

    /**
     * Creates a new EncryptedPrivateKey from the serialized data.
     *
     * @param encryptedKey Serialized key
     *
     * @throws RuntimeException End-of-data while processing serialized data
     */
    public EncryptedPrivateKey(byte[] encryptedKey) throws RuntimeException
    {
        this(ByteBuffer.wrap(encryptedKey));
    }

    /**
     * Get the byte array for this encrypted private key.
     *
     * @return The byte array with the encrypted data.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.writeBytes(m_iv);
        data.writeBytes(m_salt);
        data.writeBytes(NumberSerializer.serialize(m_encKeyBytes.length));
        data.writeBytes(m_encKeyBytes);

        return data.toByteArray();
    }

    /**
     * Returns the decrypted private key.
     *
     * @param keyPhrase Key phrase used to derive the encryption key.
     *
     * @return Private key
     */
    public BigInteger getPrivateKey(String keyPhrase) throws GeneralSecurityException
    {
        KeyParameter aesKey = deriveKey(keyPhrase, m_salt);
        BigInteger   privateKey;

        try
        {
            ParametersWithIV keyWithIV = new ParametersWithIV(aesKey, m_iv);
            CBCBlockCipher blockCipher = new CBCBlockCipher(new AESEngine());
            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(blockCipher);
            cipher.init(false, keyWithIV);

            int    bufferLength = cipher.getOutputSize(m_encKeyBytes.length);
            byte[] outputBytes  = new byte[bufferLength];
            int    length1      = cipher.processBytes(m_encKeyBytes, 0, m_encKeyBytes.length, outputBytes, 0);
            int    length2      = cipher.doFinal(outputBytes, length1);
            int    actualLength = length1 + length2;
            byte[] privKeyBytes = new byte[actualLength];

            System.arraycopy(outputBytes, 0, privKeyBytes, 0, actualLength);

            privateKey = new BigInteger(privKeyBytes);
        }
        catch (Exception exception)
        {
            throw new GeneralSecurityException("Unable to decrypt the private key", exception);
        }

        return privateKey;
    }

    /**
     * Derive the AES encryption key from the key phrase and the salt
     *
     * @param keyPhrase Key phrase
     * @param salt      Salt
     *
     * @return Key parameter.
     *
     * @throws GeneralSecurityException Unable to complete cryptographic function
     */
    private KeyParameter deriveKey(String keyPhrase, byte[] salt) throws GeneralSecurityException
    {
        KeyParameter aesKey;

        try
        {
            byte[] stringBytes = keyPhrase.getBytes("UTF-8");

            Sha256Hash digest         = Sha256Digester.digest(stringBytes);
            byte[] doubleDigest = new byte[digest.getData().length + salt.length];

            System.arraycopy(digest.getData(), 0, doubleDigest, 0, digest.getData().length);
            System.arraycopy(salt, 0, doubleDigest, digest.getData().length, salt.length);

            byte[] keyBytes = Sha256Digester.digest(doubleDigest).serialize();

            aesKey = new KeyParameter(keyBytes);
        }
        catch (Exception exception)
        {
            throw new GeneralSecurityException("Unable to convert passphrase to a byte array", exception);
        }

        return aesKey;
    }
}