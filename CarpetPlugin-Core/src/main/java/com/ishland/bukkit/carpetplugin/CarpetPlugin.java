package com.ishland.bukkit.carpetplugin;

import com.ishland.bukkit.carpetplugin.commands.ListCommand;
import com.ishland.bukkit.carpetplugin.commands.PlayerCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CarpetPlugin extends JavaPlugin {

    public static CarpetPlugin instance;
    private PlayerCommand playerCommand;
    private ListCommand listCommand;

    @Override
    public void onEnable() {
        instance = this;
        playerCommand = new PlayerCommand();
        playerCommand.register();
        listCommand = new ListCommand();
        listCommand.register();
    }

    @Override
    public void onDisable() {
        this.getLogger().warning("If this is a reload, restart your server ASAP");
        playerCommand.shutdown();
        instance = null;
    }
}
