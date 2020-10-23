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

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.blockchain.contracts.IBlockchainUpdateListener;
import com.thunderbolt.blockchain.contracts.IOutputsUpdateListener;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.EllipticCurveKeyPair;
import com.thunderbolt.security.EllipticCurveProvider;
import com.thunderbolt.security.EncryptedPrivateKey;
import com.thunderbolt.security.Sha256Hash;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.*;

import com.thunderbolt.transaction.OutputLockType;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.transaction.contracts.ITransactionsChangeListener;
import com.thunderbolt.transaction.parameters.SingleSignatureParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// IMPLEMENTATION ************************************************************/

/**
 * Keeps track of the unspent outputs spendable by the keys in the wallet file. This class also contains useful functions
 * for tracking available balance, sending and verifying the received payments.
 */
public class Wallet implements ISerializable, IOutputsUpdateListener, ITransactionsChangeListener, IBlockchainUpdateListener
{
    private static final int PUBLIC_KEY_SIZE            = 33;
    private static final int PRIVATE_KEY_SIZE           = 32;
    private static final int ENCRYPTED_PRIVATE_KEY_SIZE = 80;

    private static final Logger s_logger = LoggerFactory.getLogger(Wallet.class);

    private EncryptedPrivateKey                       m_encryptedKey        = null;
    private byte[]                                    m_publicKey           = new byte[PUBLIC_KEY_SIZE];
    private BigInteger                                m_privateKey          = null;
    private boolean                                   m_isUnlocked          = false;
    private boolean                                   m_isEncrypted         = false;
    private final List<Transaction>                   m_transactions        = new ArrayList<>();
    private final List<Transaction>                   m_pendingTransactions = new ArrayList<>();
    private Map<Sha256Hash, UnspentTransactionOutput> m_unspentOutputs      = new HashMap<>();
    private Sha256Hash                                m_syncedUpTo          = new Sha256Hash();
    private String                                    m_walletPath          = "";

    /**
     * Initializes a new instance of the Wallet class.
     *
     * Creates a new key pair.
     *
     * @param filepath The path to the wallet file.
     */
    public Wallet(Path filepath) throws IOException
    {
        if (Files.exists(filepath))
        {
            byte[] data = Files.readAllBytes(filepath);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            deserialize(buffer);
            m_walletPath = filepath.toString();
        }
        else
        {
            // Create a new unencrypted wallet.
            EllipticCurveKeyPair keyPair = new EllipticCurveKeyPair();

            m_privateKey   = keyPair.getPrivateKey();
            m_publicKey    = keyPair.getPublicKey();
            m_encryptedKey = new EncryptedPrivateKey(new byte[ENCRYPTED_PRIVATE_KEY_SIZE]);
            m_isEncrypted  = false;
            m_isUnlocked   = true;

            m_walletPath = filepath.toString();
            save(filepath.toString());
        }

        m_isUnlocked = false;
    }

    /**
     * Initializes a new instance of the Wallet class.
     *
     * Creates a new key pair.
     *
     * @param password The password to decrypt the key.
     */
    public Wallet(String password) throws GeneralSecurityException
    {
        EllipticCurveKeyPair keyPair = new EllipticCurveKeyPair();
        m_encryptedKey = new EncryptedPrivateKey(keyPair.getPrivateKey(), password);
        m_publicKey = keyPair.getPublicKey();
        m_privateKey = keyPair.getPrivateKey();

        m_isUnlocked = true;
    }

    /**
     * Initializes a new instance of the Wallet class.
     *
     * @param keyPair The key pair to initialize this wallet with.
     * @param password The password to decrypt the key.
     */
    public Wallet(EllipticCurveKeyPair keyPair, String password) throws GeneralSecurityException
    {
        m_encryptedKey = new EncryptedPrivateKey(keyPair.getPrivateKey(), password);
        m_publicKey = keyPair.getPublicKey();
        m_privateKey = keyPair.getPrivateKey();

        m_isUnlocked = true;
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
        EllipticCurveKeyPair keys = new EllipticCurveKeyPair(m_encryptedKey.getPrivateKey(password));
        m_publicKey = keys.getPublicKey();
        m_privateKey = keys.getPrivateKey();

        m_isUnlocked = true;
    }

    /**
     * Initializes a new instance of the Wallet class.
     *
     * @param buffer   The buffer containing the serialized wallet.
     * @param password The password to decrypt the encrypted key.
     */
    public Wallet(ByteBuffer buffer, String password) throws GeneralSecurityException
    {
        deserialize(buffer);

        m_privateKey = m_encryptedKey.getPrivateKey(password);
        m_isUnlocked = true;
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
            byte[] data = Files.readAllBytes(filepath);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            deserialize(buffer);

            if (!m_isEncrypted)
                throw new GeneralSecurityException("User tried to decrypt the wallet. However the wallet was not encrypted");

            m_privateKey = m_encryptedKey.getPrivateKey(password);
        }
        else
        {
            // Create a new unencrypted wallet.
            EllipticCurveKeyPair keyPair = new EllipticCurveKeyPair();
            m_encryptedKey = new EncryptedPrivateKey(keyPair.getPrivateKey(), password);
            m_privateKey   = new BigInteger(new byte[PRIVATE_KEY_SIZE]);
            m_publicKey    = keyPair.getPublicKey();
            m_isEncrypted  = true;

            m_walletPath = filepath.toString();
            save(filepath.toString());
        }

