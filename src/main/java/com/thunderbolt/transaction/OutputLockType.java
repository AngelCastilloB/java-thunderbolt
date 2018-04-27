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
package com.thunderbolt.transaction;

// IMPLEMENTATION ************************************************************/

/**
 * The output lock type.
 *
 * Thunderbolt do not inherit the scripting language from Bitcoin, instead it defines a small set of lock types
 * for the resulting outputs of the transactions.
 */
public enum OutputLockType
{
    /**
     * To unlock this kind of output, the transaction must provide a single digital signature that can be validated
     * with the public key in the locking parameters of the output.
     */
    SingleSignature((byte)0x00),

    /**
     * To unlock this kind of output, the transaction must provide a set of public keys and N of M digital
     * signatures generated with those public keys. The public keys must also be hashed in a particular order
     * and the resulting hash must match the hash in the locking parameters of the output.
     */
    MultiSignature((byte)0x01),

    /**
     * This type of output can not be unlocked. This is useful for committing information to the block chain.
     *
     * The max allow locking parameters size for this lock type is 32 bytes (sha252 digest size).
     */
    Unlockable((byte)0x02);


    // Instance fields.
    private byte m_value;

    /**
     * Initializes a new instance of the OutputLockType class.
     *
     * @param value The enum value.
     */
    OutputLockType(byte value)
    {
        m_value = value;
    }

    /**
     * Gets the byte value of thins enum instance.
     *
     * @return The byte value.
     */
    public byte getValue()
    {
        return m_value;
    }

    /**
     * Gets an enum value from a byte.
     *
     * @param value The byte to be casted.
     *
     * @return The enum value.
     */
    static public OutputLockType from(byte value)
    {
        return OutputLockType.values()[value & 0xFF];
    }
}
