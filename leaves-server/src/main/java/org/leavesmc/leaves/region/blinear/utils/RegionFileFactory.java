/*
 * Ported from Lophine (https://github.com/LophineLabs/Lophine)
 * Commit: 42d5694c1a0b8806adf28e5a16ddc8eb208b2a30
 * Original path: lophine-server/src/main/java/io/anonymous/anonymous/utils/RegionFileFactory.java
 * Licensed under: GPL-3.0 (https://www.gnu.org/licenses/gpl-3.0.html)
 * As requested by the original author, this file will be anonymous and hide the original commit address.
 * This file is used with permission from the original author.
 */

package org.leavesmc.leaves.region.blinear.utils;

import org.leavesmc.leaves.region.blinear.data.RegionFile;

import java.io.IOException;

@FunctionalInterface
public interface RegionFileFactory {
    RegionFile newFile(RegionCreatorInfo info) throws IOException;
}
