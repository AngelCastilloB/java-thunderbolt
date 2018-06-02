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
package com.thunderbolt.transaction.parameters;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;
import com.thunderbolt.security.Sha256Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* IMPLEMENTATION ************************************************************/

/**
 * Helper class to wrap the locking parameters of a multi signature transaction.
 */
public class MultiSignatureParameters implements ISerializable
{
    private static final Logger s_logger = LoggerFactory.getLogger(MultiSignatureParameters.class);

    private byte              m_totalSigners     = 0;
    private byte              m_neededSignatures = 0;
    private List<byte[]>      m_publicKeys       = new ArrayList<>();
    private Map<Byte, byte[]> m_signatures       = new HashMap<>();

    /**
     * Initializes a new instance of the MultiSignatureParameters class.
     */
    public MultiSignatureParameters()
    {
    }

    /**
     * Initializes a new instance of the MultiSignatureParameters class.
     *
     * @param buffer A buffer contained a serialized MultiSignatureParameters.
     */
    public MultiSignatureParameters(ByteBuffer buffer)
    {
        m_totalSigners = buffer.get();
        m_neededSignatures = buffer.get();

        for (int  i = 0; i < m_totalSigners; ++i)
        {
            int size = buffer.getInt();

            byte[] publicKey = new byte[size];
            buffer.get(publicKey);

            m_publicKeys.add(publicKey);
        }

        byte signatures = buffer.get();

        for (int  i = 0; i < signatures; ++i)
        {
            byte   index     = buffer.get();
            int    size      = buffer.getInt();
            byte[] signature = new byte[size];
            buffer.get(signature);

            m_signatures.put(index, signature);
        }
    }

    /**
     * Initializes a new instance of the MultiSignatureParameters class.
     *
     * @param totalSignatures  The total number of signers that own this output.
     * @param neededSignatures The number of signatures out of the total signature count needed to spend the output.
     * @param publicKeys       The list of public keys (one for each signer).
     * @param signatures       The list of signatures.
     */
    public MultiSignatureParameters(byte totalSignatures, byte neededSignatures, List<byte[]> publicKeys, List<byte[]> signatures)
    {
        m_totalSigners     = totalSignatures;
        m_neededSignatures = neededSignatures;
        m_publicKeys       = publicKeys;
    }

    /**
     * Gets the number of signer that own this output.
     *
     * @return The number of signers.
     */
    public byte getTotalSigners()
    {
        return m_totalSigners;
    }

    /**
     * Sets the number of signers that own this account.
     *
     * @param signers The signers.
     */
    public void setTotalSigners(byte signers)
    {
        m_totalSigners = signers;
    }

    /**
     * Gets the number of needed signatures to spend this output.
     *
     * @return The number of signatures needed.
     */
    public byte getNeededSignatures()
    {
        return m_neededSignatures;
    }

    /**
     * Sets the number of needed signatures to spend this output.
     *
     * @param neededSignatures The number of signatures needed.
     */
    public void setNeededSignatures(byte neededSignatures)
    {
        m_neededSignatures = neededSignatures;
    }

    /**
     * Gets the list of public keys that can sign on this output.
     *
     * @return The number of public keys.
     */
    public List<byte[]> getPublicKeys()
    {
        return m_publicKeys;
    }

    /**
     * Sets the list of public keys that can sign on this output.
     *
     * @param publicKeys The number of public keys.
     */
    public void setPublicKeys(List<byte[]> publicKeys)
    {
        m_publicKeys = publicKeys;
    }

    /**
     * Gets the list of signatures.
     *
     * @return The signatures.
     */
    public Map<Byte, byte[]> getSignatures()
    {
        return m_signatures;
    }

    /**
     * Sets the list of signatures.
     *
     * @param signatures The signatures.
     */
    public void setSignatures(Map<Byte, byte[]> signatures)
    {
        m_signatures = signatures;
    }

    /**
     * Adds a signature to the parameters.
     *
     * @param index     The index corresponding with the public key used to validate the signature.
     * @param signature The signature.
     */
    public void addSignature(byte index, byte[] signature)
    {
        m_signatures.put(index, signature);
    }

    /**
     * Gets the hash of the serialized data of this instance..
     *
     * @return The hash.
     */
    public Hash getHash()
    {
        return Sha256Digester.digest(serializeWithoutSignatures());
    }

    /**
     * Serializes this object without the signatures. This is useful for calculating the parameters hash..
     *
     * @return The serialized object.
     */
    public byte[] serializeWithoutSignatures()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.write(m_totalSigners);
        data.write(m_neededSignatures);

        for (byte[] publicKey: m_publicKeys)
        {
            try
            {
                data.write(NumberSerializer.serialize(publicKey.length));
                data.write(publicKey);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return data.toByteArray();
    }

    /**
     * Serializes an object in raw byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.write(m_totalSigners);
        data.write(m_neededSignatures);

        try
        {
            for (byte[] publicKey: m_publicKeys)
            {

                data.write(NumberSerializer.serialize(publicKey.length));
                data.write(publicKey);
            }

            data.write(NumberSerializer.serialize(m_signatures.size()));
            for (Map.Entry<Byte, byte[]> entry : m_signatures.entrySet())
            {
                data.write(entry.getKey());
                data.write(NumberSerializer.serialize(entry.getValue().length));
                data.write(entry.getValue());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();

        }
        return data.toByteArray();
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        final int firstLevelTabs = 2;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format(
                "{                            %n" +
                "  \"totalSigners\":      %s, %n" +
                "  \"neededSignature\":   %s, %n" +
                "  \"publicKeys\":",
                m_totalSigners,
                m_neededSignatures));

        stringBuilder.append(Convert.toJsonArrayLikeString(
                Convert.toHexStringArray(m_publicKeys),
                firstLevelTabs));

        stringBuilder.append(",");
        stringBuilder.append(System.lineSeparator());

        stringBuilder.append(String.format("  \"signatures\":  [%n"));

        int index = 0;
        for (Map.Entry<Byte, byte[]> entry : m_signatures.entrySet())
        {
            stringBuilder.append(
                    String.format("      {%n        \"position\":   %s,%n        \"signature\":  \"%s\"%n      }",
                            entry.getKey(),
                            Convert.toHexString(entry.getValue())));

            if (index != m_signatures.entrySet().size() - 1)
                stringBuilder.append(String.format(",%n"));

            ++index;
        }

        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("  ]");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