        m_isUnlocked = true;
    }

    /**
     * Encrypts the new wallet.
     *
     * @param password The password to be used.
     */
    public void encrypt(String password) throws GeneralSecurityException
    {
        if (m_isEncrypted)
            return;

        m_encryptedKey = new EncryptedPrivateKey(m_privateKey, password);
        m_isEncrypted  = true;

        m_isUnlocked = true;

        save(m_walletPath);
    }

    /**
     * Lock the wallet.
     */
    public void lock()
    {
        if (!m_isUnlocked || !m_isEncrypted)
            return;

        m_privateKey = m_privateKey.subtract(m_privateKey);
        m_isUnlocked = false;
    }

    /**
     * Unlocks the wallet.
     *
     * @param password The password to unlock the wallet.
     */
    public void unlock(String password) throws GeneralSecurityException
    {
        if (m_isUnlocked || !m_isEncrypted)
            return;

        m_privateKey = m_encryptedKey.getPrivateKey(password);
        m_isUnlocked = true;
    }

    /**
     * Gets whether the wallet is encrypted or not.
     *
     * @return true if the wallet is encrypted; otherwise; false.
     */
    public boolean isEncrypted()
    {
        return m_isEncrypted;
    }

    /**
     * Gets whether the wallet is unlocked or not.
     *
     * @return true if the wallet is locked; otherwise; false.
     */
    public boolean isUnlocked()
    {
        return m_isUnlocked || !m_isEncrypted;
    }

    /**
     * Loads all the unspent output related to this wallet.
     *
     * @param service The persistence service provider.
     *
     * @return True if the initialization was successful; otherwise; false.
     */
    public boolean initialize(IPersistenceService service)
    {
        try
        {
            List<UnspentTransactionOutput> outputs = service.getUnspentOutputsForAddress(getAddress());

            for (UnspentTransactionOutput output: outputs)
                m_unspentOutputs.put(output.getHash(), output);

            List<Transaction> transactions = service.getTransactionsForAddress(getAddress(), m_syncedUpTo);
            m_syncedUpTo = service.getChainHead().getHash();
            getTransactions().addAll(transactions);
        }
        catch (Exception e)
        {
            return false;
        }

        try
        {
            List<UnspentTransactionOutput> outputs = service.getUnspentOutputsForAddress(getAddress());

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
     * Gets the address of this wallet.
     *
     * @return The address of the wallet.
     */
    public Address getAddress()
    {
       return new Address(NetworkParameters.mainNet().getSingleSignatureAddressHeader(), m_publicKey);
    }

    /**
     * Gets the wallet public key.
     *
     * @return The public key pair.
     */
    public byte[] getPublicKey()
    {
        return m_publicKey;
    }

    /**
     * Gets the wallet public key.
     *
     * @return The private key.
     */
    public BigInteger getPrivateKey()
    {
        if (m_isUnlocked || !m_isEncrypted)
        {
            return m_privateKey;
        }

        s_logger.error("Wallet is locked. You must unlock it first.");
        return null;
    }

    /**
     * Gets the balance in this wallet.
     *
     * @return The balance.
     */
    public BigInteger getBalance()
    {
        BigInteger total = BigInteger.ZERO;

        for (Map.Entry<Sha256Hash, UnspentTransactionOutput> entry : m_unspentOutputs.entrySet())
        {
            UnspentTransactionOutput value = entry.getValue();

            total = total.add(value.getOutput().getAmount());
        }

        return total;
    }

    /**
     * Creates a transaction with the given amount (if the funds are enough) to the given wallet.
     *
     * @param amount  The amount to be transferred.
     * @param address The address in string format where to transfer the funds to.
     *
     * @return The transaction.
     */
    public Transaction createTransaction(long amount, String address) throws IOException
    {
        if (!m_isUnlocked)
        {
            s_logger.error("Wallet is locked. You must unlock it first.");
            return null;
        }

        return createTransaction(BigInteger.valueOf(amount), new Address(address));
    }

    /**
     * Creates a transaction with the given amount (if the funds are enough) to the given wallet.
     *
     * @param amount  The amount to be transferred.
     * @param address The address where to transfer the funds to.
     *
     * @return The transaction.
     */
    public Transaction createTransaction(BigInteger amount, Address address) throws IOException
    {
        if (!m_isUnlocked)
        {
            s_logger.error("Wallet is locked. You must unlock it first.");
            return null;
        }

        Transaction transaction = new Transaction();

        BigInteger currentBalance = getBalance();
        if (currentBalance.compareTo(amount) < 0)
        {
            throw new IllegalArgumentException(String.format("The wallet does not have enough funds. Available funds '%s', given amount '%s'", currentBalance, amount));
        }

        BigInteger total = BigInteger.ZERO;

        ArrayList<UnspentTransactionOutput> outputs = new ArrayList<>();

        for (Map.Entry<Sha256Hash, UnspentTransactionOutput> entry : m_unspentOutputs.entrySet())
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
            byte[] derSignature = EllipticCurveProvider.sign(signatureData.toByteArray(), m_privateKey);

            SingleSignatureParameters singleParam = new SingleSignatureParameters(m_publicKey, derSignature);

            // At this point this input transaction is spendable.
            input.setUnlockingParameters(singleParam.serialize());
            transaction.getInputs().add(input);
        }

        TransactionOutput newOutput = new TransactionOutput(amount, OutputLockType.SingleSignature, address.getPublicHash());

        transaction.getOutputs().add(newOutput);

        if (remainder.compareTo(BigInteger.ZERO) > 0)
        {
            TransactionOutput change = new TransactionOutput(remainder, OutputLockType.SingleSignature, getAddress().getPublicHash());
            transaction.getOutputs().add(change);
        }

        return transaction;
    }

    /**
     * Deserializes the wallet object from a byte buffer.
     *
     * @param buffer Serialized data.
     */
    public void deserialize(ByteBuffer buffer)
    {
        m_isEncrypted = buffer.get() == 0x01;
        m_encryptedKey = new EncryptedPrivateKey(buffer);
        byte[] privateKeyData = new byte[PRIVATE_KEY_SIZE];
        buffer.get(privateKeyData);

        m_privateKey = new BigInteger(privateKeyData);

        buffer.get(m_publicKey);
        m_syncedUpTo = new Sha256Hash(buffer);

        int transactionsSize = buffer.getInt();

        for (int i = 0; i < transactionsSize; ++i)
            m_transactions.add(new Transaction(buffer));
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

        data.write((byte)(m_isEncrypted ? 0x01 :0x00));
        data.writeBytes(m_encryptedKey.serialize());

        // Write all zeroes
        if (m_isEncrypted)
        {
            data.writeBytes(new byte[PRIVATE_KEY_SIZE]);
        }
        else
        {
            data.writeBytes(m_privateKey.toByteArray());
        }

        data.writeBytes(m_publicKey);
        data.writeBytes(m_syncedUpTo.serialize());
        data.writeBytes(NumberSerializer.serialize(m_transactions.size()));

        for (Transaction transaction: m_transactions)
            data.writeBytes(transaction.serialize());

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
        m_walletPath = path;
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
    public void onOutputsUpdate(List<UnspentTransactionOutput> toAdd, List<Sha256Hash> toRemove)
    {
        for (Sha256Hash sha256Hash : toRemove)
            m_unspentOutputs.remove(sha256Hash);

        for (UnspentTransactionOutput output: toAdd)
        {
            if (Arrays.equals(output.getOutput().getLockingParameters(), getAddress().getPublicHash()))
                m_unspentOutputs.put(output.getHash(), output);
        }
    }

    /**
     * Called when a change on the available unspent outputs occur.
     *
     * @param transaction The transaction that was added.
     */
    @Override
    synchronized public void onTransactionAdded(Transaction transaction)
    {
        if (!m_pendingTransactions.contains(transaction))
            m_pendingTransactions.add(transaction);
    }

    /**
     * Called when a transaction is removed to the transaction pool.
     *
     * @param transaction The transaction that was removed.
     */
    @Override
    synchronized public void onTransactionRemoved(Transaction transaction)
    {
        m_pendingTransactions.remove(transaction);
    }

    /**
     * Gets the list of transactions associated with this wallet.
     *
     * @return The list of transactions.
     */
    public List<Transaction> getTransactions()
    {
        return m_transactions;
    }

    /**
     * Gets the list of pending transactions.
     *
     * @return The list of pending transactions.
     */
    public List<Transaction> getPendingTransactions()
    {
        return m_pendingTransactions;
    }

    /**
     * Called when a new block is added to the active chain.
     *
     * @param block The new added block.
     */
    @Override
    public void onBlockAdded(Block block)
    {
        List<Transaction> transactions = block.getTransactions();

        for (Transaction transaction: transactions)
        {
            boolean detected = false;
            for (TransactionOutput output : transaction.getOutputs())
            {
                if (Arrays.equals(output.getLockingParameters(), getAddress().getPublicHash()))
                {
                    if (!m_transactions.contains(transaction))
                        m_transactions.add(transaction);

                    detected = true;
                    break;
                }
            }

            // We already know this transaction mention us, so we do not need to keep looking forward.
            if (detected)
                continue;

            for (TransactionInput input : transaction.getInputs())
            {
                if (input.isCoinBase())
                    continue;

                SingleSignatureParameters params = new SingleSignatureParameters(input.getUnlockingParameters());

                if (Arrays.equals(params.getPublicKeyHash(), getAddress().getPublicHash()))
                {
                    if (!m_transactions.contains(transaction))
                        m_transactions.add(transaction);

                    break;
                }
            }
        }
    }

    /**
     * Called when a block is removed from the active chain.
     *
     * @param block The removed block.
     */
    @Override
    public void onBlockRemoved(Block block)
    {
        List<Transaction> transactions = block.getTransactions();

        for (Transaction transaction: transactions)
            m_transactions.remove(transaction);
    }
}
