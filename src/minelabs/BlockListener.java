package minelabs;

import java.util.List;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;

public class BlockListener implements Listener {
    
    private class BlockSupport {
        public final BlockFace direction;
        public final List<BlockFace> assistingDirections;
        
        public BlockSupport(BlockFace direction, List<BlockFace> assistingDirections) {
            this.direction = direction;
            this.assistingDirections = assistingDirections; // must be length 2
        }
    }

    private final List<BlockSupport> supportingDirections = Arrays.asList(
            new BlockSupport(BlockFace.NORTH, Arrays.asList(BlockFace.NORTH_EAST, BlockFace.NORTH_WEST)),
            new BlockSupport(BlockFace.EAST, Arrays.asList(BlockFace.NORTH_EAST, BlockFace.SOUTH_EAST)),
            new BlockSupport(BlockFace.SOUTH, Arrays.asList(BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST)),
            new BlockSupport(BlockFace.WEST, Arrays.asList(BlockFace.NORTH_WEST, BlockFace.SOUTH_WEST))
    );

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {

        Block originBlock = event.getBlock();

        switch (event.getBlock().getType()) {
            // Blocks to allow to float
            case FIRE:
            case TNT:
                return;
            default:
                // No floating allowed!
                if (isSupported(originBlock) || isSupportedByNeighbors(originBlock)) {
                    return; // This block is supported, take no action
                }
                break;
        }

        // If we got to here, this block is not supported
        event.setCancelled(true);
        event.setBuild(false);
        // print message?
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockRemove(BlockBreakEvent event) {

        Block originBlock = event.getBlock();

        // Find all surrounding blocks and check them
        List<Block> surroundingBlocks = Arrays.asList(originBlock.getRelative(BlockFace.UP),
                originBlock.getRelative(BlockFace.NORTH), originBlock.getRelative(BlockFace.EAST),
                originBlock.getRelative(BlockFace.SOUTH), originBlock.getRelative(BlockFace.WEST));

        for (Block block : surroundingBlocks) {
            if (block.getType() != Material.AIR && !isSupported(block, originBlock) && !isSupportedByNeighbors(block, originBlock)) {

                // This block is no longer supported - spawn a falling block in its place and remove it
                block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5), block.getType(), block.getData());
                block.setType(Material.AIR);

                // Trigger a BlockBreakEvent to propagate the change to further blocks
                Bukkit.getServer().getPluginManager().callEvent(new BlockBreakEvent(block, event.getPlayer()));
            }
        }
    }

    /*
	 * block - block to check if supported by neighbors
	 * destroyedBlock - the block being destroyed, treat as air
     */
    private boolean isSupportedByNeighbors(Block block, Block destroyedBlock) {
        for (BlockSupport support : supportingDirections) {
            Block checking = block.getRelative(support.direction, 1);
            if (destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation())) {
                continue;
            }
            if (checking.getType() == Material.AIR) {
                continue; // this direction is broken by air
            }
            if (isSupported(checking, destroyedBlock) 
                    || isSupported(block.getRelative(support.assistingDirections.get(0)), destroyedBlock)
                    || isSupported(block.getRelative(support.assistingDirections.get(1)), destroyedBlock)) {
                return true;
            }

            checking = block.getRelative(support.direction, 2);
            if (destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation())) {
                continue;
            }
            if (checking.getType() == Material.AIR) {
                continue; // this direction is broken by air
            }
            if (isSupported(checking, destroyedBlock)) {
                return true;
            }

            checking = block.getRelative(support.direction, 3);
            if (destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation())) {
                continue;
            }
            if (checking.getType() == Material.AIR) {
                continue; // this direction is broken by air
            }
            if (isSupported(checking, destroyedBlock)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isSupportedByNeighbors(Block block) {
        return isSupportedByNeighbors(block, null);
    }

    /*
	 * block - block to check if supported
	 * destroyedBlock - the block being destroyed, treat as air
     */
    private boolean isSupported(Block block, Block destroyedBlock) {
        Block checking = block.getRelative(BlockFace.DOWN);
        if (destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation())) {
            return false;
        }
        return checking.getType() != Material.AIR;
    }
    
    private boolean isSupported(Block block) {
        return isSupported(block, null);
    }
}
