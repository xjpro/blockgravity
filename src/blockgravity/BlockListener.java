package blockgravity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class BlockListener implements Listener {

	private static int MAXIMUIM_FALLS_PER_TICK = 10;

	private final Plugin plugin;
	private final Stack<Block> blocksToCheck = new Stack<>();
	private int processingTaskId = 0;

	BlockListener(Plugin plugin) {
		this.plugin = plugin;
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) return;

		Block placed = event.getBlock();
		if (!isSupportiveBlock(placed)) {
			// Not a block that can be built upon, we don't care if it's supported
			return;
		}
		if (isSupported(placed) || isSupportedByNeighbors(placed)) {
			// This block is supported
			return;
		}

		// If we got to here, this block is not supported
		event.setBuild(false);
		event.setCancelled(true);
		// print message?
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onBlockRemove(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		handleBlockRemoved(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onBlockBurn(BlockBurnEvent event) {
		if (event.isCancelled()) return;
		handleBlockRemoved(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled()) return;
		event.blockList().forEach(this::handleBlockRemoved);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial() == Material.FLINT_AND_STEEL) {
			Block clicked = event.getClickedBlock();
			if (clicked.getType() == Material.TNT) {
				handleBlockRemoved(clicked);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		if (event.isCancelled()) return;

		ArrayList<Block> extendedBlocks = new ArrayList<>();
		for (int i = 2; i <= 13; i++) {
			extendedBlocks.add(event.getBlock().getRelative(event.getDirection(), i));
		}

		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> checkAndProcessUnsupportedBlocks(extendedBlocks, null), 4);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		if (event.isCancelled()) return;
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> handleBlockRemoved(event.getBlock().getRelative(event.getDirection())), 4);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onBlockChanged(EntityChangeBlockEvent event) {
		if (event.isCancelled()) return;

		// Anytime a block is converting to air, handle it as a removed block
		if (event.getTo() == Material.AIR) {
			// Allow this to become air before processing it
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> handleBlockRemoved(event.getBlock()), 0);
		}
		// There are also special cases where a falling block will be converting to a block that we will not allow
		else if (event.getEntityType() == EntityType.FALLING_BLOCK && !isSupported(event.getBlock())) {
			// If the falling block is going to land on a non-supportive block, spawn an item in its place instead
			event.setCancelled(true);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0, 0.5), new ItemStack(event.getTo()));
		}
	}

	private void handleBlockRemoved(Block destroyed) {
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
		checkAndProcessUnsupportedBlocks(surroundingBlocks, destroyed);
	}

	// Check a list of blocks for gravity
	private void checkAndProcessUnsupportedBlocks(List<Block> blocks, Block destroyed) {
		blocks
				.stream()
				// Select the surrounding blocks which are above bottom of world and can be built upon
				.filter(block -> block.getY() > 0 && isSupportiveBlock(block))
				// Of those blocks, filter further to those which are not supported directly or by neighbors
				.filter(block -> !isSupported(block, destroyed) && !isSupportedByNeighbors(block, destroyed))
				// Surrounding blocks meeting the above conditions should spawn falling blocks in their place
				.forEach(blocksToCheck::add);

		processQueuedFallingBlocks(); // We've added blocks, spin up the processing task
	}

	@SuppressWarnings("deprecation")
	private void processQueuedFallingBlocks() {
		if (processingTaskId == 0) return; // Already processing

		processingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
			Block block;
			int i = 0;
			while (i++ < MAXIMUIM_FALLS_PER_TICK && (block = blocksToCheck.pop()) != null) {
				// This block is no longer supported - spawn a falling block in its place
				FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5), block.getType(), block.getData());
				// By calling a new EntityChangeBlockEvent we propagate the falling to surrounding blocks
				Bukkit.getServer().getPluginManager().callEvent(new EntityChangeBlockEvent(fallingBlock, block, Material.AIR, block.getData()));
				// We should always change the block's type after we've called the EntityChangeBlockEvent so its consistent with other Minecraft events
				block.setType(Material.AIR);

				// Remove the falling block entity for any blocks that should not land
				if (!isPermittedToLand(fallingBlock)) {
					fallingBlock.remove();
				}
			}

			// If we've run out of blocks to process, end the task and clear the task id
			if (blocksToCheck.size() == 0) {
				Bukkit.getScheduler().cancelTask(processingTaskId);
				processingTaskId = 0;
			}

			// Otherwise, the task will process the next batch on next tick
		}, 0, 1);
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
			if (!isSupportiveBlock(checking)) {
				continue; // this direction is broken by non-supportive block
			}
			// Possible support block is solid, but is it supported?
			if (isSupported(checking, destroyedBlock)) {
				return true;
			}
			// Allow diagonal blocks to be the support they are within 1 distance
			Block diagonalBlock = block.getRelative(support.assistingDirections.get(0), 1);
			if (isSupportiveBlock(diagonalBlock) && isSupported(diagonalBlock, destroyedBlock)) {
				return true;
			}
			diagonalBlock = block.getRelative(support.assistingDirections.get(1), 1);
			if (isSupportiveBlock(diagonalBlock) && isSupported(diagonalBlock, destroyedBlock)) {
				return true;
			}
			diagonalBlock = block.getRelative(support.assistingDirections.get(2), 1);
			if (isSupportiveBlock(diagonalBlock) && isSupported(diagonalBlock, destroyedBlock)) {
				return true;
			}
			diagonalBlock = block.getRelative(support.assistingDirections.get(3), 1);
			if (isSupportiveBlock(diagonalBlock) && isSupported(diagonalBlock, destroyedBlock)) {
				return true;
			}

			// Distance 2
			checking = block.getRelative(support.direction, 2);
			if (destroyedBlock != null && destroyedBlock.getLocation().equals(checking.getLocation())) {
				continue;
			}
			if (!isSupportiveBlock(checking)) {
				continue; // this direction is broken by non-supportive block
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
			if (!isSupportiveBlock(checking)) {
				continue; // this direction is broken by non-supportive block
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
		return isSupportiveBlock(belowBlock);
	}

	private boolean isSupported(Block block) {
		return isSupported(block, null);
	}

	// A block is "supportive" (it can be built upon or next to) when it is solid and does not become an entity (like a door)
	private boolean isSupportiveBlock(Block block) {
		if (!block.getType().isSolid()) return false; // Cannot be supportive if not solid

		// Some blocks are solid but we won't allow them to support other blocks
		switch (block.getType()) {
			case ACACIA_DOOR:
			case DARK_OAK_DOOR:
			case BIRCH_DOOR:
			case IRON_DOOR_BLOCK:
			case JUNGLE_DOOR:
			case SPRUCE_DOOR:
			case WOOD_DOOR:
			case WOODEN_DOOR:
				// These are blocks that become entities when the block below them is removed
				return false;
			default:
				return true;
		}
	}

	// Some falling blocks are of materials not normally acquirable by players
	private boolean isPermittedToLand(FallingBlock fallingBlock) {
		switch (fallingBlock.getMaterial()) {
			case BEDROCK:
				return false;
			default:
				return true;
		}
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
