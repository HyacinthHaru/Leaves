package org.leavesmc.leaves.region.blinear;

import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.leavesmc.leaves.LeavesConfig;
import org.leavesmc.leaves.region.blinear.data.RegionFile;
import org.leavesmc.leaves.region.blinear.enums.EnumRegionFormat;
import org.leavesmc.leaves.region.blinear.utils.RegionCreatorInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegionFormats {

    public static final Pattern REGION_FILE_PATTERN = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.(?:" + Arrays.stream(EnumRegionFormat.values()).map(EnumRegionFormat::getArgument).collect(Collectors.joining("|")) + ")$");

    // configured format first, then the others; new regions use the configured format
    public static Path resolve(Path folder, String name, boolean create) {
        Path configured = folder.resolve(name + "." + LeavesConfig.region.format.getArgument());
        if (Files.isRegularFile(configured)) {
            return configured;
        }
        for (EnumRegionFormat format : EnumRegionFormat.values()) {
            Path path = folder.resolve(name + "." + format.getArgument());
            if (format != LeavesConfig.region.format && Files.isRegularFile(path)) {
                return path;
            }
        }
        return create ? configured : null;
    }

    public static boolean isRegionFile(String name) {
        return getFormat(name) != null;
    }

    public static RegionFile create(RegionStorageInfo info, Path path, Path folder, boolean sync) throws IOException {
        EnumRegionFormat format = getFormat(path.getFileName().toString());
        if (format == null) {
            throw new IOException("Unsupported region file format: " + path);
        }
        if (format == EnumRegionFormat.BLINEAR) {
            LeavesConfig.region.blinear.startFlusher();
        }
        return format.getCreator().newFile(new RegionCreatorInfo(info, path, folder, sync));
    }

    private static EnumRegionFormat getFormat(String name) {
        for (EnumRegionFormat format : EnumRegionFormat.values()) {
            if (name.endsWith("." + format.getArgument())) {
                return format;
            }
        }
        return null;
    }
}
