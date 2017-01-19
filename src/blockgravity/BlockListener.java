package blockgravity;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockListener implements Listener {

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

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockCanBuild(BlockCanBuildEvent event) {

		Block placed = event.getBlock();
		if (!placed.getType().isSolid() || placed.getType() == Material.BEDROCK) {
			// Not a solid block that can be built upon, we don't care if it's supported
			return;
		}
		if (isSupported(placed) || isSupportedByNeighbors(placed)) {
			// This block is supported
			return;
		}

		// If we got to here, this block is not supported
		event.setBuildable(false);
		// print message?
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockBurn(BlockBurnEvent event) {
		handleBlockRemoved(event.getBlock(), null);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockRemove(BlockBreakEvent event) {
		handleBlockRemoved(event.getBlock(), event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityExplode(EntityExplodeEvent event) {
		event.blockList().stream().forEach((destroyed) -> {
			handleBlockRemoved(destroyed, null); // todo can we get the player who set off TNT?
		});
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial() == Material.FLINT_AND_STEEL) {
			Block clicked = event.getClickedBlock();
			if (clicked.getType() == Material.TNT) {
				handleBlockRemoved(clicked, event.getPlayer());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("BlockGravity"),
				() -> {
					handleBlockRemoved(event.getBlock(), null);
				}, 4);
	}

	private void handleBlockRemoved(Block destroyed, Player player) {
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
				.filter((block) -> (block.getType() != Material.AIR && block.getType() != Material.BEDROCK
						&& !isSupported(block, destroyed) && !isSupportedByNeighbors(block, destroyed)))
				.forEach((block) -> {
					// This block is no longer supported - spawn a falling block in its place and remove it
					block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5), block.getType(), block.getData());
					block.setType(Material.AIR);
					Bukkit.getServer().getPluginManager().callEvent(new BlockBreakEvent(block, player));
				});
	}

	/*
	block - block to check if supported by neighbors
	destroyedBlock - the block being destroyed, treat as air
	 */
	private boolean isSupportedByNeighbors(Block block, Block destroyedBlock) {
		for (BlockSupport support : supportingDirections) {

			// Distance 1
			Block checking = block.getRelative(support.direction, 1);
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
			// Allow diagonal blocks to be the support they are within 1 distance
			Block diagonalBlock = block.getRelative(support.assistingDirections.get(0), 1);
			if (diagonalBlock.getType().isSolid() && isSupported(diagonalBlock, destroyedBlock)) {
				return true;
			}
			diagonalBlock = block.getRelative(support.assistingDirections.get(1), 1);
			if (diagonalBlock.getType().isSolid() && isSupported(diagonalBlock, destroyedBlock)) {
				return true;
			}
			diagonalBlock = block.getRelative(support.assistingDirections.get(2), 1);
			if (diagonalBlock.getType().isSolid() && isSupported(diagonalBlock, destroyedBlock)) {
				return true;
			}
			diagonalBlock = block.getRelative(support.assistingDirections.get(3), 1);
			if (diagonalBlock.getType().isSolid() && isSupported(diagonalBlock, destroyedBlock)) {
				return true;
			}

			// Distance 2
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

			// Distance 3
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

	class BlockSupport {

		final BlockFace direction;
		final List<BlockFace> assistingDirections;

		BlockSupport(BlockFace direction, List<BlockFace> assistingDirections) {
			this.direction = direction;
			this.assistingDirections = assistingDirections; // must be length 4
		}
	}
}
