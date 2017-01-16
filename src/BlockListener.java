
import java.util.List;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;

import net.minecraft.server.v1_11_R1.BlockPosition;

public class BlockListener implements Listener {

	private List<BlockFace> supportingDirections = Arrays.asList(BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST,
			BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST);

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
			if (isSupported(originBlock, null) || isSupportedByNeighbors(originBlock, null)) {
				return; // This block is supported, take no action
			}
			break;
		}

		// If we got to here, this block is not supported
		// Cancel its creation and instead spawn a falling block of the same
		// type
		// originBlock.getWorld().spawnFallingBlock(originBlock.getLocation().add(0.5,
		// 0, 0.5), originBlock.getType(),
		// originBlock.getData());
		originBlock.breakNaturally();
		// event.setCancelled(true);
		// event.setBuild(false);
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
				Location location = block.getLocation();
				((CraftPlayer) event.getPlayer()).getHandle().playerInteractManager.breakBlock(new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
//				block.breakNaturally();
				// originBlock.getWorld().spawnFallingBlock(block.getLocation().add(0.5,
				// 0, 0.5), block.getType(),
				// block.getData());
				// block.setType(Material.AIR);
//				 Bukkit.getServer().getPluginManager().callEvent(new
//				 BlockBreakEvent(block, event.getPlayer()));
			}
		}

	}

	/*
	 * block - block to check if supported by neighbors
	 * destroyedBlock - the block being destroyed, treat as air
	 */
	private boolean isSupportedByNeighbors(Block block, Block destroyedBlock) {
		for (BlockFace direction : supportingDirections) {
			Block checking = block.getRelative(direction, 1);
			if(destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation()))
				continue;
			if (checking.getType() == Material.AIR)
				continue; // this direction is broken by air
			if (isSupported(checking, destroyedBlock))
				return true;

			checking = block.getRelative(direction, 2);
			if(destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation()))
				continue;
			if (checking.getType() == Material.AIR)
				continue; // this direction is broken by air
			if (isSupported(checking, destroyedBlock))
				return true;
			
//			checking = block.getRelative(direction, 3);
//			if(destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation()))
//				continue;
//			if (checking.getType() == Material.AIR)
//				continue; // this direction is broken by air
//			if (isSupported(checking))
//				return true;
		}
		return false;
	}

	/*
	 * block - block to check if supported
	 * destroyedBlock - the block being destroyed, treat as air
	 */
	private boolean isSupported(Block block, Block destroyedBlock) {
		Block checking = block.getRelative(BlockFace.DOWN);
		if(destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation())) return false;
		return checking.getType() != Material.AIR;
	}
}
