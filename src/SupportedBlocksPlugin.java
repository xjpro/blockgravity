import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.plugin.PluginManager;

public class SupportedBlocksPlugin extends JavaPlugin {
	@Override
	public void onEnable() {
		PluginManager manager = this.getServer().getPluginManager();
		manager.registerEvents(new BlockListener(), this);
	}
}
