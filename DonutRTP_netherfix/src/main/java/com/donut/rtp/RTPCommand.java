
package com.donut.rtp;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class RTPCommand implements CommandExecutor {
    private final DonutRTP plugin;
    private final Map<UUID, Long> cooldowns=new HashMap<>();

    public RTPCommand(DonutRTP p){ this.plugin=p; }

    private String color(String msg){ return ChatColor.translateAlternateColorCodes('&', msg); }

    @Override
    public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player p)) return true;

        long now=System.currentTimeMillis();
        cooldowns.putIfAbsent(p.getUniqueId(),0L);
        long left=(cooldowns.get(p.getUniqueId())-now)/1000;

        if(left>0){
            p.sendMessage(color(plugin.getConfig().getString("messages.cooldown").replace("%s",String.valueOf(left))));
            return true;
        }

        openGUI(p);
        return true;
    }

    private void openGUI(Player p){
        Inventory inv=Bukkit.createInventory(null,27,color(plugin.getConfig().getString("messages.gui-title")));

        addItem(inv,11,Material.GRASS_BLOCK,"&aOverworld (&fworld&a)");
        addItem(inv,13,Material.NETHERRACK,"&cNether (&fworld_nether&c)");
        addItem(inv,15,Material.END_STONE,"&dThe End (&fworld_the_end&d)");

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK,1,1);

        Listener listener = new Listener() {
            @org.bukkit.event.EventHandler
            public void click(InventoryClickEvent e){
                if(!e.getView().getTitle().equals(color(plugin.getConfig().getString("messages.gui-title")))) return;
                e.setCancelled(true);
                if(e.getCurrentItem()==null) return;

                String raw=e.getCurrentItem().getItemMeta().getDisplayName();
                String world="";
                if(raw.contains("world_nether")) world="world_nether";
                else if(raw.contains("world_the_end")) world="world_the_end";
                else world="world";

                p.closeInventory();
                startRTP(p, world);
                HandlerList.unregisterAll(this);
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    private void addItem(Inventory inv,int slot,Material m,String name){
        ItemStack i=new ItemStack(m);
        ItemMeta im=i.getItemMeta();
        im.setDisplayName(color(name));
        i.setItemMeta(im);
        inv.setItem(slot,i);
    }

    private void startRTP(Player p,String worldName){
        int delay=plugin.getConfig().getInt("delay",5);
        Location start=p.getLocation().clone();

        new BukkitRunnable(){
            int t=delay;
            @Override public void run(){
                if(p.getLocation().distance(start)>0.1){
                    p.sendMessage(color(plugin.getConfig().getString("messages.cancelled")));
                    cancel();return;
                }
                if(t<=0){
                    asyncTP(p, worldName);
                    cancel();return;
                }
                String msg=color(plugin.getConfig().getString("messages.teleporting").replace("%s",String.valueOf(t)));
                p.sendActionBar(msg);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT,1,1);
                t--;
            }
        }.runTaskTimer(plugin,0,20);
    }

    private void asyncTP(Player p,String worldName){
        World w=Bukkit.getWorld(worldName);
        Random r=new Random();
        int min=plugin.getConfig().getInt("radius.min");
        int max=plugin.getConfig().getInt("radius.max");

        int x = r.nextInt(max-min)+min;
        int z = r.nextInt(max-min)+min;

        w.getChunkAtAsync(x>>4,z>>4).thenAccept(chunk -> {

            int y;

            if(w.getEnvironment()==World.Environment.NETHER){
                y=Math.min(w.getHighestBlockYAt(x,z), 120);
            } else {
                y=w.getHighestBlockYAt(x,z);
            }

            Location loc=new Location(w,x,y+1,z);

            Bukkit.getScheduler().runTask(plugin,()->{
                p.teleport(loc);
                cooldowns.put(p.getUniqueId(),System.currentTimeMillis()+plugin.getConfig().getInt("cooldown")*1000L);
                p.playSound(p.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,1,1);
                p.sendMessage(color(plugin.getConfig().getString("messages.done")));
            });
        });
    }
}
