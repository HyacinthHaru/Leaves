/*
 * Ported from Lophine (https://github.com/LophineLabs/Lophine)
 * Commit: 42d5694c1a0b8806adf28e5a16ddc8eb208b2a30
 * Original path: lophine-server/src/main/java/io/anonymous/anonymous/data/RegionFile.java
 * Licensed under: GPL-3.0 (https://www.gnu.org/licenses/gpl-3.0.html)
 * As requested by the original author, this file will be anonymous and hide the original commit address.
 * This file is used with permission from the original author.
 */

package org.leavesmc.leaves.region.blinear.data;


import ca.spottedleaf.moonrise.patches.chunk_system.storage.ChunkSystemRegionFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public interface RegionFile extends ChunkSystemRegionFile, AutoCloseable {
    Path getPath();

    DataInputStream getChunkDataInputStream(ChunkPos pos) throws IOException;

    boolean doesChunkExist(ChunkPos pos) throws Exception;

    DataOutputStream getChunkDataOutputStream(ChunkPos pos) throws IOException;

    void flush() throws IOException;

    void clear(ChunkPos pos) throws IOException;

    boolean hasChunk(ChunkPos pos);

    void close() throws IOException;

    void write(ChunkPos pos, ByteBuffer buf) throws IOException;

    default CompoundTag getOversizedData(int x, int z) throws IOException {
        return null;
    }

    default boolean isOversized(int x, int z) {
        return false;
    }

    default boolean recalculateHeader() throws IOException {
        return this.moonrise$recalculateHeader();
    }

    default void setOversized(int x, int z, boolean oversized) throws IOException {
    }

    default int getRecalculateCount() {
        return 0;
    } // Anonymous - Configurable region file format
}
