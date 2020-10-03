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

package com.thunderbolt.persistence.contracts;

/* IMPORTS *******************************************************************/

import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.NetworkAddressMetadata;

import java.time.LocalDateTime;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * The pool of known address networks.
 */
public interface INetworkAddressPool
{
    /**
     * Adds a new address to the pool. If the pool previously contained the address,
     * the old value is replaced by the specified value.
     *
     * @param addressMetadata The metadata of the network address.
     *
     * @return true if the address was added; otherwise; false.
     */
    boolean upsertAddress(NetworkAddressMetadata addressMetadata) throws StorageException;

    /**
     * Gets all the address stored in the pool.
     *
     * @return The list of all address.
     */
    List<NetworkAddressMetadata> getAddresses();

    /**
     * Gets all the metadata for the address.
     *
     * @return The address metadata that matches the given address.
     */
    NetworkAddressMetadata getAddress(byte[] address);

    /**
     * Tried to retrieve a random selection of addresses matching the mount given, if not enough address are present
     * returns all the addresses in the pool.
     *
     * @param amount Number of addresses to be requested.
     *
     * @return A list of network addresses metadata.
     */
    List<NetworkAddressMetadata> getRandom(int amount);

    /**
     * Gets whether this address is in the pool.
     *
     * @param address The address to query for.
     *
     * @return True if the address is in the pool; otherwise; false.
     */
    boolean contains(byte[] address);

    /**
     * Removes an address to the storage.
     *
     * @param address The address to be removed.
     */
    boolean removeAddress(NetworkAddressMetadata address) throws StorageException;

    /**
     * Gets the amount of address currently in the pool.
     *
     * @return The number of addresses.
     */
    int count();

    /**
     * Removed addresses that haven't been seeing in a long time.
     */
    void cleanUp() throws StorageException;

    /**
     * Updates the ban status of each address.
     */
    void checkReleaseBan() throws StorageException;

    /**
     * Updates the last seen from this peer address.
     *
     * @param address The network address.
     * @param dateTime The datetime.
     */
    void updateLastSeen(NetworkAddress address, LocalDateTime dateTime) throws StorageException;
}
