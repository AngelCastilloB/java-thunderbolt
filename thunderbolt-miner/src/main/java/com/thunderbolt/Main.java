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
import com.thunderbolt.mining.NonceRange;
import com.thunderbolt.mining.contracts.IMiner;
import com.thunderbolt.mining.miners.CpuMiner;
import com.thunderbolt.rpc.MinerWork;
import com.thunderbolt.rpc.RpcClient;
import com.thunderbolt.security.Sha256Digester;
import org.apache.http.conn.HttpHostConnectException;
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
    private static final Logger s_logger        = LoggerFactory.getLogger(Main.class);
    private static long         s_currentHeight = 0;
    private static RpcClient    s_client        = null;
    private static IMiner       s_miner         = null;
    static NonceRange[]         s_noneRanges    = new NonceRange[]
    {
        new NonceRange(0x00000000L, 0x3FFFFFFF),
        new NonceRange(0x40000000L, 0x7FFFFFFE),
        new NonceRange(0x7FFFFFFFL, 0xC0000000),
        new NonceRange(0xC0000001L, 0xFFFFFFFF)
    };

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws InterruptedException, IOException
    {
        Configuration.initialize(CONFIG_FILE_PATH.toString());

        s_client = new RpcClient(Configuration.getRpcUser(), Configuration.getRpcPassword(),
                String.format("http://localhost:%s", Configuration.getRpcPort()));

        s_currentHeight = s_client.createRequest()
                .method("getBlockchainHeight")
                .id(1)
                .returnAs(Long.class)
                .execute();

        s_miner = new CpuMiner();
        s_miner.addJobFinishListener(Main::onJobFinish);
        s_miner.start();

        while (true)
        {
            List<Job> jobs = getWork();

            // If no new jobs, sleep tight.
            if (jobs.isEmpty())
            {
                Thread.sleep(1000);
                continue;
            }

            s_miner.cancelAllJobs();

            for (Job job: jobs)
                s_miner.queueJob(job);

            Thread.sleep(1000);
        }
    }

    /**
     * Gets the batch of work.
     *
     * @return The list of new jobs; of an empty list if no new jobs are needed.
     */
    private static List<Job> getWork()
    {
        List<Job> jobs = new ArrayList<>();

        long blockchainTip = s_client.createRequest()
                .method("getBlockchainHeight")
                .id(1)
                .returnAs(Long.class)
                .execute();

        // If we detect that the tip of the blockchain changed, we need to scrap the old jobs and starting working
        // on the new jobs asap.
        if (s_currentHeight == blockchainTip && s_miner.getActiveJobs() != 0)
            return jobs;

        if (s_currentHeight != blockchainTip)
        {
            s_logger.info("A new tip of the blockchain has been detected. Previous: {}, Current: {}.",
                    s_currentHeight, blockchainTip);
            s_currentHeight = blockchainTip;
        }

        s_logger.info("Rescheduling work.");

        MinerWork work = s_client.createRequest()
                .method("getWork")
                .id(1)
                .returnAs(MinerWork.class)
                .execute();

        // Create a new block with the information given by the node.
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

        for (int i = 0; i < s_noneRanges.length; ++i)
        {
            Job job  = new Job(midstate, data, block, i + 1);
            job.setNonceRange(s_noneRanges[i]);
            jobs.add(job);
        }

        return jobs;
    }

    /**
     * Event handler for when the job is finish.
     *
     * @param job The job that just finish.
     */
    static void onJobFinish(Job job)
    {
        s_logger.info("Block {}.", job.isSolved() ? "Solved" : "Not Solved");

        // Nonce must be provided to the application as big endian. Since it will be reverted before hashing
        // so we need to make sure that when it is reverted the hash matches.
        s_logger.info("Nonce: {} ({})",
                Convert.toHexString(NumberSerializer.serialize(Integer.reverseBytes(job.getNonce()))),
                Integer.reverseBytes(job.getNonce()));
        s_logger.info("Hash:  {}",
                Convert.toHexString(job.getHash().getData()));
        s_logger.info("Time Elapsed:  {} seconds", job.getElapsed().getTotalSeconds());

        if (job.isSolved())
            s_miner.cancelAllJobs();

        job.getBlock().getHeader().setNonce(job.getNonce());

        Boolean result = s_client.createRequest()
                .method("submitBlock")
                .id(1)
                .param("block", job.getBlock())
                .returnAs(Boolean.class)
                .execute();

        s_logger.info("Block Accepted: {}", result);
    }
}