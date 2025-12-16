package me.droptext;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DropFloatingText extends JavaPlugin implements Listener {

    private final Map<UUID, ArmorStand> holograms = new HashMap<>();
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("droptext").setExecutor((sender, cmd, label, args) -> {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "DropFloatingText config reloaded.");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Usage: /droptext reload");
            return true;
        });
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!getConfig().getBoolean("enabled")) return;

        Item item = e.getItemDrop();
        World world = item.getWorld();

        if (getConfig().getBoolean("worlds.whitelist")
                && !getConfig().getStringList("worlds.list").contains(world.getName())) {
            return;
        }

        spawnHologram(item);
    }

    @EventHandler
    public void onMerge(ItemMergeEvent e) {
        updateText(e.getTarget());
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent e) {
        remove(e.getEntity());
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        remove(e.getItem());
    }

    private void spawnHologram(Item item) {
        UUID id = item.getUniqueId();

        double height = getConfig().getDouble("hologram.height");
        int interval = getConfig().getInt("update-interval-ticks");

        ArmorStand stand = item.getWorld().spawn(item.getLocation().add(0, height, 0), ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setCustomNameVisible(true);
            as.setSmall(true);
        });

        holograms.put(id, stand);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!item.isValid()) {
                remove(item);
                return;
            }
            stand.teleport(item.getLocation().add(0, height, 0));
            stand.setCustomName(format(item.getItemStack()));
        }, 0L, interval);

        tasks.put(id, task);
    }

    private void updateText(Item item) {
        ArmorStand stand = holograms.get(item.getUniqueId());
        if (stand != null) {
            stand.setCustomName(format(item.getItemStack()));
        }
    }

    private void remove(Item item) {
        UUID id = item.getUniqueId();

        if (tasks.containsKey(id)) {
            tasks.get(id).cancel();
            tasks.remove(id);
        }

        if (holograms.containsKey(id)) {
            holograms.get(id).remove();
            holograms.remove(id);
        }
    }

    private String format(ItemStack item) {
        String name = item.getType().name().toLowerCase().replace("_", " ");
        name = name.substring(0, 1).toUpperCase() + name.substring(1);

        String format = getConfig().getString("hologram.format");
        return ChatColor.translateAlternateColorCodes('&',
                format.replace("{item}", name)
                        .replace("{amount}", String.valueOf(item.getAmount()))
        );
    }
}
