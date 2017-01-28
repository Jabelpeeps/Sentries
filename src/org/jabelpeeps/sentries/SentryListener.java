package org.jabelpeeps.sentries;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Horse;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDamageEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.editor.Editor;

public class SentryListener implements Listener {
    
    private Random random = new Random();

    SentryListener() {}

    @EventHandler
    public void kill( EntityDeathEvent event ) {

        LivingEntity deceased = event.getEntity();

        if ( Sentries.debug ) Sentries.debugLog( event.getEventName() + " called for:- " + deceased.toString() );

        Entity killer = deceased.getKiller();
        
        if ( killer == null ) {
            // lets try another route to find the killer...
            EntityDamageEvent ev = deceased.getLastDamageCause();
            // test whether death caused by another entity & reallocate if so.
            if ( ev != null && ev instanceof EntityDamageByEntityEvent ) {

                killer = Utils.getSource( ((EntityDamageByEntityEvent) ev).getDamager() );
            }
        }
        SentryTrait inst = Utils.getSentryTrait( killer );

        if ( inst != null && !inst.killsDrop ) {
            event.getDrops().clear();
            event.setDroppedExp( 0 );
        }
    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void entteleportevent( EntityTeleportEvent event ) {
        // stop warlocks teleporting when they throw enderpearls

        SentryTrait inst = Utils.getSentryTrait( event.getEntity() );

        if (    inst != null 
                && inst.epCount != 0 
                && inst.isWarlock1() ) {
            event.setCancelled( true );
        }
    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void entteleportevent( PlayerTeleportEvent event ) {
        // stop player-type warlocks teleporting when they throw enderpearls

        SentryTrait inst = Utils.getSentryTrait( event.getPlayer() );

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
        SentryTrait inst = Utils.getSentryTrait( Utils.getSource( projectile ) );
        if ( inst == null ) return;

        if ( inst.isWarlock1() || projectile instanceof EnderPearl ) {

            inst.epCount--;
            if ( inst.epCount < 0 ) inst.epCount = 0;

            projectile.getWorld().playEffect( projectile.getLocation(), Effect.ENDER_SIGNAL, 1, 100 );
            return;
        }
        // put out any fires caused by attacks. 
        if ( inst.lightsFires() ) {

            final Block block = projectile.getLocation().getBlock();

            Bukkit.getScheduler().scheduleSyncDelayedTask( Sentries.plugin, 
                    () -> {
                            for ( BlockFace face : BlockFace.values() ) {
                                if ( block.getRelative( face ).getType() == Material.FIRE )
                                    block.getRelative( face ).setType( Material.AIR );
                            }
                            if ( block.getType() == Material.FIRE )
                                block.setType( Material.AIR );
                        });
        }
    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void EnvDamage( NPCDamageEvent event ) {

        if ( event instanceof NPCDamageByEntityEvent ) return;     
        if ( Sentries.debug ) Sentries.debugLog( event.getEventName() + " called for:- " + event.getNPC().getName() );

        SentryTrait inst = Utils.getSentryTrait( event.getNPC() );

        if ( inst == null ) return;

        event.setCancelled( true );
        
        if ( inst.guardeeName != null && inst.guardeeEntity == null ) return;         
        if ( inst.getMyStatus().isDeadOrDieing() ) return;
        if ( System.currentTimeMillis() < inst.okToTakedamage ) return;

        switch ( event.getCause() ) {    
            case FALL: default:
                return;               
            case LIGHTNING:
                if ( inst.isStormcaller() ) return;
                break;
            case FIRE: case FIRE_TICK: case LAVA: 
                if ( inst.isNotFlammable() ) return;
                break;
            case POISON: case MAGIC:
                if ( inst.isWitchDoctor() ) return;
            case BLOCK_EXPLOSION: case ENTITY_EXPLOSION:
                if ( inst.isGrenadier() ) return;
                
            case CONTACT: case DROWNING: case VOID: case SUICIDE:
            case CUSTOM: case SUFFOCATION: 
        }
        NPC npc = inst.getNPC();
        
        if ( npc == null || !npc.isSpawned() || inst.invincible ) return;

        inst.okToTakedamage = System.currentTimeMillis() + 500;

        LivingEntity myEntity = (LivingEntity) inst.getNPC().getEntity();
        double finaldamage = event.getDamage();
        DamageCause cause = event.getCause();

        if (    cause == DamageCause.CONTACT
                || cause == DamageCause.BLOCK_EXPLOSION ) {
            finaldamage = inst.getFinalDamage( finaldamage );
        }

        if ( finaldamage > 0 ) {
            myEntity.playEffect( EntityEffect.HURT );

            if ( cause == DamageCause.FIRE ) {

                Navigator navigator = inst.getNavigator();

                if ( !navigator.isNavigating() )
                    navigator.setTarget( myEntity.getLocation().add( 
                            random.nextInt( 2 ) - 1, 0, random.nextInt( 2 ) - 1 ) );
            }
            if ( inst.getHealth() - finaldamage <= 0 )
                inst.die( true, event );
            else
                myEntity.damage( finaldamage );
        }
    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void onDamage( EntityDamageByEntityEvent event ) {
        // handles damage inflicted by Sentries
        
        Entity entity = event.getEntity();
        if ( !(entity instanceof LivingEntity) ) return;
        
        LivingEntity victim = (LivingEntity) entity;
        
        Entity damagerEnt = event.getDamager();        
        LivingEntity shooter = (LivingEntity) Utils.getSource( damagerEnt );
               
        SentryTrait instDamager = Utils.getSentryTrait( shooter );

        if ( instDamager != null ) {
            // projectiles go through ignore targets.
            // TODO consider whether this is wanted behaviour? or maybe cancelling the event is enough?
            if (    damagerEnt instanceof Projectile
                    && instDamager.isIgnoring( victim ) ) {

                event.setCancelled( true );
                damagerEnt.remove();

                Projectile newProjectile = (Projectile) damagerEnt.getWorld().spawnEntity(
                                                    damagerEnt.getLocation().add( damagerEnt.getVelocity() ),
                                                    damagerEnt.getType() );

                newProjectile.setVelocity( damagerEnt.getVelocity() );
                if ( shooter != null ) newProjectile.setShooter( shooter );
                newProjectile.setTicksLived( damagerEnt.getTicksLived() );
                return;
            }

            // set damage dealt by a sentry
            event.setDamage( instDamager.strength );

            // uncancel if not bodyguard.
            if (    instDamager.guardeeName == null
                    || !Sentries.bodyguardsObeyProtection )
                event.setCancelled( false );

            SentryTrait instVictim = Utils.getSentryTrait( victim );
            
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

                double h = instDamager.strength + 3;
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
    
    @EventHandler( priority = EventPriority.HIGHEST )
    public void processNPCdamage( NPCDamageByEntityEvent event ) {
        // handles damage to sentries - including critical hits (if enabled)

        NPC npc = event.getNPC();
        SentryTrait instVictim = Utils.getSentryTrait( npc );
       
        if ( instVictim == null ) return;
            
        // stop repeated calls to this event handler for the same NPC
        if ( System.currentTimeMillis() < instVictim.okToTakedamage + 500 ) return;
        instVictim.okToTakedamage = System.currentTimeMillis();
                    
        // Damage to a sentry cannot be handled by the server.
        event.setCancelled( true );

        LivingEntity damager = (LivingEntity) Utils.getSource( event.getDamager() );
        
        // don't take damage from the entity the sentry is guarding.
        if ( damager == null || damager == instVictim.guardeeEntity ) return;  
        
        // handle class protections
        switch ( event.getCause() ) {
            case LIGHTNING:
                if ( instVictim.isStormcaller() ) return;
            case FIRE: case FIRE_TICK:
                if ( instVictim.isNotFlammable() ) return;
            default:
        }
        SentryTrait instDamager = Utils.getSentryTrait( damager );
        
        // don't take damage from co-guards.
        if (    instDamager != null
                && instDamager.guardeeEntity != null
                && instVictim.guardeeEntity != null
                && instDamager.guardeeEntity == instVictim.guardeeEntity ) {
            return;
        }
         
        Hits hit = Hits.Hit;
        double damage = event.getDamage();

        if ( instVictim.acceptsCriticals ) {
            hit = Hits.getHit();
            damage = Math.round( damage * hit.damageModifier );
        }

        Entity myEntity = npc.getEntity();
        
        if ( damage > 0 ) {

            // do knockback
            myEntity.setVelocity( damager.getLocation().getDirection().multiply( 1.0 / instVictim.weight ) );
            // Apply armour
            damage = instVictim.getFinalDamage( damage );

            // there was damage before armour.
            if ( damage <= 0 ) {
                myEntity.getWorld().playEffect( myEntity.getLocation(), Effect.ZOMBIE_CHEW_IRON_DOOR, 1 );
                hit = Hits.Block;
            }
        }

        if (    damager instanceof Player
                && !damager.hasMetadata("NPC") ) {

            Player player = (Player) damager;
            instVictim.myDamagers.add( player );

            String msg = hit.message;

            if ( msg != null && !msg.isEmpty() ) {
                String formatted = Utils.format( msg, npc, damager, 
                                                 player.getInventory().getItemInMainHand(),
                                                 String.valueOf( damage ) );
                player.sendMessage( formatted );
                
                if ( Sentries.debug ) Sentries.debugLog( formatted );                               
            }
        }

        if ( damage > 0 ) {
            
            myEntity.playEffect( EntityEffect.HURT );
            ((LivingEntity) myEntity).damage( damage, damager );
            
            // is he dead?
            if ( instVictim.getHealth() <= 0 )
                instVictim.die( true, event );
        }
        if ( Sentries.debug )
            Sentries.debugLog( "Damage: from:" + damager.getName() + " to:" + npc.getName() + " cancelled:[" + event.isCancelled() 
                                + "] damage:["  + event.getDamage() + "] cause:" + event.getCause() );   
    }
    
    @EventHandler( priority = EventPriority.MONITOR )
    public void processEventForTargets( EntityDamageByEntityEvent event ) {
        // event to check each sentry for events that need a response.
        
        LivingEntity damager = (LivingEntity) Utils.getSource( event.getDamager() );
        if ( damager == null ) return;
        
        Entity victim = event.getEntity();        
        if ( !(victim instanceof LivingEntity) ) return; 
        
        double damage = event.getDamage();

        processEventForTargets( damager, (LivingEntity) victim, damage );
    }
    
    public static void processEventForTargets( LivingEntity damager, LivingEntity victim, double damage ) {
              
        if ( Sentries.debug ) Sentries.debugLog( "processEventForTargets() called for:- " + damager.getName() + " Vs " + victim.getName() );
        
        if (    damager != victim
                && damage > 0 ) {

            SentryTrait victimInst = Utils.getSentryTrait( victim );
            
            if ( victimInst != null && victimInst.iRetaliate )
                victimInst.setAttackTarget( damager );
            
            for ( NPC npc : Sentries.registry ) {
                
                if  (   !npc.isSpawned()
                        || npc.getEntity().getWorld() != victim.getWorld() ) {
                     continue;       
                }
                SentryTrait inst = Utils.getSentryTrait( npc );

                if ( inst == null || inst == victimInst || inst.isIgnoring( damager ) || inst.isBodyguardOnLeave() ) {
                    continue; 
                }
                // is the sentry guarding the victim?
                if (    inst.guardeeEntity == victim ) {
                    inst.setAttackTarget( damager );
                    continue;
                }
                // is the sentry set to retaliate, and its mount was the victim?
                if (    inst.iRetaliate
                        && inst.hasMount()
                        && inst.getMountNPC().getEntity() == victim ) {
                    inst.setAttackTarget( damager );
                    continue;
                }
                // respond to configured event targetTypes
                if (    inst.getMyStatus() == SentryStatus.LOOKING
                        && damager instanceof Player
                        && !damager.hasMetadata( "NPC" )
                        && inst.events.parallelStream().anyMatch( e -> e.includes( victim ) ) ) {
                    
                    Location npcLoc = npc.getEntity().getLocation();
                    // is the event within range of the sentry?
                    if (    (   npcLoc.distanceSquared( victim.getLocation() ) <= Utils.sqr( inst.range )
                            ||  npcLoc.distanceSquared( damager.getLocation() ) <= Utils.sqr( inst.range ) )

                        // is it light enough for the sentry to see (with night-vision)?
                        &&  (   damager.getLocation().getBlock().getLightLevel() + inst.nightVision > 15
                            ||  victim.getLocation().getBlock().getLightLevel() + inst.nightVision > 15 )
    
                        // does the sentry have line-of-sight?
                        &&  (   inst.hasLOS( damager ) || inst.hasLOS( victim ) ) ) {
                        
                    // phew! we made it! the event is a valid trigger. Attack the aggressor!
                    inst.setAttackTarget( damager );
                    }                  
                }
            }
        }
    }

    public void onEntityDeath( NPCDeathEvent event ) {
        // event to handle the deaths of sentries & mounts
        
        final NPC npc = event.getNPC();

        if ( Sentries.debug ) Sentries.debugLog( event.getEventName() + " called for:- " + npc.getFullName() );
        
        SentryTrait inst = Utils.getSentryTrait( npc );
        LivingEntity deceased = (LivingEntity) npc.getEntity();
        
        if ( inst != null && deceased != null ) {
        
            if ( inst.dropInventory ) {
                deceased.getWorld()
                        .spawn( deceased.getLocation(), ExperienceOrb.class )
                        .setExperience( Sentries.sentryEXP );
            }
            List<ItemStack> items = new LinkedList<>();
        
            if ( deceased instanceof HumanEntity ) {
        
                PlayerInventory inventory = ((HumanEntity) deceased).getInventory();
        
                for ( ItemStack is : inventory.getArmorContents() ) {
        
                    if ( is != null && is.getType() != null )
                        items.add( is );
                }
                ItemStack is = inventory.getItemInMainHand();
        
                if ( is.getType() != null ) items.add( is );
        
                is = inventory.getItemInOffHand();
        
                if ( is.getType() != null ) items.add( is );
        
                inventory.clear();
            }
        
            if ( items.isEmpty() )
                deceased.playEffect( EntityEffect.DEATH );
            else
                deceased.playEffect( EntityEffect.HURT );
        
            if ( !inst.dropInventory ) items.clear();
        
            items.parallelStream().forEach( i -> deceased.getWorld().dropItemNaturally( deceased.getLocation(), i ) );
        
            if ( Sentries.dieLikePlayers && deceased instanceof Player )
                deceased.setHealth( 0 );
            else
                Bukkit.getPluginManager().callEvent( new EntityDeathEvent( deceased, items ) );            
        
            
            // if the mount dies carry aggression over.
            
            // first we need to find out whether the dead entity was a mount.
            for ( NPC each : Sentries.registry ) {
    
                SentryTrait eachInst = Utils.getSentryTrait( each );
                
                if ( eachInst == null || !each.isSpawned() || !eachInst.hasMount() )
                    continue; // not a sentry, not spawned, or not mounted
    
                if ( npc.getId() == eachInst.mountID ) {
    
                    Entity killer = deceased.getKiller();
                    if ( killer == null ) {
                        // might have been a projectile.
                        EntityDamageEvent ev = deceased.getLastDamageCause();
    
                        if (    ev != null
                                && ev instanceof EntityDamageByEntityEvent ) {
    
                            killer = Utils.getSource( ((EntityDamageByEntityEvent) ev).getDamager() );
                        }
                    }
    
                    final LivingEntity perp = (killer instanceof LivingEntity) ? (LivingEntity) killer 
                                                                               : null;
                    if ( Sentries.denizenActive ) {
                        DenizenHook.denizenAction( each, "mount death", (perp instanceof Player) ? (Player) perp 
                                                                                                 : null );
                    }
                    if ( perp != null && !eachInst.isIgnoring( perp ) ) eachInst.setAttackTarget( perp );
                    
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onNPCRightClick( NPCRightClickEvent event ) {

        SentryTrait inst = Utils.getSentryTrait( event.getNPC() );
        if ( inst == null ) return;
        
        Player player = event.getClicker();

        // stops players, other than the guardeeEntity, from using right-click on horses
        if (    inst.getNPC().getEntity() instanceof Horse
                && inst.guardeeEntity != player ) {

            event.setCancelled( true );
        }
        // updates Armour and attackType if a sentry's equipment is being edited with the citizens editor.
        if ( Editor.hasEditor( player ) ) {
            inst.updateArmour();
            inst.updateAttackType();
        }
    }
    
    @EventHandler( priority = EventPriority.MONITOR )
    public void onNPCSpawning( NPCSpawnEvent event ) {
        NPC npc = event.getNPC();
        for ( NPC each : Sentries.registry ) {
            SentryTrait inst = Utils.getSentryTrait( each );
            if ( inst != null && inst.isBodyguardOnLeave() )
                inst.checkForGuardee( npc );
        }
    }
    
    @EventHandler( priority = EventPriority.MONITOR )
    public void onPlayerJoining( PlayerJoinEvent event ) {
        Player player = event.getPlayer();
        for ( NPC each : Sentries.registry ) {
            SentryTrait inst = Utils.getSentryTrait( each );
            if ( inst != null && inst.isBodyguardOnLeave() )
                inst.checkForGuardee( player );
        }
    }

    @EventHandler
    public void onCitReload( CitizensReloadEvent event ) {
        for ( NPC each : Sentries.registry ) {
            SentryTrait inst = Utils.getSentryTrait( each );
            if ( inst != null )
                inst.cancelRunnable();
        }
    } 
    
    static EnumSet<EntityType> exploders = EnumSet.of( EntityType.PRIMED_TNT, EntityType.WITHER_SKULL );
    @EventHandler
    public void onTNTExplode( EntityExplodeEvent event ) {
        if  (   exploders.contains( event.getEntityType() )
                && ThrownEntities.getThrower( event.getEntity() ) != null ) 
            event.blockList().clear();
    }
    
    @EventHandler
    public void onEggBreaks( PlayerEggThrowEvent event ) {
        if ( ThrownEntities.getThrower( event.getEgg() ) != null ) {
            event.setHatching( false );
        }
    }
}
