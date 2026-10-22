package org.leavesmc.leaves.region.blinear;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.leavesmc.leaves.LeavesLogger;
import org.leavesmc.leaves.region.blinear.data.BufferedLinearRegionFile;
import org.leavesmc.leaves.region.blinear.data.BufferedLinearRegionFileFlusher;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

public class LinearMigration {

    private static final String CONFIRM_PROPERTY = "leaves.convert.linear";
    private static final List<String> STORAGE_FOLDERS = List.of("region", "entities", "poi");
    private static final Pattern LINEAR_FILE = Pattern.compile("^r\\.-?[0-9]+\\.-?[0-9]+\\.linear$");
    private static final String FORMAT = "settings.region.format";
    private static final String LINEAR_LEVEL = "settings.region.linear.compression-level";
    private static final String BLINEAR_LEVEL = "settings.region.blinear.compression-level";

    public static boolean run(LevelStorageSource.LevelStorageAccess access, File configFile) throws IOException {
        List<Path> linearFiles = new ArrayList<>();
        Path level = access.getLevelDirectory().path();
        for (Path world : List.of(level, level.resolveSibling(access.getLevelId() + "_nether"), level.resolveSibling(access.getLevelId() + "_the_end"))) {
            scan(world, linearFiles);
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
            if (configFile.isFile()) {
                config.load(configFile);
            }
        } catch (InvalidConfigurationException e) {
            if (linearFiles.isEmpty()) {
                return true;
            }
            LeavesLogger.LOGGER.error("Failed to read {}, can not convert Linear region files", configFile, e);
            return false;
        }

        boolean linearConfig = "LINEAR".equalsIgnoreCase(config.getString(FORMAT));
        if (!linearConfig && linearFiles.isEmpty()) {
            return true;
        }

        List<Path> conflicts = linearFiles.stream().filter(file -> Files.exists(sibling(file, ".b_linear"))).toList();
        if (!conflicts.isEmpty()) {
            LeavesLogger.LOGGER.error("Can not convert Linear region files, BLinear region files with the same name already exist:");
            conflicts.forEach(file -> LeavesLogger.LOGGER.error("  {}", file));
            LeavesLogger.LOGGER.error("Leaves will not guess which file holds the current data. The world and leaves.yml have not been changed.");
            LeavesLogger.LOGGER.error("Please keep only one file of each pair (e.g. restore a backup), then start the server again.");
            return false;
        }

        Integer carriedLevel = null;
        if (!config.contains(BLINEAR_LEVEL) && config.isInt(LINEAR_LEVEL) && config.getInt(LINEAR_LEVEL) >= 1 && config.getInt(LINEAR_LEVEL) <= 22) {
            carriedLevel = config.getInt(LINEAR_LEVEL);
        }
        int compressionLevel = carriedLevel != null ? carriedLevel : config.getInt(BLINEAR_LEVEL, 1);
        if (compressionLevel < 1 || compressionLevel > 22) {
            compressionLevel = 1;
        }

        LeavesLogger.LOGGER.warn("==================== ! LINEAR REGION FORMAT ! ====================");
        LeavesLogger.LOGGER.warn("This world uses the Linear region format, which is no longer supported by Leaves.");
        LeavesLogger.LOGGER.warn("{} Linear region file(s) will be converted to BLinear (.b_linear), other region files are kept and still read.", linearFiles.size());
        if (linearConfig) {
            LeavesLogger.LOGGER.warn("region.format in leaves.yml will be changed from LINEAR to BLINEAR.");
        }
        if (carriedLevel != null) {
            LeavesLogger.LOGGER.warn("region.blinear.compression-level in leaves.yml will be set to {} (from region.linear.compression-level).", carriedLevel);
        }
        LeavesLogger.LOGGER.warn("No backup will be made. Please back up your worlds before continuing!");
        LeavesLogger.LOGGER.warn("Type \"confirm\" to start the conversion, or anything else to stop the server.");
        LeavesLogger.LOGGER.warn("==================================================================");

        if (Boolean.getBoolean(CONFIRM_PROPERTY)) {
            LeavesLogger.LOGGER.warn("Conversion confirmed by -D{}=true", CONFIRM_PROPERTY);
        } else if (!readConfirm()) {
            LeavesLogger.LOGGER.error("Conversion not confirmed, stopping the server. The world and leaves.yml have not been changed.");
            LeavesLogger.LOGGER.error("To confirm without console input, add -D{}=true to the JVM arguments.", CONFIRM_PROPERTY);
            return false;
        }

        if (linearConfig) {
            config.set(FORMAT, "BLINEAR");
        }
        if (carriedLevel != null) {
            config.set(BLINEAR_LEVEL, carriedLevel);
        }
        return convert(linearFiles, compressionLevel, config, configFile, linearConfig || carriedLevel != null);
    }

