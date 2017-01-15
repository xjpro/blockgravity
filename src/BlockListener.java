
//import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;

public class BlockListener implements Listener {

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

			if (!isSupported(originBlock) && !isSupported(originBlock.getRelative(BlockFace.NORTH))
					&&  !isSupported(originBlock.getRelative(BlockFace.EAST))
					&&  !isSupported(originBlock.getRelative(BlockFace.SOUTH))
					&&  !isSupported(originBlock.getRelative(BlockFace.WEST))) {
				cancelEvent(event);
			}
			break;
		}

	}

	private boolean isSupported(Block block) {
		if (block.getRelative(BlockFace.DOWN).getType() == Material.AIR) {
			// Not supported
			return false;
		}
		return true;
	}

	private void cancelEvent(BlockPlaceEvent event) {
		event.setCancelled(true);
		event.setBuild(false);
	}
}
