package com.diiegoof.cct;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final CCTPlugin plugin;

    // Lista de subcomandos disponibles
    private static final String[] COMMANDS = { "help", "reload", "toggle" };

    public CommandManager(CCTPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("cct.admin")) {
            return null;
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList(COMMANDS), completions);
        }

        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("cct.admin")) {
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfigs();

            String reloadMessage = plugin.getLangConfig().getString("reload_plugin", "&c[CCT] &7Plugin restarted!");

            sender.sendMessage(colorize(reloadMessage));
            return true;
        }

        sendHelpMessage(sender);
        return true;
    }

    /**
     * Envía el menú de ayuda al CommandSender.
     * @param sender El administrador o consola que ejecuta el comando.
     */
    private void sendHelpMessage(CommandSender sender) {
        FileConfiguration lang = plugin.getLangConfig();

        // 1. Cabecera
        sender.sendMessage(colorize(lang.getString("help_header")));

        // 2. Comandos
        sender.sendMessage(colorize(lang.getString("help_command_help")));
        sender.sendMessage(colorize(lang.getString("help_command_reload")));
        sender.sendMessage(colorize(lang.getString("help_command_toggle")));

        // 3. Pie de página
        sender.sendMessage(colorize(lang.getString("help_footer")));
    }

    /**
     * Helper para convertir códigos de color de Minecraft (&) a ChatColor.
     * @param message El mensaje con códigos de color.
     * @return El mensaje con colores funcionales.
     */
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}