    private static void scan(Path world, List<Path> linearFiles) throws IOException {
        Files.walkFileTree(world, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (LINEAR_FILE.matcher(file.getFileName().toString()).matches() && STORAGE_FOLDERS.contains(file.getParent().getFileName().toString())) {
                    linearFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean readConfirm() {
        if (!org.bukkit.craftbukkit.Main.useConsole) {
            return false;
        }

        StringBuilder line = new StringBuilder();
        try {
            for (int c = System.in.read(); c != -1 && c != '\n'; c = System.in.read()) {
                line.append((char) c);
            }
        } catch (IOException e) {
            return false;
        }
        return line.toString().trim().equalsIgnoreCase("confirm");
    }

    private static boolean convert(List<Path> linearFiles, int compressionLevel, YamlConfiguration config, File configFile, boolean saveConfig) {
        long start = System.currentTimeMillis();
        BufferedLinearRegionFileFlusher flusher = new BufferedLinearRegionFileFlusher(1, 20, 3000);

        List<Path> created = new ArrayList<>();
        String current = null;
        try {
            for (int i = 0; i < linearFiles.size(); i++) {
                Path linear = linearFiles.get(i);
                current = linear.toString();
                Path temp = sibling(linear, ".b_linear.tmp");
                created.add(temp);
                convert(linear, temp, compressionLevel, flusher);
                LeavesLogger.LOGGER.info("({}/{}) {}", i + 1, linearFiles.size(), linear);
            }
            for (Path linear : linearFiles) {
                current = linear.toString();
                Path target = sibling(linear, ".b_linear");
                Files.move(sibling(linear, ".b_linear.tmp"), target);
                created.add(target);
            }
            if (saveConfig) {
                current = configFile.toString();
                config.save(configFile);
            }
        } catch (Exception e) {
            LeavesLogger.LOGGER.error("Conversion failed at {}, the world and leaves.yml are left unchanged", current, e);
            for (Path path : created) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    LeavesLogger.LOGGER.error("Failed to delete {}", path, ex);
                }
            }
            return false;
        } finally {
            flusher.shutdown();
        }

        for (Path linear : linearFiles) {
            try {
                Files.delete(linear);
            } catch (IOException e) {
                LeavesLogger.LOGGER.error("The world has been converted to BLinear, but failed to delete {}", linear, e);
                LeavesLogger.LOGGER.error("Please delete the remaining .linear files manually, then start the server again.");
                return false;
            }
        }

        LeavesLogger.LOGGER.info("Converted {} Linear region file(s) to BLinear in {}s", linearFiles.size(), (System.currentTimeMillis() - start) / 1000);
        return true;
    }

    private static void convert(Path linear, Path temp, int compressionLevel, BufferedLinearRegionFileFlusher flusher) throws IOException {
        Files.copy(linear, temp, StandardCopyOption.REPLACE_EXISTING);

        BitSet chunks = new BitSet(1024);
        try (BufferedLinearRegionFile region = new BufferedLinearRegionFile(temp, compressionLevel, flusher)) {
            for (int i = 0; i < 1024; i++) {
                chunks.set(i, region.doesChunkExist(new ChunkPos(i & 31, i >> 5)));
            }
        }

        try (BufferedLinearRegionFile region = new BufferedLinearRegionFile(temp, compressionLevel, flusher)) {
            for (int i = 0; i < 1024; i++) {
                try (DataInputStream data = region.getChunkDataInputStream(new ChunkPos(i & 31, i >> 5))) {
                    if ((data != null) != chunks.get(i)) {
                        throw new IOException("Chunk " + (i & 31) + "," + (i >> 5) + " does not match after conversion");
                    }
                }
            }
        }
    }

    private static Path sibling(Path linear, String extension) {
        String name = linear.getFileName().toString();
        return linear.resolveSibling(name.substring(0, name.length() - ".linear".length()) + extension);
    }
}
