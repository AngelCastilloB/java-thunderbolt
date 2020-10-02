/*
 * MIT License
 *
 * Copyright (c) 2020 Angel Castillo.
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

import com.thunderbolt.common.Convert;
import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.persistence.contracts.INetworkAddressPool;
import com.thunderbolt.persistence.structures.NetworkAddressMetadata;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Predicate;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

/* IMPLEMENTATION ************************************************************/

/**
 * Uses a level DB to store the pool of known network address.
 */
public class LevelDbNetworkAddressPool implements INetworkAddressPool
{
    private static final Logger s_logger = LoggerFactory.getLogger(LevelDbNetworkAddressPool.class);

    // Constants
    static private final byte   ADDRESS_PREFIX   = 'a';
    static private final String ADDRESS_DB_NAME  = "peers";
    static private final int    EXPIRATION_TIME  = 10; // in days.
    static private final int    BAN_TIME         = 24 * 3600; // in seconds.

    // Instance Fields
    private final DB                                  m_addressDatabase;
    private final Map<String, NetworkAddressMetadata> m_addressPool;

    /**
     * Initializes a new instance of the LevelDbMetadataProvider class.
     *
     * @param path The path where the databases are located.
     */
    public LevelDbNetworkAddressPool(Path path) throws StorageException
    {
        Options options = new Options();
        options.createIfMissing(true);
        options.logger(s_logger::debug);

        try
        {
            s_logger.debug("Reading peer data from: {}", Paths.get(path.toString(), ADDRESS_DB_NAME));
            m_addressDatabase = factory.open(Paths.get(path.toString(), ADDRESS_DB_NAME).toFile(), options);
            m_addressPool = readAll();
            s_logger.debug("{} network addresses loaded.", m_addressPool.size());
        }
        catch (Exception exception)
        {
            throw new StorageException("Unable to open the metadata database.", exception);
        }
    }

    /**
     * Adds a new address to the pool. If the pool previously contained the address,
     * the old value is replaced by the specified value.
     *
     * @param addressMetadata The metadata of the network address.
     *
     * @return true if the address was added; otherwise; false.
     */
    @Override
    public boolean upsertAddress(NetworkAddressMetadata addressMetadata) throws StorageException
    {
        try
        {
            ByteArrayOutputStream key = new ByteArrayOutputStream();
            key.write(ADDRESS_PREFIX);
            key.write(addressMetadata.getNetworkAddress().serialize());

            m_addressDatabase.put(key.toByteArray(), addressMetadata.serialize());
            m_addressPool.put(Convert.toHexString(addressMetadata.getNetworkAddress().getAddress().getAddress()),
                    addressMetadata);
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to add metadata for address '%s'", addressMetadata.getNetworkAddress()), exception);
        }

        return true;
    }

    /**
     * Gets all the address stored in the pool.
     *
     * @return The list of all address.
     */
    @Override
    public List<NetworkAddressMetadata> getAddresses()
    {
        return new ArrayList<>(m_addressPool.values());
    }

    /**
     * Gets all the metadata for the address.
     *
     * @return The address metadata that matches the given address.
     */
    @Override
    public NetworkAddressMetadata getAddress(byte[] address)
    {
        return m_addressPool.get(Convert.toHexString(address));
    }

    /**
     * Gets whether this address is in the pool.
     *
     * @param address The address to query for.
     *
     * @return True if the address is in the pool; otherwise; false.
     */
    @Override
    public boolean contains(byte[] address)
    {
        return m_addressPool.containsKey(Convert.toHexString(address));
    }

    /**
     * Removes an address to the storage.
     *
     * @param metadata The address to be removed.
     */
    @Override
    public boolean removeAddress(NetworkAddressMetadata metadata) throws StorageException
    {
        try
        {
            ByteArrayOutputStream key = new ByteArrayOutputStream();
            key.write(ADDRESS_PREFIX);
            key.write(metadata.getNetworkAddress().serialize());

            m_addressDatabase.delete(key.toByteArray());
            m_addressPool.remove(Convert.toHexString(metadata.getNetworkAddress().getAddress().getAddress()));
        }
        catch (Exception exception)
        {
            throw new StorageException(
                    String.format("Unable to delete address %s.",
                            metadata.getNetworkAddress().getAddress()),
                    exception);
        }

        return true;
    }

