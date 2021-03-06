package bskyblock.addon.warps;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.bukkit.World;

import bskyblock.addon.warps.commands.WarpCommand;
import bskyblock.addon.warps.commands.WarpsCommand;
import bskyblock.addon.warps.config.PluginConfig;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.addons.Addon;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.util.Util;

import bskyblock.addon.acidisland.AcidIsland;

/**
 * Addin to BSkyBlock that enables welcome warp signs
 * @author tastybento
 *
 */
public class Warp extends Addon {

    private static final String BSKYBLOCK_LEVEL = "BSkyBlock-Level";

    // The BSkyBlock plugin instance.
    private BSkyBlock plugin;

    // Warp panel object
    private WarpPanelManager warpPanelManager;

    // Warps signs object
    private WarpSignsManager warpSignsManager;

    // Level addon
    private Optional<Addon> levelAddon;

    private Set<World> registeredWorlds;

    @Override
    public void onEnable() {
        // Load the plugin's config
        new PluginConfig(this);
        // Get the BSkyBlock plugin. This will be available because this plugin depends on it in plugin.yml.
        plugin = this.getBSkyBlock();
        // Check if it is enabled - it might be loaded, but not enabled.
        if (!plugin.isEnabled()) {
            this.setEnabled(false);
            return;
        }
        // We have to wait for the worlds to load, so we do the rest 1 tick later
        getServer().getScheduler().runTask(this.getBSkyBlock(), () -> {
            registeredWorlds = new HashSet<>();
            // Start warp signs
            warpSignsManager = new WarpSignsManager(this, plugin);
            warpPanelManager = new WarpPanelManager(this);
            // Load the listener
            getServer().getPluginManager().registerEvents(new WarpSignsListener(this, plugin), plugin);
            // Register commands
            CompositeCommand bsbIslandCmd = (CompositeCommand) BSkyBlock.getInstance().getCommandsManager().getCommand("island");
            new WarpCommand(this, bsbIslandCmd);
            new WarpsCommand(this, bsbIslandCmd);
            registeredWorlds.add(plugin.getIWM().getIslandWorld());
            // AcidIsland hook in
            getServer().getScheduler().runTask(getBSkyBlock(), () -> {
                this.getBSkyBlock().getAddonsManager().getAddonByName("AcidIsland").ifPresent(acidIsland -> {
                    CompositeCommand acidIslandCmd = getBSkyBlock().getCommandsManager().getCommand("ai");
                    new WarpCommand(this, acidIslandCmd);
                    new WarpsCommand(this, acidIslandCmd);
                    registeredWorlds.add(((AcidIsland)acidIsland).getAiw().getOverWorld());
                });
            });
            
            // Get the level addon if it exists
            setLevelAddon(getBSkyBlock().getAddonsManager().getAddonByName(BSKYBLOCK_LEVEL));
        });
        // Done
    }

    @Override
    public void onDisable(){
        // Save the warps
        if (warpSignsManager != null)
            warpSignsManager.saveWarpList();
    }

    /**
     * Get warp panel manager
     * @return
     */
    public WarpPanelManager getWarpPanelManager() {
        return warpPanelManager;
    }

    public WarpSignsManager getWarpSignsManager() {
        return warpSignsManager;
    }

    public Optional<Addon> getLevelAddon() {
        return levelAddon;
    }

    public void setLevelAddon(Optional<Addon> levelAddon) {
        this.levelAddon = levelAddon;
    }

    public String getPermPrefix(World world) {
        this.getBSkyBlock().getIWM().getPermissionPrefix(world);
        return null;
    }

    /**
     * Check if an event is in a registered world
     * @param world - world to check
     * @return true if it is
     */
    public boolean inRegisteredWorld(World world) {
        return registeredWorlds.contains(Util.getWorld(world));
    }

}
