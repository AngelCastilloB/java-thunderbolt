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
package com.thunderbolt.persistence.storage;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.persistence.contracts.IMetadataProvider;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.persistence.structures.NetworkAddressMetadata;
import com.thunderbolt.persistence.structures.TransactionMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.wallet.Address;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

/* IMPLEMENTATION ************************************************************/

/**
 * Stores the metadata in two LevelDB databases. The first database contains metadata about the blocks and the
 * transactions. The second database contains a collection of all the unspent transaction outputs (UXTO).
 */
public class LevelDbMetadataProvider implements IMetadataProvider
{
    private static final Logger s_logger = LoggerFactory.getLogger(LevelDbMetadataProvider.class);

    // Constants
    static private final String METADATA_DB_NAME   = "blockchain";
    static private final String STATE_DB_NAME      = "state";
    static private final byte   BLOCK_PREFIX       = 'b';
    static private final byte   HEAD_PREFIX        = 'h';
    static private final byte   TRANSACTION_PREFIX = 't';

    // Instance Fields
    private final DB m_stateDatabase;
    private final DB m_metadataDatabase;

    /**
     * Initializes a new instance of the LevelDbMetadataProvider class.
     *
     * @param path The path where the databases are located.
     */
    public LevelDbMetadataProvider(Path path) throws StorageException
    {
        Options options = new Options();
        options.createIfMissing(true);
        options.logger(s_logger::debug);

        try
        {
            m_metadataDatabase = factory.open(Paths.get(path.toString(), METADATA_DB_NAME).toFile(), options);
            m_stateDatabase = factory.open(Paths.get(path.toString(), STATE_DB_NAME).toFile(), options);
        }
        catch (Exception exception)
        {
            throw new StorageException("Unable to open the metadata database.", exception);
        }
    }

    /**
     * Gets the block metadata entry from the provider.
     *
     * @param id The hash of the block header.
     *
     * @return The block metadata.
     */
    @Override
    public BlockMetadata getBlockMetadata(Sha256Hash id) throws StorageException
    {
        BlockMetadata metadata;

        try
        {
            ByteArrayOutputStream key = new ByteArrayOutputStream();
            key.write(BLOCK_PREFIX);
            key.write(id.serialize());

            byte[] rawHash = m_metadataDatabase.get(key.toByteArray());
            metadata = new BlockMetadata(ByteBuffer.wrap(rawHash));
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to get block metadata for block '%s'", id), exception);
        }

        return metadata;
    }

    /**
     * Adds a block metadata entry to the provider.
     *
     * @param metadata The metadata to be added.
     */
    @Override
    public boolean addBlockMetadata(BlockMetadata metadata) throws StorageException
    {
        try
        {
            ByteArrayOutputStream key = new ByteArrayOutputStream();
            key.write(BLOCK_PREFIX);
            key.write(metadata.getHash().serialize());

            m_metadataDatabase.put(key.toByteArray(), metadata.serialize());

            //s_logger.debug(String.format("Metadata added for block '%s'", metadata.getHash()));
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to add metadata for block '%s'", metadata.getHash()), exception);
        }

        return true;
    }

    /**
     * Sets the block chain head in the provider.
     *
     * @param metadata The metadata of the block chain head.
     */
    @Override
    public boolean setChainHead(BlockMetadata metadata)
    {
        m_metadataDatabase.put(new byte[] {HEAD_PREFIX}, metadata.serialize());

        return true;
    }

    /**
     * Gets the block chain head metadata entry from the provider.
     *
     * @return The block metadata.
     */
    @Override
    public BlockMetadata getChainHead()
    {
        BlockMetadata metadata;

        byte[] head = m_metadataDatabase.get(new byte[] {HEAD_PREFIX});

        // If we cant find the chain head, the blockchain is not yet initialized.
        if (head == null)
            return null;

        metadata = new BlockMetadata(ByteBuffer.wrap(head));

        return metadata;
    }

