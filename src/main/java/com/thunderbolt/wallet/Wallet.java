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
package com.thunderbolt.wallet;

// IMPORTS *******************************************************************/

import com.thunderbolt.common.ISerializable;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.EllipticCurveKeyPair;
import com.thunderbolt.security.EncryptedPrivateKey;
import com.thunderbolt.security.Hash;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// IMPLEMENTATION ************************************************************/

/**
 * Keeps track of the unspent outputs spendable by the keys in the wallet file. This class also contains useful functions
 * for tracking available balance, sending and verifying the received payments.
 */
public class Wallet implements ISerializable
{
    private Map<Hash, UnspentTransactionOutput> m_unspentOutputs = new HashMap<>();
    private EllipticCurveKeyPair                m_keys           = new EllipticCurveKeyPair();
    private EncryptedPrivateKey                 m_encryptedKey   = null;

    /**
     * Initializes a new instance of the Wallet class.
     *
     * Creates a new key pair.
     *
     * @param password The password to decrypt the key.
     */
    public Wallet(String password) throws GeneralSecurityException
    {
        m_encryptedKey = new EncryptedPrivateKey(m_keys.getPrivateKey(), password);
    }

    /**
     * Initializes a new instance of the Wallet class.
     *
     * @param keyPair The key pair to initialize this wallet with.
     * @param password     The password to decrypt the key.
     */
    public Wallet(EllipticCurveKeyPair keyPair, String password) throws GeneralSecurityException
    {
        m_keys = keyPair;
        m_encryptedKey = new EncryptedPrivateKey(m_keys.getPrivateKey(), password);
    }

    /**
     * Initializes a new instance of the Wallet class.
     *
     * @param encryptedKey The encrypted private key to initialize this wallet with.
     * @param password     The password to decrypt the key.
     */
    public Wallet(EncryptedPrivateKey encryptedKey, String password) throws GeneralSecurityException
    {
        m_encryptedKey = encryptedKey;
        m_keys = new EllipticCurveKeyPair(m_encryptedKey.getPrivateKey(password));
    }

    /**
     * Initializes a new instance of the Wallet class.
     *
     * @param buffer   The buffer containing the serialized wallet.
     * @param password The password to decrypt the encrypted key.
     */
    public Wallet(ByteBuffer buffer, String password) throws GeneralSecurityException
    {
        m_encryptedKey = new EncryptedPrivateKey(buffer.array());
        m_keys = new EllipticCurveKeyPair(m_encryptedKey.getPrivateKey(password));
    }


    /**
     * Updates the list of unspent outputs available, adding new outputs and removing no longer available ones.
     *
     * @param toAdd    The list of new outputs.
     * @param toRemove The list of outputs no longer available.
     */
    public void updateOutputs(List<UnspentTransactionOutput> toAdd, List<Hash> toRemove)
    {
        for (Hash hash: toRemove)
            m_unspentOutputs.remove(hash);

        for (UnspentTransactionOutput output: toAdd)
        {
            m_unspentOutputs.put(output.getHash(), output);
        }
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(m_encryptedKey.serialize());
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        return new byte[0];
    }
}
