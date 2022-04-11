<p align="center">
  <img align="middle" src=
  "https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/thunderbolt_logo.png"
  height="250" /></br>
  <sup><sup><sup><sup>The Thunderbolt logo is licensed under
  <a href="https://creativecommons.org/licenses/by/3.0/">Creative
  Commons 3.0 Attributions license</a></sup></sup></sup></sup>
</p>
 
![Build Status](https://travis-ci.org/AngelCastilloB/java-thunderbolt.svg?branch=master) [![Build status](https://ci.appveyor.com/api/projects/status/k5qa96tmn861qffu?svg=true)](https://ci.appveyor.com/project/AngelCastilloB/java-thunderbolt)
 ![license](https://img.shields.io/badge/license-MIT-blue.svg?longCache=true&style=flat) 
 [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?longCache=true&style=flat)](http://makeapullrequest.com)
 
Thunderbolt is a peer-to-peer trustless digital currency implemented from scratch in Java. The project is inspired by
[Bitcoin](https://github.com/bitcoin/bitcoin) and was created as a learning tool for understanding the blockchain technology.

NOTE: Currently the cryptocurrency network is down.

## Specifications
Specification | Value
--- | ---
Protocol | PoW (proof of Work)
Algorithms | SHA-256
Blocktime | 10 minutes
Total Supply | 21.000.000 THB
RPC port | 9567
P2P port | 8332

## Download

Download the Thunderbolt Node, CLI, Wallet and Miner for your target OS from:

<a id="raw-url" href="https://github.com/AngelCastilloB/java-thunderbolt/releases/download/1.0-SNAPSHOT/thunderbolt_linux.tar.gz"> - Download Linux v1.0.SNAPSHOP</a>

<a id="raw-url" href="https://github.com/AngelCastilloB/java-thunderbolt/releases/download/1.0-SNAPSHOT/thunderbolt_windows.zip"> - Download Windows v1.0.SNAPSHOP</a>

The release includes 4 applications.

- The Node Application: This application runs the P2P protocol and all the blockchain related business logic (consensus rules, persitense of block and transactions etc.). The node application also serves as an JSON-RPC service (at port 9567 by default), other application can send request to the RPC service to query data from the blockchain or make the node execute commands like sending blocks, making transactions and in general managing the peers we are connected to. This node application must be started first, as the wallet, cli and miner application all rely on the node RPC server to work.

- The CLI: The command line interface application allows the user to query and execute commands on the node using the command line.

- The Wallet: The wallet allows you to keep track of your balance and transactions. You can also transfer funds to other addresses, dump your keys and encrypt your wallet.

- The Miner: The miner application will mine blocks using the CPU or a specific ASIC miner (The GekkoScience NewPac 130Gh/s+ because is what I had at hand).

## Node

The node can be stearted as a background process using the "start-node.bat/sh" script or as a simple program using the "run-node.bat/sh":

<p align="left"><img src="https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/start_node.png" alt="Start The Node"></p>

The node application will add a system tray icon on operating systems that support it.

You can stop the node either by clocking on the stop option of the tray icon:

<p align="left"><img src="https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/stop_node.png" alt="Stop"></p>

or by simply calling the stop command on the CLI:



    thunderbolt-cli.sh stop
    
    

In Windows you can also just click the script icon "stop-all.bat" and it will close the node application and the wallet software if open.

## CLI Commands

The CLI application has the following command available:



    USAGE: thunderbolt-cli.sh <command> [params]
    
    

Command | Description | Arguments
---  | --- | ---
getUptime|Returns the total uptime of the server.|NONE
getConfirmedTransactions|Gets all the confirmed transactions related to the node wallet.|NONE
banPeer|Bans a peer for 24 hours.|NETWORK_ADDRESS
getBestBlockHash|Returns the hash of the best (tip) block in the longest blockchain.|NONE
getPeerInfo|Gets all the information regarding a peer.|NETWORK_ADDRESS
backupWallet|Backups the wallet.|BACKUP_WALLET_PATH
getAddress|Gets the address of the wallet.|NONE
getBlockCount|Returns the number of blocks in the longest blockchain.|NONE
isWalletUnlocked|Gets whether the wallet is unlocked or not.|NONE
getPendingTransactions|Gets all the pending transactions related to the node wallet.|NONE
getUnspentOutput|Gets the unspent output that matches the given transaction id and index inside that transaction.|TRANSACTION_ID INDEX
getTransaction|Gets the transaction with the given hash.|TRANSACTION_HASH
sendToAddress|Transfer funds from the current node wallet to the specified address.|RECIPIENT ADDRESS AMOUNT
getPeerCount|The number of peers connected to this node.|NONE
getBlock|Gets the block with the given hash.|BLOCK_HASH
getTransactionPoolCount|Gets the number of transactions currently sitting in the transaction pool.|NONE
encryptWallet|Encrypts the wallet with 'passphrase'. This is for first time encryption.|PASSPHRASE
getInfo|Returns an object containing various state info.|NONE
getPendingBalance|Gets the specified address pending balance. If no address is specified. The pending balance of the current active wallet is returned.|ADDRESS optional
isWalletEncrypted|Gets whether the wallet is encrypted or not.|NONE
addPeer|Adds a new address to the pool.|NETWORK_ADDRESS
getPublicKey|Gets the public key of the wallet.|NONE
getTransactionPoolSize|Gets the size of the transaction pool in bytes.|NONE
GetTransactionMetadataCommand|Gets the metadata for this transaction.|TRANSACTION_HASH
listBannedPeers|Gets all banned peers.|NONE
lockWallet|Locks the wallet if it is encrypted.|NONE
disconnectPeer|Disconnects a currently connect peer from the node.|NETWORK_ADDRESS
getBalance|Gets the specified address balance. If no address is specified. The balance of the current active wallet is returned.|ADDRESS optional
stop|Stops the server.|NONE
unlockWallet|Unlocks the wallet if it is encrypted.|PASSPHRASE
getBlockHeader|Gets the block header of the block with the given hash.|BLOCK_HASH
unbanPeer|Lift a ban from a peer.|NETWORK_ADDRESS
listPeers|Lists all connected peers.|NONE
getPrivateKey|Gets the private key of the wallet.|NONE
getDifficulty|Returns the proof-of-work difficulty as a multiple of the minimum difficulty.|NONE
getTotalBalance|Gets the amount of coins in circulation (spendable).|NONE
removePeer|Removes an address from the storage.|NETWORK_ADDRESS
getNetworkAddress|Gets our current public address.|NONE

## Wallet

The wallet software allows you to keep track of transactions and transfer funds among other things:

<p align="center"><img src="https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/overview.png" alt="Overview"></p>
<p align="center"><img src="https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/send.png" alt="Send Coins"></p>
<p align="center"><img src="https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/confirm_send.png" alt="Confirm"></p>
<p align="center"><img src="https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/rec.png" alt="Receive"></p>
<p align="center"><img src="https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/transaction_list.png" alt="Transaction list"></p>
<p align="center"><img src="https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/dump_keys.png" alt="Dump Keys"></p>
<p align="center"><img src="https://github.com/AngelCastilloB/java-thunderbolt/blob/master/assets/encrypt_keys.png" alt="Dump Keys"></p>

## Miner

The miner software can mine on the CPU or on a particular ASIC miner, if set to mine by using tthe CPU (default) it will start a mining thread for each core of the CPU and it will divide the nonce range accordingly.

The only ASIC miner that is currently supported is the GekkoScience NewPac 130Gh/s+ since it was what I had in hand.

To run the miner:

    run-miner.sh
    
    
And to run the miner with the Gekko:

    run-miner.sh asic

Build
-----

The project was created with IntelliJ IDEA but any build environment with Maven and the Java compiler should work.

To build manually, create the executable with:

```sh
 mvn clean package
```
License
-------

Thunderbolt is released under the terms of the MIT license. See [LICENSE](LICENSE) for more
information or see https://opensource.org/licenses/MIT.
