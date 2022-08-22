package me.nc.plugins.bedrockminer;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerDigType;

public class PacketListener extends PacketAdapter {
    private BedrockMiner plugin;
    private Map<Player, Integer> players;

    PacketListener(BedrockMiner plugin) {
        super(plugin, PacketType.Play.Client.BLOCK_DIG);
        this.plugin = plugin;
        this.players = new HashMap<>();
    }

    private void stopDigging(final BlockPosition position, final Player player) {
        if (players.containsKey(player)) {
            Bukkit.getScheduler().cancelTask(players.remove(player));
            new BukkitRunnable() {

                @Override
                public void run() {
                    PacketUtils.broadcastBlockBreakAnimationPacket(position, -1);
                }
            }.runTaskLater(plugin, 1);
        }
    }

    private void breakBlock(Block block, BlockPosition position, Player player) {
        BlockBreakEvent breakEvt = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(breakEvt);
        if (!breakEvt.isCancelled()) {
            block.breakNaturally();
            PacketUtils.broadcastBlockBreakEffectPacket(position, Material.BEDROCK);
        }
    }

    public void onPacketReceiving(PacketEvent evt) {
        final Player player = evt.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        final BlockPosition position = evt.getPacket().getBlockPositionModifier().read(0);
        PlayerDigType type = evt.getPacket().getPlayerDigTypes().read(0);
        switch (type) {
            case ABORT_DESTROY_BLOCK:
            case STOP_DESTROY_BLOCK:
                stopDigging(position, player);
                break;
            case START_DESTROY_BLOCK:
                if (position.getY() < 1) {
                    return;
                }
                Location location = position.toLocation(player.getWorld());
                if (!location.getChunk().isLoaded() || location.getBlock().getType() != Material.BEDROCK) {
                    return;
                }
                players.put(player, new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            stopDigging(position, player);
                            return;
                        }
                        ItemStack inHand = player.getItemInHand();
                        if (!plugin.allowedTools.contains(inHand.getType())) {
                            return;
                        }
                        ticks += 5;
                        int stage;
                        long ticksPerStage = Math.round(plugin.baseTime / Math.pow(1.3, inHand.getEnchantmentLevel(Enchantment.DIG_SPEED)) / 9);
                        Block block = position.toLocation(player.getWorld()).getBlock();
                        if (block.getType() == Material.BEDROCK && ticksPerStage != 0 && (stage = (int) (ticks / ticksPerStage)) <= 9) {
                            PacketUtils.broadcastBlockBreakAnimationPacket(position, stage);
                        } else {
                            stopDigging(position, player);
                            if (block.getType() == Material.BEDROCK)
                                breakBlock(block, position, player);
                        }
                    }
                }.runTaskTimer(plugin, 0, 5).getTaskId());
                break;
        }
    }
}
