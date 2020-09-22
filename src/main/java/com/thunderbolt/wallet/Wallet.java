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

import com.thunderbolt.blockchain.contracts.IOutputsUpdateListener;
import com.thunderbolt.common.ServiceLocator;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.EllipticCurveKeyPair;
import com.thunderbolt.security.EllipticCurveProvider;
import com.thunderbolt.security.EncryptedPrivateKey;
import com.thunderbolt.security.Hash;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;

import com.thunderbolt.transaction.OutputLockType;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.transaction.parameters.SingleSignatureParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// IMPLEMENTATION ************************************************************/

/**
 * Keeps track of the unspent outputs spendable by the keys in the wallet file. This class also contains useful functions
 * for tracking available balance, sending and verifying the received payments.
 */
public class Wallet implements ISerializable, IOutputsUpdateListener
{
    private static final Logger s_logger = LoggerFactory.getLogger(Wallet.class);

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
     * Initializes a new instance of the Wallet class. If the wallet already exists, attempts to decrypt and read it;
     * otherwise; creates a new wallet file with a new key.
     *
     * @param path     The path where the wallet file is located.
     * @param password The password to decrypt the encrypted key.
     */
    public Wallet(String path, String password) throws GeneralSecurityException, IOException
    {
        Path filepath = Path.of(path);

        if (Files.exists(filepath))
        {
            byte[] data = Files.readAllBytes(Path.of(path));
            ByteBuffer buffer = ByteBuffer.wrap(data);
            m_encryptedKey = new EncryptedPrivateKey(buffer.array());
            m_keys = new EllipticCurveKeyPair(m_encryptedKey.getPrivateKey(password));
        }
        else
        {
            m_encryptedKey = new EncryptedPrivateKey(m_keys.getPrivateKey(), password);
            save(path);
        }
    }

    /**
     * Loads all the unspent output related to this wallet.
     *
     * @return True if the initialization was successful; otherwise; false.
     */
    public boolean initialize()
    {
        try
        {
            List<UnspentTransactionOutput> outputs =
                    ServiceLocator.getService(IPersistenceService.class).getUnspentOutputsForAddress(m_keys.getPublicKey());

            for (UnspentTransactionOutput output: outputs)
                m_unspentOutputs.put(output.getHash(), output);
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    /**
     * Gets the wallet key pair.
     *
     * @return The key pair.
     */
    public EllipticCurveKeyPair getKeyPair()
    {
        return m_keys;
    }

    /**
     * Gets the balance in this wallet.
     *
     * @return The balance.
     */
    public BigInteger getBalance()
    {
        BigInteger total = BigInteger.ZERO;

        for (Map.Entry<Hash, UnspentTransactionOutput> entry : m_unspentOutputs.entrySet())
        {
            UnspentTransactionOutput value = entry.getValue();

            total = total.add(value.getOutput().getAmount());
        }

        return total;
    }

    /**
     * Creates a transaction with the given amount (if the funds are enough) to the given wallet.
     *
     * @param amount    The amount to be transferred.
     * @param publicKey The publioc key of the wallet to transfer the funds too.
     *
     * @return The transaction.
     */
    public Transaction createTransaction(BigInteger amount, byte[] publicKey) throws IOException
    {
        Transaction transaction = new Transaction();

        if (getBalance().compareTo(amount) < 0)
            return null; // TODO: Throw.

        BigInteger total = BigInteger.ZERO;

        ArrayList<UnspentTransactionOutput> outputs = new ArrayList<>();

        for (Map.Entry<Hash, UnspentTransactionOutput> entry : m_unspentOutputs.entrySet())
        {
            if (total.compareTo(amount) >= 0)
                break;

            UnspentTransactionOutput value = entry.getValue();

            outputs.add(value);
            total = total.add(value.getOutput().getAmount());
        }

        BigInteger remainder = amount.subtract(total).abs(); // We give ourselves change.

        transaction = new Transaction();

        for (UnspentTransactionOutput out : outputs)
        {
            TransactionInput input = new TransactionInput(out.getTransactionHash(), out.getIndex());

            ByteArrayOutputStream signatureData = new ByteArrayOutputStream();
            signatureData.write(input.getReferenceHash().serialize());
            signatureData.write(input.getIndex());
            signatureData.write(out.getOutput().getAmount().toByteArray());
            signatureData.write(out.getOutput().getLockType().getValue());
            signatureData.write(out.getOutput().getLockingParameters());

            // The signature in DER format is the unlocking parameter of the referenced output. We need to add this to the unlocking parameters
            // list of the transaction at the same position at which we added the transaction.
            byte[] derSignature = EllipticCurveProvider.sign(signatureData.toByteArray(), m_keys.getPrivateKey());

            SingleSignatureParameters singleParam = new SingleSignatureParameters(m_keys.getPublicKey(), derSignature);

            // At this point this input transaction is spendable.
            input.setUnlockingParameters(singleParam.serialize());
            transaction.getInputs().add(input);
        }

        TransactionOutput newOutput = new TransactionOutput(amount, OutputLockType.SingleSignature, publicKey);

        transaction.getOutputs().add(newOutput);

        if (remainder.compareTo(BigInteger.ZERO) > 0)
        {
            TransactionOutput change = new TransactionOutput(remainder, OutputLockType.SingleSignature, m_keys.getPublicKey());
            transaction.getOutputs().add(change);
        }

        return transaction;
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

        return data.toByteArray();
    }

    /**
     * Saves the wallet to the disk in the fiven path.
     *
     * @param path The path where to store the wallet.
     *
     * @return True if the wallet could be saved; otherwise; false.
     */
    public boolean save(String path)
    {
        boolean result = true;

        try (FileOutputStream fos = new FileOutputStream(path))
        {
          fos.write(serialize());
        }
        catch (IOException e)
        {
          e.printStackTrace();
          result = false;
        }

        return result;
    }

    /**
     * Called when a change on the available unspent outputs occur.
     *
     * @param toAdd The new unspent outputs that were added.
     * @param toRemove The unspent outputs that are no longer available.
     */
    public void outputsUpdated(List<UnspentTransactionOutput> toAdd, List<Hash> toRemove)
    {
        for (Hash hash: toRemove)
            m_unspentOutputs.remove(hash);

        for (UnspentTransactionOutput output: toAdd)
        {
            if (Arrays.equals(output.getOutput().getLockingParameters(), m_keys.getPublicKey()))
                m_unspentOutputs.put(output.getHash(), output);
        }
    }
}
