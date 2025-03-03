package no.jckf.dhsupport.bukkit.handler;

import no.jckf.dhsupport.bukkit.DhSupportBukkitPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerHandler implements Listener
{
    protected DhSupportBukkitPlugin plugin;

    public PlayerHandler(DhSupportBukkitPlugin plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event)
    {
        this.plugin.getDhSupport().clearPlayerConfiguration(event.getPlayer().getUniqueId());
    }
}
