package me.droptext;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DropFloatingText extends JavaPlugin implements Listener {

    private final Map<UUID, ArmorStand> holograms = new HashMap<>();

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

    // ───────────────── EVENTS ─────────────────

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Item item = e.getItemDrop();
        if (holograms.containsKey(item.getUniqueId())) return;

        spawnHologram(item);
    }

    @EventHandler
    public void onMerge(ItemMergeEvent e) {
        Item source = e.getEntity();
        Item target = e.getTarget();

        // ❌ remove hologram from source item
        remove(source);

        // ✅ update hologram text on target item
        ArmorStand stand = holograms.get(target.getUniqueId());
        if (stand != null) {
            stand.setCustomName(format(target.getItemStack()));
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        remove(e.getItem());
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent e) {
        remove(e.getEntity());
    }

    // ───────────────── LOGIC ─────────────────

    private void spawnHologram(Item item) {
        ArmorStand stand = item.getWorld().spawn(item.getLocation(), ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setSmall(true);
            as.setCustomNameVisible(true);
            as.setCustomName(format(item.getItemStack()));
        });

        // ✅ Attach as passenger (smooth movement, no slideshow)
        item.addPassenger(stand);

        holograms.put(item.getUniqueId(), stand);
    }

    private void remove(Item item) {
        UUID id = item.getUniqueId();
        ArmorStand stand = holograms.remove(id);
        if (stand != null) {
            stand.remove();
        }
    }

    private String format(ItemStack item) {
        String name = item.getType().name().toLowerCase().replace("_", " ");
        name = name.substring(0, 1).toUpperCase() + name.substring(1);

        String format = getConfig().getString("hologram.format", "&f{item} &7× {amount}");

        return ChatColor.translateAlternateColorCodes('&',
                format.replace("{item}", name)
                      .replace("{amount}", String.valueOf(item.getAmount()))
        );
    }
}
