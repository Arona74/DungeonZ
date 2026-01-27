package net.dungeonz.dungeon;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Server-side manager for dungeon runtime data.
 * Stores large dungeon geometry data in separate files to avoid block entity NBT size limits.
 * Uses memory cache for fast access during gameplay.
 */
public class DungeonDataManager {

    private static final Logger LOGGER = LogManager.getLogger("DungeonDataManager");
    private static final Map<BlockPos, DungeonRuntimeData> CACHE = new HashMap<>();
    private static final String FOLDER_NAME = "dungeonz";

    /**
     * Gets runtime data for a dungeon portal. Loads from file if not in cache.
     */
    public static DungeonRuntimeData getData(ServerWorld world, BlockPos portalPos) {
        if (CACHE.containsKey(portalPos)) {
            return CACHE.get(portalPos);
        }

        DungeonRuntimeData data = loadFromFile(world, portalPos);
        if (data == null) {
            data = new DungeonRuntimeData();
            LOGGER.debug("Created new runtime data for dungeon portal at {} in {}", portalPos, world.getRegistryKey().getValue());
        } else {
            LOGGER.debug("Loaded runtime data from file for dungeon portal at {} in {}", portalPos, world.getRegistryKey().getValue());
        }

        CACHE.put(portalPos, data);
        return data;
    }

    /**
     * Saves runtime data for a dungeon portal. Updates cache and saves to file.
     */
    public static void saveData(ServerWorld world, BlockPos portalPos, DungeonRuntimeData data) {
        CACHE.put(portalPos, data);
        saveToFileAsync(world, portalPos, data);
    }

    /**
     * Removes runtime data from cache and deletes the file.
     */
    public static void deleteData(ServerWorld world, BlockPos portalPos) {
        CACHE.remove(portalPos);
        Path file = getDataFile(world, portalPos);
        if (Files.exists(file)) {
            try {
                Files.delete(file);
                LOGGER.info("Deleted runtime data for dungeon portal at {} in {} ({})",
                    portalPos, world.getRegistryKey().getValue(), file.getFileName());
            } catch (IOException e) {
                LOGGER.error("Failed to delete runtime data file for portal at {} in {}: {}",
                    portalPos, world.getRegistryKey().getValue(), e.getMessage());
            }
        }
    }

    /**
     * Clears the entire cache (useful for server shutdown).
     */
    public static void clearCache() {
        CACHE.clear();
        LOGGER.info("Cleared dungeon runtime data cache");
    }

    /**
     * Gets the data directory for this world and dimension.
     * Structure: [save]/data/dungeonz/[namespace]/[dimension]/
     */
    private static Path getDataDirectory(ServerWorld world) {
        Path saveDir = world.getServer().getSavePath(WorldSavePath.ROOT);

        String dimensionNamespace = world.getRegistryKey().getValue().getNamespace();
        String dimensionPath = world.getRegistryKey().getValue().getPath();

        Path dimensionDir = saveDir.resolve("data").resolve(FOLDER_NAME).resolve(dimensionNamespace).resolve(dimensionPath);

        try {
            Files.createDirectories(dimensionDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create dungeon data directory: {}", e.getMessage());
        }

        return dimensionDir;
    }

    /**
     * Gets the file path for a specific portal's runtime data.
     */
    private static Path getDataFile(ServerWorld world, BlockPos portalPos) {
        Path dir = getDataDirectory(world);
        String filename = String.format("portal_%d_%d_%d.dat", portalPos.getX(), portalPos.getY(), portalPos.getZ());
        return dir.resolve(filename);
    }

    /**
     * Loads runtime data from file synchronously.
     */
    private static DungeonRuntimeData loadFromFile(ServerWorld world, BlockPos portalPos) {
        Path file = getDataFile(world, portalPos);

        if (!Files.exists(file)) {
            return null;
        }

        try {
            NbtCompound nbt = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
            DungeonRuntimeData data = DungeonRuntimeData.readFromNbt(nbt);

            LOGGER.info("Loaded runtime data from file: {} (size: {} bytes)",
                file.getFileName(), Files.size(file));

            return data;
        } catch (IOException e) {
            LOGGER.error("Failed to load runtime data from file {}: {}", file.getFileName(), e.getMessage());
            return null;
        }
    }

    /**
     * Saves runtime data to file synchronously.
     */
    private static void saveToFile(ServerWorld world, BlockPos portalPos, DungeonRuntimeData data) {
        Path file = getDataFile(world, portalPos);

        try {
            Files.createDirectories(file.getParent());

            NbtCompound nbt = data.writeToNbt();
            NbtIo.writeCompressed(nbt, file);

            LOGGER.debug("Saved runtime data to file: {} (size: {} bytes)",
                file.getFileName(), Files.size(file));

        } catch (IOException e) {
            LOGGER.error("Failed to save runtime data to file {}: {}", file.getFileName(), e.getMessage());
        }
    }

    /**
     * Saves runtime data to file asynchronously to avoid blocking the server thread.
     */
    private static void saveToFileAsync(ServerWorld world, BlockPos portalPos, DungeonRuntimeData data) {
        CompletableFuture.runAsync(() -> {
            saveToFile(world, portalPos, data);
        }).exceptionally(throwable -> {
            LOGGER.error("Async save failed for portal at {}: {}", portalPos, throwable.getMessage());
            return null;
        });
    }

    /**
     * Migration helper: Saves data immediately if it came from old NBT format.
     */
    public static void migrateFromOldNbt(ServerWorld world, BlockPos portalPos, DungeonRuntimeData data) {
        CACHE.put(portalPos, data);
        Path file = getDataFile(world, portalPos);
        try {
            Files.createDirectories(file.getParent());
            NbtCompound nbt = data.writeToNbt();
            NbtIo.writeCompressed(nbt, file);
            LOGGER.info("Migrated runtime data from old NBT format for portal at {} in {} (saved {} bytes to {})",
                portalPos, world.getRegistryKey().getValue(), Files.size(file), file.getFileName());
        } catch (IOException e) {
            LOGGER.error("Failed to save migrated data for portal at {} in {}: {}",
                portalPos, world.getRegistryKey().getValue(), e.getMessage());
        }
    }
}
