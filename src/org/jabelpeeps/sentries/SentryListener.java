package org.jabelpeeps.sentries;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
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
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.jabelpeeps.sentries.targets.OwnerTarget;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDamageEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.editor.Editor;

public class SentryListener implements Listener {
    
    private Random random = new Random();

    SentryListener() {}

    @EventHandler
    public void onKill( EntityDeathEvent event ) {
        // This event is for handling the deaths of the victims of sentries, not the sentries themselves
        
        LivingEntity deceased = event.getEntity();
        if ( deceased.hasMetadata( S.SENTRIES_META ) ) return;

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
        NPC npc = inst.getNPC();
        
        if ( npc == null || !npc.isSpawned() || inst.invincible ) return;
        if ( inst.guardeeName != null && inst.guardeeEntity == null ) return;         
        if ( inst.getMyStatus().isDeadOrDieing() ) return;
        
        DamageCause cause = event.getCause();

        if ( isNotVulnerable( inst, cause ) ) return;
        
        switch ( cause ) { 
            case DRAGON_BREATH: case ENTITY_ATTACK: case FALL: case FALLING_BLOCK: case FLY_INTO_WALL:
            case MELTING: case PROJECTILE: case STARVATION: case THORNS: case WITHER: case CRAMMING:
                return;               
                
            default:
        }
        if ( System.currentTimeMillis() < inst.okToTakedamage ) return;
        
        inst.okToTakedamage = System.currentTimeMillis() + 500;

        LivingEntity myEntity = (LivingEntity) npc.getEntity();
        double finaldamage = event.getDamage();

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
    
    private boolean isNotVulnerable( SentryTrait inst, DamageCause cause ) {
        switch ( cause ) {
            case LIGHTNING:
                if ( inst.isStormcaller() ) return true;
                break;
            case FIRE: case FIRE_TICK: case LAVA: case HOT_FLOOR:
                if ( inst.isNotFlammable() ) return true;
                break;
            case POISON: case MAGIC:
                if ( inst.isWitchDoctor() ) return true;
                break;
            case BLOCK_EXPLOSION: case ENTITY_EXPLOSION:
                if ( inst.isGrenadier() ) return true;
                break;
            default:
        }
        return false;
    }

    @EventHandler( priority = EventPriority.HIGHEST )
    public void onDamage( EntityDamageByEntityEvent event ) {
        // handles damage inflicted by Sentries
        
        Entity entity = event.getEntity();
        if ( !(entity instanceof LivingEntity) ) return;
        
        LivingEntity victim = (LivingEntity) entity;
        
        Entity damagerEnt = event.getDamager();        
        Entity shooter = Utils.getSource( damagerEnt );
               
        SentryTrait instDamager = Utils.getSentryTrait( shooter );

        if ( instDamager == null ) return;
        
        // projectiles go through ignore targets.
        // TODO consider whether this is wanted behaviour? or maybe cancelling the event is enough?
        if (    damagerEnt instanceof Projectile
                && instDamager.isIgnoring( victim ) ) {

            event.setCancelled( true );
            damagerEnt.remove();

            Projectile newProjectile = 
                    (Projectile) damagerEnt.getWorld()
                                           .spawnEntity( damagerEnt.getLocation().add( damagerEnt.getVelocity() ),
                                                         damagerEnt.getType() );
            if  (   shooter != null 
                    && shooter instanceof ProjectileSource ) 
                newProjectile.setShooter( (ProjectileSource) shooter );

            newProjectile.setVelocity( damagerEnt.getVelocity() );
            newProjectile.setTicksLived( damagerEnt.getTicksLived() );
            return;
        }
        // set damage dealt by a sentry
        double damage = instDamager.strength;
        
        if ( instDamager.myEnchants != null ) {
            
            for ( Entry<Enchantment, Integer> each : instDamager.myEnchants.entrySet() ) {
                if ( each.getKey() == Enchantment.DAMAGE_ALL ) {
                    damage += 0.5 + 0.5 * each.getValue();
                }
                else if ( each.getKey() == Enchantment.DAMAGE_UNDEAD ) {
                    if ( undead.contains( victim.getType() ) )
                        damage += 2.5 * each.getValue();
                }
                else if ( each.getKey() == Enchantment.DAMAGE_ARTHROPODS ) {
                    if ( arthropods.contains( victim.getType() ) )
                        damage += 2.5 * each.getValue();
                }
                else if ( each.getKey() == Enchantment.ARROW_DAMAGE ) {
                    switch ( each.getValue() ) {
                        case 1: damage += 3; break;
                        case 2: damage += 5; break;
                        case 3: damage += 6; break;
                        case 4: damage += 8; break;
                        case 5: damage += 9; break;
                    }
                }
            }
        }
        event.setDamage( damage );

        // uncancel if not bodyguard.
        if ( instDamager.guardeeName == null || !Sentries.bodyguardsObeyProtection )
            event.setCancelled( false );

        // don't hurt guard target.
        if ( victim == instDamager.guardeeEntity ) event.setCancelled( true );
        
        if ( victim.hasMetadata( "NPC" ) )
            event.setCancelled( Sentries.registry.getNPC( victim ).isProtected() );
        
        if ( Sentries.debug )
            Sentries.debugLog( "Damage: from:" + shooter.getName() + " to:" + victim.getName() + " isCancelled:[" 
                            + event.isCancelled() + "] damage:["  + event.getDamage() + "] cause:" + event.getCause() );   
        
        if ( event.isCancelled() ) return;

        if ( instDamager.weaponSpecialEffects != null )
            victim.addPotionEffects( instDamager.weaponSpecialEffects );

        if ( instDamager.isWarlock1() ) {
            // Warlock1 sentries should do no direct damage, except to entities who take fall damage.
            // Their strength value is used as the number of blocks vertically the victim will be thrown.
            event.setCancelled( true );
            victim.getVelocity().setY( Math.sqrt( instDamager.strength * 0.16 ) );
        }
    }
    
    private EnumSet<EntityType> undead = EnumSet.of( EntityType.SKELETON, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, 
                                                     EntityType.PIG_ZOMBIE, EntityType.WITHER, EntityType.WITHER_SKELETON,
                                                     EntityType.STRAY, EntityType.HUSK );
    private EnumSet<EntityType> arthropods = EnumSet.of( EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.SILVERFISH, 
                                                         EntityType.ENDERMITE );
    
    @EventHandler( priority = EventPriority.HIGHEST )
    public void processNPCdamage( NPCDamageByEntityEvent event ) {
        // handles damage to sentries - including critical hits (if enabled)

        NPC npc = event.getNPC();
        SentryTrait instVictim = Utils.getSentryTrait( npc );
       
        if ( instVictim == null || instVictim.invincible ) return;
            
        // stop repeated calls to this event handler for the same NPC
        if ( System.currentTimeMillis() < instVictim.okToTakedamage + 500 ) return;
        instVictim.okToTakedamage = System.currentTimeMillis();
                    
        // Damage to a sentry cannot be handled by the server.
        event.setCancelled( true );

        LivingEntity damager = (LivingEntity) Utils.getSource( event.getDamager() );
        
        // don't take damage from the entity the sentry is guarding.
        if ( damager == null || damager == instVictim.guardeeEntity ) return;  
        
        if ( isNotVulnerable( instVictim, event.getCause() ) ) return;
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
                        && inst.events.stream().anyMatch( e -> e.includes( victim ) ) ) {
                    
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
                event.setDroppedExp( Sentries.sentryEXP );
            }
            
            List<ItemStack> items = new ArrayList<>();
            EntityEquipment equip = deceased.getEquipment();
        
        
            for ( ItemStack is : equip.getArmorContents() ) {
    
                if ( is != null && is.getType() != null )
                    items.add( is );
            }
            ItemStack is = equip.getItemInMainHand();
    
            if ( is.getType() != null ) items.add( is );
    
            is = equip.getItemInOffHand();
    
            if ( is.getType() != null ) items.add( is );
    
            equip.clear();

            if ( inst.dropInventory ) 
                items.stream().forEach( i -> deceased.getWorld().dropItemNaturally( deceased.getLocation(), i ) );          
                 
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
            
            if ( inst != null ) {
                // the isOwnedBy() method adds a UUID to the owner trait if it is missing.
                if  (   each.getTrait( Owner.class ).isOwnedBy( player ) 
                        && inst.ignores.removeIf( i -> ( i instanceof OwnerTarget ) ) ) {
                    CommandHandler.callCommand( inst, "ignore", "add", "owner" );
                }
                
                if ( inst.isBodyguardOnLeave() )
                    inst.checkForGuardee( player );
            }
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
                && ThrownEntities.hasThrower( event.getEntity() ) ) 
            event.blockList().clear();
    }
    
    @EventHandler
    public void onEggBreaks( PlayerEggThrowEvent event ) {
        if ( ThrownEntities.hasThrower( event.getEgg() ) ) {
            event.setHatching( false );
        }
    }
}
