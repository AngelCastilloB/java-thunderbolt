/*
 * MIT License
 *
 * Copyright (c) 2020 Angel Castillo.
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

package com.thunderbolt.rpc;

/* IMPORTS *******************************************************************/

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.arteam.simplejsonrpc.client.JsonRpcClient;
import com.github.arteam.simplejsonrpc.client.Transport;
import com.github.arteam.simplejsonrpc.client.builder.RequestBuilder;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.thunderbolt.blockchain.Block;
import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.TransactionMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.Transaction;
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

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Remote procedure call client. Using this client we can make RPC calls to our node.
 */
public class RpcClient
{
    private JsonRpcClient m_client       = null;
    private long          m_currentNonce = 0;

    /**
     * Initializes a new instance of the RpcClient class.
     *
     * @param user The username.
     * @param password The password.
     * @param uri The URI of the RPC client.
     */
    public RpcClient(String user, String password, String uri)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        m_client = new JsonRpcClient(new Transport()
        {
            final CloseableHttpClient httpClient = HttpClients.createDefault();

            @NotNull
            @Override
            public String pass(@NotNull String request) throws IOException
            {
                String auth = String.format("%s:%s", user, password);

                byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));

                String authHeader = "Basic " + new String(encodedAuth);

                HttpPost post = new HttpPost(uri);
                post.setEntity(new StringEntity(request, Charsets.UTF_8));
                post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                post.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

                try (CloseableHttpResponse httpResponse = httpClient.execute(post))
                {
                    return EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
                }
            }
        }, objectMapper);
    }

    /**
     * Gets whether the node service is online.
     *
     * @return true if the node is online; otherwise; false.
     */
    public boolean isNodeOnline()
    {
        try
        {
            getAddress();
            return true;
        }
        catch (IllegalStateException exception)
        {
            return false;
        }
    }

    /**
     * Gets the node information.
     *
     * @return The node information.
     */
    public String getInfo()
    {
        return m_client.createRequest()
                .method("getInfo")
                .id(m_currentNonce++)
                .returnAs(String.class)
                .execute();
    }

    /**
     * Gets whether this node is still syncing.
     */
    public boolean isInitialBlockDownload()
    {
        return m_client.createRequest()
                .method("isInitialBlockDownload")
                .id(m_currentNonce++)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Returns the proof-of-work difficulty as a multiple of the minimum difficulty.
     *
     * @return The current network difficulty.
     */
    public double getDifficulty()
    {
        return m_client.createRequest()
                .method("getDifficulty")
                .id(m_currentNonce++)
                .returnAs(Double.class)
                .execute();
    }

    /**
     * Stops the node.
     */
    public void stop()
    {
        m_client.createRequest()
                .method("stop")
                .id(m_currentNonce++)
                .executeNullable();
    }

    /**
     * Gets the totla time this node has been running.
     *
     * @return The uptime.
     */
    public TimeSpan getUptime()
    {
        return m_client.createRequest()
                .method("getUptime")
                .id(m_currentNonce++)
                .returnAs(TimeSpan.class)
                .execute();
    }

    /**
     * Encrypts the wallet.
     *
     * @return Gets whether the wallet was successfully encrypted or not.
     */
    public boolean encryptWallet(String password)
    {
        return m_client.createRequest()
                .method("encryptWallet")
                .id(m_currentNonce++)
                .param("password", password)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Unlock wallet method.
     *
     * @param password The password to unlock the wallet.
     *
     * @return true if the wallet was unlocked; otherwise; false.
     */
    public boolean unlockWallet(String password)
    {
        return m_client.createRequest()
                .method("unlockWallet")
                .id(m_currentNonce++)
                .param("password", password)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Locks wallet method.
     */
    public void lockWallet()
    {
        m_client.createRequest()
                .method("lockWallet")
                .id(m_currentNonce++)
                .executeNullable();
    }

    /**
     * Gets whether the wallet is unlocked or not.
     *
     * @return true if is unlocked; otherwise; false.
     */
    public boolean isWalletUnlocked()
    {
        return m_client.createRequest()
                .method("isWalletUnlocked")
                .id(m_currentNonce++)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Gets whether the wallet is encrypted or not.
     *
     * @return true if is encrypted; otherwise; false.
     */
    public boolean isWalletEncrypted()
    {
        return m_client.createRequest()
                .method("isWalletEncrypted")
                .id(m_currentNonce++)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Gets the specified address balance. If no address is specified. The balance of the current active wallet
     * is returned.
     *
     * @param address The address to to get the balance from.
     *
     * @return The balance.
     */
    public double getBalance(String address)
    {
        if (address == null)
        {
            return m_client.createRequest()
                    .method("getBalance")
                    .id(m_currentNonce++)
                    .returnAs(Double.class)
                    .execute();
        }

        return m_client.createRequest()
                .method("getBalance")
                .id(m_currentNonce++)
                .param("address", address)
                .returnAs(Double.class)
                .execute();
    }

    /**
     * Get the pending balance for the nodes wallet.
     *
     * @param address The address to to get the balance from.
     *
     * @return The pending balance.
     */
    public double getPendingBalance(String address)
    {
        return m_client.createRequest()
                .method("getPendingBalance")
                .id(m_currentNonce++)
                .param("address", address)
                .returnAs(Double.class)
                .execute();
    }

    /**
     * Gets total coins in circulation.
     *
     * @return The total balance.
     */
    public double getTotalBalance()
    {
        return m_client.createRequest()
                .method("getTotalBalance")
                .id(m_currentNonce++)
                .returnAs(Double.class)
                .execute();
    }

    /**
     * Transfer funds from the current node wallet to the specified address.
     *
     * @param address The address to send the funds to.
     * @param amount The amount to be transferred.
     */
    public boolean sendToAddress(String address, double amount)
    {
        return m_client.createRequest()
                .method("sendToAddress")
                .id(m_currentNonce++)
                .param("address", address)
                .param("amount", amount)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Transfer funds from the current node wallet to the specified address.
     *
     * @param address The address to send the funds to.
     * @param amount The amount to be transferred.
     * @param fee The miner fee.
     */
    public boolean sendToAddress(String address, double amount, double fee)
    {
        return m_client.createRequest()
                .method("sendToAddress")
                .id(m_currentNonce++)
                .param("address", address)
                .param("amount", amount)
                .param("fee", fee)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Gets all the transactions related to the current wallet.
     *
     * @return The list of transactions for the given wallet.
     */
    public List<Transaction> getConfirmedTransactions()
    {
        return m_client.createRequest()
                .method("getConfirmedTransactions")
                .id(m_currentNonce++)
                .returnAsList(Transaction.class)
                .execute();
    }

    /**
     * Gets all the pending transactions related to the current wallet.
     *
     * @return The list of pending transactions for the given wallet.
     */
    public List<Transaction> getPendingTransactions()
    {
        return m_client.createRequest()
                .method("getPendingTransactions")
                .id(m_currentNonce++)
                .returnAsList(Transaction.class)
                .execute();
    }


    /**
     * Gets the unspent output that matches the given transaction id and index inside that transaction.
     *
     * @param transactionId The transaction ID that contains the output.
     * @param index The index inside the transaction.
     *
     * @return The transaction output, or null if the output is not available or was already spent.
     */
    public UnspentTransactionOutput getUnspentOutput(String transactionId, int index)
    {
        return m_client.createRequest()
                .method("getUnspentOutput")
                .id(m_currentNonce++)
                .param("transactionId", transactionId)
                .param("index", index)
                .returnAs(UnspentTransactionOutput.class)
                .execute();
    }

    /**
     * Gets the address of the wallet.
     *
     * @return The address.
     */
    public String getAddress()
    {
        return m_client.createRequest()
                .method("getAddress")
                .id(m_currentNonce++)
                .returnAs(String.class)
                .execute();
    }

    /**
     * Gets the public key of the wallet.
     *
     * @return the public key.
     */
    public Byte[] getPublicKey()
    {
        return m_client.createRequest()
                .method("getPublicKey")
                .id(m_currentNonce++)
                .returnAsArray(Byte.class)
                .execute();
    }

    /**
     * Gets the private key of the wallet.
     *
     * @return the private key.
     */
    public Byte[] getPrivateKey()
    {
        return m_client.createRequest()
                .method("getPrivateKey")
                .id(m_currentNonce++)
                .returnAsArray(Byte.class)
                .execute();
    }

    /**
     * Backups your wallet file.
     *
     * @return The new path for the wallet.
     */
    public boolean backupWallet(String path)
    {
        return m_client.createRequest()
                .method("backupWallet")
                .id(m_currentNonce++)
                .param("path", path)
                .returnAs(boolean.class)
                .execute();
    }

    /**
     * Gets the required information to mine.
     *
     * @return The information necessary to generate a new block.
     */
    public MinerWork getWork()
    {
        return m_client.createRequest()
                .method("getWork")
                .id(m_currentNonce++)
                .returnAs(MinerWork.class)
                .execute();
    }

    /**
     * Submits a solved block.
     *
     * @return The submitted block.
     */
    public boolean submitBlock(Block block)
    {
        if (!block.isValid())
            return false;

        return m_client.createRequest()
                .method("submitBlock")
                .id(m_currentNonce++)
                .param("block", block)
                .returnAs(boolean.class)
                .execute();
    }

    /**
     * Returns the number of blocks in the longest blockchain.
     *
     * @return The current block count.
     */
    public long getBlockCount()
    {
        return m_client.createRequest()
                .method("getBlockCount")
                .id(m_currentNonce++)
                .returnAs(Long.class)
                .execute();
    }

    /**
     * Returns the hash of the best (tip) block in the longest blockchain.c
     *
     * @return the block hash, hex-encoded
     */
    public Sha256Hash getBestBlockHash()
    {
        return m_client.createRequest()
                .method("getBestBlockHash")
                .id(m_currentNonce++)
                .returnAs(Sha256Hash.class)
                .execute();
    }

    /**
     * Gets the number of transactions currently sitting in the transaction pool.
     *
     * @return the transaction pool count.
     */
    public long getTransactionPoolCount()
    {
        return m_client.createRequest()
                .method("getTransactionPoolCount")
                .id(m_currentNonce++)
                .returnAs(Long.class)
                .execute();
    }

    /**
     * Gets the size of the transaction pool in bytes.
     *
     * @return Size of the transaction pool in bytes.
     */
    public long getTransactionPoolSize()
    {
        return m_client.createRequest()
                .method("getTransactionPoolSize")
                .id(m_currentNonce++)
                .returnAs(Long.class)
                .execute();
    }

    /**
     * Gets the time stamp on the last update of the mempool.
     *
     * @return The timestamp of the last update.
     */
    public String getMemPoolLastUpdateTime()
    {
        return m_client.createRequest()
                .method("getMemPoolLastUpdateTime")
                .id(m_currentNonce++)
                .returnAs(String.class)
                .execute();
    }

    /**
     * Gets the block header of the block with the given hash.
     *
     * @param hash The HEX encoded hash of the block header.
     *
     * @return The block header.
     */
    public BlockHeader getBlockHeader(String hash)
    {
        return m_client.createRequest()
                .method("getBlockHeader")
                .id(m_currentNonce++)
                .param("hash", hash)
                .returnAs(BlockHeader.class)
                .execute();
    }

    /**
     * Gets the block with the given hash.
     *
     * @param hash The HEX encoded block.
     *
     * @return The block.
     */
    public Block getBlock(String hash)
    {
        return m_client.createRequest()
                .method("getBlock")
                .id(m_currentNonce++)
                .param("hash", hash)
                .returnAs(Block.class)
                .execute();
    }

    /**
     * Gets the transaction with the given hash.
     *
     * @param hash The HEX encoded transaction id.
     *
     * @return The transaction.
     */
    public Transaction getTransaction(String hash)
    {
        return m_client.createRequest()
                .method("getTransaction")
                .id(m_currentNonce++)
                .param("hash", hash)
                .returnAs(Transaction.class)
                .execute();
    }

    /**
     * Gets the metadata for this transaction..
     *
     * @return the transaction metadata.
     */
    public TransactionMetadata getTransactionMetadata(String hash)
    {
        return m_client.createRequest()
                .method("getTransactionMetadata")
                .id(m_currentNonce++)
                .param("hash", hash)
                .returnAs(TransactionMetadata.class)
                .execute();
    }

    /**
     * Gets out current public address.
     *
     * @return Our public address.
     */
    public String getNetworkAddress()
    {
        return m_client.createRequest()
                .method("getNetworkAddress")
                .id(m_currentNonce++)
                .returnAs(String.class)
                .execute();
    }

    /**
     * Adds a new address to the pool. If the pool previously contained the address,
     * the old value is replaced by the specified value.
     *
     * @param url The URI of the network address.
     *
     * @return true if the address was added; otherwise; false.
     */
    public boolean addPeer(String url)
    {
        return m_client.createRequest()
                .method("addPeer")
                .id(m_currentNonce++)
                .param("url", url)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Removes an address from the storage.
     *
     * @param url The URI of the network address.
     */
    public boolean removePeer(String url)
    {
        return m_client.createRequest()
                .method("removePeer")
                .id(m_currentNonce++)
                .param("url", url)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Disconnects a currently connect peer from the node.
     *
     * @param url The URL of the peer to be disconnected.
     *
     * @return true if the peer was disconnected; otherwise; false.
     */
    public boolean disconnectPeer(String url)
    {
        return m_client.createRequest()
                .method("disconnectPeer")
                .id(m_currentNonce++)
                .param("url", url)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Bans a peer for 24 hours.
     *
     * @param url The URI of the peer to be banned.
     *
     * @return true if the peer was banned; otherwise; false.
     */
    public boolean banPeer(String url)
    {
        return m_client.createRequest()
                .method("banPeer")
                .id(m_currentNonce++)
                .param("url", url)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Lift a ban from a peer.
     *
     * @param url The URL of the peer to unban.
     *
     * @return true if the peer was unbanned; otherwise; false.
     */
    public boolean unbanPeer(String url)
    {
        return m_client.createRequest()
                .method("unbanPeer")
                .id(m_currentNonce++)
                .param("url", url)
                .returnAs(Boolean.class)
                .execute();
    }

    /**
     * Gets a list of all banned peers.
     *
     * @return The banned peers.
     */
    @JsonRpcMethod("listBannedPeers")
    public List<String> listBannedPeers()
    {
        return m_client.createRequest()
                .method("listBannedPeers")
                .id(m_currentNonce++)
                .returnAsList(String.class)
                .execute();
    }

    /**
     * Gets the amount of peers currently in the pool.
     *
     * @return The amount of peers connected to this node.
     */
    public int getPeerCount()
    {
        return m_client.createRequest()
                .method("getPeerCount")
                .id(m_currentNonce++)
                .returnAs(int.class)
                .execute();
    }

    /**
     * List all currently connected peers.
     *
     * @return The list of connected peers.
     */
    public List<String> listPeers()
    {
        return m_client.createRequest()
                .method("listPeers")
                .id(m_currentNonce++)
                .returnAsList(String.class)
                .execute();
    }

    /**
     * Gets all the information regarding a peer.
     *
     * @return The peer information.
     */
    public String getPeerInfo(String url)
    {
        return m_client.createRequest()
                .method("getPeerInfo")
                .id(m_currentNonce++)
                .param("url", url)
                .returnAs(String.class)
                .execute();
    }

    /**
     * Creates an RPC request.
     *
     * @return The request builder.
     */
    public RequestBuilder<Object> createRequest()
    {
        return m_client.createRequest();
    }
}
