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

// IMPLEMENTATION ************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.security.Sha256Digester;

/**
 * Represents an account in the blockchain. You can send coins to a given address. The address is a hash of the public
 * key plus an extra byte to specify the wallet type and two checksum bytes at the end.
 */
public class Address
{
    private byte[] m_address;

    /**
     * Initializes a new instance of the Address class.
     *
     * @param prefix The prefix of the wallet, this prefix will depend on the network the wallet belongs too and the
     *               type of wallet.
     * @param data The actual data of the key, it can be either the public key of a wallet or the hash of the multi
     *              signature public keys.
     */
    Address(byte prefix, byte[] data)
    {
        byte[] checksum = new byte[] { 0x00, 0x00, 0x00, 0x00 };

        // Gets RIPEMD160 of main data.
        byte[] hash160 = Sha256Digester.hash160(data);

        // Concat prefix plus data.
        byte[] hash160PlusPrefix = new byte[data.length + 1];
        hash160PlusPrefix[0] = prefix;
        System.arraycopy(hash160, 0, hash160PlusPrefix, 1, hash160.length);

        // Calculate checksum
        byte[] sha256 = Sha256Digester.digest(hash160PlusPrefix).getData();
        System.arraycopy(sha256, 0, checksum, 0, checksum.length);

        m_address = new byte[1 + hash160.length + checksum.length];

        m_address[0] = prefix;
        System.arraycopy(hash160, 0, m_address, 1, hash160.length);
        System.arraycopy(checksum, 0, m_address, 1 + hash160.length, checksum.length);
    }

    /**
     * String representation of the address.
     *
     * @param address The string representation of the address.
     */
    Address(String address)
    {
        //TODO: implement.
    }

    /**
     * Gets the string representation of this address.
     *
     * @return The string representation.
     */
    public String toString()
    {
        return "0x" + Convert.toHexString(m_address).toLowerCase();
    }

    /**
     * Gets the prefix of this address.
     *
     * @return The prefix. This prefix identifies the network and the type of wallet.
     */
    public byte getPrefix()
    {
        return m_address[0];
    }
}
