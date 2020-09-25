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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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

    static int bytereverse(int x)
    {
        return (((x) << 24) | (((x) << 8) & 0x00ff0000) | (((x) >> 8) & 0x0000ff00) | ((x) >> 24));
    }

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
        String blocks = "00000000000000620D492F559522E89B1CFE667646881392184916CD10DA7056D2138739D68E8728F0DD3CF11A7F3DCCAF46610979FA11CD0CA3B0E943ABAE7629454CAA5F6CC4811DFFFFF8017CF16E0000000200000000000000017FFFFFFF000000000000000000000000000000000000000000000000000000000000000000000008000000000000000300000001000000012A05F2000000000014A42FF651E4CFEDDCABCC1AFD47048547AAA64A7A0000000000000000000000000000000100000000ED5E946C60CB1A7F58B8DB61ECA86431B72C7603BE9D22114D22BC854098435D000000690340ADFAD3067489DD020A5DC39041D12BB243FF59FECBB6123FE0F3D9D0FB53A8473045022100CA1C7E06777957167207022F6578CF882FF415E32535A92E03D6ABA409E0B13E0220091D0BDA12A4B840B35A4163D9A25410D62DCF69C06092C6C9C6632F9CD1592B0000000200000000000000FA0000000014BC2FAE1186356DE357036FDC453D76347F4C9152000000012A05F1060000000014A42FF651E4CFEDDCABCC1AFD47048547AAA64A7A0000000000000000";

        ByteBuffer buffer = ByteBuffer.wrap(Convert.hexStringToByteArray(blocks));
        Block blockn = new Block(buffer);
        Block newBlock = new Block();

        newBlock.getHeader().setTimeStamp((int) OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond());
        newBlock.getHeader().setTargetDifficulty(blockn.getTargetDifficulty());
        newBlock.getHeader().setParentBlockHash(blockn.getHeaderHash());

        Sha256Digester digester = new Sha256Digester();
        digester.hash(newBlock.getHeader().serialize());

        System.out.println(Convert.toHexString(digester.getMidstate(0)));
        System.out.println(Convert.toHexString(digester.getBlock(1)));

        newBlock.getHeader().setNonce(26125374);
        System.out.println(newBlock.getHeaderHash());
    }/*
DE-08-00-57-00-7E-98-2F-8A-42-91-44-37-71-CF-FB-C0-B5-A5-DB-B5-E9-5B-C2-56-39-F1-11-F1-59-A4-82-3F-92-D5-5E-1C-AB-98-AA-07-D8-01-5B-83-12-BE-85-31-24-C3-7D-0C-55-74-5D-BE-72-FE-B1-DE-80-A7-06-DC-9B-74-F1-9B-C1-C1-69-9B-E4-86-47-BE-EF-C6-9D
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
//DB-50-89-37-4C-65-58-6D-06-46-7F-76-34-04-F3-92-4A-D9-42-83-1D-BD-B2-09-D4-3E-F3-84-CD-D2-51-2D-FA-5C-51-77-37-86-62-76-E7-B4-68-89-CD-23-A9-11-B5-9A-7E-B9-DD-E9-D6-A2-AB-6C-D5-E0-7F-20-6C-A7-13-B6-E6-02-26-24-2D-C0-03-38-6E-0B-E0-53-8E-01