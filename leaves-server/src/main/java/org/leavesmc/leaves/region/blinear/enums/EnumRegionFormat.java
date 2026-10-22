/*
 * Ported from Lophine (https://github.com/LophineLabs/Lophine)
 * Commit: 42d5694c1a0b8806adf28e5a16ddc8eb208b2a30
 * Original path: lophine-server/src/main/java/io/anonymous/anonymous/enums/EnumRegionFormat.java
 * Licensed under: GPL-3.0 (https://www.gnu.org/licenses/gpl-3.0.html)
 * As requested by the original author, this file will be anonymous and hide the original commit address.
 * This file is used with permission from the original author.
 */

package org.leavesmc.leaves.region.blinear.enums;

import net.minecraft.world.level.chunk.storage.RegionFile;
import org.leavesmc.leaves.LeavesConfig;
import org.leavesmc.leaves.region.blinear.data.BufferedLinearRegionFile;
import org.leavesmc.leaves.region.blinear.utils.RegionFileFactory;

public enum EnumRegionFormat {
    ANVIL("mca", (info) -> new RegionFile(info.info(), info.filePath(), info.folder(), info.sync())),
    BLINEAR("b_linear", (info) -> new BufferedLinearRegionFile(info.filePath(), LeavesConfig.region.blinear.compressionLevel, LeavesConfig.region.blinear.flusher));

    private final String argument;
    private final RegionFileFactory creator;

    EnumRegionFormat(String argument, RegionFileFactory creator) {
        this.argument = argument;
        this.creator = creator;
    }

    public RegionFileFactory getCreator() {
        return this.creator;
    }

    public String getArgument() {
        return this.argument;
    }
}
