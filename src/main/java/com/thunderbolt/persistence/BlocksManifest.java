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
package com.thunderbolt.persistence;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;
import org.iq80.leveldb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;

import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Data structure that holds how are the blocks stored in disk. (which file and at what index).
 */
public class BlocksManifest
{
    private static final Logger s_logger = LoggerFactory.getLogger(BlocksManifest.class);

    /**
     * Gets the metadata entry from the manifest.
     *
     * @param blockId The hash of the block header.
     *
     * @return The block metadata.
     *
     * @throws IOException If there is any IO error.
     */
    public static BlockMetadata getMetadata(Hash blockId) throws IOException
    {
        BlockMetadata metadata;

        Options options = new Options();
        options.createIfMissing(true);
        DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options);

        try
        {
            metadata = new BlockMetadata(ByteBuffer.wrap(db.get(blockId.serialize())));
        }
        finally
        {
            db.close();
        }

        return metadata;
    }

    /**
     * Adds a block metadata entry to the manifest.
     *
     * @param metadata The metadata to be added.
     *
     * @throws IOException If there is any IO error.
     */
    public static void addBlockMetadata(BlockMetadata metadata) throws IOException
    {
        Options options = new Options();
        options.createIfMissing(true);
        DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options);

        try
        {
            int lastFile = 0;

            // If the entry "l" does not exists create it.
            try
            {
                lastFile = ByteBuffer.wrap(db.get(bytes("l"))).getInt();
            }
            catch (Exception e)
            {
                s_logger.debug(e.toString());
                db.put(bytes("l"), NumberSerializer.serialize(lastFile));
            }

            //String fileKey = String.format("block%05d.bin", lastFile);
            db.put(metadata.getHeader().getHash().serialize(), metadata.serialize());
        }
        finally
        {
            db.close();
        }
    }
}
