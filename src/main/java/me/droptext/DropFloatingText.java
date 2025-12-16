package me.droptext;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class DropFloatingText extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        spawnHologram(item);
    }

    @EventHandler
    public void onMerge(ItemMergeEvent event) {
        updateHologram(event.getTarget());
    }

    private void spawnHologram(Item item) {
        ItemStack stack = item.getItemStack();

        ArmorStand stand = item.getWorld().spawn(item.getLocation().add(0, 0.6, 0), ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setCustomNameVisible(true);
            as.setSmall(true);
            as.setCustomName(format(stack));
        });

        Bukkit.getScheduler().runTaskTimer(this, task -> {
            if (!item.isValid() || item.isDead()) {
                stand.remove();
                task.cancel();
                return;
            }
            stand.teleport(item.getLocation().add(0, 0.6, 0));
            stand.setCustomName(format(item.getItemStack()));
        }, 0L, 10L);
    }

    private void updateHologram(Item item) {
        item.getNearbyEntities(0.3, 0.3, 0.3).stream()
                .filter(e -> e instanceof ArmorStand)
                .map(e -> (ArmorStand) e)
                .forEach(as -> as.setCustomName(format(item.getItemStack())));
    }

    private String format(ItemStack item) {
        String name = item.getType().name().toLowerCase().replace("_", " ");
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        return "§f" + name + " §7× " + item.getAmount();
    }
}
