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

import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.common.Convert;
import com.thunderbolt.mining.MiningException;
import com.thunderbolt.persistence.storage.*;
import com.thunderbolt.security.Sha256Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

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
    static private final Path   WALLET_PATH      = Paths.get(USER_HOME_PATH.toString(), "wallet.bin");
    static private final Path   WALLET_PATH_1    = Paths.get(USER_HOME_PATH.toString(), "wallet1.bin");
    static private final Path   WALLET_PATH_2    = Paths.get(USER_HOME_PATH.toString(), "wallet2.bin");

    static private final String BLOCK_PATTERN    = "block%05d.bin";
    static private final String REVERT_PATTERN   = "revert%05d.bin";

    private static final Logger s_logger = LoggerFactory.getLogger(Main.class);

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws IOException, GeneralSecurityException, StorageException, MiningException, InterruptedException {/*
        IPersistenceService      persistenceService    = createPersistenceService();
        ITransactionsPoolService memPool               = new MemoryTransactionsPoolService();
        ITransactionValidator    transactionValidator  = new StandardTransactionValidator(persistenceService, NetworkParameters.mainNet());
        IBlockchainCommitter     committer             = new StandardBlockchainCommitter(persistenceService, memPool);
        Blockchain               blockchain            = new Blockchain(NetworkParameters.mainNet(), transactionValidator, committer, persistenceService);

        Wallet wallet = new Wallet(WALLET_PATH.toString(), "1234");
        wallet.initialize(persistenceService);
        s_logger.debug(wallet.getBalance().toString());
        s_logger.debug(wallet.getAddress().toString());

        Wallet wallet1 = new Wallet(WALLET_PATH_1.toString(), "1234");
        wallet1.initialize(persistenceService);
        s_logger.debug(wallet1.getBalance().toString());
        s_logger.debug(wallet1.getAddress().toString());

        Wallet wallet2 = new Wallet(WALLET_PATH_2.toString(), "1234");
        wallet2.initialize(persistenceService);
        s_logger.debug(wallet2.getBalance().toString());
        s_logger.debug(wallet2.getAddress().toString());

        Transaction newTransaction = wallet.createTransaction(BigInteger.valueOf(250L), wallet1.getAddress());
        memPool.addTransaction(newTransaction);

        blockchain.addOutputsUpdateListener(wallet);
        blockchain.addOutputsUpdateListener(wallet1);
        blockchain.addOutputsUpdateListener(wallet2);

        StandardMiner miner = new StandardMiner(memPool, blockchain, wallet);
        Block newBlock = miner.mine();
        blockchain.add(newBlock);

        s_logger.debug(wallet.getBalance().toString());
        s_logger.debug(wallet1.getBalance().toString());
        s_logger.debug(wallet2.getBalance().toString());

        System.out.println(Convert.toHexString(newBlock.serialize()));
        System.out.println(newBlock.getHeader().getNonce());*/
        //CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("COM5");
        //CommPort commPort = portIdentifier.open("Main", 2000);
        // SerialPort serialPort = (SerialPort) commPort;
        //serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

        // This show how Icarus use the block and midstate data
        // This will produce nonce 063c5e01 -> debug by using a bogus URL

        //  Operation:
        // No detection implement.
        //    Input: 64B = 32B midstate + 20B fill bytes + last 12 bytes of block head.
        //    Return: send back 32bits immediately when Icarus found a valid nonce.
        //            no query protocol implemented here, if no data send back in ~11.3
        //            seconds (full cover time on 32bit nonce range by 380MH/s speed)
        //            just send another work.
        //  Notice:
        //    1. Icarus will start calculate when you push a work to them, even they
        //       are busy.
        //    2. The 2 FPGAs on Icarus will distribute the job, one will calculate the
        //       0 ~ 7FFFFFFF, another one will cover the 80000000 ~ FFFFFFFF.
        //    3. It's possible for 2 FPGAs both find valid nonce in the meantime, the 2
        //       valid nonce will all be send back.
        //   4. Icarus will stop work when: a valid nonce has been found or 32 bits
        //       nonce range is completely calculated.

        // Marker byte to point where to padding start (80)
        // Pad n bytes with (0x00)
        // 64 bit integer to specify padding.
        String block = "0000000120c8222d0497a7ab44a1a2c7bf39de941c9970b1dc7cdc400000079700000000e88aabe1f353238c668d8a4df9318e614c10c474f8cdf8bc5f6397b946c33d7c4e7242c31a098ea500000000800000000000000000000000000000000000000000000000000000000000000000000000000000000080020000";
        String midstate = "33c5bf5751ec7f7e056443b5aee3800331432c83f404d9de38b94ecbf907b92d";

        byte[] data2 = Convert.hexStringToByteArray(block);
        ByteBuffer buffer = ByteBuffer.wrap(data2);

        BlockHeader blockn = new BlockHeader(buffer);

        // Revers every 4 bytes;
        byte[] firstSegment = new byte[64];
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 4; ++j) {
                firstSegment[i * 4 + j] = data2[i * 4 + (3 - j)];
            }
        }

        System.out.println(Convert.toHexString(Sha256Digester.getMidstate(firstSegment)));
    }/*

        //System.out.println(Sha256Digester.digest(blockn.serialize()));
        for (int  i = 0; i < 16; ++i)
        {
            byte[] s = new byte[4];
            midStateBuffer.get(s);
            System.out.print(Sha256Digester.digest(s).toString());
        }

        //System.out.println(Convert.toHexString(blockn.serialize()));
    }
        /*
        byte[] rdata2  = block.decode('hex')[95:63:-1]
        byte[] rmid    = midstate.decode('hex')[::-1]
        byte[] payload = rmid + rdata2

        print("Push payload1 to icarus: " + binascii.hexlify(payload))
        ser.write(payload)

        b=ser.read(4)
        print("Result:(should be: 063c5e01): " + binascii.hexlify(b))
    }

    /**
     * Creates the persistence service.
     *
     * @return The newly created persistence service.
     *//*
    private static IPersistenceService createPersistenceService() throws StorageException
    {
        IContiguousStorage blockStorage     = new DiskContiguousStorage(BLOCKS_PATH, BLOCK_PATTERN);
        IContiguousStorage revertsStorage   = new DiskContiguousStorage(REVERT_PATH, REVERT_PATTERN);
        IMetadataProvider  metadataProvider = new LevelDbMetadataProvider(METADATA_PATH);

        return new StandardPersistenceService(blockStorage, revertsStorage, metadataProvider);
    }*/
}
