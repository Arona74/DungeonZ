package net.dungeonz.compat;

import net.dungeonz.DungeonzMain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class LootrCompat {

    private static final Block LOOTR_CHEST = Registries.BLOCK.get(new Identifier("lootr", "lootr_chest"));
    private static final Block LOOTR_TRAPPED_CHEST = Registries.BLOCK.get(new Identifier("lootr", "lootr_trapped_chest"));
    private static final Block LOOTR_BARREL = Registries.BLOCK.get(new Identifier("lootr", "lootr_barrel"));

    public static boolean isLootrAvailable() {
        return DungeonzMain.isLootrLoaded && LOOTR_CHEST != Blocks.AIR;
    }

    public static void convertToLootrChest(ServerWorld world, BlockPos pos, String lootTableString) {
        if (!isLootrAvailable()) {
            return;
        }

        BlockState currentState = world.getBlockState(pos);
        Block currentBlock = currentState.getBlock();
        Block lootrBlock = getLootrEquivalent(currentBlock);

        if (lootrBlock == null) {
            return;
        }

        BlockState newState = lootrBlock.getDefaultState();

        if (currentState.contains(Properties.HORIZONTAL_FACING) && newState.contains(Properties.HORIZONTAL_FACING)) {
            newState = newState.with(Properties.HORIZONTAL_FACING, currentState.get(Properties.HORIZONTAL_FACING));
        }

        world.setBlockState(pos, newState, 3);

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof LootableContainerBlockEntity lootableContainer) {
            lootableContainer.setLootTable(new Identifier(lootTableString), pos.asLong());
        }
    }

    private static Block getLootrEquivalent(Block block) {
        if (block == Blocks.CHEST) {
            return LOOTR_CHEST;
        } else if (block == Blocks.TRAPPED_CHEST) {
            return LOOTR_TRAPPED_CHEST;
        } else if (block == Blocks.BARREL) {
            return LOOTR_BARREL;
        }
        return null;
    }
}
