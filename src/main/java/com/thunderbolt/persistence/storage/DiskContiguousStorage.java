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
package com.thunderbolt.persistence.storage;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.NumberSerializer;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

/* IMPLEMENTATION ************************************************************/

/**
 * Stores data sequentially in a set of 128 megabyte files. This class is not thread safe, moreover
 * this class assumes that no other process is accessing the data.
 *
 * The DiskContiguousStorage also makes use of a leveldb database to store relevant metadata about the
 * segment files.
 */
public class DiskContiguousStorage implements IContiguousStorage
{
    private static final Logger s_logger = LoggerFactory.getLogger(DiskContiguousStorage.class);

    // Constants
    static private final long   FILE_SIZE          = 1024 * 1000 * 128 ; // 128 MB
    static private final int    FILE_MAGIC         = 0xAAAAAAAA; // This magic needs to be the network magic.
    static private final String METADATA_FILE_NAME = "metadata";
    static private final String LAST_FILE_PREFIX   = "l";

    // Instance Fields
    private String m_storagePath;
    private String m_segmentNamePattern;
    private DB     m_database;

    /**
     * Initializes a new instance of the DiskContiguousStorage class.
     * @param storagePath        The folder where the files will be stored.
     * @param segmentNamePattern The pattern used to create the files.
     */
    public DiskContiguousStorage(Path storagePath, String segmentNamePattern) throws StorageException
    {
        m_storagePath        = storagePath.toString();
        m_segmentNamePattern = segmentNamePattern;

        if (!storagePath.toFile().exists())
        {
            s_logger.debug(String.format("Storage folder '%s' does not exist. Creating...", storagePath.toString()));
            boolean created = storagePath.toFile().mkdirs();

            if (!created)
                throw new StorageException("Unable to create the storage directories.");
        }

        Options options = new Options();
        options.createIfMissing(true);

        try
        {
            m_database = factory.open(Paths.get(m_storagePath, METADATA_FILE_NAME).toFile(), options);
        }
        catch (Exception exception)
        {
            throw new StorageException("Unable to open the metadata database.", exception);
        }
    }

    /**
     * Stores the given byte array in the contiguous storage.
     *
     * @param buffer The data to be stored.
     *
     * @return An storage pointer. This pointer will be needed later to retrieve the data.
     */
    @Override
    public StoragePointer store(byte[] buffer) throws StorageException
    {
        StoragePointer pointer = new StoragePointer();

        pointer.segment = getCurrentUsedFile();
        String filename = String.format(m_segmentNamePattern, pointer.segment);
        File file = Paths.get(m_storagePath, filename).toFile();

        try
        {
            if (file.exists())
            {
                long size = file.length();

                pointer.offset = size;

                // If the is file bigger than we expect; create a new file.
                if (size > FILE_SIZE)
                {
                    s_logger.debug(String.format("File %s is already full, creating new file...", filename));

                    pointer.segment = getNextBlocksFileName();
                    pointer.offset = 0;

                    filename = String.format(m_segmentNamePattern, pointer.segment);
                    file     = Paths.get(m_storagePath, filename).toFile();
                }

                FileOutputStream fileStream = new FileOutputStream(new File(file.toString()), true);

                fileStream.write(NumberSerializer.serialize(FILE_MAGIC));
                fileStream.write(NumberSerializer.serialize(buffer.length));
                fileStream.write(buffer);

                fileStream.close();
            }
            else
            {
                FileOutputStream fileStream = new FileOutputStream(new File(file.toString()), true);

                fileStream.write(NumberSerializer.serialize(FILE_MAGIC));
                fileStream.write(NumberSerializer.serialize(buffer.length));
                fileStream.write(buffer);

                fileStream.close();
            }
        }
        catch (Exception exception)
        {
            // Rethrow as StorageException to comply with the interface.
            throw new StorageException("Unable to add the given data to the storage.", exception);
        }

        return pointer;
    }

    /**
     * Stores the given byte buffer in the contiguous storage.
     *
     * @param buffer The data to be stored.
     *
     * @return An StoragePointer pointer. This pointer will be needed later to retrieve the data.
     */
    @Override
    public StoragePointer store(ByteBuffer buffer) throws StorageException
    {
        return store(buffer.array());
    }

    /**
     * Retrieves the data located at the given storage pointer.
     *
     * @param pointer The pointer marking the start of the data entry.
     *
     * @return The data.
     */
    @Override
    public byte[] retrieve(StoragePointer pointer) throws StorageException
    {
        String filename = String.format(m_segmentNamePattern, pointer.segment);
        Path filePath = Paths.get(m_storagePath, filename);

        byte[] payload;

        try
        {
            InputStream data = Files.newInputStream(filePath);
            data.skip(pointer.offset);

            byte[] entryHeader = new byte[Integer.BYTES * 2];
            data.read(entryHeader, 0, Integer.BYTES * 2);

            ByteBuffer buffer = ByteBuffer.wrap(entryHeader);
            int magic = buffer.getInt();
            int size  = buffer.getInt();

            if (magic != FILE_MAGIC)
                throw new StorageException("Invalid magic header.");

            payload = new byte[size];
            data.read(payload, 0, size);
        }
        catch (Exception exception)
        {
            // Rethrow as StorageException to comply with the interface.
            throw new StorageException("Unable to add the given data to the storage.", exception);
        }

        return payload;
    }

    /**
     * Gets the index of the last used segment.
     *
     * @return The index of the last used block.
     */
    private int getCurrentUsedFile()
    {
        int lastUsed = 0;

        byte[] rawLastUsed = m_database.get(bytes(LAST_FILE_PREFIX));

        // We try once to get the file, if it cant find the key, this is the first time we open the DB, so we add it
        // and requesting again.
        if (rawLastUsed == null)
            m_database.put(bytes(LAST_FILE_PREFIX), NumberSerializer.serialize(lastUsed));

        lastUsed =  ByteBuffer.wrap(m_database.get(bytes(LAST_FILE_PREFIX))).getInt();


        return lastUsed;
    }

    /**
     * Gets the next blocks file name.
     *
     * @return Returns the name of the new blocks file.
     */
    private int getNextBlocksFileName()
    {
        int lastFile = ByteBuffer.wrap(m_database.get(bytes(LAST_FILE_PREFIX))).getInt();

        ++lastFile;

        m_database.put(bytes(LAST_FILE_PREFIX), NumberSerializer.serialize(lastFile));

        return lastFile;
    }
}
