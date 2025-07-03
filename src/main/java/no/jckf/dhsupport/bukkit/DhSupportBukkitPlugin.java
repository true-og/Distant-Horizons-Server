/*
 * DH Support, server-side support for Distant Horizons.
 * Copyright (C) 2024 Jim C K Flaten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package no.jckf.dhsupport.bukkit;

import no.jckf.dhsupport.bukkit.commands.DhCommand;
import no.jckf.dhsupport.bukkit.commands.DhsCommand;
import no.jckf.dhsupport.bukkit.handler.ConfigLoader;
import no.jckf.dhsupport.bukkit.handler.PlayerHandler;
import no.jckf.dhsupport.bukkit.handler.PluginMessageProxy;
import no.jckf.dhsupport.bukkit.handler.WorldHandler;
import no.jckf.dhsupport.core.DhSupport;
import no.jckf.dhsupport.core.configuration.DhsConfig;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.InputStreamReader;

public class DhSupportBukkitPlugin extends JavaPlugin
{
    protected DhSupport dhSupport;

    protected Metrics metrics;

    protected ConfigLoader configLoader;

    protected PluginMessageProxy pluginMessageProxy;

    protected BukkitScheduler scheduler;

    @Override
    public void onEnable()
    {
        String pluginVersion = "N/A";
        String mcVersion = "N/A";

        try {
            YamlConfiguration meta = new YamlConfiguration();
            meta.load(new InputStreamReader(this.getResource("plugin.yml")));

            pluginVersion = meta.getString("version");
            mcVersion = meta.getString("mc-version");
        } catch (Exception exception) {

        }

        this.getLogger().info("I am DHS " + pluginVersion + " for MC " + mcVersion + ".");

        this.dhSupport = new DhSupport(pluginVersion, mcVersion);
        this.dhSupport.setLogger(this.getLogger());
        this.dhSupport.setDataDirectory(this.getDataFolder().getAbsolutePath());

        this.metrics = new Metrics(this, 21843);

        this.loadDhsConfig();

        this.pluginMessageProxy = new PluginMessageProxy(this);
        this.pluginMessageProxy.onEnable();

        this.dhSupport.onEnable();

        this.scheduler = new BukkitScheduler(this);
        this.dhSupport.setScheduler(this.scheduler);

        int lodRefreshInterval = this.getDhSupport().getConfig().getInt(DhsConfig.LOD_REFRESH_INTERVAL) * 20;

        this.scheduler.runTimer(() -> {
            this.dhSupport.updateTouchedLods();
            this.dhSupport.printGenerationCount();
        }, lodRefreshInterval, lodRefreshInterval);

        this.getServer().getPluginManager().registerEvents(new WorldHandler(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerHandler(this), this);

        this.getCommand("dhs").setExecutor(new DhsCommand(this));
        this.getCommand("dh").setExecutor(new DhCommand(this));

        this.getLogger().info("Ready!");
    }

    @Override
    public void onDisable()
    {
        if (this.scheduler != null) {
            this.scheduler.cancelTasks();
        }

        if (this.pluginMessageProxy != null) {
            this.pluginMessageProxy.onDisable();
            this.pluginMessageProxy = null;
        }

        if (this.configLoader != null) {
            this.configLoader.onDisable();
            this.configLoader = null;
        }

        if (this.dhSupport != null) {
            this.dhSupport.onDisable();
            this.dhSupport = null;
        }

        this.getLogger().info("Lights out!");
    }

    public void loadDhsConfig()
    {
        if (this.configLoader != null) {
            this.configLoader.onDisable();
        }

        this.configLoader = new ConfigLoader(this);
        this.configLoader.onEnable();
    }

    public @Nullable World getWorld(String name)
    {
        for (World world : this.getServer().getWorlds()) {
            if (world.getName().replace(' ', '_').equals(name)) {
                return world;
            }
        }

        return null;
    }

    @Nullable
    public DhSupport getDhSupport()
    {
        return this.dhSupport;
    }
}
