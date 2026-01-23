package net.dungeonz.dungeon;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        // Check cache first
        if (CACHE.containsKey(portalPos)) {
            return CACHE.get(portalPos);
        }

        // Try to load from file
        DungeonRuntimeData data = loadFromFile(world, portalPos);
        if (data == null) {
            // Create new empty data if file doesn't exist
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
     * Checks if runtime data exists for a portal (in cache or on disk).
     */
    public static boolean hasData(ServerWorld world, BlockPos portalPos) {
        if (CACHE.containsKey(portalPos)) {
            return true;
        }
        return getDataFile(world, portalPos).exists();
    }

    /**
     * Removes runtime data from cache and deletes the file.
     */
    public static void deleteData(ServerWorld world, BlockPos portalPos) {
        CACHE.remove(portalPos);
        File file = getDataFile(world, portalPos);
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
                LOGGER.info("Deleted runtime data for dungeon portal at {} in {} ({})",
                    portalPos, world.getRegistryKey().getValue(), file.getName());
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
     * Structure: world/data/dungeonz/[namespace]/[dimension]/
     * Example: world/data/dungeonz/minecraft/overworld/portal_x_y_z.dat
     */
    private static File getDataDirectory(ServerWorld world) {
        // Get the world save directory
        File worldDir = world.getServer().getRunDirectory().toPath().resolve("saves").resolve(world.getServer().getSaveProperties().getLevelName()).toFile();

        // If that doesn't work (e.g., dedicated server), try alternate path
        if (!worldDir.exists()) {
            worldDir = world.getServer().getRunDirectory();
        }

        // Get dimension identifier (e.g., "minecraft:overworld" -> namespace="minecraft", path="overworld")
        String dimensionNamespace = world.getRegistryKey().getValue().getNamespace();
        String dimensionPath = world.getRegistryKey().getValue().getPath();

        // Build directory structure: data/dungeonz/[namespace]/[dimension]/
        File dataDir = new File(worldDir, "data");
        File dungeonzDir = new File(dataDir, FOLDER_NAME);
        File namespaceDir = new File(dungeonzDir, dimensionNamespace);
        File dimensionDir = new File(namespaceDir, dimensionPath);

        if (!dimensionDir.exists()) {
            dimensionDir.mkdirs();
        }

        return dimensionDir;
    }

    /**
     * Gets the file path for a specific portal's runtime data.
     * Example: world/data/dungeonz/minecraft/overworld/portal_100_64_200.dat
     */
    private static File getDataFile(ServerWorld world, BlockPos portalPos) {
        File dir = getDataDirectory(world);
        String filename = String.format("portal_%d_%d_%d.dat", portalPos.getX(), portalPos.getY(), portalPos.getZ());
        return new File(dir, filename);
    }

    /**
     * Loads runtime data from file synchronously.
     */
    private static DungeonRuntimeData loadFromFile(ServerWorld world, BlockPos portalPos) {
        File file = getDataFile(world, portalPos);

        if (!file.exists()) {
            return null;
        }

        try {
            NbtCompound nbt = NbtIo.readCompressed(file);
            DungeonRuntimeData data = DungeonRuntimeData.readFromNbt(nbt);

            LOGGER.info("Loaded runtime data from file: {} (size: {} bytes)",
                file.getName(), file.length());

            return data;
        } catch (IOException e) {
            LOGGER.error("Failed to load runtime data from file {}: {}", file.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Saves runtime data to file synchronously.
     */
    private static void saveToFile(ServerWorld world, BlockPos portalPos, DungeonRuntimeData data) {
        File file = getDataFile(world, portalPos);

        try {
            // Ensure directory exists
            file.getParentFile().mkdirs();

            NbtCompound nbt = data.writeToNbt();
            NbtIo.writeCompressed(nbt, file);

            // Use debug level to reduce log spam during frequent saves
            LOGGER.debug("Saved runtime data to file: {} (size: {} bytes)",
                file.getName(), file.length());

        } catch (IOException e) {
            LOGGER.error("Failed to save runtime data to file {}: {}", file.getName(), e.getMessage());
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
     * This is called during readNbt() when upgrading from old versions.
     */
    public static void migrateFromOldNbt(ServerWorld world, BlockPos portalPos, DungeonRuntimeData data) {
        CACHE.put(portalPos, data);
        // Save synchronously during migration to ensure data is persisted
        File file = getDataFile(world, portalPos);
        try {
            file.getParentFile().mkdirs();
            NbtCompound nbt = data.writeToNbt();
            NbtIo.writeCompressed(nbt, file);
            LOGGER.info("Migrated runtime data from old NBT format for portal at {} in {} (saved {} bytes to {})",
                portalPos, world.getRegistryKey().getValue(), file.length(), file.getPath().replace(file.getParentFile().getParentFile().getParent() + File.separator, ""));
        } catch (IOException e) {
            LOGGER.error("Failed to save migrated data for portal at {} in {}: {}",
                portalPos, world.getRegistryKey().getValue(), e.getMessage());
        }
    }
}
