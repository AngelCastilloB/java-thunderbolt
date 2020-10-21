/*
 * Copyright (c) 2020 Angel Castillo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thunderbolt;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.common.Convert;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.mining.Job;
import com.thunderbolt.mining.miners.CpuMiner;
import com.thunderbolt.rpc.MinerWork;
import com.thunderbolt.rpc.RpcClient;
import com.thunderbolt.security.Sha256Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    static private final Path   CONFIG_FILE_PATH = Paths.get(DEFAULT_PATH.toString(), "thunderbolt.conf");

    // Static variables
    private static final Logger s_logger = LoggerFactory.getLogger(Main.class);

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws InterruptedException, IOException
    {
        Configuration.initialize(CONFIG_FILE_PATH.toString());

        RpcClient client = new RpcClient(Configuration.getRpcUser(), Configuration.getRpcPassword(),
                String.format("http://localhost:%s", Configuration.getRpcPort()));

        CpuMiner miner = new CpuMiner();

        miner.addJobFinishListener(ended ->
        {
            s_logger.info("Block {}.", ended.isSolved() ? "Solved" : "Not Solved");

            // Nonce must be provided to the application as big endian. Since it will be reverted before hashing
            // so we need to make sure that when it is reverted the hash matches.
            s_logger.info("Nonce: {} ({})",
                    Convert.toHexString(NumberSerializer.serialize(Integer.reverseBytes((int)ended.getNonce()))),
                    Integer.reverseBytes((int)ended.getNonce()));
            s_logger.info("Hash:  {}",
                    Convert.toHexString(ended.getHash().getData()));
            s_logger.info("Time Elapsed:  {} seconds", ended.getElapsed().getTotalSeconds());

            if (ended.isSolved())
                miner.cancelAllJobs();

         /*   block.getHeader().setNonce(ended.getNonce());
            Boolean result = client.createRequest()
                    .method("submitBlock")
                    .id(1)
                    .param("block", block)
                    .returnAs(Boolean.class)
                    .execute();*/

          //  s_logger.info("Block Accepted: {}", result);
        });

        miner.start();

        List<Job> jobs = new ArrayList<>();

        while(true)
        {
            MinerWork work = client.createRequest()
                    .method("getWork")
                    .id(1)
                    .returnAs(MinerWork.class)
                    .execute();

            int blockchainHeight = 0;

            // If the tip hasn't change, we keep working.
            while (blockchainHeight == work.getHeight())
                Thread.sleep(100);

            Block block = new Block();
            block.addTransaction(work.getCoinbaseTransaction());
            block.addTransactions(work.getTransactions());
            BlockHeader header = block.getHeader();
            header.setParentBlockHash(work.getParentBlock());
            header.setTimeStamp(work.getTimeStamp());
            header.setTargetDifficulty(work.getDifficulty());

            Sha256Digester digester = new Sha256Digester();
            digester.hash(Convert.reverseEndian(header.serialize()));

            byte[] midstate = digester.getMidstate(0);
            byte[] data     = digester.getBlock(1);

            Job job  = new Job(midstate, data, (short) 1);
            job.setNonce(0);
            miner.queueJob(job);

            Job job2  = new Job(midstate, data, (short) 2);
            job.setNonce(1073741823);
            miner.queueJob(job2);

            Job job3 = new Job(midstate, data, (short) 3);
            job.setNonce(1073741823 * 2);
            miner.queueJob(job3);

            Job job4  = new Job(midstate, data, (short) 4);
            job.setNonce(1073741823 * 3);
            miner.queueJob(job4);

            Thread.sleep(100);
        }
    }
}