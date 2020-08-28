package com.ishland.bukkit.carpetplugin;

import com.ishland.bukkit.carpetplugin.commands.PlayerCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CarpetPlugin extends JavaPlugin {

    public static CarpetPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        new PlayerCommand().register();
    }

    @Override
    public void onDisable() {
        this.getLogger().warning("If this is a reload, restart your server ASAP");
        instance = null;
    }
}
