package blockgravity.service;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Arrays;
import java.util.List;

public class SupportCheckService {

	static class BlockSupport {
		final BlockFace direction;
		final List<BlockFace> assistingDirections;

		BlockSupport(BlockFace direction, List<BlockFace> assistingDirections) {
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

	/*
	block - block to check if supported by neighbors
	destroyedBlock - the block being destroyed, treat as air
	 */
	boolean isSupportedByNeighbors(Block block, Block destroyedBlock) {
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

	public boolean isSupportedByNeighbors(Block block) {
		return isSupportedByNeighbors(block, null);
	}

	/*
	block - block to check if supported
	destroyedBlock - the block being destroyed, treat as air
	 */
	boolean isSupported(Block block, Block destroyed) {
		Block belowBlock = block.getRelative(BlockFace.DOWN);
		if (destroyed != null && destroyed.getLocation().equals(belowBlock.getLocation())) {
			return false; // Treat destroyedBlock as air
		}
		return isSupportiveBlock(belowBlock);
	}

	public boolean isSupported(Block block) {
		return isSupported(block, null);
	}

	// A block is "supportive" (it can be built upon or next to) when it is solid and does not become an entity (like a door)
	public boolean isSupportiveBlock(Block block) {
		if (!block.getType().isSolid()) return false; // Cannot be supportive if not solid

		// Some blocks are solid but we won't allow them to support other blocks
		switch (block.getType()) {
			case ACACIA_DOOR:
			case DARK_OAK_DOOR:
			case BIRCH_DOOR:
			case JUNGLE_DOOR:
			case SPRUCE_DOOR:
			case IRON_DOOR:
			case OAK_DOOR:
				// These are blocks that become entities when the block below them is removed
				return false;
			default:
				return true;
		}
	}
}
