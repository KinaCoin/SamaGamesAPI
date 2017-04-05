package net.samagames.tools.cameras;

import net.samagames.tools.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *                )\._.,--....,'``.
 * .b--.        /;   _.. \   _\  (`._ ,.
 * `=,-,-'~~~   `----(,_..'--(,_..'`-.;.'
 *
 * Created by Jérémy L. (BlueSlime) on 18/01/2017
 */
public class Camera
{
    private final ConcurrentMap<UUID, GameMode> viewers;
    private final boolean fixed;
    private EntityCamera entityCamera;

    Camera(Location initialPosition, boolean fixed)
    {
        this.viewers = new ConcurrentHashMap<>();
        this.fixed = fixed;

        try
        {
            Class<?> worldClass = Reflection.getNMSClass("World");
            Class<?> entityClass = Reflection.getNMSClass("Entity");
            Method addEntityMethod = worldClass.getMethod("addEntity", entityClass, CreatureSpawnEvent.SpawnReason.class);
            Object world = Reflection.getHandle(initialPosition.getWorld());

            if (Reflection.PackageType.getServerVersion().equals("v1_9_R2"))
                this.entityCamera = new EntityCamera(world);

            this.entityCamera.setPosition(initialPosition.getX(), initialPosition.getY(), initialPosition.getZ());
            addEntityMethod.invoke(this.entityCamera, CreatureSpawnEvent.SpawnReason.CUSTOM);
            ((LivingEntity) this.entityCamera.getBukkitEntity()).addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
        }

        if (this.entityCamera == null)
            throw new UnsupportedOperationException("You cannot use this API with your Minecraft version.");
    }

    public void move(Location to)
    {
        this.entityCamera.getBukkitEntity().teleport(to);
    }

    void remove(Location backPosition)
    {
        this.viewers.keySet().stream().filter(viewer -> Bukkit.getPlayer(viewer) != null).forEach(viewer -> this.removeViewer(Bukkit.getPlayer(viewer), backPosition));
        this.entityCamera.die();
    }

    public void addViewer(Player player)
    {
        this.viewers.put(player.getUniqueId(), player.getGameMode());

        player.setGameMode(GameMode.SPECTATOR);
        player.setSpectatorTarget(this.entityCamera.getBukkitEntity());
    }

    public void removeViewer(Player player)
    {
        this.removeViewer(player, null);
    }

    public void removeViewer(Player player, Location backPosition)
    {
        player.setSpectatorTarget(null);
        player.setGameMode(this.viewers.get(player.getUniqueId()));

        if (backPosition != null)
            player.teleport(backPosition);

        this.viewers.remove(player.getUniqueId());
    }

    public EntityCamera getEntityCamera()
    {
        return this.entityCamera;
    }

    public boolean isPlayerViewing(Player player)
    {
        return this.viewers.containsKey(player.getUniqueId());
    }

    public boolean isFixed()
    {
        return this.fixed;
    }
}
