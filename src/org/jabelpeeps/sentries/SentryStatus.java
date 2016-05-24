package org.jabelpeeps.sentries;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.npc.NPC;

/** 
 * An Enum of states that a Sentry can be in.
 * <p>
 * Calling {@link#update(SentryTrait)} will carry out the appropriate actions, updating the state if needed.
 * */
enum SentryStatus {

    /** Sentries with this status will be respawned after the configured time. */
    isDEAD {

        @Override
        boolean update( SentryTrait inst ) {

            if (    System.currentTimeMillis() > inst.isRespawnable
                    && inst.respawnDelay > 0
                    && inst.spawnLocation.getWorld().isChunkLoaded( inst.spawnLocation.getBlockX() >> 4,
                                                                    inst.spawnLocation.getBlockZ() >> 4 ) ) {

                NPC npc = inst.getNPC();
                
                if ( Sentries.debug )
                    Sentries.debugLog( "respawning" + npc.getName() );

                inst.myStatus = SentryStatus.isSPAWNING;

                if ( inst.guardeeEntity == null )
                    npc.spawn( inst.spawnLocation );
                else
                    npc.spawn( inst.guardeeEntity.getLocation().add( 2, 0, 2 ) );

                return true;
            }
            return false;
        }
    },

    /** Sentries with this status will have their death's handled, along with any drops, and then 
     * various items tidied up before having their status set to {@link#isDEAD}*/
    isDYING {

        @Override
        boolean update( SentryTrait inst ) {

            LivingEntity myEntity = inst.getMyEntity();

            inst.clearTarget();

            if ( Sentries.denizenActive ) {

                DenizenHook.denizenAction( inst.getNPC(), "death", null );
                DenizenHook.denizenAction( inst.getNPC(), "death by" + inst.causeOfDeath.toString().replace( " ", "_" ), null );

                Entity killer = myEntity.getKiller();

                if ( killer == null ) {
                    // might have been a projectile.
                    EntityDamageEvent ev = myEntity.getLastDamageCause();
                    if (    ev != null
                            && ev instanceof EntityDamageByEntityEvent ) {
                        killer = ((EntityDamageByEntityEvent) ev).getDamager();
                    }
                }

                if ( killer != null ) {

                    if (    killer instanceof Projectile
                            && ((Projectile) killer).getShooter() != null
                            && ((Projectile) killer).getShooter() instanceof Entity )
                        killer = (Entity) ((Projectile) killer).getShooter();

                    if ( Sentries.debug )
                        Sentries.debugLog( "Running Denizen actions for " + inst.getNPC().getName() + " with killer: " + killer.toString() );

                    if ( killer instanceof OfflinePlayer ) {
                        DenizenHook.denizenAction( inst.getNPC(), "death by player", (OfflinePlayer) killer );
                    }
                    else {
                        DenizenHook.denizenAction( inst.getNPC(), "death by entity", null );
                        DenizenHook.denizenAction( inst.getNPC(), "death by " + killer.getType().toString(), null );
                    }
                }
            }

            if ( inst.dropInventory )
                myEntity.getWorld()
                        .spawn( myEntity.getLocation(), ExperienceOrb.class )
                        .setExperience( Sentries.sentryEXP );

            List<ItemStack> items = new LinkedList<ItemStack>();

            if ( myEntity instanceof HumanEntity ) {

                PlayerInventory inventory = ((HumanEntity) myEntity).getInventory();

                for ( ItemStack is : inventory.getArmorContents() ) {

                    if ( is != null && is.getType() != null )
                        items.add( is );
                }

                ItemStack is = inventory.getItemInMainHand();

                if ( is.getType() != null )
                    items.add( is );

                is = inventory.getItemInOffHand();

                if ( is.getType() != null )
                    items.add( is );

                inventory.clear();
                inventory.setArmorContents( null );
                inventory.setItemInMainHand( null );
                inventory.setItemInOffHand( null );
            }

            if ( items.isEmpty() )
                myEntity.playEffect( EntityEffect.DEATH );
            else
                myEntity.playEffect( EntityEffect.HURT );

            if ( !inst.dropInventory )
                items.clear();

            for ( ItemStack is : items )
                myEntity.getWorld().dropItemNaturally( myEntity.getLocation(), is );

            if ( Sentries.dieLikePlayers )
                myEntity.setHealth( 0 );
            else
                Bukkit.getPluginManager().callEvent( new EntityDeathEvent( myEntity, items ) );

            if ( inst.respawnDelay == -1 ) {

                if ( inst.hasMount() )
                    Util.removeMount( inst.mountID );

                inst.cancelRunnable();
                inst.getNPC().destroy();
            }
            else
                inst.isRespawnable = System.currentTimeMillis() + inst.respawnDelay * 1000;

            inst.myStatus = SentryStatus.isDEAD;
            return false;
        }
    },

    isSPAWNING {

        @Override
        boolean update( SentryTrait inst ) {
            
            if ( inst.getNPC().isSpawned() )
                inst.myStatus = SentryStatus.isLOOKING;
            return false;
        }
    },
    
