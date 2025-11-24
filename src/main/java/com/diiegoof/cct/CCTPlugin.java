package com.diiegoof.cct;

/*
 * GNU General Public License v3.0
 *
 * Copyright (c) 2025 Diego (Diiegoof)
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public class CCTPlugin extends JavaPlugin {

    private static CCTPlugin instance;
    private FileConfiguration langConfig = null;
    private File langFile = null;

    @Override
    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();

        this.saveAllLanguageFiles();

        this.loadLanguageFile();

        CommandManager manager = new CommandManager(this);
        this.getCommand("cct").setExecutor(manager);
        this.getCommand("cct").setTabCompleter(manager);

        this.getServer().getPluginManager().registerEvents(new CooldownListener(this), this);

        getLogger().info("Custom Cooldown Teleport has been activated.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Custom Cooldown Teleport has been disabled.");
    }

    // --- MÉTODOS DE GESTIÓN DE IDIOMA ---

    private void saveAllLanguageFiles() {
        // Lista de rutas de idioma
        String[] paths = new String[]{"language/language_en.yml", "language/language_es.yml"};

        for (String path : paths) {
            File file = new File(this.getDataFolder(), path);

            if (!file.exists()) {
                file.getParentFile().mkdirs();

                this.saveResource(path, false);
            }
        }
    }

    public void reloadConfigs() {
        this.reloadConfig();
        this.loadLanguageFile();
    }

    public void loadLanguageFile() {
        String languageCode = this.getConfig().getString("plugin_language", "en");
        String resourcePath = "language/language_" + languageCode + ".yml";

        this.langFile = new File(this.getDataFolder(), resourcePath);
        this.langConfig = YamlConfiguration.loadConfiguration(this.langFile);

        try {
            Reader defConfigStream = new InputStreamReader(this.getResource(resourcePath), "UTF8");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                this.langConfig.setDefaults(defConfig);
            }
        } catch (UnsupportedEncodingException e) {
            getLogger().severe("Error loading language file: " + e.getMessage());
        }
    }

    // --- GETTERS ---

    public static CCTPlugin getInstance() {
        return instance;
    }

    public FileConfiguration getLangConfig() {
        return this.langConfig;
    }
}