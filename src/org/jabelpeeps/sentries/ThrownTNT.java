package org.jabelpeeps.sentries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;

class ThrownTNT implements Projectile, TNTPrimed {
    
    static Map<Entity, ThrownTNT> thrown = new HashMap<>();
    
    static ThrownTNT getThrown( Entity ent ) { return thrown.get( ent ); }

    @Getter Entity tnt;
    
    ThrownTNT( Entity ent ) {
        tnt = ent;
    }
    
    @Getter @Setter ProjectileSource shooter;
    @Override public Entity getSource() { return (Entity) shooter; }
    @Getter @Setter float yield;
    @Getter @Setter int fuseTicks;
    @Getter EntityType type = EntityType.PRIMED_TNT;
    
    @Setter boolean bounce;
    @Override public boolean doesBounce() { return bounce; }
    
    @Getter boolean incendiary;
    @Override public void setIsIncendiary( boolean b ) { incendiary = b; }
    
    @Override public Location getLocation() { return tnt.getLocation(); }
    @Override public Location getLocation( Location loc ) { return tnt.getLocation( loc ); }
    @Override public void setVelocity( Vector velocity ) { tnt.setVelocity( velocity ); }
    @Override public Vector getVelocity() { return tnt.getVelocity(); }
    @Override public boolean isOnGround() { return tnt.isOnGround(); }
    @Override public World getWorld() { return tnt.getWorld(); }
    @Override public List<Entity> getNearbyEntities( double x, double y, double z ) { return tnt.getNearbyEntities( x, y, z ); }
    @Override public int getEntityId() { return tnt.getEntityId(); }
    @Override public int getFireTicks() { return tnt.getFireTicks(); }
    @Override public int getMaxFireTicks() { return tnt.getMaxFireTicks(); }
    @Override public void setFireTicks( int ticks ) { tnt.setFireTicks( ticks ); }
    @Override public void remove() { tnt.remove(); }
    @Override public boolean isDead() { return tnt.isDead(); }
    @Override public boolean isValid() { return tnt.isValid(); }
    @Override public Server getServer() { return tnt.getServer(); }
    @Override public UUID getUniqueId() { return tnt.getUniqueId(); }
    @Override public int getTicksLived() { return tnt.getTicksLived(); }
    @Override public void setTicksLived( int value ) { tnt.setTicksLived( value ); }
    @Override public void playEffect( EntityEffect t ) { tnt.playEffect( t ); }
    @Override public void setMetadata( String s, MetadataValue m ) { tnt.setMetadata( s, m ); }
    @Override public List<MetadataValue> getMetadata( String s ) {  return tnt.getMetadata( s ); }
    @Override public boolean hasMetadata( String s ) { return tnt.hasMetadata( s ); }
    @Override public void removeMetadata( String s, Plugin p ) { tnt.removeMetadata( s, p ); }
    @Override public void setGravity( boolean g ) { tnt.setGravity( g ); }
    @Override public boolean hasGravity() { return tnt.hasGravity(); }

    @Override public boolean isInsideVehicle() { return false; }
    @Override public boolean leaveVehicle() { return false; }
    @Override public Entity getVehicle() { return null; }
    
    @Getter @Setter boolean customNameVisible = false;
    @Getter @Setter boolean glowing = false;
    @Getter @Setter boolean invulnerable = false;
    @Getter @Setter boolean silent = false;
    @Getter @Setter int portalCooldown = 0;        
    
    @Override public Set<String> getScoreboardTags() { return null; }
    @Override public boolean addScoreboardTag( String tag ) { return false; }
    @Override public boolean removeScoreboardTag( String tag ) { return false; }
    @Override public Spigot spigot() { return null; }
    @Override public void sendMessage( String message ) { }
    @Override public void sendMessage( String[] messages ) { }
    @Override public String getName() { return null; }
    @Override public boolean teleport( Location location ) { return false; }
    @Override public boolean teleport( Location location, TeleportCause cause ) { return false; }
    @Override public boolean teleport( Entity destination ) { return false; }
    @Override public boolean teleport( Entity destination, TeleportCause cause ) { return false; }
    @Override public Entity getPassenger() { return null; }
    @Override public boolean setPassenger( Entity passenger ) { return false; }
    @Override public boolean isEmpty() { return false; }
    @Override public boolean eject() { return false; }
    @Override public float getFallDistance() { return 0; }
    @Override public void setFallDistance( float distance ) { }
    @Override public void setLastDamageCause( EntityDamageEvent event ) { }
    @Override public EntityDamageEvent getLastDamageCause() { return null; }
    @Override public boolean isPermissionSet( String name ) { return false; }
    @Override public boolean isPermissionSet( Permission perm ) { return false; }
    @Override public boolean hasPermission( String name ) { return false; }
    @Override public boolean hasPermission( Permission perm ) { return false; }
    @Override public PermissionAttachment addAttachment( Plugin p, String n, boolean v ) { return null; }
    @Override public PermissionAttachment addAttachment( Plugin plugin ) { return null; }
    @Override public PermissionAttachment addAttachment( Plugin p, String n, boolean v, int t ) { return null; }
    @Override public PermissionAttachment addAttachment( Plugin plugin, int ticks ) { return null; }
    @Override public void removeAttachment( PermissionAttachment attachment ) { }
    @Override public void recalculatePermissions() { }
    @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return null; }
    @Override public boolean isOp() { return false; }
    @Override public void setOp( boolean value ) { }
    @Override public String getCustomName() { return null; }
    @Override public void setCustomName( String name ) { }
    @Override public LivingEntity _INVALID_getShooter() { return null; }
    @Override public void _INVALID_setShooter( LivingEntity l ) { }
}