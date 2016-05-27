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

import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.npc.NPC;

/** 
 * An Enum of states that a Sentry can be in.
 * <p>
 * Calling {@link #update(SentryTrait)} on the current status will carry out the appropriate 
 * actions, returning an updated state (if needed). */
enum SentryStatus {

    /** Sentries with this status will have their death's handled, along with any drops, and then 
     * various items tidied up before having their status set to {@link SentryStatus#isDEAD} */
    isDYING {

        @Override
        SentryStatus update( SentryTrait inst ) {

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

            if ( inst.dropInventory ) {
                myEntity.getWorld()
                        .spawn( myEntity.getLocation(), ExperienceOrb.class )
                        .setExperience( Sentries.sentryEXP );
            }
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

            return SentryStatus.isDEAD;
        }
    },
    
    /** Sentries with this status will be re-spawned after the configured time. */
    isDEAD {

        @Override
        SentryStatus update( SentryTrait inst ) {

            if (    System.currentTimeMillis() > inst.isRespawnable
                    && inst.respawnDelay > 0
                    && inst.spawnLocation.getWorld().isChunkLoaded( inst.spawnLocation.getBlockX() >> 4,
                                                                    inst.spawnLocation.getBlockZ() >> 4 ) ) {

                NPC npc = inst.getNPC();
                
                if ( Sentries.debug )
                    Sentries.debugLog( "respawning" + npc.getName() );

                if ( inst.guardeeEntity == null )
                    npc.spawn( inst.spawnLocation );
                else
                    npc.spawn( inst.guardeeEntity.getLocation().add( 2, 0, 2 ) );
                
                return SentryStatus.isSPAWNING;
            }
            return this;
        }
    },
    
    isSPAWNING {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            if ( inst.getNPC().isSpawned() ) {
                
                inst.tryToHeal();
                inst.reMountMount();

                return isGuard( inst );
            }
            return this;
        }
    },
    
    /** Sentries with this status will look for and navigate to the entities they are guarding (if set). 
     *  Once reunited with the guardee (or none it set), the status will be changed to 
     *  {@link SentryStatus#isLOOKING} */
    isFOLLOWING {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            LivingEntity myEntity = inst.getMyEntity();
            
            NPC npc = inst.getNPC();

            if ( inst.getNavigator().isPaused() ) inst.getNavigator().setPaused( false );

            if ( myEntity.isInsideVehicle() ) inst.faceAlignWithVehicle();

            if (    inst.guardeeEntity instanceof Player
                    && !((Player) inst.guardeeEntity).isOnline() ) {
                inst.guardeeEntity = null;
                // TODO With no-one to guard, what should the Sentry do now?
            }
            else if ( inst.guardeeName != null 
                    && inst.guardeeEntity == null
                    && !inst.findGuardEntity( inst.guardeeName, false ) ) {
                inst.findGuardEntity( inst.guardeeName, true );
            }

            if ( inst.guardeeEntity != null ) {

                Location npcLoc = myEntity.getLocation();
                Location guardEntLoc = inst.guardeeEntity.getLocation();

                if (    guardEntLoc.getWorld() != npcLoc.getWorld()
                        || !inst.isMyChunkLoaded() ) {

                    if ( System.currentTimeMillis() > inst.oktoreasses ) {
                        String worldname = inst.guardeeEntity.getWorld().getName();
                    
                        if ( Util.CanWarp( inst.guardeeEntity, worldname ) ) {
                            
                            inst.ifMountedGetMount().teleport( guardEntLoc.add( 1, 0, 1 ), TeleportCause.PLUGIN );
                            return SentryStatus.isLOOKING;
                        }
                        
                        ((Player) inst.guardeeEntity).sendMessage( String.join( " ", npc.getName(), S.CANT_FOLLOW, worldname ) );
                        inst.guardeeEntity = null;
                        inst.oktoreasses = System.currentTimeMillis() + 3000;
                    }
                    return this;
                }
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
                    return this;
                }
                else if ( dist < inst.followDistance && isNavigating ) {
                    navigator.cancelNavigation();
                }
                return SentryStatus.isLOOKING;
            }
            return this;
        }
    },
    
    /** Sentries with this status will search for possible targets, and be receptive to events 
     *  within their detection range. <p>  They will also heal whilst in this state. */
    isLOOKING {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            inst.tryToHeal();
            
            LivingEntity target = null;

            // find and set a target to attack (if no current target)
            if (    inst.targetFlags > 0 
                    && inst.attackTarget == null
                    && System.currentTimeMillis() > inst.oktoreasses ) {

                target = inst.findTarget( inst.sentryRange );
                
                if ( target != null ) {
                    inst.oktoreasses = System.currentTimeMillis() + 3000;
                    inst.setAttackTarget( target );
                    return SentryStatus.isATTACKING;
                }
            }
            return this;
        }
    },
    
    /** The status for Sentries who are attacking! */
    isATTACKING {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            if ( !inst.isMyChunkLoaded() ) {
                inst.clearTarget();
                return SentryStatus.isGuard( inst );
            }

            LivingEntity myEntity = inst.getMyEntity();

            // attack the current target
            if (    inst.attackTarget != null 
                    && !inst.attackTarget.isDead()
                    && inst.attackTarget.getWorld() == myEntity.getLocation().getWorld() ) {

                Navigator navigator = inst.getNavigator();
                
                if ( !navigator.isNavigating() )
                    inst.faceEntity( myEntity, inst.attackTarget );
                
                if ( inst.myAttacks != AttackType.brawler ) {

                    if ( inst._projTargetLostLoc == null )
                        inst._projTargetLostLoc = inst.attackTarget.getLocation();



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
                }
                else {
                    // check if the desired target is already the current destination.
                    if (    navigator.getEntityTarget() == null
                            || navigator.getEntityTarget().getTarget() != inst.attackTarget ) {
                        
                        GoalController goalController = inst.getGoalController();
                        // pause goalcontroller to keep sentry focused on this attack
                        if ( !goalController.isPaused() )
                            goalController.setPaused( true );
    
                        navigator.setTarget( inst.attackTarget, true );
                        navigator.getLocalParameters().speedModifier( inst.getSpeed() );
                        navigator.getLocalParameters().stuckAction( inst.giveup );
                        navigator.getLocalParameters().stationaryTicks( 5 * 20 );
                    }
                }
                
                if ( inst.hasMount() )
                    inst.faceEntity( myEntity, inst.attackTarget );

                double dist = inst.attackTarget.getLocation().distance( myEntity.getLocation() );
                // block if in range
                inst.draw( dist < 3 );
                // is it still in range? then keep attacking...
                if ( dist <= inst.sentryRange )
                    return this;
            }
            
            // somehow we failed to attack the chosen target, so lets clear it, and look for another.
            inst.clearTarget();
            return SentryStatus.isGuard( inst );
        }
    };

    /** Call this method to perform the activities needed for the current state. 
     *  @param inst - the SentryTrait instance to be updated.
     *  @return a new state when a change is needed. */
    abstract SentryStatus update( SentryTrait inst );
    
    /** Convenience method that returns {@link SentryStatus#isFOLLOWING} for guards, 
     *  and {@link SentryStatus#isLOOKING} for non-guards. */
    static SentryStatus isGuard( SentryTrait inst ) {
        
        if ( inst.guardeeName == null )
            return SentryStatus.isLOOKING;
        
        return SentryStatus.isFOLLOWING;
    }
}
