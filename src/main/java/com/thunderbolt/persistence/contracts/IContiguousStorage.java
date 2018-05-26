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
package com.thunderbolt.persistence.contracts;

/* IMPORTS *******************************************************************/

import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.storage.StoragePointer;

import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Stores data sequentially in a set of solid uninterrupted segments. The data is written to the segments contiguously;
 * however; the segments are not necessarily contiguous (I.E located in the same file, or even the same disk).
 *
 * The strategy of arranging said segments will depend on each concrete implementation of this interface. The consumer
 * of the interface however should treat the storage as a single contiguous block of data.
 *
 * After a data entry is added to the storage an StoragePointer pointer is returned, using this storage pointer the data
 * can later be retrieved.
 */
public interface IContiguousStorage
{
    /**
     * Stores the given byte array in the contiguous storage.
     *
     * @param buffer The data to be stored.
     *
     * @return An storage pointer. This pointer will be needed later to retrieve the data.
     */
    StoragePointer store(byte[] buffer) throws StorageException;

    /**
     * Stores the given byte buffer in the contiguous storage.
     *
     * @param buffer The data to be stored.
     *
     * @return An storage pointer. This pointer will be needed later to retrieve the data.
     */
    StoragePointer store(ByteBuffer buffer) throws StorageException;

    /**
     * Retrieves the data located at the given storage pointer.
     *
     * @param pointer The pointer marking the start of the data entry.
     *
     * @return The data.
     */
    byte[] retrieve(StoragePointer pointer) throws StorageException;
}
