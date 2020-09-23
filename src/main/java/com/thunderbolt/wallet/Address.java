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
import com.thunderbolt.security.Ripemd160Digester;
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.security.Sha256Hash;

import java.util.Arrays;

/**
 * Represents an account in the blockchain. You can send coins to a given address. The address is a hash of the public
 * key plus an extra byte to specify the wallet type and two checksum bytes at the end.
 */
public class Address
{
    private static final int CHECK_SUM_SIZE      = 4;
    private static final int PREFIX_SIZE         = 1;
    private static final int ADDRESS_STRING_SIZE = 52;

    private byte[] m_address;

    /**
     * Initializes a new instance of the Address class.
     *
     * @param prefix The prefix of the wallet, this prefix will depend on the network the wallet belongs too and the
     *               type of wallet.
     * @param data The actual data of the key, it can be either the public key of a wallet or the hash of the multi
     *             signature public keys.
     */
    Address(byte prefix, byte[] data)
    {
        byte[] publicHash = Ripemd160Digester.digest(Sha256Digester.digest(data).getData());
        byte[] checksum   = computeCheckSum(prefix, publicHash);

        m_address = new byte[PREFIX_SIZE + publicHash.length + checksum.length];

        m_address[0] = prefix;
        System.arraycopy(publicHash, 0, m_address, PREFIX_SIZE, publicHash.length);
        System.arraycopy(checksum, 0, m_address, PREFIX_SIZE + publicHash.length, checksum.length);
    }

    /**
     * String representation of the address.
     *
     * @param address The string representation of the address.
     */
    public Address(String address)
    {
        if (address.length() != ADDRESS_STRING_SIZE)
            throw new IllegalArgumentException("Invalid address string format. The address must be 50 chars long.");

        if (!address.startsWith("0x"))
            throw new IllegalArgumentException("Invalid address string format. The address must start with 0x.");

        byte[] data = Convert.hexStringToByteArray(address.substring(2));

        boolean isValid = isChecksumValid(data);

        if (!isValid)
            throw new IllegalArgumentException("Invalid address, Checksum did not match.");

        m_address = data;
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

    /**
     * Gets the public hash of this address.
     *
     * @return The public hash of the address.
     */
    public byte[] getPublicHash()
    {
        byte[] result = Arrays.copyOfRange(m_address, PREFIX_SIZE, m_address.length - CHECK_SUM_SIZE);
        return result;
    }

    /**
     * Computes the checksum of the given data.
     *
     * @param data The data to get checksum of.
     *
     * @return The 4 byte checksum.
     */
    private static byte[] computeCheckSum(byte addressPrefix, byte[] data)
    {
        byte[] publicHashPlusPrefix = new byte[data.length + 1];
        publicHashPlusPrefix[0] = addressPrefix;
        System.arraycopy(data, 0, publicHashPlusPrefix, 1, data.length);

        Sha256Hash sha256Hash = Sha256Digester.digest(publicHashPlusPrefix);
        byte[] result = new byte[CHECK_SUM_SIZE];

        System.arraycopy(sha256Hash.serialize(), 0, result, 0, CHECK_SUM_SIZE);

        return result;
    }

    /**
     * Verifies the given checksum..
     *
     * @param data The data with the checksum.
     *
     * @return True if the checksum is valid; otherwise; false.
     */
    private static boolean isChecksumValid(byte[] data)
    {
        byte   prefix         = data[0];
        byte[] result         = Arrays.copyOfRange(data, PREFIX_SIZE, data.length - CHECK_SUM_SIZE);
        byte[] givenCheckSum  = Arrays.copyOfRange(data, data.length - CHECK_SUM_SIZE, data.length);
        byte[] actualCheckSum = computeCheckSum(prefix, result);

        return Arrays.equals(givenCheckSum, actualCheckSum);
    }
}
