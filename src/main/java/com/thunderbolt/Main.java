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

import com.thunderbolt.common.ServiceLocator;
import com.thunderbolt.persistence.IPersistenceService;
import com.thunderbolt.persistence.StandardPersistenceService;
import com.thunderbolt.persistence.storage.*;
import com.thunderbolt.security.*;
import com.thunderbolt.transaction.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.HashMap;

/* IMPLEMENTATION ************************************************************/

/**
 * Application main class.
 */
public class Main
{
    // Constants
    static private final String USER_HOME_PATH   = System.getProperty("user.home");
    static private final String DATA_FOLDER_NAME = ".thunderbolt";
    static private final Path   DEFAULT_PATH     = Paths.get(USER_HOME_PATH, DATA_FOLDER_NAME);
    static private final Path   BLOCKS_PATH      = Paths.get(DEFAULT_PATH.toString(), "blocks");
    static private final Path   REVERT_PATH      = Paths.get(DEFAULT_PATH.toString(), "reverts");
    static private final Path   METADATA_PATH    = Paths.get(DEFAULT_PATH.toString(), "metadata");
    static private final String BLOCK_PATTERN    = "block%05d.bin";
    static private final String REVERT_PATTERN   = "revert%05d.bin";

    private static final Logger s_logger = LoggerFactory.getLogger(Main.class);

    static EllipticCurveKeyPair s_genesisKeyPair     = new EllipticCurveKeyPair();
    static EllipticCurveKeyPair s_genesisKeyPair2    = new EllipticCurveKeyPair();
    static TransactionOutput    s_genesisOutput      = new TransactionOutput(BigInteger.valueOf(1500), OutputLockType.SingleSignature, s_genesisKeyPair.getPublicKey());
    static Transaction          s_genesisTransaction = new Transaction();

