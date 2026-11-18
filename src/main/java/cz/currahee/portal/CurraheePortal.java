package cz.currahee.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CurraheePortal extends JavaPlugin implements Listener {
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Map<UUID, Long> pending = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("CurraheePortal je pripraven. Pouzij /portal build na spawnu.");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("curraheeportal.admin")) {
            sender.sendMessage("§cNemáš oprávnění spravovat portál."); return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§6/portal build §7- vytvoří vodní portál pod tebou");
            sender.sendMessage("§6/portal remove §7- vypne teleportování");
            sender.sendMessage("§6/portal info §7- ukáže nastavení");
            return true;
        }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        if (sub.equals("info")) {
            sender.sendMessage("§ePortál: " + (getConfig().getBoolean("portal.enabled") ? "§azapnutý" : "§cvypnutý"));
            sender.sendMessage("§eCílový svět: §f" + getConfig().getString("destination.world", "survival"));
            return true;
        }
        if (sub.equals("remove")) {
            getConfig().set("portal.enabled", false); saveConfig();
            sender.sendMessage("§aPortál vypnut. Postavené bloky zůstaly na místě."); return true;
        }
        if (!sub.equals("build")) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tento příkaz použij ve hře na místě portálu."); return true;
        }
        String worldName = getConfig().getString("destination.world", "survival");
        if (player.getWorld().getName().equalsIgnoreCase(worldName)) {
            player.sendMessage("§cPortál musí být v jiném světě než cílový svět."); return true;
        }
        World target = Bukkit.getWorld(worldName);
        if (target == null) {
            sender.sendMessage("§eVytvářím nový survival svět §f" + worldName + "§e...");
            target = Bukkit.createWorld(new WorldCreator(worldName).environment(World.Environment.NORMAL));
            if (target == null) { sender.sendMessage("§cSvět se nepodařilo vytvořit. Zkontroluj konzoli."); return true; }
        }
        Location p = player.getLocation();
        int x = p.getBlockX(), y = p.getBlockY() - 1, z = p.getBlockZ();
        // 5x5 stone frame, 3x3 shallow water basin; no player-made buildings nearby should be overwritten.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int bx=x+dx, bz=z+dz;
            p.getWorld().getBlockAt(bx, y-1, bz).setType(Material.STONE_BRICKS, false);
            if (Math.abs(dx)==2 || Math.abs(dz)==2) {
                p.getWorld().getBlockAt(bx, y, bz).setType(Material.POLISHED_DEEPSLATE, false);
            } else {
                p.getWorld().getBlockAt(bx, y, bz).setType(Material.WATER, false);
            }
        }
        getConfig().set("portal.world", p.getWorld().getName());
        getConfig().set("portal.x", x); getConfig().set("portal.y", y); getConfig().set("portal.z", z);
        getConfig().set("portal.enabled", true); saveConfig();
        player.sendMessage("§aHotovo! §fSkoč do vody, portál tě přenese do světa §e" + worldName + "§f.");
        return true;
    }

    @EventHandler public void onMove(PlayerMoveEvent event) {
        if (!getConfig().getBoolean("portal.enabled")) return;
        Location to = event.getTo();
        if (to == null) return;
        Location from = event.getFrom();
        if (to.getBlockX()==from.getBlockX() && to.getBlockY()==from.getBlockY() && to.getBlockZ()==from.getBlockZ()) return;
        if (!to.getWorld().getName().equals(getConfig().getString("portal.world", ""))) return;
        int px=getConfig().getInt("portal.x"), py=getConfig().getInt("portal.y"), pz=getConfig().getInt("portal.z");
        if (to.getBlockY()!=py || Math.abs(to.getBlockX()-px)>1 || Math.abs(to.getBlockZ()-pz)>1) return;
        if (to.getBlock().getType()!=Material.WATER) return;
        Player player=event.getPlayer();
        long now=System.currentTimeMillis();
        if (cooldown.getOrDefault(player.getUniqueId(),0L)>now || pending.containsKey(player.getUniqueId())) return;
        pending.put(player.getUniqueId(), now);
        Bukkit.getScheduler().runTask(this, () -> {
            pending.remove(player.getUniqueId());
            if (!player.isOnline()) return;
            String name=getConfig().getString("destination.world", "survival");
            World world=Bukkit.getWorld(name);
            if (world==null) {
                player.sendMessage("§cCílový svět není načtený. Kontaktuj admina."); return;
            }
            Location spawn=world.getSpawnLocation();
            // Spawn has been chosen by Minecraft; teleport to center of spawn block.
            Location destination=new Location(world,spawn.getBlockX()+0.5,spawn.getY(),spawn.getBlockZ()+0.5,spawn.getYaw(),spawn.getPitch());
            cooldown.put(player.getUniqueId(),System.currentTimeMillis()+3000);
            if (player.teleport(destination)) player.sendMessage("§aVítej ve světě §e" + world.getName() + "§a!");
            else player.sendMessage("§cTeleport byl zrušen jiným pluginem.");
        });
    }
}
