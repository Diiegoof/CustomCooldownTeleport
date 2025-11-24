package com.diiegoof.cct;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.command.PluginCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownListener implements Listener {

    private final CCTPlugin plugin;
    private final Map<UUID, BukkitRunnable> runningTasks = new HashMap<>();
    private final Map<UUID, Location> initialLocation = new HashMap<>();

    public CooldownListener(CCTPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String fullCommand = event.getMessage().toLowerCase();

        String command = fullCommand.split(" ")[0].substring(1);

        PluginCommand pluginCommand = plugin.getServer().getPluginCommand(command);

        if (pluginCommand == null && !fullCommand.startsWith("/cct")) {
            return;
        }

        // 1. Check de Ignorar Permisos (CCT.ignore.all o CCT.ignore.[comando])
        if (player.hasPermission("cct.ignore.all") || player.hasPermission("cct.ignore." + command)) {
            return;
        }

        // 2. Determinar segundos y si aplica el contador
        int seconds = getCooldownSeconds(command);


        if (seconds > 0) {
            event.setCancelled(true);

            if (runningTasks.containsKey(player.getUniqueId())) {
                String message = plugin.getLangConfig().getString("global_message")
                        .replace("%seconds%", String.valueOf(runningTasks.get(player.getUniqueId()).getTaskId()));
                player.sendMessage(colorize(message));
                return;
            }

            // Iniciar la tarea de cuenta regresiva
            startCountdown(player, fullCommand, command, seconds);
        }
    }

    // --- LISTENERS DE CANCELACIÓN (Movimiento y Daño) ---

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (runningTasks.containsKey(playerUUID) && plugin.getConfig().getBoolean("movement", false) == false) {
            Location from = initialLocation.get(playerUUID);
            Location to = event.getTo();

            if (from != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
                cancelCooldown(playerUUID);
                player.sendMessage(colorize(plugin.getLangConfig().getString("movement_cancel")));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UUID playerUUID = player.getUniqueId();

            if (runningTasks.containsKey(playerUUID) && plugin.getConfig().getBoolean("movement", false) == false) {
                cancelCooldown(playerUUID);
                player.sendMessage(colorize(plugin.getLangConfig().getString("movement_cancel")));
            }
        }
    }

    private void cancelCooldown(UUID playerUUID) {
        BukkitRunnable task = runningTasks.get(playerUUID);
        if (task != null) {
            task.cancel();
        }
        runningTasks.remove(playerUUID);
        initialLocation.remove(playerUUID);
    }

    // --- LÓGICA DE TIEMPO ---

    private int getCooldownSeconds(String command) {
        FileConfiguration config = plugin.getConfig();
        boolean globalCommandActive = config.getBoolean("global_command", false);

        ConfigurationSection commandsSection = config.getConfigurationSection("commands");

        // ------------------------------------------
        // 1. Lógica del GLOBAL COMMAND (global_command: true)
        // ------------------------------------------
        if (globalCommandActive) {
            int globalSeconds = config.getInt("global_seconds", 5);

            if (config.getStringList("whitelist_commands").contains(command)) {
                return 0; // Está en la whitelist, NO aplica cooldown
            }

            return globalSeconds;
        }

        // ------------------------------------------
        // 2. Lógica de COMANDOS ESPECÍFICOS (global_command: false)
        // ------------------------------------------

        if (commandsSection == null) {
            return 0;
        }

        // Si global_command: false, SOLO buscamos los comandos listados en 'commands:'
        for (String commandLabel : commandsSection.getKeys(false)) {
            String commandPath = commandLabel + ".command";
            String commandName = commandsSection.getString(commandPath);

            if (commandName != null && commandName.equalsIgnoreCase(command)) {
                return commandsSection.getInt(commandLabel + ".seconds", 0);
            }
        }

        return 0;
    }

    /**
     * Inicia el temporizador de cuenta regresiva.
     */
    private void startCountdown(Player player, String fullCommand, String command, int seconds) {
        String originalMessage = fullCommand;
        UUID playerUUID = player.getUniqueId();

        if (plugin.getConfig().getBoolean("movement", false) == false) {
            initialLocation.put(playerUUID, player.getLocation());
        }

        BukkitRunnable task = new BukkitRunnable() {
            int counter = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelCooldown(playerUUID);
                    return;
                }

                if (counter <= 0) {
                    plugin.getServer().dispatchCommand(player, originalMessage.substring(1));
                    cancelCooldown(playerUUID);
                    return;
                }

                String message = plugin.getLangConfig().getString("global_message")
                        .replace("%seconds%", String.valueOf(counter));
                player.sendMessage(colorize(message));

                counter--;
            }
        };

        task.runTaskTimer(plugin, 10, 20);
        runningTasks.put(playerUUID, task);
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}