    /**
     * Tried to retrieve a random selection of addresses matching the amount given, if not enough address are present
     * returns all the addresses in the pool that matches the criteria.
     *
     * @param amount Number of addresses to be requested.
     *
     * @return A list of network addresses metadata.
     */
    @Override
    public List<NetworkAddressMetadata> getRandom(int amount)
    {
        Predicate<NetworkAddressMetadata> isBanned = NetworkAddressMetadata::isBanned;
        Predicate<NetworkAddressMetadata> isActive = NetworkAddressMetadata::isActive;

        List<NetworkAddressMetadata> total = new LinkedList<>(m_addressPool.values());

        // Don't return an address if is banned.
        total.removeIf(isBanned);

        // Don't return address that we haven't heard from since three hours ago.
        total.removeIf(Predicate.not(isActive));

        int toBeRemoved = total.size() - amount;

        for (int i = 0; i < toBeRemoved; ++i)
        {
            int randomIndex = new Random().nextInt(total.size());
            total.remove(randomIndex);
        }

        return total;
    }

    /**
     * Gets the amount of address currently in the pool.
     *
     * @return The number of addresses.
     */
    @Override
    public int count()
    {
        return m_addressPool.size();
    }

    /**
     * Removed addresses that haven't been seeing in a long time.
     */
    @Override
    public void cleanUp() throws StorageException
    {
        for (Map.Entry<String, NetworkAddressMetadata> mapValue : m_addressPool.entrySet())
        {
            NetworkAddressMetadata metadata = mapValue.getValue();

            Period period =
                    Period.between(metadata.getLastMessageDate().toLocalDate(), LocalDateTime.now().toLocalDate());

            if (period.getDays() >= EXPIRATION_TIME)
            {
                removeAddress(metadata);
            }
        }
    }

    /**
     * Releases the ban status of an address if 24 hours already elapsed.
     */
    @Override
    public void checkReleaseBan() throws StorageException
    {
        for (Map.Entry<String, NetworkAddressMetadata> mapValue : m_addressPool.entrySet())
        {
            NetworkAddressMetadata metadata = mapValue.getValue();

            long period =
                    LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - metadata.getBanDate().toEpochSecond(ZoneOffset.UTC);

            if (period >= BAN_TIME)
            {
                metadata.setIsBanned(false);
                metadata.setBanScore((byte)0);
                upsertAddress(metadata);
            }
        }
    }

    /**
     * Bans the given address for 24 hours.
     *
     * @param address The address to be banned.
     */
    @Override
    public void banPeer(NetworkAddress address) throws StorageException
    {
        NetworkAddressMetadata originalData = getAddress(address.getAddress().getAddress());

        if (originalData != null)
        {
            originalData.setIsBanned(true);
            originalData.setBanDate(LocalDateTime.now());
            upsertAddress(originalData);
        }
    }

    /**
     * Updates the last seen from this peer address.
     *
     * @param address The network address.
     * @param dateTime The datetime.
     */
    @Override
    public void updateLastSeen(NetworkAddress address, LocalDateTime dateTime) throws StorageException
    {
        NetworkAddressMetadata originalData = getAddress(address.getAddress().getAddress());

        if (originalData != null)
        {
            originalData.setLastMessageDate(dateTime);

            upsertAddress(originalData);
        }
    }

    /**
     * Reads all the addresses on the disk and adds them to a map.
     *
     * @return The list of addresses.
     */
    private Map<String, NetworkAddressMetadata> readAll()
    {
        Map<String, NetworkAddressMetadata> result = new HashMap<>();

        try (DBIterator iterator = m_addressDatabase.iterator())
        {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next())
            {
                byte[] data = iterator.peekNext().getValue();

                NetworkAddressMetadata output = new NetworkAddressMetadata(ByteBuffer.wrap(data));
                result.put(Convert.toHexString(output.getNetworkAddress().getAddress().getAddress()), output);

                s_logger.debug(" - {}", output.getNetworkAddress());
            }
        }
        catch (Exception exception)
        {
            s_logger.error("Unable to get metadata for network addresses.", exception);
            s_logger.warn("Address pool will be empty.");
        }

        return result;
    }
}
