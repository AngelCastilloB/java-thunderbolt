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

package com.thunderbolt.commands;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.contracts.ICommand;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.rpc.RpcClient;

/* IMPLEMENTATION ************************************************************/

/**
 * Gets the unspent output with the given hash.
 */
public class GetUnspentOutputCommand implements ICommand
{
    private RpcClient s_client = null;

    /**
     * Initializes an instance of the GetUnspentOutputCommand Command class.
     */
    public GetUnspentOutputCommand(RpcClient client)
    {
        s_client = client;
    }

    /**
     * Executes the given command.
     *
     * @return true if the command could be executed; otherwise; false.
     */
    @Override
    public boolean execute(String[] args)
    {
        if (args.length != 3)
            return false;

        UnspentTransactionOutput result = s_client.getUnspentOutput(args[1], Integer.parseInt(args[2]));
        System.out.printf("%s%n", result);
        return true;
    }

    /**
     * Gets the name of the command.
     *
     * @return the name of the command.
     */
    @Override
    public String getName()
    {
        return "getUnspentOutput";
    }

    /**
     * Gets the description of the command.
     *
     * @return the description of the command.
     */
    @Override
    public String getDescription()
    {
        return "  Gets the unspent output that matches the given transaction id and index inside that transaction.\n" +
               "  ARGUMENTS: <TRANSACTION_ID> <INDEX>";
    }
}
