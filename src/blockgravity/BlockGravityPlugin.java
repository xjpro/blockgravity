package blockgravity;

import blockgravity.listener.BlockListener;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockGravityPlugin extends JavaPlugin {

	@Override
	public void onEnable() {
		PluginManager manager = this.getServer().getPluginManager();
		manager.registerEvents(new BlockListener(this), this);
	}

	public void logFallingBlock(Block block) {
		if (getCoreProtect() != null) {
			getCoreProtect().logRemoval("BlockGravityPlugin", block.getLocation(), block.getType(), block.getData());
		}
	}

	public void logLandingBlock(Block block, Material to) {
		if (getCoreProtect() != null) {
			getCoreProtect().logPlacement("BlockGravityPlugin", block.getLocation(), to, block.getData());
		}
	}

	private CoreProtectAPI getCoreProtect() {
		Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

		// Check that CoreProtect is loaded
		if (!(plugin instanceof CoreProtect)) {
			return null;
		}

		// Check that the API is enabled
		CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
		if (!CoreProtect.isEnabled()) {
			return null;
		}

		// Check that a compatible version of the API is loaded
		if (CoreProtect.APIVersion() < 4) {
			return null;
		}

		return CoreProtect;
	}
}
