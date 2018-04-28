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

package com.thunderbolt;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.security.EllipticCurveKeyPair;
import com.thunderbolt.security.EllipticCurveProvider;
import com.thunderbolt.security.Hash;
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.transaction.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;

/* IMPLEMENTATION ************************************************************/

/**
 * Application main class.
 */
public class Main
{
    static EllipticCurveKeyPair s_genesisKeyPair     = new EllipticCurveKeyPair();
    static EllipticCurveKeyPair s_genesisKeyPair2    = new EllipticCurveKeyPair();
    static TransactionOutput    s_genesisOutput      = new TransactionOutput(1500, OutputLockType.SingleSignature, s_genesisKeyPair.getPublicKey());
    static Transaction          s_genesisTransaction = new Transaction();

    static HashMap<Hash, Transaction> s_UXTOPoll = new HashMap<>();

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws IOException, GeneralSecurityException
    {
        s_genesisTransaction.getOutputs().add(s_genesisOutput);

        Hash genesisHash = Sha256Digester.digest(s_genesisTransaction.serialize());

        s_UXTOPoll.put(genesisHash, s_genesisTransaction);

        // Create Transaction

        // Outpoint pointing to the first output in the genesis transaction.
        TransactionOutpoint outpoint = new TransactionOutpoint(Sha256Digester.digest(s_genesisTransaction.serialize()), 0);

        Transaction referencedTransaction = s_UXTOPoll.get(Sha256Digester.digest(s_genesisTransaction.serialize()));
        TransactionOutput referencedUxto = referencedTransaction.getOutputs().get(outpoint.getIndex());

        // When we sign, we use the locking parameters of the referenced transaction in place of the actual
        // unlocking parameter (which will be the resulting signature). So we add the locking parameters,
        // generate the signature, and then replace the locking parameters with the signature (the actual unlocking parameter).
        ByteArrayOutputStream unlockingParameters = new ByteArrayOutputStream();
        unlockingParameters.write(referencedUxto.getTransactionType().getValue());
        unlockingParameters.write(referencedUxto.getLockingParameters());

        TransactionInput input = new TransactionInput(outpoint, unlockingParameters.toByteArray(), 0);

        byte[] derSignature = EllipticCurveProvider.sign(input.serialize(), s_genesisKeyPair.getPrivateKey());
        input.setUnlockingParameters(derSignature);

        // At this point this input transaction is spendable.
        Transaction transaction = new Transaction();
        transaction.getInputs().add(input);

        // Transfer 1000 another user.
        transaction.getOutputs().add(new TransactionOutput(1000, OutputLockType.SingleSignature, s_genesisKeyPair2.getPublicKey()));

        // Return the change to myself.
        transaction.getOutputs().add(new TransactionOutput(500, OutputLockType.SingleSignature, s_genesisKeyPair.getPublicKey()));

        // Broadcast the transaction.
        byte[] xtb = transaction.serialize();

        // REC the transaction
        Transaction recXt = new Transaction(ByteBuffer.wrap(xtb));

        Hash secondGenXt = Sha256Digester.digest(recXt.serialize());

        // Validate transaction
        TransactionInput input1 = recXt.getInputs().get(0);

        // Pull the outpoint referenced by this input.
        TransactionOutpoint outpoint1 = input1.getPreviousOutput();
        // Find the whole transaction.
        Transaction referencedTransaction1 = s_UXTOPoll.get(outpoint1.getReferenceHash());
        // Get the referenced input.
        TransactionOutput out1 = referencedTransaction1.getOutputs().get(outpoint1.getIndex());
        // Get the signature.
        byte[] signature1 = input1.getUnlockingParameters();

        // To validate the signature we need to sign the transaction with the locking parameters in the unlocking parameters field.
        ByteArrayOutputStream unlockingParameters2 = new ByteArrayOutputStream();
        unlockingParameters2.write(out1.getTransactionType().getValue());
        unlockingParameters2.write(out1.getLockingParameters());

        input1.setUnlockingParameters(unlockingParameters.toByteArray());

        boolean isValid = EllipticCurveProvider.verify(input1.serialize(), signature1, out1.getLockingParameters());

        System.out.println(String.format("Can spend: %b", isValid));

        s_UXTOPoll.put(secondGenXt, recXt);


        Hash aHas = new Hash();
        Hash bHas = new Hash();
        BlockHeader header = new BlockHeader(10, aHas, bHas, 97, 50, 13);

        writeFile("C:\\Users\\Angel\\Downloads\\blobkheader1.bin", header.serialize());

        byte[] rawBh = readFile("C:\\Users\\Angel\\Downloads\\blobkheader1.bin");
        BlockHeader header2 = new BlockHeader(ByteBuffer.wrap(rawBh));

        ArrayList<Transaction> xts = new ArrayList<>();
        xts.add(referencedTransaction1);
        xts.add(recXt);

        Block block = new Block(header2, xts);
        writeFile("C:\\Users\\Angel\\Downloads\\blobk1.bin", block.serialize());

        byte[] rawB = readFile("C:\\Users\\Angel\\Downloads\\blobk1.bin");
        Block block2 = new Block(ByteBuffer.wrap(rawB));

        int a = 0;
    }

    /**
     * Reads a file for the disk.
     *
     * @param path The file.
     *
     * @return The data of the file.
     *
     * @throws IOException Thrown if the file is not found.
     */
    static byte[] readFile(String path) throws IOException
    {
        File            file       = new File(path);
        FileInputStream fileStream = new FileInputStream(file);
        byte[]          data       = new byte[(int) file.length()];

        fileStream.read(data);
        fileStream.close();

        return data;
    }

    /**
     * Writes a writes to the disk.
     *
     * @param path The file.
     * @param data The data to be saved.
     *
     * @throws IOException Thrown if the file is not found.
     */
    static void writeFile(String path, byte[] data) throws IOException
    {
        File             file       = new File(path);
        FileOutputStream fileStream = new FileOutputStream(file);

        fileStream.write(data);
        fileStream.close();
    }

    /** Returns the given byte array hex encoded. */
    public static String bytesToHexString(byte[] bytes) {
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        for (byte b : bytes) {
            String s = Integer.toString(0xFF & b, 16);
            if (s.length() < 2)
                buf.append('0');
            buf.append(s);
        }
        return buf.toString();
    }
}
