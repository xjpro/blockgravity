package summitmc;

import java.util.List;
import java.util.ArrayList;
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
            this.assistingDirections = assistingDirections; // must be length 4
        }
    }

    private final List<BlockSupport> supportingDirections = Arrays.asList(
            new BlockSupport(
                    BlockFace.NORTH,
                    Arrays.asList(
                            BlockFace.NORTH_WEST,
                            BlockFace.NORTH_NORTH_WEST,
                            BlockFace.NORTH_NORTH_EAST,
                            BlockFace.NORTH_EAST
                    )
            ),
            new BlockSupport(
                    BlockFace.EAST,
                    Arrays.asList(
                            BlockFace.NORTH_EAST,
                            BlockFace.EAST_NORTH_EAST,
                            BlockFace.EAST_SOUTH_EAST,
                            BlockFace.SOUTH_EAST
                    )
            ),
            new BlockSupport(
                    BlockFace.SOUTH,
                    Arrays.asList(
                            BlockFace.SOUTH_EAST,
                            BlockFace.SOUTH_SOUTH_EAST,
                            BlockFace.SOUTH_SOUTH_WEST,
                            BlockFace.SOUTH_WEST
                    )
            ),
            new BlockSupport(
                    BlockFace.WEST,
                    Arrays.asList(
                            BlockFace.NORTH_WEST,
                            BlockFace.WEST_NORTH_WEST,
                            BlockFace.WEST_SOUTH_WEST,
                            BlockFace.SOUTH_WEST
                    )
            )
    );

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {

        Block placed = event.getBlock();
        if (!placed.getType().isSolid()) {
            // Not a solid block that can be built upon, we don't care if it's supported
            return;
        }
        if (isSupported(placed) || isSupportedByNeighbors(placed)) {
            // This block is supported
            return;
        }

        // If we got to here, this block is not supported
        event.setCancelled(true);
        event.setBuild(false);
        // print message?
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockRemove(BlockBreakEvent event) {

        Block destroyed = event.getBlock();

        List<Block> surroundingBlocks = new ArrayList<>();
        surroundingBlocks.add(destroyed.getRelative(BlockFace.NORTH));
        surroundingBlocks.add(destroyed.getRelative(BlockFace.EAST));
        surroundingBlocks.add(destroyed.getRelative(BlockFace.SOUTH));
        surroundingBlocks.add(destroyed.getRelative(BlockFace.WEST));

        // Add all upward blocks
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                surroundingBlocks.add(destroyed.getRelative(x, 1, z));
            }
        }

        surroundingBlocks
                .stream()
                // Filter to non-air blocks that are not supported directly or by neighbors
                .filter((block) -> (block.getType() != Material.AIR && !isSupported(block, destroyed) && !isSupportedByNeighbors(block, destroyed)))
                .forEach((block) -> {
                    // This block is no longer supported - spawn a falling block in its place and remove it
                    block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5), block.getType(), block.getData());
                    block.setType(Material.AIR);
                    Bukkit.getServer().getPluginManager().callEvent(new BlockBreakEvent(block, event.getPlayer()));
                });
    }

    /*
    block - block to check if supported by neighbors
    destroyedBlock - the block being destroyed, treat as air
     */
    private boolean isSupportedByNeighbors(Block block, Block destroyedBlock) {
        for (BlockSupport support : supportingDirections) {
            Block checking = block.getRelative(support.direction, 1);
            if (destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation())) {
                continue;
            }
            if (!checking.getType().isSolid()) {
                continue; // this direction is broken by non-solid block
            }
            // Possible support block is solid, but is it supported?
            if (isSupported(checking, destroyedBlock)
                    // Allow diagonal blocks to be the support they are within 1 distance
                    || isSupported(block.getRelative(support.assistingDirections.get(0), 1), destroyedBlock)
                    || isSupported(block.getRelative(support.assistingDirections.get(1), 1), destroyedBlock)
                    || isSupported(block.getRelative(support.assistingDirections.get(2), 1), destroyedBlock)
                    || isSupported(block.getRelative(support.assistingDirections.get(3), 1), destroyedBlock)) {
                return true;
            }

            checking = block.getRelative(support.direction, 2);
            if (destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation())) {
                continue;
            }
            if (!checking.getType().isSolid()) {
                continue; // this direction is broken by non-solid block
            }
            // Possible support block is solid, but is it supported?
            if (isSupported(checking, destroyedBlock)) {
                return true;
            }

            checking = block.getRelative(support.direction, 3);
            if (destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation())) {
                continue;
            }
            if (!checking.getType().isSolid()) {
                continue; // this direction is broken by non-solid block
            }
            // Possible support block is solid, but is it supported?
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
    block - block to check if supported 
    destroyedBlock - the block being destroyed, treat as air
     */
    private boolean isSupported(Block block, Block destroyed) {
        Block belowBlock = block.getRelative(BlockFace.DOWN);
        if (destroyed != null && destroyed.getLocation().equals(belowBlock.getLocation())) {
            return false; // Treat destroyedBlock as air
        }
        return belowBlock.getType().isSolid();
    }

    private boolean isSupported(Block block) {
        return isSupported(block, null);
    }
}
