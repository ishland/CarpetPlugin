package com.ishland.bukkit.carpetplugin;

import com.ishland.bukkit.carpetplugin.commands.PlayerCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CarpetPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new PlayerCommand(), this);
    }

    @Override
    public void onDisable() {
        this.getLogger().warning("If this is a reload, restart your server ASAP");
    }
}
