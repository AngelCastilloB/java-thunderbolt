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

import com.thunderbolt.contracts.ICommand;
import com.thunderbolt.rpc.RpcClient;

/* IMPLEMENTATION ************************************************************/

/**
 * Unlock wallet method.
 */
public class UnlockWalletCommand implements ICommand
{
    private RpcClient s_client = null;

    /**
     * Initializes an instance of the UnlockWalletCommand class.
     */
    public UnlockWalletCommand(RpcClient client)
    {
        s_client = client;
    }

    /**
     * Unlock wallet method.
     *
     * @return true if the wallet was unlocked; otherwise; false.
     */
    @Override
    public boolean execute(String[] args)
    {
        if (args.length != 2)
            return false;

        boolean result = s_client.unlockWallet(args[1]);

        if (result)
        {
            System.out.println("Wallet unlocked.");
        }
        else
        {
            System.out.println("Wallet could not be unlocked. Please refer to the node logs for more information.");
        }

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
        return "unlockWallet";
    }

    /**
     * Gets the description of the command.
     *
     * @return the description of the command.
     */
    @Override
    public String getDescription()
    {
        return "  Unlocks the wallet if it is encrypted.\n" +
               "  ARGUMENTS: <PASSPHRASE>";
    }
}
