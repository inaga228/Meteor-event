package ru.inaga228.meteorevent;

import org.bukkit.plugin.java.JavaPlugin;
import ru.inaga228.meteorevent.commands.MeteorCommand;
import ru.inaga228.meteorevent.managers.EventManager;

public class MeteorEvent extends JavaPlugin {

    private static MeteorEvent instance;
    private EventManager eventManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        eventManager = new EventManager(this);

        getCommand("meteorevent").setExecutor(new MeteorCommand(this));

        // Автозапуск по интервалу
        int interval = getConfig().getInt("event.auto-start-interval", 0);
        if (interval > 0) {
            getServer().getScheduler().runTaskTimer(this, () -> {
                if (!eventManager.isRunning()) {
                    eventManager.startEvent();
                }
            }, interval * 20L, interval * 20L);
        }

        getLogger().info("MeteorEvent плагин включён!");
    }

    @Override
    public void onDisable() {
        if (eventManager != null && eventManager.isRunning()) {
            eventManager.stopEvent(false);
        }
        getLogger().info("MeteorEvent плагин выключён.");
    }

    public static MeteorEvent getInstance() {
        return instance;
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}
