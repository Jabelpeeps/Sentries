package org.jabelpeeps.sentries;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.Navigator;
//import net.citizensnpcs.api.ai.GoalController;
//import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.npc.NPC;
//import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.NMS;

/** 
 * An Enum of states that a Sentry can be in.
 * <p>
 * Calling {@link #update(SentryTrait)} on the current status will carry out the appropriate 
 * actions, returning an updated state (if needed). */
public enum SentryStatus {

    /** Sentries with this status will have their death's handled, and then 
     * various items tidied up before having their status set to {@link SentryStatus#DEAD} */
    DIEING {

        @Override
        SentryStatus update( SentryTrait inst ) {
            // item drop handling moved to SentryListener.
            
            inst.clearTarget();

            if ( Sentries.denizenActive ) {

                DenizenHook.denizenAction( inst.getNPC(), "death", null );
                DenizenHook.denizenAction( inst.getNPC(), "death by" + inst.causeOfDeath.toString().replace( " ", "_" ), null );

                if ( inst.killer != null ) {
                    
                    Entity killer = Util.getArcher( inst.killer );

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

            if ( inst.respawnDelay == -1 ) {

                if ( inst.hasMount() ) inst.dismount();

                inst.cancelRunnable();
                inst.getNPC().destroy();
            }
            else
                inst.respawnTime = System.currentTimeMillis() + inst.respawnDelay * 1000;

            return SentryStatus.DEAD;
        }
    },    
    /** Sentries with this status will be re-spawned after the configured time. */
    DEAD {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            if (    System.currentTimeMillis() > inst.respawnTime
                    && inst.respawnDelay > 0
                    && inst.spawnLocation.getWorld().isChunkLoaded( inst.spawnLocation.getBlockX() >> 4,
                                                                    inst.spawnLocation.getBlockZ() >> 4 ) ) {

                NPC npc = inst.getNPC();
                
                if ( inst.guardeeEntity == null )
                    npc.spawn( inst.spawnLocation );
                else
                    npc.spawn( inst.guardeeEntity.getLocation().add( 2, 0, 2 ) );
                
                return SentryStatus.NOT_SPAWNED;
            }
            return this;
        }
    },    
    NOT_SPAWNED {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            inst.tryToHeal();  
            
            if ( inst.getNPC().isSpawned() ) {

                inst.reMountMount();
                return is_A_Guard( inst );
            }
            return this;
        }
    },    
    /** Sentries with this status will look for and navigate to the entities they are guarding (if set). 
     *  Once reunited with the guardee (or none it set), the status will be changed to 
     *  {@link SentryStatus#LOOKING} */
    FOLLOWING {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            LivingEntity myEntity = inst.getMyEntity();            
            NPC npc = inst.getNPC();

            if ( inst.getNavigator().isPaused() ) inst.getNavigator().setPaused( false );

            if ( myEntity.isInsideVehicle() ) 
                NMS.look( myEntity, myEntity.getVehicle().getLocation().getYaw(), 0 );

            if (    inst.guardeeEntity instanceof Player
                    && !((Player) inst.guardeeEntity).isOnline() ) {
                inst.guardeeEntity = null;
            }
            else if ( inst.guardeeName != null 
                    && inst.guardeeEntity == null
                    && !inst.findPlayerGuardEntity( inst.guardeeName ) ) {
                inst.findOtherGuardEntity( inst.guardeeName );
            }

            if ( inst.guardeeEntity != null ) {

                Location npcLoc = myEntity.getLocation();
                Location guardEntLoc = inst.guardeeEntity.getLocation();

                if (    guardEntLoc.getWorld() != npcLoc.getWorld()
                        || !inst.isMyChunkLoaded() ) {

                    if ( System.currentTimeMillis() > inst.reassesTime ) {
                        String worldname = inst.guardeeEntity.getWorld().getName();
                    
                        if ( Util.CanWarp( inst.guardeeEntity, worldname ) ) {
                            
                            inst.ifMountedGetMount().teleport( guardEntLoc.add( 1, 0, 1 ), TeleportCause.PLUGIN );
                            return SentryStatus.LOOKING;
                        }
                        
                        ((Player) inst.guardeeEntity).sendMessage( String.join( " ", npc.getName(), S.CANT_FOLLOW, worldname ) );
                        inst.guardeeEntity = null;
                        inst.reassesTime = System.currentTimeMillis() + 3000;
                    }
                    return this;
                }
                inst.getGoalController().setPaused( true );
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
                return SentryStatus.LOOKING;
            }
            return this;
        }
    },
    /** 
     *  Sentries with this status will navigate back to their spawn points, using a teleport
     *  if it is too far, or they are in a different world.
     *  <p>
     *  Once they have complete the navigation, {@link SentryListener#onFinishedNavigating()}
     *  will update their status to {@link#LOOKING}
     */
    RETURNING_TO_SPAWNPOINT {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            Navigator navigator = inst.getNavigator();
            if ( navigator.isNavigating() ) return this;
            
            LivingEntity myEntity = inst.getMyEntity(); 

            if ( inst.getNavigator().isPaused() ) inst.getNavigator().setPaused( false );

            if ( myEntity.isInsideVehicle() ) 
                NMS.look( myEntity, myEntity.getVehicle().getLocation().getYaw(), 0 );
            
            if (    myEntity.getWorld() != inst.spawnLocation.getWorld() 
                    || myEntity.getLocation().distanceSquared( inst.spawnLocation ) > 1024 ) {
                
                inst.ifMountedGetMount().teleport( inst.spawnLocation, TeleportCause.PLUGIN );
                return SentryStatus.LOOKING;
            }
            navigator.setTarget( inst.spawnLocation );
            
            return this;
        }
        
    },
    /** Sentries with this status will search for possible targets, and be receptive to _events 
     *  within their detection range. <p>  They will also heal whilst in this state. */
    LOOKING {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            inst.tryToHeal();
            
            GoalController goalController = inst.getGoalController();
            
            if ( goalController.isPaused() )
                goalController.setPaused( false );
            
            LivingEntity target = null;

            // find and set a target to attack (if no current target)
            if (    !inst.targets.isEmpty() 
                    && inst.attackTarget == null
                    && System.currentTimeMillis() > inst.reassesTime ) {

                target = inst.findTarget();
                inst.reassesTime = System.currentTimeMillis() + 3000;
                
                if ( target != null ) {
                    inst.setAttackTarget( target );
                    return SentryStatus.ATTACKING;
                }
            }
            return this;
        }
    },   
    /** The status for Sentries who are attacking! */
    ATTACKING {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            if ( !inst.isMyChunkLoaded() ) {
                inst.clearTarget();
                return SentryStatus.is_A_Guard( inst );
            }

            LivingEntity myEntity = inst.getMyEntity();

            // attack the current target
            if (    inst.attackTarget != null 
                    && !inst.attackTarget.isDead()
                    && inst.attackTarget.getWorld() == myEntity.getWorld() ) {

                Navigator navigator = inst.getNavigator();
                
                if ( !navigator.isNavigating() )
                    NMS.look( myEntity, inst.attackTarget );
                
                if ( inst.myAttack != AttackType.BRAWLER ) {

                    if ( !navigator.isPaused() ) {
                        navigator.setPaused( true );
                        navigator.cancelNavigation();
                        navigator.setTarget( inst.attackTarget, true );
                    }

                    if ( System.currentTimeMillis() > inst.oktoFire ) {

                        inst.oktoFire = (long) (System.currentTimeMillis() + inst.arrowRate * 1000.0);
                        inst.fire( inst.attackTarget );
                    }
                }
                else if (    navigator.getEntityTarget() == null
                            || navigator.getEntityTarget().getTarget() != inst.attackTarget ) {
                        
                    GoalController goalController = inst.getGoalController();
                    // pause goalcontroller to keep sentry focused on this attack
                    if ( !goalController.isPaused() )
                        goalController.setPaused( true );

                    navigator.setTarget( inst.attackTarget, true );
                    navigator.getLocalParameters().speedModifier( inst.getSpeed() );
                    navigator.getLocalParameters().stuckAction( SentryTrait.giveup );
                    navigator.getLocalParameters().stationaryTicks( 5 * 20 );                 
                }

                double dist = inst.attackTarget.getLocation().distanceSquared( myEntity.getLocation() );
                // is it still in range? then keep attacking...
                if ( dist <= inst.range * inst.range )
                    return this;
            }            
            // somehow we failed to attack the chosen target, so lets clear it, and look for another.
            inst.clearTarget();
            return SentryStatus.is_A_Guard( inst );
        }
    };

    /** Call this method to perform the activities needed for the current state. 
     *  @param inst - the SentryTrait instance to be updated.
     *  @return a new state when a change is needed. */
    abstract SentryStatus update( SentryTrait inst );
    
    /** Convenience method that returns {@link SentryStatus#FOLLOWING} for guards, 
     *  and {@link SentryStatus#RETURNING_TO_SPAWNPOINT} for non-guards. */
    static SentryStatus is_A_Guard( SentryTrait inst ) {
        
        if ( inst.guardeeName == null )
            return SentryStatus.RETURNING_TO_SPAWNPOINT;
        // else...
        return SentryStatus.FOLLOWING;
    }
    
    boolean isDeadOrDieing() {
        return this == DEAD || this == DIEING;
    }
}
