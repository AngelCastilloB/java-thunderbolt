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

import com.thunderbolt.security.EllipticCurveKeyPair;
import com.thunderbolt.security.EllipticCurveProvider;
import com.thunderbolt.security.Sha256Digester;

import java.math.BigInteger;

/* IMPLEMENTATION ************************************************************/

/**
 * Application main class.
 */
public class Main
{
    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args)
    {
        byte[]               content      = new byte[] { 0x01, 0x02 };
        byte[]               contentsHash = Sha256Digester.doubleDigest(content);
        EllipticCurveKeyPair keyPair      = new EllipticCurveKeyPair();

        BigInteger[] signature        = EllipticCurveProvider.sign(contentsHash, keyPair.getPrivateKey());
        boolean      signatureIsValid = EllipticCurveProvider.verify(contentsHash, signature, keyPair.getPublicKey());

        System.out.println(String.format("Signature valid: %b", signatureIsValid));
    }
}
