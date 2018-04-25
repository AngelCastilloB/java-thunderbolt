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
import com.thunderbolt.security.EncryptedPrivateKey;
import com.thunderbolt.security.Sha256Digester;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;

import java.io.*;
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
    public static void main(String[] args) throws IOException
    {
        byte[]               content      = new byte[] { 0x01, 0x02 };
        byte[]               contentsHash = Sha256Digester.doubleDigest(content);

        File file = new File("/tmp/key.bin");
        FileInputStream fis = new FileInputStream(file);
        byte[] keyData = new byte[(int) file.length()];
        fis.read(keyData);
        fis.close();

        File file2 = new File("/tmp/signature.bin");
        FileInputStream fis2 = new FileInputStream(file);
        byte[] signatureData = new byte[(int) file2.length()];
        fis2.read(signatureData);
        fis2.close();

        BigInteger[] signature = decodeFromDer(signatureData);

        EncryptedPrivateKey encryptedPrivateKey = new EncryptedPrivateKey(keyData);
        EllipticCurveKeyPair ellipticCurveKeyPair = new EllipticCurveKeyPair(encryptedPrivateKey.getPrivateKey("TEST"));
        boolean signatureIsValid = EllipticCurveProvider.verify(contentsHash, signature, ellipticCurveKeyPair.getPublicKey());

        System.out.println(String.format("Signature valid: %b", signatureIsValid));
    }


    /**
     * Creates a digital signature from the DER-encoded values
     *
     * @param       encodedStream       DER-encoded value
     */
    public static BigInteger[] decodeFromDer(byte[] encodedStream) {

        BigInteger[] signature = new BigInteger[2];
        try {
            try (ASN1InputStream decoder = new ASN1InputStream(encodedStream)) {
                DLSequence seq = (DLSequence)decoder.readObject();
                signature[0] = ((ASN1Integer)seq.getObjectAt(0)).getPositiveValue();
                signature[1] = ((ASN1Integer)seq.getObjectAt(1)).getPositiveValue();
            }
        } catch (ClassCastException | IOException exc) {
            throw new RuntimeException("Unable to decode signature", exc);
        }

        return signature;
    }


    /**
     * Encodes R and S as a DER-encoded byte stream
     *
     * @return DER-encoded byte stream
     */
    public static byte[] encodeToDER(BigInteger r, BigInteger s) {
        byte[] encodedBytes = null;
        try {
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream(80)) {
                DERSequenceGenerator seq = new DERSequenceGenerator(outStream);
                seq.addObject(new ASN1Integer(r));
                seq.addObject(new ASN1Integer(s));
                seq.close();
                encodedBytes = outStream.toByteArray();
            }
        } catch (IOException exc) {
            throw new IllegalStateException("Unexpected IOException", exc);
        }
        return encodedBytes;
    }
}