    /**
     * Adds a transaction metadata entry to the provider.
     *
     * @param metadata The metadata to be added.
     */
    @Override
    public void addTransactionMetadata(TransactionMetadata metadata) throws StorageException
    {
        try
        {
            ByteArrayOutputStream key = new ByteArrayOutputStream();
            key.write(TRANSACTION_PREFIX);
            key.write(metadata.getHash().serialize());

            m_metadataDatabase.put(key.toByteArray(), metadata.serialize());

            //s_logger.debug(String.format("Metadata added for transaction '%s'", metadata.getHash()));
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to add metadata for transaction '%s'", metadata.getHash()), exception);
        }
    }

    /**
     * Gets the metadata entry from the provider.
     *
     * @param id The hash of the transaction.
     *
     * @return The transaction metadata.
     */
    @Override
    public TransactionMetadata getTransactionMetadata(Sha256Hash id) throws StorageException
    {
        TransactionMetadata metadata;

        try
        {
            ByteArrayOutputStream key = new ByteArrayOutputStream();
            key.write(TRANSACTION_PREFIX);
            key.write(id.serialize());

            byte[] rawData = m_metadataDatabase.get(key.toByteArray());
            metadata = new TransactionMetadata(ByteBuffer.wrap(rawData));
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to get transaction '%s'", id), exception);
        }

        return metadata;
    }

    /**
     * Adds an unspent transaction to the provider. This outputs is now spendable by any other transaction in
     * the mem pool.
     *
     * @param output The unspent outputs to be added.
     */
    @Override
    public boolean addUnspentOutput(UnspentTransactionOutput output) throws StorageException
    {
        try
        {
            ByteArrayOutputStream key = new ByteArrayOutputStream();
            key.write(output.getTransactionHash().serialize());
            key.write(NumberSerializer.serialize(output.getIndex()));

            m_stateDatabase.put(key.toByteArray(), output.serialize());

            s_logger.debug(String.format("Unspent output %s added for transaction '%s'", output.getIndex(), output.getTransactionHash()));
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to add unspent output %s for transaction '%s'", output.getIndex(), output.getTransactionHash()), exception);
        }

        return true;
    }

    /**
     * Gets an unspent transaction from the provider.
     *
     * @param id    The id of the transaction that contains the unspent output.
     * @param index The index of the output inside the transaction.
     */
    @Override
    public UnspentTransactionOutput getUnspentOutput(Sha256Hash id, int index) throws StorageException
    {
        UnspentTransactionOutput output;

        try
        {
            ByteArrayOutputStream key = new ByteArrayOutputStream();
            key.write(id.serialize());
            key.write(NumberSerializer.serialize(index));

            byte[] data = m_stateDatabase.get(key.toByteArray());

            output = new UnspentTransactionOutput(ByteBuffer.wrap(data));
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to get unspent output %s for transaction '%s'", index, id), exception);
        }

        return output;
    }

    /**
     * Gets all the unspent outputs of a given public key.
     *
     * @param address The address of the wallet to get the unspent outputs for.
     *
     * @return An array with all the unspent outputs related to a given public address.
     */
    public ArrayList<UnspentTransactionOutput> getUnspentOutputsForAddress(Address address) throws StorageException
    {
        ArrayList<UnspentTransactionOutput> result = new ArrayList<>();

        try (DBIterator iterator = m_stateDatabase.iterator())
        {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next())
            {
                byte[] data = iterator.peekNext().getValue();

                UnspentTransactionOutput output = new UnspentTransactionOutput(ByteBuffer.wrap(data));

                if (Arrays.equals(output.getOutput().getLockingParameters(), address.getPublicHash()))
                    result.add(output);
            }
        }
         catch (Exception exception)
        {
            throw new StorageException(
                    String.format("Unable to get unspent outputs for address'%s'", address.toString()),
                    exception);
        }

        return result;
    }

    /**
     * Removes the unspent output transaction from the metadata provider.
     *
     * @param id    The id of the transaction that contains the unspent output.
     * @param index The index of the output inside the transaction.
     */
    @Override
    public boolean removeUnspentOutput(Sha256Hash id, int index) throws StorageException
    {
        try
        {
            ByteArrayOutputStream key = new ByteArrayOutputStream();
            key.write(id.serialize());
            key.write(NumberSerializer.serialize(index));

            m_stateDatabase.delete(key.toByteArray());

            s_logger.debug(String.format("Unspent output %s delete for transaction '%s'", index, id));
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to delete unspent output %s for transaction '%s'", index, id), exception);
        }

        return true;
    }
}
