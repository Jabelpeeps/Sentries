package org.jabelpeeps.sentries;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDamageEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;

public class SentryListener implements Listener {

    SentryListener() {}

    @EventHandler
    public void kill( EntityDeathEvent event ) {

        LivingEntity deceased = event.getEntity();

        if ( Sentries.debug ) Sentries.debugLog( event.getEventName() + " called for:- " + deceased.toString() );
        
        // don't mess with player death.
        if ( deceased instanceof Player && !deceased.hasMetadata( "NPC" ) ) return;

        Entity killer = deceased.getKiller();
        
        if ( killer == null ) {
            // lets try another route to find the killer...

            // let start by getting the specific damage event.
            EntityDamageEvent ev = deceased.getLastDamageCause();

            // test whether death caused by another entity.
            if ( ev != null && ev instanceof EntityDamageByEntityEvent ) {

                // re-allocate killer to reference new entity
                killer = ((EntityDamageByEntityEvent) ev).getDamager();

                killer = Util.getArcher( killer );

                // TODO consider what to do if killer is not a living entity.
                // (e.g. it is possible killer references a projectile shot by a
                // dispenser.)
            }
        }
        SentryTrait inst = Util.getSentryTrait( killer );

        if ( inst != null && !inst.killsDrop ) {
            event.getDrops().clear();
            event.setDroppedExp( 0 );
        }
    }

