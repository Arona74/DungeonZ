package net.dungeonz;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.dungeonz.dungeon.Dungeon;
import net.dungeonz.init.BlockInit;
import net.dungeonz.init.CommandInit;
import net.dungeonz.init.ParticleInit;
import net.dungeonz.init.ConfigInit;
import net.dungeonz.init.CriteriaInit;
import net.dungeonz.init.DimensionInit;
import net.dungeonz.init.EventInit;
import net.dungeonz.init.ItemInit;
import net.dungeonz.init.LoaderInit;
import net.dungeonz.init.SoundInit;
import net.dungeonz.init.TagInit;
import net.dungeonz.init.WorldInit;
import net.dungeonz.block.entity.DungeonPortalEntity;
import net.dungeonz.init.DimensionInit;
import net.dungeonz.network.DungeonServerPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DungeonzMain implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("DungeonZ");

    public static final boolean isPartyAddonLoaded = FabricLoader.getInstance().isModLoaded("partyaddon");
    public static final boolean isRpgDifficultyLoaded = FabricLoader.getInstance().isModLoaded("rpgdifficulty");
    public static final boolean isLevelZLoaded = FabricLoader.getInstance().isModLoaded("levelz");
    public static final boolean isLootrLoaded = FabricLoader.getInstance().isModLoaded("lootr");
    public static final boolean isTrinketsLoaded = FabricLoader.getInstance().isModLoaded("trinkets");

    public static final List<Dungeon> DUNGEONS = new ArrayList<Dungeon>();

    @Override
    public void onInitialize() {
        BlockInit.init();
        ParticleInit.init();
        DimensionInit.init();
        DungeonServerPacket.init();
        WorldInit.init();
        LoaderInit.init();
        CriteriaInit.init();
        ConfigInit.init();
        SoundInit.init();
        ItemInit.init();
        EventInit.init();
        TagInit.init();
        CommandInit.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long time = server.getOverworld().getTime();

            // Fallback boss-death detection: runs every 100 ticks regardless of portal chunk load state.
            // Catches modded bosses that override onDeath() without calling super.
            if (time % 100 == 0) {
                ServerWorld dungeonWorld = server.getWorld(DimensionInit.DUNGEON_WORLD);
                if (dungeonWorld != null) {
                    for (DungeonPortalEntity portal : DungeonPortalEntity.ACTIVE_BOSS_PORTALS) {
                        java.util.UUID bossUuid = portal.getBossEntityUuid();
                        if (bossUuid == null) {
                            DungeonPortalEntity.ACTIVE_BOSS_PORTALS.remove(portal);
                            continue;
                        }
                        net.minecraft.entity.Entity boss = dungeonWorld.getEntity(bossUuid);
                        if (boss == null || boss.isRemoved()) {
                            LOGGER.info("[DungeonZ] Boss {} {} in dungeon world - triggering dungeon completion for portal at {}",
                                    bossUuid, boss == null ? "not found" : "is removed", portal.getPos());
                            portal.setBossEntityUuid(null);
                            portal.finishDungeon(dungeonWorld, portal.getBossBlockPos());
                        } else {
                            LOGGER.debug("[DungeonZ] Boss tick check: {} ({}) still alive at {}",
                                    bossUuid, boss.getType().toString(), boss.getBlockPos());
                        }
                    }
                }
            }

            for (DungeonPortalEntity portal : DungeonPortalEntity.ACTIVE_TIMER_PORTALS) {
                if (!portal.isDungeonTimerActive()) {
                    DungeonPortalEntity.ACTIVE_TIMER_PORTALS.remove(portal);
                    continue;
                }
                if (portal.isDungeonTimerExpired()) {
                    portal.handleDungeonTimerExpired();
                    continue;
                }
                if (time % 20 == 0) {
                    portal.syncGuiToAllViewers();
                    int timeRemaining = portal.getDungeonTimeRemaining();
                    int minutes = timeRemaining / 60;
                    int seconds = timeRemaining % 60;
                    Text timerText = Text.literal(String.format("%d:%02d", minutes, seconds))
                            .formatted(timeRemaining <= 30 ? Formatting.RED : Formatting.YELLOW);
                    ServerWorld dungeonWorld = server.getWorld(DimensionInit.DUNGEON_WORLD);
                    if (dungeonWorld != null) {
                        for (java.util.UUID playerUuid : portal.getDungeonPlayerUuids()) {
                            ServerPlayerEntity player = (ServerPlayerEntity) dungeonWorld.getPlayerByUuid(playerUuid);
                            if (player != null) {
                                player.sendMessage(timerText, true);
                            }
                        }
                    }
                }
            }
        });
    }

}
