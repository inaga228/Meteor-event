package ru.inaga228.meteorevent.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.inaga228.meteorevent.MeteorEvent;
import ru.inaga228.meteorevent.utils.MessageUtil;

public class MeteorCommand implements CommandExecutor {

    private final MeteorEvent plugin;

    public MeteorCommand(MeteorEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("meteorevent.admin")) {
            sender.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.no-permission", "&cНет прав.")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.usage", "&eИспользование: /meteorevent <start|stop|reload>")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (plugin.getEventManager().isRunning()) {
                    sender.sendMessage(MessageUtil.color(
                            plugin.getConfig().getString("messages.already-running", "&cИвент уже запущен!")));
                } else {
                    plugin.getEventManager().startEvent();
                    sender.sendMessage(MessageUtil.color("&aИвент запущен!"));
                }
                break;

            case "stop":
                if (!plugin.getEventManager().isRunning()) {
                    sender.sendMessage(MessageUtil.color(
                            plugin.getConfig().getString("messages.not-running", "&cИвент не запущен!")));
                } else {
                    plugin.getEventManager().stopEvent(true);
                    sender.sendMessage(MessageUtil.color("&aИвент остановлен."));
                }
                break;

            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(MessageUtil.color(
                        plugin.getConfig().getString("messages.reloaded", "&aКонфиг перезагружен!")));
                break;

            default:
                sender.sendMessage(MessageUtil.color(
                        plugin.getConfig().getString("messages.usage", "&eИспользование: /meteorevent <start|stop|reload>")));
                break;
        }

        return true;
    }
}
