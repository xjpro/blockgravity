package blockgravity.listener;

import blockgravity.BlockGravityPlugin;
import blockgravity.service.FallingBlockService;
import blockgravity.service.SupportCheckService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

public class BlockListener implements Listener {

	private final Plugin plugin;
	private final SupportCheckService supportCheckService = new SupportCheckService();
	private final FallingBlockService fallingBlockService;

	public BlockListener(BlockGravityPlugin plugin) {
		this.plugin = plugin;
		this.fallingBlockService = new FallingBlockService(plugin);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) return;

		Block placed = event.getBlock();
		if (!supportCheckService.isSupportiveBlock(placed)) {
			// Not a block that can be built upon, we don't care if it's supported
			return;
		}
		if (supportCheckService.isSupported(placed) || supportCheckService.isSupportedByNeighbors(placed)) {
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
		fallingBlockService.handleBlockRemoved(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onBlockBurn(BlockBurnEvent event) {
		if (event.isCancelled()) return;
		fallingBlockService.handleBlockRemoved(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled()) return;
		event.blockList().forEach(fallingBlockService::handleBlockRemoved);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getMaterial() == Material.FLINT_AND_STEEL) {
			Block clicked = event.getClickedBlock();
			if (clicked.getType() == Material.TNT) {
				fallingBlockService.handleBlockRemoved(clicked);
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

		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> fallingBlockService.checkAndProcessUnsupportedBlocks(extendedBlocks, null), 4);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		if (event.isCancelled()) return;
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> fallingBlockService.handleBlockRemoved(event.getBlock().getRelative(event.getDirection())), 4);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onBlockChanged(EntityChangeBlockEvent event) {
		if (event.isCancelled()) return;

		// Anytime a block is converting to air, handle it as a removed block
		if (event.getTo() == Material.AIR) {
			// Allow this to become air before processing it
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> fallingBlockService.handleBlockRemoved(event.getBlock()), 0);
		}
		// There are also special cases where a falling block will be converting to a block that we will not allow
		else if (event.getEntityType() == EntityType.FALLING_BLOCK && !supportCheckService.isSupported(event.getBlock())) {
			// If the falling block is going to land on a non-supportive block, spawn an item in its place instead
			event.setCancelled(true);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0, 0.5), new ItemStack(event.getTo()));
		}
	}
}