    /** Sentries with this status will first look for and navigate to the entities they are guarding (if set) 
     * and will then scan for possible targets to attack. */
    isLOOKING {

        @Override
        boolean update( SentryTrait inst ) {
            
            LivingEntity myEntity = inst.getMyEntity();
            NPC npc = inst.getNPC();

            if ( inst.getNavigator().isPaused() ) {
                inst.getNavigator().setPaused( false );
            }

            if ( myEntity.isInsideVehicle() == true )
                inst.faceAlignWithVehicle();

            if (    inst.guardeeEntity instanceof Player
                    && !((Player) inst.guardeeEntity).isOnline() ) {
                inst.guardeeEntity = null;
            }
            else if ( inst.guardeeName != null 
                    && inst.guardeeEntity == null
                    && inst.findGuardEntity( inst.guardeeName, false ) ) {
                inst.findGuardEntity( inst.guardeeName, true );
            }

            if ( inst.guardeeEntity != null ) {

                Location npcLoc = myEntity.getLocation();
                Location guardEntLoc = inst.guardeeEntity.getLocation();

                if (    guardEntLoc.getWorld() != npcLoc.getWorld()
                        || !inst.isMyChunkLoaded() ) {

                    String worldname = inst.guardeeEntity.getWorld().getName();
                    
                    if ( Util.CanWarp( inst.guardeeEntity, worldname ) ) {
                        
                        inst.ifMountedGetMount().teleport( guardEntLoc.add( 1, 0, 1 ), TeleportCause.PLUGIN );
                    }
                    else {
                        ((Player) inst.guardeeEntity).sendMessage( String.join( " ", npc.getName(), S.CANT_FOLLOW, worldname ) );
                        inst.guardeeEntity = null;
                    }
                }
                else {
                    Navigator navigator = inst.getNavigator();
                    
                    boolean isNavigating = navigator.isNavigating();
                    double dist = npcLoc.distanceSquared( guardEntLoc );

                    if ( Sentries.debug )
                        Sentries.debugLog( npc.getName() + ": following " + navigator.getEntityTarget().getTarget().getName() );

                    if ( dist > 1024 ) {
                        inst.ifMountedGetMount().teleport( guardEntLoc.add( 1, 0, 1 ), TeleportCause.PLUGIN );
                    }
                    else if ( dist > inst.followDistance && !isNavigating ) {
                        navigator.setTarget( inst.guardeeEntity, false );
                        navigator.getLocalParameters().stationaryTicks( 3 * 20 );
                    }
                    else if ( dist < inst.followDistance && isNavigating ) {
                        navigator.cancelNavigation();
                    }
                }
            }

            LivingEntity target = null;

            if ( inst.targetFlags > 0 ) {
                target = inst.findTarget( inst.sentryRange );
            }

            if ( target != null ) {
                inst.oktoreasses = System.currentTimeMillis() + 3000;
                inst.setAttackTarget( target );
            }
            return false;
        }
    },
    
    isATTACKING {

        @Override
        boolean update( SentryTrait inst ) {
            
            if ( !inst.isMyChunkLoaded() ) {
                inst.clearTarget();
                return false;
            }

            // find and set a target to attack (if no current target)
            if (    inst.targetFlags > 0 
                    && inst.attackTarget == null
                    && System.currentTimeMillis() > inst.oktoreasses ) {

                LivingEntity target = inst.findTarget( inst.sentryRange );
                inst.setAttackTarget( target );
                inst.oktoreasses = System.currentTimeMillis() + 3000;
            }
            
            LivingEntity myEntity = inst.getMyEntity();

            // attack the current target
            if (    inst.attackTarget != null 
                    && !inst.attackTarget.isDead()
                    && inst.attackTarget.getWorld() == myEntity.getLocation().getWorld() ) {

                if ( inst.myAttacks != AttackType.brawler ) {

                    if ( inst._projTargetLostLoc == null )
                        inst._projTargetLostLoc = inst.attackTarget.getLocation();

                    Navigator navigator = inst.getNavigator();
                    
                    if ( !navigator.isNavigating() )
                        inst.faceEntity( myEntity, inst.attackTarget );

                    if ( !navigator.isPaused() ) {
                        navigator.setPaused( true );
                        navigator.cancelNavigation();
                        navigator.setTarget( inst.attackTarget, true );
                    }

                    inst.draw( true );

                    if ( System.currentTimeMillis() > inst.oktoFire ) {

                        inst.oktoFire = (long) (System.currentTimeMillis() + inst.attackRate * 1000.0);
                        inst.fire( inst.attackTarget );
                    }

                    if ( inst.attackTarget != null )
                        inst._projTargetLostLoc = inst.attackTarget.getLocation();

                    return true;
                }
                
                // section for brawlers only
                if ( inst.hasMount() )
                    inst.faceEntity( myEntity, inst.attackTarget );

                double dist = inst.attackTarget.getLocation().distance( myEntity.getLocation() );
                // block if in range
                inst.draw( dist < 3 );
                // Did it get away?
                if ( dist > inst.sentryRange )
                    inst.clearTarget();
            }
            else
                // somehow we failed to attack the chosen target, so lets clear it.
                inst.clearTarget();
            return false;
        }
    };

    abstract boolean update( SentryTrait inst );
}
