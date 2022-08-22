package me.nc.plugins.bedrockminer;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class BedrockMiner extends JavaPlugin {
    long baseTime;
    Set<Material> allowedTools;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        allowedTools = new HashSet<>();
        for (String name : getConfig().getStringList("allowed-tools")) {
            allowedTools.add(Material.valueOf(name));
        }
        baseTime = getConfig().getLong("base-time", 200);
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(this));
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
    }
}