    @EventHandler( ignoreCancelled = true )
    public void despawn( NPCDespawnEvent event ) {
        // don't despawn active bodyguards on chunk unload
        
        if ( Sentries.debug ) Sentries.debugLog( event.getEventName() + " called for:- " + event.getNPC().getFullName() );

        SentryTrait inst = Util.getSentryTrait( event.getNPC() );

        if (    inst != null 
                && event.getReason() == DespawnReason.CHUNK_UNLOAD
                && inst.guardeeEntity != null ) {
            event.setCancelled( true );
            inst.myStatus = SentryStatus.FOLLOWING;
        }
    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void entteleportevent( EntityTeleportEvent event ) {
        // stop warlocks teleporting when they throw enderpearls

        SentryTrait inst = Util.getSentryTrait( event.getEntity() );

        if (    inst != null 
                && inst.epCount != 0 
                && inst.isWarlock1() ) {
            event.setCancelled( true );
        }
    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void entteleportevent( PlayerTeleportEvent event ) {
        // stop player-type warlocks teleporting when they throw enderpearls

        SentryTrait inst = Util.getSentryTrait( event.getPlayer() );

        if (    inst != null 
                && inst.epCount != 0 
                && inst.isWarlock1() 
                && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL ) {
            event.setCancelled( true );
        }
    }

    @EventHandler( priority = EventPriority.MONITOR )
    public void projectilehit( ProjectileHitEvent event ) {
        // event to handle thrown enderpearls & put out fires from small fireballs

        Projectile projectile = event.getEntity();
        SentryTrait inst = Util.getSentryTrait( (Entity) projectile.getShooter() );

        if ( projectile instanceof EnderPearl ) {

            if ( inst != null ) {

                inst.epCount--;
                if ( inst.epCount < 0 )
                    inst.epCount = 0;

                projectile.getWorld().playEffect(
                        event.getEntity().getLocation(),
                        Effect.ENDER_SIGNAL, 1, 100 );
            }
            return;
        }

        // put out any fires from pyromancer1 fire-balls
        if (    projectile instanceof SmallFireball 
                && inst != null 
                && inst.isPyromancer1() ) {

            final Block block = projectile.getLocation().getBlock();

            final Runnable blockDamage = new Runnable() {
                @Override
                public void run() {

                    for ( BlockFace face : BlockFace.values() ) {
                        if ( block.getRelative( face ).getType() == Material.FIRE )
                            block.getRelative( face ).setType( Material.AIR );
                    }
                    if ( block.getType() == Material.FIRE )
                        block.setType( Material.AIR );
                }
            };
            Bukkit.getScheduler().scheduleSyncDelayedTask( Sentries.plugin, blockDamage );
        }
    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void EnvDamage( NPCDamageEvent event ) {

        if ( event instanceof NPCDamageByEntityEvent ) return;
        
        if ( Sentries.debug ) Sentries.debugLog( event.getEventName() + " called for:- " + event.getNPC().getName() );

        SentryTrait inst = Util.getSentryTrait( event.getNPC() );

        if ( inst != null ) {

            event.setCancelled( true );

            switch ( event.getCause() ) {

                case CONTACT:
                case DROWNING:
                case LAVA:
                case VOID:
                case SUICIDE:
                case CUSTOM:
                case BLOCK_EXPLOSION:
                case SUFFOCATION:
                case MAGIC:
                    inst.onEnvironmentDamage( event );
                    break;

                case LIGHTNING:
                    if ( !inst.isStormcaller() )
                        inst.onEnvironmentDamage( event );
                    break;

                case FIRE:
                case FIRE_TICK:
                    if ( inst.isFlammable() )
                        inst.onEnvironmentDamage( event );
                    break;

                case POISON:
                    if ( !inst.isWitchDoctor() )
                        inst.onEnvironmentDamage( event );
                    break;

                case FALL:
                default:
            }
        }
    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void onDamage( EntityDamageByEntityEvent event ) {
        // handles damage inflicted by Sentries
        
        Entity entity = event.getEntity();
        if ( !(entity instanceof LivingEntity) ) return;
        
        LivingEntity victim = (LivingEntity) entity;
        
        Entity damagerEnt = event.getDamager();        
        LivingEntity shooter = (LivingEntity) Util.getArcher( damagerEnt );
               
        SentryTrait instDamager = Util.getSentryTrait( shooter );
        SentryTrait instVictim = Util.getSentryTrait( victim );

        if ( instDamager != null ) {

            // projectiles go through ignore targets.
            // TODO consider whether this is wanted behaviour? or maybe cancelling the event is enough?
            if (    damagerEnt instanceof Projectile
                    && instDamager.isIgnoring( victim ) ) {

                event.setCancelled( true );
                damagerEnt.remove();

                Projectile newProjectile = (Projectile) shooter.getWorld().spawnEntity(
                                                    damagerEnt.getLocation().add( damagerEnt.getVelocity() ),
                                                    damagerEnt.getType() );

                newProjectile.setVelocity( damagerEnt.getVelocity() );
                newProjectile.setShooter( shooter );
                newProjectile.setTicksLived( damagerEnt.getTicksLived() );
                return;
            }

            // set damage dealt by a sentry
            event.setDamage( instDamager.getStrength() );

            // uncancel if not bodyguard.
            if (    instDamager.guardeeName == null
                    || !Sentries.bodyguardsObeyProtection )
                event.setCancelled( false );

            // cancel if invulnerable, non-sentry npc
            if ( instVictim == null ) {

                NPC npc = Sentries.registry.getNPC( victim );

                if ( npc != null ) {
                    event.setCancelled( npc.isProtected() );
                }
            }
            // don't hurt guard target.
            if ( victim == instDamager.guardeeEntity ) event.setCancelled( true );

            // apply potion effects
            if (    instDamager.weaponSpecialEffects != null
                    && !event.isCancelled() ) {
                victim.addPotionEffects( instDamager.weaponSpecialEffects );
            }

            // warlock 1 should do no direct damage, except to sentries who take fall damage.
            if ( instDamager.isWarlock1() && !event.isCancelled() ) {

                if ( instVictim == null ) event.setCancelled( true );

                double h = instDamager.getStrength() + 3;
                double v = 7.7 * Math.sqrt( h ) + 0.2;

                if ( h <= 3 ) v -= 2;
                if ( v > 150 ) v = 150;

                victim.setVelocity( new Vector( 0, v / 20, 0 ) );
            }
            
            if ( Sentries.debug )
                Sentries.debugLog( "Damage: from:" + shooter.getName() + " to:" + victim.getName() + " cancelled:[" + event.isCancelled() 
                                    + "] damage:["  + event.getDamage() + "] cause:" + event.getCause() );   
        }
    }
    
    @SuppressWarnings( "null" )
    @EventHandler( priority = EventPriority.HIGHEST )
    public void processNPCdamage( NPCDamageByEntityEvent event ) {
        // handles damage to sentries (including critical hits - if enabled)

        NPC npc = event.getNPC();
        SentryTrait instVictim = Util.getSentryTrait( npc );
        
        // tests if the victim is a sentry
        if ( instVictim != null ) { 
            
            // stop repeated calls to this event handler for the same NPC
            if ( System.currentTimeMillis() < instVictim.okToTakedamage + 500 ) return;
            instVictim.okToTakedamage = System.currentTimeMillis();
                        
            // Damage to a sentry cannot be handled by the server. Always cancel the event here.
            event.setCancelled( true );

            LivingEntity damager = (LivingEntity) Util.getArcher( event.getDamager() );
            
            // don't take damage from the entity the sentry is guarding.
            if ( damager == instVictim.guardeeEntity ) return;  
            
            // handle class protections
            DamageCause cause = event.getCause();
            
            if (    cause == DamageCause.LIGHTNING
                    && instVictim.isStormcaller() )
                return;

            if (    (   cause == DamageCause.FIRE
                    ||  cause == DamageCause.FIRE_TICK )
               &&   instVictim.isFlammable() )
                return;

            SentryTrait instDamager = Util.getSentryTrait( damager );
            
            if (    damager != null 
                    && instDamager != null
                    && instDamager.guardeeEntity != null
                    && instVictim.guardeeEntity != null
                    && instDamager.guardeeEntity == instVictim.guardeeEntity ) {

                // don't take damage from co-guards.
                return;
            }
             
            Hits hit = Hits.Hit;
            double damage = event.getDamage();

            if ( instVictim.acceptsCriticals ) {

                hit = Hits.getHit();
                damage = Math.round( damage * hit.damageModifier );
            }
            
            int armour = instVictim.getArmor();
            Entity myEntity = npc.getEntity();
            
            if ( damager != null && damage > 0 ) {

                // do knockback
                myEntity.setVelocity( damager.getLocation()
                                             .getDirection()
                                             .multiply( 1.0 / ( instVictim.weight + (armour / 5) ) ) 
                );
                // Apply armour
                damage -= armour;

                // there was damage before armour.
                if ( damage <= 0 ) {
                    myEntity.getWorld().playEffect( myEntity.getLocation(), Effect.ZOMBIE_CHEW_IRON_DOOR, 1 );
                    hit = Hits.Block;
                }
            }

            if (    damager instanceof Player
                    && !damager.hasMetadata("NPC") ) {

                Player player = (Player) damager;
                instVictim._myDamamgers.add( player );

                String msg = hit.message;

                if ( msg != null && !msg.isEmpty() ) {
                    String formatted = Util.format( msg,
                                                    npc,
                                                    damager, 
                                                    player.getInventory().getItemInMainHand().getType(),
                                                    String.valueOf( damage ) );
                    player.sendMessage( formatted );
                    
                    if ( Sentries.debug ) Sentries.debugLog( formatted );                               
                }
            }

            if ( damage > 0 ) {
                
                myEntity.playEffect( EntityEffect.HURT );
                ((LivingEntity) myEntity).damage( damage, damager );
                
                // is he dead?
                if ( instVictim.getHealth() - damage <= 0 )
                    instVictim.die( true, cause );

            }
            if ( Sentries.debug )
                Sentries.debugLog( "Damage: from:" + damager.getName() + " to:" + npc.getName() + " cancelled:[" + event.isCancelled() 
                                    + "] damage:["  + event.getDamage() + "] cause:" + event.getCause() );   
        }
    }  
    
    @EventHandler( priority = EventPriority.MONITOR )
    public void processEventForTargets( EntityDamageByEntityEvent event ) {
        // event to check each sentry for events that need a response.
        
        Entity victim = event.getEntity();        
        if ( !(victim instanceof LivingEntity) ) return;
        
        LivingEntity damager = (LivingEntity) Util.getArcher( event.getDamager() );
        
        if ( Sentries.debug ) Sentries.debugLog( "processEventForTargets() called for:- " + damager.getName() + " Vs " + victim.getName() );
        
        if (    damager != victim
                && event.getDamage() > 0 ) {

            for ( NPC npc : Sentries.registry ) {

                SentryTrait inst = Util.getSentryTrait( npc );

                if (    inst == null 
                        || !npc.isSpawned()
                        || npc.getEntity().getWorld() != victim.getWorld() ) {
                    // not a sentry, or not this world, or dead.
                    continue; 
                }

                // is the sentry guarding the victim?
                if (    inst.guardeeEntity == victim ) {
                    inst.setAttackTarget( damager );
                    continue;
                }

                // is the sentry set to retaliate, or was its mount the victim?
                if (    inst.iRetaliate
                        ||  (   inst.iRetaliate
                                && inst.hasMount()
                                && inst.getMountNPC().getEntity() == victim ) ) {
                    inst.setAttackTarget( damager );
                    continue;
                }

                if (    inst.hasTargetType( SentryTrait.events )
                        && inst.myStatus == SentryStatus.LOOKING
                        && damager instanceof Player
                        && !damager.hasMetadata( "NPC" )
                        && !inst.isIgnoring( damager ) ) {

                    Location npcLoc = npc.getEntity().getLocation();
                    // is the event within range of the sentry?
                    if (    (   npcLoc.distance( victim.getLocation() ) <= inst.range
                            ||  npcLoc.distance( damager.getLocation() ) <= inst.range )

                        // is it too dark for the sentry to see?
                        &&  (   inst.nightVision >= damager.getLocation().getBlock().getLightLevel()
                            ||  inst.nightVision >= victim.getLocation().getBlock().getLightLevel())
    
                        // does the sentry have line-of-sight?
                        &&  (   inst.hasLOS( damager ) || inst.hasLOS( victim ) )
    
                        // does the event correspond to configured event triggers?
                        &&  (  (    inst.targetsContain( "event:pve" )
                                    && !(victim instanceof Player) )
    
                            || (    inst.targetsContain( "event:pvp" )
                                    && victim instanceof Player
                                    && !victim.hasMetadata("NPC") )
    
                            || (    inst.targetsContain( "event:pvnpc" )
                                    && victim.hasMetadata("NPC") )
    
                            || (    inst.targetsContain( "event:pvsentry" ) 
                                    && Util.getSentryTrait( victim ) != null ) ) 
                        ) {
                        
                    // phew! we made it! the event is a valid trigger. Attack the aggressor!
                    inst.setAttackTarget( damager );
                    }
                }
            }
        }
    }

    @EventHandler( ignoreCancelled = true )
    public void onEntityDeath( NPCDeathEvent event ) {
        // event to handle the death of mounts
        // TODO why not have it handle all sentry deaths?
        
        final NPC mount = event.getNPC();

        if ( Sentries.debug ) Sentries.debugLog( event.getEventName() + " called for:- " + mount.getFullName() );

        // if the mount dies carry aggression over.
        for ( NPC each : Sentries.registry ) {

            final SentryTrait inst = Util.getSentryTrait( each );
            if ( inst == null || !each.isSpawned() || !inst.hasMount() )
                continue; // not a sentry, not spawned, or not mounted

            if ( mount.getId() == inst.mountID ) {

                Entity killer = ((LivingEntity) mount.getEntity()).getKiller();
                if ( killer == null ) {
                    // might have been a projectile.
                    EntityDamageEvent ev = mount.getEntity().getLastDamageCause();

                    if (    ev != null
                            && ev instanceof EntityDamageByEntityEvent ) {

                        killer = Util.getArcher( ((EntityDamageByEntityEvent) ev).getDamager() );
                    }
                }

                final LivingEntity perp = (killer instanceof LivingEntity) ? (LivingEntity) killer 
                                                                           : null;
                if ( Sentries.denizenActive ) {
                    DenizenHook.denizenAction( each, "mount death", (perp instanceof Player) ? (Player) perp 
                                                                                             : null );
                }
                if ( perp != null && !inst.isIgnoring( perp ) ) inst.setAttackTarget( perp );
                
                break;
            }
        }
    }

    @EventHandler
    public void onNPCRightClick( NPCRightClickEvent event ) {
        // stops players, other than the guardeeEntity, from using right-click on horses

        SentryTrait inst = Util.getSentryTrait( event.getNPC() );

        if ( inst == null ) return;

        if (    inst.getNPC().getEntity() instanceof Horse
                && inst.guardeeEntity != event.getClicker() ) {

            event.setCancelled( true );
        }
    }
}
