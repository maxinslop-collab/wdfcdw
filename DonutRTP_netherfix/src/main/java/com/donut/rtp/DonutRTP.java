package com.donut.rtp;

import org.bukkit.plugin.java.JavaPlugin;

public class DonutRTP extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("rtp").setExecutor(new RTPCommand(this));
    }
}