    static HashMap<Hash, Transaction> s_UXTOPoll = new HashMap<>();

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws IOException, GeneralSecurityException, CloneNotSupportedException, StorageException
    {
        initializeServices();
        //Block genesisBlock = NetworkParameters.createGenesis();
        //ServiceLocator.getService(IPersistenceService.class).persist(genesisBlock, 0, genesisBlock.getWork());

        Transaction xt = ServiceLocator.getService(IPersistenceService.class).getTransaction(new Hash("71D7E987F134CB712A247ECFCA3CCBC42B8B7D0C8654115B81F077561E08B97B"));

        ServiceLocator.register(Transaction.class, xt);
        Transaction copy = ServiceLocator.getService(Transaction.class);

        s_logger.debug("Valid: {}", xt.isValid());
        /*
        initializeServices();

        //StandardPersistenceService.getInstance().persist(NetworkParameters.createGenesis(), 0);
        Transaction spentXT = StandardPersistenceService.getInstance().getTransaction(new Hash("71D7E987F134CB712A247ECFCA3CCBC42B8B7D0C8654115B81F077561E08B97B"));
        Block block = StandardPersistenceService.getInstance().getBlock(new Hash("00000004063B34C6FE99D1DB8A8C7F041B46487E64B0ED74C0EE8B7D4FA8F4E9"));

        s_logger.debug(String.format("Block is valid: %s", block.isValid()));
        //UnspentTransactionOutput uxto = StandardPersistenceService.getInstance().getUnspentOutput(new Hash("71D7E987F134CB712A247ECFCA3CCBC42B8B7D0C8654115B81F077561E08B97B"), 0);
        int a = 3;
        ++a;
/*
        UnspentTransactionOutput uxto = new UnspentTransactionOutput();
        uxto.setTransactionHash(spentXT.getTransactionId());
        uxto.setVersion(spentXT.getVersion());
        uxto.setIndex(0);
        uxto.setBlockHeight(0);
        uxto.setIsCoinbase(uxto.isIsCoinbase());
        uxto.setOutput(spentXT.getOutputs().get(0));

        StandardPersistenceService.getInstance().addUnspentOutput(uxto);*/
        /*
        Block newBlock = NetworkParameters.createGenesis();
        BigInteger hash = newBlock.getHeaderHash().toBigInteger();
        boolean solved = false;
        while (!solved)
        {
            solved = !(hash.compareTo(newBlock.getTargetDifficultyAsInteger()) > 0);
            if (solved)
                break;
            //System.out.println(String.format("Block hash is higher than target difficulty: %s > %s", newBlock.getHeaderHash(), Convert.toHexString(newBlock.getTargetDifficultyAsInteger().toByteArray())));
            newBlock.getHeader().setNonce(newBlock.getHeader().getNonce() + 1);
            hash = newBlock.getHeaderHash().toBigInteger();
        }

        s_logger.debug(String.format("Block solved! hash is lower than target difficulty (%d): %s > %s", newBlock.getHeader().getNonce(), newBlock.getHeaderHash(), Convert.toHexString(newBlock.getTargetDifficultyAsInteger().toByteArray())));
*/

        /*
        UnspentTransactionOutput spentXT = StandardPersistenceService.getInstance().getUnspentOutput(new Hash("E2DBE246FEEAFD8B57CB2C08A6C62DA2F2CF98BE9BA21CD5CE3E6FD485D21E8D"));

        s_logger.debug(String.format("%s", spentXT.getTransactionHash()));

        TransactionInput outpoint = new TransactionInput(new Hash("E2DBE246FEEAFD8B57CB2C08A6C62DA2F2CF98BE9BA21CD5CE3E6FD485D21E8D"), 0);
        TransactionInput input = new TransactionInput(outpoint, 0);

        // When we sign the transaction input plus the locking parameters of the referenced output.
        ByteArrayOutputStream signatureData = new ByteArrayOutputStream();
        signatureData.write(input.serialize());
        signatureData.write(spentXT.getOutputs().get(0).getTransactionType().getValue());
        signatureData.write(spentXT.getOutputs().get(0).getLockingParameters());

        // The signature in DER format is the unlocking parameter of the referenced output. We need to add this to the unlocking parameters
        // list of the transaction at the same position at which we added the transaction.
        byte[] derSignature = EllipticCurveProvider.sign(signatureData.toByteArray(), s_genesisKeyPair.getPrivateKey());

        // At this point this input transaction is spendable.
        Transaction transaction = new Transaction();
        transaction.getInputs().add(input);
        transaction.getUnlockingParameters().add(derSignature);

        // Transfer 1000 another user.
        transaction.getOutputs().add(new TransactionOutput(BigInteger.valueOf(1000), OutputLockType.SingleSignature, s_genesisKeyPair2.getPublicKey()));

        // Return the change to myself.
        transaction.getOutputs().add(new TransactionOutput(BigInteger.valueOf(500), OutputLockType.SingleSignature, s_genesisKeyPair.getPublicKey()));

        Block newBlock = new Block();
        newBlock.addTransactions(transaction);
        newBlock.getHeader().setTimeStamp(1525003294);
        newBlock.getHeader().setBits(0x1d07fff8L);
        newBlock.getHeader().setParentBlockHash(NetworkParameters.createGenesis().getHeaderHash());

        BigInteger hash = newBlock.getHeaderHash().toBigInteger();
        boolean solved = false;
        while (!solved)
        {
            solved = !(hash.compareTo(newBlock.getTargetDifficultyAsInteger()) > 0);
            if (solved)
                break;
            //System.out.println(String.format("Block hash is higher than target difficulty: %s > %s", newBlock.getHeaderHash(), Convert.toHexString(newBlock.getTargetDifficultyAsInteger().toByteArray())));
            newBlock.getHeader().setNonce(newBlock.getHeader().getNonce() + 1);
            hash = newBlock.getHeaderHash().toBigInteger();
        }

        s_logger.debug(String.format("Block solved! hash is lower than target difficulty (%d): %s > %s", newBlock.getHeader().getNonce(), newBlock.getHeaderHash(), Convert.toHexString(newBlock.getTargetDifficultyAsInteger().toByteArray())));

        StandardPersistenceService.getInstance().persist(newBlock, 0);

        s_logger.debug(String.format("Added Block %s, with transaction %s", newBlock.getHeader().getTransactionHash(), newBlock.getTransaction(0).getTransactionId()));

        /*
        Block genesisBlock = NetworkParameters.createGenesis();
        s_genesisTransaction.getOutputs().add(s_genesisOutput);

        StandardPersistenceService.getInstance().persist(genesisBlock, 0);

        Block loaded = StandardPersistenceService.getInstance().getBlock(genesisBlock.getHeaderHash());

        BlockMetadata metadata = new BlockMetadata();
        metadata.setHeader(genesisBlock.getHeader());

        metadata.setHeight(20);
        metadata.setBlockSegment(32);

        s_logger.debug(String.format("Adding block %s metadata to db", metadata.getTransactionHash().toString()));
        BlocksManifest.addBlockMetadata(metadata);

        BlockMetadata metadata2 = BlocksManifest.getBlockMetadata(metadata.getHeader().getTransactionHash());
        s_logger.debug(String.format("Read block %s metadata from db", metadata2.getTransactionHash().toString()));


        NetworkParameters params = NetworkParameters.mainNet();

        writeFile("C:\\Users\\Angel\\Downloads\\genesisBlock.bin", genesisBlock.serialize());

        Hash genesisHash = Sha256Digester.digest(s_genesisTransaction.serialize());

        EncryptedPrivateKey eCk = new EncryptedPrivateKey(s_genesisKeyPair.getPrivateKey(), "angel");

        byte[] adressRaw = new byte[24];
        byte[] addressHash = Sha256Digester.sha256hash160(s_genesisKeyPair.getPublicKey());

        //writeFile("C:\\Users\\Angel\\Downloads\\genesisKey.bin", eCk.serialize());

        s_UXTOPoll.put(genesisHash, s_genesisTransaction);


        //System.out.println(Base58.encode(s_genesisKeyPair.getPublicKey()));
        // Create Transaction

        // Outpoint pointing to the first output in the genesis transaction.
        TransactionInput outpoint = new TransactionInput(Sha256Digester.digest(s_genesisTransaction.serialize()), 0);

        Transaction referencedTransaction = s_UXTOPoll.get(Sha256Digester.digest(s_genesisTransaction.serialize()));
        TransactionOutput referencedUxto = referencedTransaction.getOutputs().get(outpoint.getIndex());


        TransactionInput input = new TransactionInput(outpoint, 0);

        // When we sign the transaction input plus the locking parameters of the referenced output.
        ByteArrayOutputStream signatureData = new ByteArrayOutputStream();
        signatureData.write(input.serialize());
        signatureData.write(referencedUxto.getTransactionType().getValue());
        signatureData.write(referencedUxto.getLockingParameters());


        // The signature in DER format is the unlocking parameter of the referenced output. We need to add this to the unlocking parameters
        // list of the transaction at the same position at which we added the transaction.
        byte[] derSignature = EllipticCurveProvider.sign(signatureData.toByteArray(), s_genesisKeyPair.getPrivateKey());

        // At this point this input transaction is spendable.
        Transaction transaction = new Transaction();
        transaction.getInputs().add(input);
        transaction.getUnlockingParameters().add(derSignature);

        // Transfer 1000 another user.
        transaction.getOutputs().add(new TransactionOutput(BigInteger.valueOf(1000), OutputLockType.SingleSignature, s_genesisKeyPair2.getPublicKey()));

        // Return the change to myself.
        transaction.getOutputs().add(new TransactionOutput(BigInteger.valueOf(500), OutputLockType.SingleSignature, s_genesisKeyPair.getPublicKey()));

        // Broadcast the transaction.
        byte[] xtb = transaction.serialize();

        // REC the transaction
        Transaction recXt = new Transaction(ByteBuffer.wrap(xtb));

        Hash secondGenXt = Sha256Digester.digest(recXt.serialize());

        // Validate transaction
        TransactionInput input1 = recXt.getInputs().get(0);

        // Pull the outpoint referenced by this input.
        TransactionInput outpoint1 = input1.getPreviousOutput();
        // Find the whole transaction.
        Transaction referencedTransaction1 = s_UXTOPoll.get(outpoint1.getReferenceHash());
        // Get the referenced input.
        TransactionOutput out1 = referencedTransaction1.getOutputs().get(outpoint1.getIndex());
        // Get the signature.
        byte[] signature1 = recXt.getUnlockingParameters().get(0);

        // To validate the signature we need to sign the transaction with the locking parameters in the unlocking parameters field.
        ByteArrayOutputStream signatureData2 = new ByteArrayOutputStream();
        signatureData2.write(input1.serialize());
        signatureData2.write(out1.getTransactionType().getValue());
        signatureData2.write(out1.getLockingParameters());

        boolean isValid = EllipticCurveProvider.verify(signatureData2.toByteArray(), signature1, out1.getLockingParameters());

        s_logger.debug(String.format("Can spend: %b", isValid));

        s_UXTOPoll.put(secondGenXt, recXt);


        Hash aHas = new Hash();
        Hash bHas = new Hash();
        BlockHeader header = new BlockHeader(10, aHas, bHas, 97, 13);

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

        s_logger.debug(String.format("Header hash: %s", block2.getHeaderHash()));

        byte[] genesisRaw = NetworkParameters.createGenesis().serialize();

        Block deserializedGenesis = new Block(ByteBuffer.wrap(genesisRaw));

        BigInteger hash = deserializedGenesis.getHeaderHash().toBigInteger();
        boolean solved = false;
        while (!solved)
        {
            solved = !(hash.compareTo(deserializedGenesis.getTargetDifficultyAsInteger()) > 0);
            if (solved)
                break;
            //System.out.println(String.format("Block hash is higher than target difficulty: %s > %s", block2.getHeaderHash(), Convert.toHexString(block2.getTargetDifficultyAsInteger().toByteArray())));
            deserializedGenesis.getHeader().setNonce(deserializedGenesis.getHeader().getNonce() + 1);
            hash = deserializedGenesis.getHeaderHash().toBigInteger();
        }
        StandardPersistenceService.getInstance().persist(deserializedGenesis, 0);
        s_logger.debug(String.format("Block solved! hash is lower than target difficulty (%d): %s > %s", deserializedGenesis.getHeader().getNonce(), genesisBlock.getHeaderHash(), Convert.toHexString(block2.getTargetDifficultyAsInteger().toByteArray())));
        */
    }

    /**
     * Initializes the persistence manager.
     *
     * @throws StorageException If there is any error opening the storage.
     */
    static void initializeServices() throws StorageException
    {
        DiskContiguousStorage   blockStorage     = new DiskContiguousStorage(BLOCKS_PATH, BLOCK_PATTERN);
        DiskContiguousStorage   revertsStorage   = new DiskContiguousStorage(REVERT_PATH, REVERT_PATTERN);
        LevelDbMetadataProvider metadataProvider = new LevelDbMetadataProvider(METADATA_PATH);

        ServiceLocator.register(
                IPersistenceService.class,
                new StandardPersistenceService(blockStorage, revertsStorage, metadataProvider));

        ServiceLocator.register(
                ITransactionsPoolService.class,
                new MemoryTransactionsPoolService());
    }
}
