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

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.EllipticCurveProvider;
import com.thunderbolt.security.Hash;
import com.thunderbolt.transaction.contracts.ITransactionValidator;
import com.thunderbolt.transaction.parameters.MultiSignatureParameters;
import com.thunderbolt.transaction.parameters.SingleSignatureParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

/* IMPLEMENTATION ************************************************************/

/**
 * Standard transaction validator. This validator applies the following rules to the given transaction(s).
 *
 * Applies the following rules:
 *  1. For each input, look in the main branch to find the referenced output transaction. Reject if the output
 *     transaction is missing for any input. This would mean the input index is out of range, the output was
 *     already spent or simply the output does not exist.
 *  2. For each input, if the referenced output transaction is coinbase (i.e. only 1 input, with hash=0),
 *     it must have at least COINBASE_MATURITY confirmations; else reject.
 *  3. Verify unlocking parameters for each input; reject if any are bad.
 *  4. Using the referenced output transactions to get input values, reject if the sum of input values < sum
 *     of output values.
 */
public class StandardTransactionValidator implements ITransactionValidator
{
    // Static Fields
    private static final Logger s_logger = LoggerFactory.getLogger(StandardTransactionValidator.class);

    // Instance fields
    private IPersistenceService m_persistence;
    private NetworkParameters   m_params;

    /**
     * Initializes a new instance of the StandardTransactionValidator class.
     *
     * @param service    The persistence service.
     * @param parameters The network parameters.
     */
    public StandardTransactionValidator(IPersistenceService service, NetworkParameters parameters)
    {
        m_persistence = service;
        m_params      = parameters;
    }

    /**
     * Validates a single transaction.
     *
     * @param transaction The transaction to be validated.
     * @param height      The height of the block that contains this transaction. This is needed to perform
     *                    the coinbase maturity validation..
     *
     * @return True if the transaction is valid, otherwise, false.
     */
    @Override
    public boolean validate(Transaction transaction, long height) throws StorageException
    {
        if (!transaction.isValid())
            return false;

        BigInteger totalInputValue  = BigInteger.ZERO;
        BigInteger totalOutputValue = BigInteger.ZERO;

        int inputIndex = 0;
        for (TransactionInput input: transaction.getInputs())
        {
            UnspentTransactionOutput unspentOutput = m_persistence.getUnspentOutput(input.getReferenceHash(), input.getIndex());

            if (unspentOutput == null)
            {
                s_logger.debug(
                        "The transaction {} references an output ({}) that is not present in the UTXO database.",
                        input.getReferenceHash(), input.getIndex());

                return false;
            }

            // Check coin base maturity.
            long coinbaseMaturity = unspentOutput.getBlockHeight() + m_params.getCoinbaseMaturiry();
            if (unspentOutput.isIsCoinbase() && coinbaseMaturity > height)
            {
                s_logger.debug(
                        "The coinbase transaction {} can not be spend until height {}.",
                        unspentOutput.getTransactionHash(), coinbaseMaturity);

                return false;
            }

            // Check that the provided parameters can spend the referenced output.
            byte[] unlockingParameters = transaction.getUnlockingParameters().get(inputIndex);

            boolean canUnlock = checkUnlockingParameters(unspentOutput.getOutput(), input, unlockingParameters);

            if (!canUnlock)
            {
                s_logger.debug(
                        "The input {} in transaction {} cant spent the reference output ({}).",
                        inputIndex, transaction, input.getReferenceHash());

                return false;

            }

            totalInputValue = totalInputValue.add(unspentOutput.getOutput().getAmount());

            ++inputIndex;
        }

        for (TransactionOutput output: transaction.getOutputs())
        {
            totalOutputValue = totalOutputValue.add(output.getAmount());
            ++inputIndex;
        }

        if (totalOutputValue.longValue() > totalInputValue.longValue())
        {
            s_logger.debug(
                    "The sum of the outputs ({}) is bigger than the sum of the inputs ({}).",
                    totalOutputValue.longValue(), totalInputValue.longValue());

            return false;
        }

        return true;
    }

    /**
     * Verifies that the unlocking parameters from this input can unlock the referenced output.
     *
     * @param output              The output being spent.
     * @param input               The input trying to spend the output.
     * @param unlockingParameters The unlocking parameters.
     *
     * @return True if the input can unlock the referenced output; otherwise; false.
     */
    private boolean checkUnlockingParameters(TransactionOutput output, TransactionInput input, byte[] unlockingParameters)
    {
        boolean result = true;

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(input.serialize());
            data.write(output.getLockType().getValue());
            data.write(output.getLockingParameters());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        switch (output.getLockType())
        {
            case SingleSignature:
            {
               // To validate this type of lock, we just need to reconstruct the data that was signed and validate the
               // signature on said data. The unlocking parameters for this output lock should be the signature and
               // the locking parameters the public key.

                SingleSignatureParameters parameters = new SingleSignatureParameters(ByteBuffer.wrap(unlockingParameters));

                if (!Arrays.equals(output.getLockingParameters(), parameters.getPublicKeyHash()))
                {
                    s_logger.debug(
                            "Public key does not match. Locking Public Key {}, Unlocking Public Key {}",
                            Convert.toHexString(output.getLockingParameters()),
                            Convert.toHexString(parameters.getPublicKeyHash()));

                    result = false;
                }
                else
                {
                    result = EllipticCurveProvider.verify(data.toByteArray(), parameters.getSignature(), parameters.getPublicKey());
                }

                break;
            }
            case MultiSignature:
            {
               // The locking parameter is the hash of the actual locking parameters, this is done this way
               // because it would be too complicated to get the sender to construct the transaction. So we just
               // give the sender a multi signature address which is just the hash of the locking parameters of our
               // multi signature wallet. Then when we want to spent, we need to provide both the locking parameters
               // and the unlocking parameters. Of course the locking parameters when hashed need to match the hash
               // provided by the output lock.

                MultiSignatureParameters parameters = new MultiSignatureParameters(ByteBuffer.wrap(unlockingParameters));

                Hash outputLockingParametersHash = new Hash(output.getLockingParameters());
                Hash inputLockingParametersHash  = parameters.getHash();

                if (outputLockingParametersHash != inputLockingParametersHash)
                {
                    s_logger.debug("The provided hashed locking parameters provided by the input do not match the hash provided by the output.\n" +
                            "Output: {}\n" +
                            "Input: {}", outputLockingParametersHash, inputLockingParametersHash);
                    result = false;
                }

                if (parameters.getNeededSignatures() != parameters.getSignatures().size())
                {
                    s_logger.debug(
                            "Not enough signatures provided. Need {}, provide {}",
                            parameters.getNeededSignatures(),
                            parameters.getSignatures().size());
                    result = false;
                }

                for (Map.Entry<Byte, byte[]> entry : parameters.getSignatures().entrySet())
                {
                    if (!EllipticCurveProvider
                            .verify(
                                    data.toByteArray(),
                                    entry.getValue(),
                                    parameters.getPublicKeys().get(entry.getKey())))
                    {
                        s_logger.debug("One of the signatures is invalid.");
                        result = false;
                    }
                }
                break;
            }
            case Unlockable:
            {
               // This kind of output cant be spent. They are use for proof of burn or for committing data to the
               // blockchain.
                s_logger.debug("One of the referenced output by the transaction is not spendable.");
                result = false;
                break;
            }
            default:
                s_logger.debug("Unsupported output lock type.");
                result = false;
        }

        return result;
    }
}
