package net.dungeonz.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public class PropertyUtil {
    
    public static int getHorizontalFacing(BlockState blockState) {
        if (blockState.contains(Properties.HORIZONTAL_FACING)) {
            return blockState.get(Properties.HORIZONTAL_FACING).getHorizontal();
        }
        return 0;
    }
    
    // For 1.20.1, you'll need to check specific properties based on block type
    public static int getBlockFacing(BlockState blockState) {
        // Check for wall-mounted blocks (like buttons, levers)
        if (blockState.contains(Properties.WALL_MOUNT_LOCATION)) {
            switch (blockState.get(Properties.WALL_MOUNT_LOCATION)) {
                case FLOOR: return 1;
                case WALL: return 2;
                case CEILING: return 3;
            }
        }
        
        // Check for facing direction and infer position
        if (blockState.contains(Properties.FACING)) {
            Direction facing = blockState.get(Properties.FACING);
            switch (facing) {
                case UP: return 3; // ceiling
                case DOWN: return 1; // floor
                default: return 2; // wall
            }
        }
        
        return 0;
    }
    
    // Since BlockFace doesn't exist in 1.20.1, using WallMountLocation
    public static WallMountLocation getBlockFaceFromInt(int blockFacing) {
        return switch (blockFacing) {
            case 1 -> WallMountLocation.FLOOR; // FLOOR equivalent
            case 2 -> WallMountLocation.WALL; // WALL equivalent  
            case 3 -> WallMountLocation.CEILING; // CEILING equivalent
            default -> WallMountLocation.FLOOR;
        };
    }
}