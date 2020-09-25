/*
 * MIT License
 *
 * @author 2017 Fabian Meyer
 * @author 2020 Angel Castillo
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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

/* IMPLEMENTATION ************************************************************/

/**
 * Offers a method for hashing messages with SHA-256.
 */
public class Sha256Digester
{
    private static final int[] K =
    {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    private static final int[] H0 =
    {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };

    private static final int BLOCK_BITS  = 512;
    private static final int BLOCK_BYTES = BLOCK_BITS / 8;

    // working arrays
    private final int[]             m_w         = new int[64];
    private final int[]             m_h         = new int[8];
    private final int[]             m_tmp       = new int[8];
    private final ArrayList<byte[]> m_midStates = new ArrayList<>();
    private final ArrayList<byte[]> m_blocks    = new ArrayList<>();

    /**
     * Gets the hash of the given data.
     *
     * @param message The data to get the hash from.
     *
     * @return The hash.
     */
    public Sha256Hash hash(byte[] message)
    {
        m_midStates.clear();
        m_blocks.clear();

        // let H = H0
        System.arraycopy(H0, 0, m_h, 0, H0.length);

        // initialize all words
        int[] words = pad(message);

        // enumerate all blocks (each containing 16 words)
        for (int i = 0, n = words.length / 16; i < n; ++i)
        {
            // initialize W from the block's words
            System.arraycopy(words, i * 16, m_w, 0, 16);

            // Copy the block's int as bytes to the blocks array for future inspection.
            byte[] blockData = new byte[64];
            for (int intIndex = 0; intIndex < 16; ++intIndex)
            {
                byte[] numberSerialized = NumberSerializer.serialize(m_w[intIndex]);
                System.arraycopy(numberSerialized, 0, blockData, intIndex * 4, 4);
            }
            m_blocks.add(blockData);

            for (int t = 16; t < m_w.length; ++t)
                m_w[t] = smallSig1(m_w[t - 2]) + m_w[t - 7] + smallSig0(m_w[t - 15]) + m_w[t - 16];

            // let TEMP = H
            System.arraycopy(m_h, 0, m_tmp, 0, m_h.length);

            // operate on TEMP
            for (int t = 0; t < m_w.length; ++t)
            {
                int t1 = m_tmp[7] + bigSig1(m_tmp[4]) + choice(m_tmp[4], m_tmp[5], m_tmp[6]) + K[t] + m_w[t];
                int t2 = bigSig0(m_tmp[0]) + majority(m_tmp[0], m_tmp[1], m_tmp[2]);
                System.arraycopy(m_tmp, 0, m_tmp, 1, m_tmp.length - 1);
                m_tmp[4] += t1;
                m_tmp[0] = t1 + t2;
            }

            // add values in TEMP to values in H
            for (int t = 0; t < m_h.length; ++t)
                m_h[t] += m_tmp[t];

            m_midStates.add(toByteArray(m_h));
        }

        return new Sha256Hash(toByteArray(m_h));
    }

    /**
     * Gets the hash of the given data.
     *
     * @param message The data to get the hash from.
     *
     * @return The hash.
     */
    static public Sha256Hash digest(byte[] message)
    {
        return new Sha256Digester().hash(message);
    }

    /**
     * Gets the midstate at each round.
     *
     * @param round The index of the midstate.
     *
     * @return Byte array of of the midstate at the given round.
     */
    public byte[] getMidstate(int round)
    {
        return m_midStates.get(round);
    }

    /**
     * Gets thes the block data.
     *
     * @param index The block index.
     *
     * @return Byte array of matching the given index.
     */
    public byte[] getBlock(int index)
    {
        return m_blocks.get(index);
    }

    /**
     * <b>Internal method, no need to call.</b> Pads the given message to have a length
     * that is a multiple of 512 bits (64 bytes), including the addition of a
     * 1-bit, k 0-bits, and the message length as a 64-bit integer.
     * The result is a 32-bit integer array with big-endian byte representation.
     *
     * @param message The message to pad.
     * @return A new array with the padded message bytes.
     */
    public static int[] pad(byte[] message)
    {
        // new message length: original + 1-bit and padding + 8-byte length
        // --> block count: whole blocks + (padding + length rounded up)
        int finalBlockLength = message.length % BLOCK_BYTES;
        int blockCount = message.length / BLOCK_BYTES + (finalBlockLength + 1 + 8 > BLOCK_BYTES ? 2 : 1);

        final IntBuffer result = IntBuffer.allocate(blockCount * (BLOCK_BYTES / Integer.BYTES));

        // copy as much of the message as possible
        ByteBuffer buf = ByteBuffer.wrap(message);
        for (int i = 0, n = message.length / Integer.BYTES; i < n; ++i) {
            result.put(buf.getInt());
        }
        // copy the remaining bytes (less than 4) and append 1 bit (rest is zero)
        ByteBuffer remainder = ByteBuffer.allocate(4);
        remainder.put(buf).put((byte) 0b10000000).rewind();
        result.put(remainder.getInt());

        // ignore however many pad bytes (implicitly calculated in the beginning)
        result.position(result.capacity() - 2);
        // place original message length as 64-bit integer at the end
        long msgLength = message.length * 8L;
        result.put((int) (msgLength >>> 32));
        result.put((int) msgLength);

        return result.array();
    }

    /**
     * Converts the given int array into a byte array via big-endian conversion
     * (1 int becomes 4 bytes).
     *
     * @param ints The source array.
     * @return The converted array.
     */
    private static byte[] toByteArray(int[] ints)
    {
        ByteBuffer buf = ByteBuffer.allocate(ints.length * Integer.BYTES);
        for (int i : ints) {
            buf.putInt(i);
        }
        return buf.array();
    }

    /**
     * This function uses the x bit to choose between the y and z bits. It chooses the y bit if x=1, and
     * chooses the z bit if x=0.
     *
     * Ch(x, y, z) = (x & y) ^ (~x & z)
     *
     * @param x The x bit.
     * @param y The y bit.
     * @param z The z bit.
     *
     * @return The chosen bit.
     */
    private static int choice(int x, int y, int z)
    {
        return (x & y) | ((~x) & z);
    }

    /**
     * This function returns the majority of the three bits.
     *
     * Maj(x, y, z) = (x & y) ^ (x & z) ^ (y & z)
     *
     * @param x First bit.
     * @param y Second bit.
     * @param z Third bit.
     *
     * @return The majority bit.
     */
    private static int majority(int x, int y, int z)
    {
        return (x & y) | (x & z) | (y & z);
    }

    /**
     * Performs:
     *
     *  Σ0(x) = ROTR2(x) ^ ROTR13(x) ^ ROTR22(x)
     *
     * Over the parameters x.
     *
     * @param x The int to be rotated.
     *
     * @return The result of the rotation operations.
     */
    private static int bigSig0(int x)
    {
        return Integer.rotateRight(x, 2)
                ^ Integer.rotateRight(x, 13)
                ^ Integer.rotateRight(x, 22);
    }

    /**
     * Performs:
     *
     *  Σ1(x) = ROTR6(x) ^ ROTR11(x) ^ ROTR25(x)
     *
     * Over the parameters x.
     *
     * @param x The int to be rotated.
     *
     * @return The result of the rotation operations.
     */
    private static int bigSig1(int x)
    {
        return Integer.rotateRight(x, 6)
                ^ Integer.rotateRight(x, 11)
                ^ Integer.rotateRight(x, 25);
    }

    /**
     * Performs:
     *
     *  σ0(x) = ROTR7(x) ^ ROTR18(x) ^ SHR3(x)
     *
     * Over the parameters x.
     *
     * @param x The int to be rotated.
     *
     * @return The result of the rotation operations.
     */
    private static int smallSig0(int x)
    {
        return Integer.rotateRight(x, 7)
                ^ Integer.rotateRight(x, 18)
                ^ (x >>> 3);
    }

    /**
     * Performs:
     *
     *  σ1(x) = ROTR17(x) ^ ROTR19(x) ^ SHR10(x)
     *
     * Over the parameters x.
     *
     * @param x The int to be rotated.
     *
     * @return The result of the rotation operations.
     */
    private static int smallSig1(int x)
    {
        return Integer.rotateRight(x, 17)
                ^ Integer.rotateRight(x, 19)
                ^ (x >>> 10);
    }
}