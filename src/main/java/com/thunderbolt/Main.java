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

import com.thunderbolt.security.*;

import java.io.*;
import java.security.GeneralSecurityException;

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
    public static void main(String[] args) throws IOException, GeneralSecurityException
    {
        byte[] content      = new byte[] { 0x01, 0x02 };
        byte[] contentsHash = Sha256Digester.doubleDigest(content);

        /*
        EllipticCurveKeyPair pair       = new EllipticCurveKeyPair();
        EncryptedPrivateKey  privateKey = new EncryptedPrivateKey(pair.getPrivateKey(), "AAAA");

        byte[] signature = EllipticCurveProvider.sign(contentsHash, pair.getPrivateKey());


        EncryptedPrivateKey  deserializedKey      = new EncryptedPrivateKey(privateKey);
        EllipticCurveKeyPair ellipticCurveKeyPair = new EllipticCurveKeyPair(deserializedKey.getPrivateKey("AAAA"));

        boolean signatureIsValid = EllipticCurveProvider.verify(contentsHash, signature, ellipticCurveKeyPair.getPublicKey());

        System.out.println(String.format("Signature valid: %b", signatureIsValid));*/
    }

    /**
     * Reads a file for the disk.
     *
     * @param path The file.
     *
     * @return The data of the file.
     *
     * @throws IOException Thrown if the file is not found.
     */
    static byte[] readFile(String path) throws IOException
    {
        File            file       = new File(path);
        FileInputStream fileStream = new FileInputStream(file);
        byte[]          data       = new byte[(int) file.length()];

        fileStream.read(data);
        fileStream.close();

        return data;
    }

    /**
     * Writes a writes to the disk.
     *
     * @param path The file.
     * @param data The data to be saved.
     *
     * @throws IOException Thrown if the file is not found.
     */
    static void writeFile(String path, byte[] data) throws IOException
    {
        File             file       = new File(path);
        FileOutputStream fileStream = new FileOutputStream(file);

        fileStream.write(data);
        fileStream.close();
    }
}
