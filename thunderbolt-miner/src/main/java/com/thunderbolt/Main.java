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

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arteam.simplejsonrpc.client.JsonRpcClient;
import com.github.arteam.simplejsonrpc.client.Transport;
import com.thunderbolt.common.Convert;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.mining.Job;
import com.thunderbolt.mining.miners.CpuMiner;
import com.thunderbolt.rpc.MinerWork;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/* IMPLEMENTATION ************************************************************/


/**
 * Application main class.
 */
public class Main
{
    // Static variables
    private static final Logger s_logger = LoggerFactory.getLogger(Main.class);
    private static final Object s_lock  = new Object();

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws InterruptedException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonRpcClient client = new JsonRpcClient(new Transport() {

            CloseableHttpClient httpClient = HttpClients.createDefault();

            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException {
                // Apache HttpClient 4.3.1 is used as an example

                String auth = "user:pass";
                byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
                String authHeader = "Basic " + new String(encodedAuth);


                HttpPost post = new HttpPost("http://localhost:3685");
                post.setEntity(new StringEntity(request, Charsets.UTF_8));
                post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                post.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
                try (CloseableHttpResponse httpResponse = httpClient.execute(post)) {
                    return EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
                }
            }
        }, objectMapper);

        //boolean result = client.createRequest().method("create")
        MinerWork work = client.createRequest()
                .method("getWork")
                .id(1)
                .returnAs(MinerWork.class)
                .execute();

        byte[] midstate = Convert.hexStringToByteArray("05D387352B75D4529F235910CCDDAEB836B7C9629AB6DFAF4249F9CAB90B4481");
        byte[] data     = Convert.hexStringToByteArray("1B4FBE471EB4E55AFFFF001D7891EA91800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000280");

        CpuMiner miner = new CpuMiner();

        miner.addJobFinishListener(ended ->
        {
            s_logger.debug("Job {} ended.", ended.getId());
            s_logger.debug("Block {}.", ended.isSolved() ? "Solved" : "Not Solved");

            // Nonce must be provided to the application as big endian. Since it will be reverted before hashing
            // so we need to make sure that when it is reverted the hash matches.
            s_logger.debug("Nonce: {} ({})",
                    Convert.toHexString(NumberSerializer.serialize(Integer.reverseBytes((int)ended.getNonce()))),
                    Integer.reverseBytes((int)ended.getNonce()));
            s_logger.debug("Hash:  {}",
                    Convert.toHexString(ended.getHash().getData()));

            if (ended.isSolved())
                miner.cancelAllJobs();
        });

        miner.start();

        Job job  = new Job(midstate, data,(short)0x75);
        miner.queueJob(job);

        while(true)
        {
            Thread.sleep(100);
        }
    }
}