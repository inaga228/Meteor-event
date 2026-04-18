package ru.inaga228.meteorevent.managers;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.inaga228.meteorevent.MeteorEvent;
import ru.inaga228.meteorevent.utils.LootGenerator;
import ru.inaga228.meteorevent.utils.MessageUtil;

import java.util.Random;

public class EventManager {

    private final MeteorEvent plugin;
    private final Random random = new Random();

    private boolean running = false;
    private Location chestLocation;
    private Block chestBlock;
    private BukkitTask meteorTask;
    private BukkitTask timerTask;
    private BukkitTask particleTask;
    private int timeLeft;

    public EventManager(MeteorEvent plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() {
        return running;
    }

    public void startEvent() {
        if (running) return;
        running = true;

        World world = Bukkit.getWorlds().get(0);
        chestLocation = findSafeLocation(world);

        // Ставим сундук
        chestBlock = chestLocation.getBlock();
        chestBlock.setType(Material.CHEST);

        // Наполняем лутом сразу (недоступно до открытия)
        Chest chest = (Chest) chestBlock.getState();
        LootGenerator.fillChest(chest.getInventory(), plugin.getConfig());
        chest.update();

        int timer = plugin.getConfig().getInt("event.chest-timer", 60);
        timeLeft = timer;

        // Оповещение
        String msg = plugin.getConfig().getString("messages.event-start", "&eИвент начался!")
                .replace("{x}", String.valueOf(chestLocation.getBlockX()))
                .replace("{y}", String.valueOf(chestLocation.getBlockY()))
                .replace("{z}", String.valueOf(chestLocation.getBlockZ()));
        MessageUtil.broadcast(msg);

        // Запускаем частицы вокруг сундука
        startParticleEffect();

        // Запускаем метеориты
        startMeteorShower();

        // Таймер
        startTimer();
    }

    private void startParticleEffect() {
        particleTask = new BukkitRunnable() {
            double angle = 0;

            @Override
            public void run() {
                if (!running || chestLocation == null) {
                    cancel();
                    return;
                }
                World world = chestLocation.getWorld();
                if (world == null) return;

                // Кольцо частиц вокруг сундука
                for (int i = 0; i < 12; i++) {
                    double a = angle + (Math.PI * 2 / 12) * i;
                    double x = chestLocation.getX() + Math.cos(a) * 3;
                    double z = chestLocation.getZ() + Math.sin(a) * 3;
                    Location particleLoc = new Location(world, x, chestLocation.getY() + 0.5, z);
                    world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
                    world.spawnParticle(Particle.CRIT_MAGIC, particleLoc, 1, 0, 0.3, 0, 0.1);
                }
                // Над сундуком
                world.spawnParticle(Particle.PORTAL, chestLocation.clone().add(0.5, 1.5, 0.5), 10, 0.5, 0.5, 0.5, 0.3);

                angle += 0.15;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void startMeteorShower() {
        int totalMetors = plugin.getConfig().getInt("event.meteor-count", 30);
        int timer = plugin.getConfig().getInt("event.chest-timer", 60);
        // Интервал между метеоритами в тиках
        long interval = Math.max(10L, (timer * 20L) / totalMetors);

        meteorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running || chestLocation == null) {
                    cancel();
                    return;
                }
                spawnMeteor();
            }
        }.runTaskTimer(plugin, 20L, interval);
    }

    private void spawnMeteor() {
        if (chestLocation == null) return;
        World world = chestLocation.getWorld();
        if (world == null) return;

        int radius = plugin.getConfig().getInt("event.meteor-radius", 15);
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = random.nextDouble() * radius;

        double targetX = chestLocation.getX() + Math.cos(angle) * dist;
        double targetZ = chestLocation.getZ() + Math.sin(angle) * dist;
        double targetY = world.getHighestBlockYAt((int) targetX, (int) targetZ) + 1;

        // Старт высоко в небе
        double startY = targetY + 50 + random.nextInt(30);
        Location start = new Location(world, targetX + random.nextInt(10) - 5, startY, targetZ + random.nextInt(10) - 5);
        Location target = new Location(world, targetX, targetY, targetZ);

        float explosionPower = (float) plugin.getConfig().getDouble("event.meteor-explosion-power", 2.5);

        // Анимация падения метеорита
        new BukkitRunnable() {
            final Location current = start.clone();
            final double stepX = (target.getX() - start.getX()) / 20.0;
            final double stepY = (target.getY() - start.getY()) / 20.0;
            final double stepZ = (target.getZ() - start.getZ()) / 20.0;
            int steps = 0;

            @Override
            public void run() {
                if (!running) { cancel(); return; }

                current.add(stepX, stepY, stepZ);
                steps++;

                // Частицы хвоста
                world.spawnParticle(Particle.FLAME, current, 5, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.SMOKE_LARGE, current, 3, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.LAVA, current, 2, 0.1, 0.1, 0.1, 0);

                if (steps >= 20) {
                    // Взрыв при приземлении
                    world.createExplosion(target, explosionPower, false, false);
                    world.spawnParticle(Particle.EXPLOSION_HUGE, target, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.FLAME, target, 30, 1, 1, 1, 0.1);
                    world.spawnParticle(Particle.SMOKE_LARGE, target, 20, 0.5, 0.5, 0.5, 0.05);
                    // Звук
                    world.playSound(target, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Звук предупреждения при появлении метеорита
        world.playSound(start, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 0.5f);
    }

    private void startTimer() {
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }

                timeLeft--;

                // Оповещение каждые 15 секунд и в последние 5
                if (timeLeft > 0 && (timeLeft % 15 == 0 || timeLeft <= 5)) {
                    MessageUtil.broadcast("&6[&cМетеорит&6] &eСундук откроется через: &c" + timeLeft + " сек.");
                }

                if (timeLeft <= 0) {
                    openChest();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void openChest() {
        if (chestLocation == null) return;
        World world = chestLocation.getWorld();
        if (world == null) return;

        float finalExplosion = (float) plugin.getConfig().getDouble("event.final-explosion-power", 4.0);

        // Останавливаем метеориты и частицы
        if (meteorTask != null) meteorTask.cancel();
        if (particleTask != null) particleTask.cancel();

        // Финальный взрыв
        world.createExplosion(chestLocation, finalExplosion, false, false);
        world.spawnParticle(Particle.EXPLOSION_HUGE, chestLocation, 5, 1, 1, 1, 0);
        world.spawnParticle(Particle.FIREWORKS_SPARK, chestLocation, 100, 2, 2, 2, 0.3);
        world.playSound(chestLocation, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
        world.playSound(chestLocation, Sound.BLOCK_CHEST_OPEN, 1f, 0.8f);

        // Сообщение
        String msg = plugin.getConfig().getString("messages.event-end", "&eСундук открылся!");
        MessageUtil.broadcast(msg);

        // Помечаем сундук открытым (оставляем на месте для лута)
        // Сундук уже наполнен, просто показываем что он "открылся" визуально
        world.spawnParticle(Particle.TOTEM, chestLocation.clone().add(0.5, 1, 0.5), 80, 0.5, 1, 0.5, 0.5);

        // Запускаем частицы обозначения сундука после открытия
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (chestLocation == null || chestBlock == null || chestBlock.getType() != Material.CHEST) {
                    running = false;
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.END_ROD, chestLocation.clone().add(0.5, 2, 0.5), 3, 0.1, 0.3, 0.1, 0.05);
                ticks += 5;
                if (ticks >= 600) { // 30 секунд
                    // Убираем сундук через 30 секунд после открытия
                    chestBlock.setType(Material.AIR);
                    running = false;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    public void stopEvent(boolean announce) {
        if (!running) return;

        if (meteorTask != null) { meteorTask.cancel(); meteorTask = null; }
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        if (particleTask != null) { particleTask.cancel(); particleTask = null; }

        if (chestBlock != null && chestBlock.getType() == Material.CHEST) {
            chestBlock.setType(Material.AIR);
        }

        running = false;
        chestLocation = null;
        chestBlock = null;

        if (announce) {
            String msg = plugin.getConfig().getString("messages.event-stopped", "&cИвент остановлен.");
            MessageUtil.broadcast(msg);
        }
    }

    private Location findSafeLocation(World world) {
        int radius = plugin.getConfig().getInt("event.spawn-radius", 200);
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x, y + 1, z);
            // Проверяем что не в воде и не в лаве
            if (loc.getBlock().getType() == Material.AIR
                    && loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                return loc;
            }
        }
        // Если не нашли — спавн у мирового нуля
        return new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0);
    }
}
