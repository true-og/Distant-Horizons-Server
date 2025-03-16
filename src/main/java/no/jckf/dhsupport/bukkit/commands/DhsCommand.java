package no.jckf.dhsupport.bukkit.commands;

import no.jckf.dhsupport.bukkit.DhSupportBukkitPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DhsCommand implements CommandExecutor
{
    protected DhSupportBukkitPlugin plugin;

    public DhsCommand(DhSupportBukkitPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        if (args.length < 1) {
            return false;
        }

        switch (args[0]) {
            case "reload":
                return this.reload(sender);
        }

        return false;
    }

    protected boolean reload(CommandSender sender)
    {
        sender.sendMessage("Reloading config...");

        this.plugin.reloadConfig();

        sender.sendMessage("Reload complete.");

        return true;
    }
}
