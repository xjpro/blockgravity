package blockgravity;

import blockgravity.listener.BlockListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockGravityPlugin extends JavaPlugin {

	@Override
	public void onEnable() {
		PluginManager manager = this.getServer().getPluginManager();
		manager.registerEvents(new BlockListener(this), this);
	}
}
