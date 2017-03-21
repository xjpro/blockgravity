package blockgravity.service;

import blockgravity.BlockGravityPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FallingBlockService {

	private static int MAX_FALLS_ON_EVENT = 10;
	private static int MAX_FALLS_PER_TICK = 100;

	private final BlockGravityPlugin plugin;
	private final SupportCheckService supportCheckService = new SupportCheckService();
	private final Stack<Block> fallingBlockQueue = new Stack<>();
	private int processingTaskId = 0;

	public FallingBlockService(BlockGravityPlugin plugin) {
		this.plugin = plugin;
	}

	// Check the area around a destroyed block for gravity
	public void handleBlockRemoved(Block destroyed) {
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
	public void checkAndProcessUnsupportedBlocks(List<Block> blocksToCheck, Block destroyed) {
		// Gather blocks that are no longer supported
		Block[] unsupportedBlocks = blocksToCheck
				.stream()
				// Select the surrounding blocks which are above bottom of world and can be built upon
				.filter(block -> block.getY() > 0 && supportCheckService.isSupportiveBlock(block))
				// todo filter out sand and gravel since they fall on their own?
				// Of those blocks, filter further to those which are not supported directly or by neighbors
				.filter(block -> !supportCheckService.isSupported(block, destroyed) && !supportCheckService.isSupportedByNeighbors(block, destroyed))
				.toArray(Block[]::new);

		// Iterate through unsupportedBlocks, making them fall immediately unless
		// our cap is exceeded in which case add them to queue to be processed later
		for (int i = 0; i < unsupportedBlocks.length; i++) {
			if (i < MAX_FALLS_ON_EVENT && fallingBlockQueue.size() < MAX_FALLS_ON_EVENT) {
				makeBlockFall(unsupportedBlocks[i]);
			} else {
				fallingBlockQueue.add(unsupportedBlocks[i]);
			}
		}

		processQueuedFallingBlocks(); // Spin up the processing task
	}

	private void processQueuedFallingBlocks() {
		if (fallingBlockQueue.size() == 0) return; // Nothing to process
		if (processingTaskId != 0) return; // Already processing

		processingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
			int i = 0;
			while (i++ < MAX_FALLS_PER_TICK && fallingBlockQueue.size() > 0) {
				makeBlockFall(fallingBlockQueue.pop());
			}

			// If we've run out of blocks to process, end the task and clear the task id
			if (fallingBlockQueue.size() == 0) {
				Bukkit.getScheduler().cancelTask(processingTaskId);
				processingTaskId = 0;
			}

			// Otherwise, the task will process the next batch on next tick
		}, 0, 0);
	}

	// Block is no longer supported - spawn a falling block in its place
	// todo appears blocks can be converted to sand if the player leaves the area?
	@SuppressWarnings("deprecation")
	private void makeBlockFall(Block block) {
		FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0, 0.5), block.getType(), block.getData());
		// Note that we create the falling block entity even in cases where we will be immediately removing it from
		// the game world (bedrock, leaves) because we need to pass it along in the EntityChangeBlockEvent

		// By calling a new EntityChangeBlockEvent we propagate falling to surrounding blocks
		Bukkit.getServer().getPluginManager().callEvent(new EntityChangeBlockEvent(fallingBlock, block, Material.AIR, block.getData()));
		// We should always change the block's type AFTER we've called the EntityChangeBlockEvent to be consistent with other Minecraft events
		plugin.logFallingBlock(block);
		block.setType(Material.AIR);

		if (shouldSpawnItemInsteadOfFalling(fallingBlock)) {
			// Drop an item instead of having a falling block
			block.getWorld().dropItemNaturally(fallingBlock.getLocation(), new ItemStack(fallingBlock.getMaterial()));
			fallingBlock.remove();
		} else {
			// A falling block has now taken its place, unless...
			if (shouldDisappearInsteadOfFalling(fallingBlock)) {
				// Remove the falling block entity for any blocks that should not land
				fallingBlock.remove();
			}
		}
	}

	// Some blocks should spawn an item when affected by gravity
	private boolean shouldSpawnItemInsteadOfFalling(FallingBlock fallingBlock) {
		return false;
	}

	// Some blocks should disappear from the game world when affected by gravity
	private boolean shouldDisappearInsteadOfFalling(FallingBlock fallingBlock) {
		switch (fallingBlock.getMaterial()) {
			case BEDROCK:
				return true;
			default:
				return false;
		}
	}
}
