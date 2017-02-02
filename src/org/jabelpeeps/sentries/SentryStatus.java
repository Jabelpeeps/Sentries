package org.jabelpeeps.sentries;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;

/** 
 * An Enum of states that a Sentry can be in.
 * <p>
 * Calling {@link #update(SentryTrait)} on the current status will carry out the appropriate 
 * actions, returning an updated state (if needed). */
public enum SentryStatus {

    /** Sentries with this status will have their death's handled, and then 
     * various items tidied up before having their status set to {@link SentryStatus#DEAD} */
    DIEING {
        @Override SentryStatus update( SentryTrait inst ) {
            // item drop handling moved to SentryListener.
            
            inst.cancelAttack();

            if ( Sentries.denizenActive ) {
                NPC npc = inst.getNPC();

                DenizenHook.denizenAction( npc, "death", null );
                DenizenHook.denizenAction( npc, "death by" + inst.causeOfDeath.toString().replace( " ", "_" ), null );

                if ( inst.killer != null ) {
                    
                    Entity killer = Utils.getSource( inst.killer );

                    if ( Sentries.debug )
                        Sentries.debugLog( "Running Denizen actions for " + npc.getName() + " with killer: " + killer.toString() );

                    if ( killer instanceof OfflinePlayer )
                        DenizenHook.denizenAction( npc, "death by player", (OfflinePlayer) killer );
                    else {
                        DenizenHook.denizenAction( npc, "death by entity", null );
                        DenizenHook.denizenAction( npc, "death by " + killer.getType().toString(), null );
                    }
                }
            }
            if ( inst.respawnDelay == -1 ) {
                if ( inst.hasMount() ) inst.dismount();
                inst.cancelRunnable();
                inst.getNPC().destroy();
            }
            else
                inst.kill();

            return SentryStatus.DEAD;
        }
    },    
    /** Sentries with this status will be switched to {@link SentryStatus#NOT_SPAWNED} 
     *  after the configured respawn time. */
    DEAD {
        @Override SentryStatus update( SentryTrait inst ) {
            
            if (    System.currentTimeMillis() > inst.respawnTime
                    && inst.respawnDelay > 0 ) {                
                return SentryStatus.NOT_SPAWNED;
            }
            return this;
        }
    }, 
    /** Sentries with this status, will attempt to spawn at either their spawnLocation, or 
     *  the location of the entity they are guarding. */
    NOT_SPAWNED {
        @Override SentryStatus update( SentryTrait inst ) {
            
            inst.tryToHeal();             
            NPC npc = inst.getNPC();
            
            if ( npc.isSpawned() ) {
                inst.reMountMount();
                return SentryStatus.LOOKING;
            }
            if ( inst.isBodyguardOnLeave() ) return this;
            
            if ( inst.guardeeEntity != null ) {
                Location loc = inst.guardeeEntity.getLocation();
                if ( Utils.CanWarp( inst.guardeeEntity, loc.getWorld().getName() ) )
                    npc.spawn( loc.add( 1, 0, 1 ) );
            }  
            else if  ( Util.isLoaded( inst.spawnLocation ) ) {
                npc.spawn( inst.spawnLocation );
            }          
            return this;
        }
    },    
    /** Sentries with this status will look for and navigate to the entities they are guarding (if set). 
     *  Once reunited with the guardee (or none it set), the status will be changed to 
     *  {@link SentryStatus#LOOKING} */
    FOLLOWING {
        @Override SentryStatus update( SentryTrait inst ) {
            
            Entity myEntity = inst.getNPC().getEntity(); 
            if ( myEntity == null ) return SentryStatus.NOT_SPAWNED;
            
            if ( inst.guardeeEntity == null ) return checkPosition( inst );
            
            Navigator navigator = inst.getNavigator();
            Location guardEntLoc = inst.guardeeEntity.getLocation();
            boolean isFlying = isFlying( myEntity.getType() );
            
            if  (   isFlying
                    || !navigator.isNavigating()
                    || navigator.getEntityTarget() == null
                    || !navigator.getEntityTarget().getTarget().equals( inst.guardeeEntity ) ) {
                             
                if ( isFlying ) guardEntLoc = guardEntLoc.add( 0, 5, 0 ); 
                
                if ( navigateOrTP( inst, myEntity, guardEntLoc.add( 1, 0, 1 ) ) ) {
                    
                    if ( isFlying ) 
                        navigator.setTarget( guardEntLoc );
                    else 
                        navigator.setTarget( inst.guardeeEntity, false );
                    
                    navigator.getLocalParameters().distanceMargin( inst.followDistance - 1 );             
                    return this;
                }
            }
            return checkPosition( inst );
        }
    },
    /** Sentries with this status will navigate back to their spawn points, using a teleport
     *  if it is too far, or they are in a different world.
     *  <p>
     *  Once they have complete the navigation, {@link SentryListener#onFinishedNavigating()}
     *  will update their status to {@link#LOOKING}
     */
    RETURNING_TO_SPAWNPOINT {
        @Override SentryStatus update( SentryTrait inst ) {
            inst.tryToHeal();
            
            Entity myEntity = inst.getNPC().getEntity(); 
            if ( myEntity == null ) return SentryStatus.NOT_SPAWNED;
            
            Navigator navigator = inst.getNavigator();
            if  (   !navigator.isNavigating() 
                    || !navigator.getTargetAsLocation().getBlock().equals( inst.spawnLocation.getBlock() ) ) {
                if ( navigateOrTP( inst, myEntity, inst.spawnLocation ) )
                    navigator.setTarget( inst.spawnLocation );
            }
            return checkPosition( inst );
        }
    },
    /** A status for Sentries who have become stuck while navigating, or have become separated from 
     *  the entity they are guarding. */
    STUCK {
        @Override SentryStatus update( SentryTrait inst ) {
            return checkPosition( inst );
        }
    },
    /** Sentries with this status will search for possible targets, and be receptive to events 
     *  within their detection range. <p>  They will also heal whilst in this state. */
    LOOKING {
        @Override SentryStatus update( SentryTrait inst ) {
            
            inst.tryToHeal();

            if  (   !inst.targets.isEmpty() 
                    && !inst.isBodyguardOnLeave()
                    && System.currentTimeMillis() > inst.reassesTime ) {

                LivingEntity target = inst.findTarget();
                inst.reassesTime = System.currentTimeMillis() + 1000;
                
                if  (   target != null &&
                        inst.setAttackTarget( target ) ) {
                    return SentryStatus.ATTACKING;
                }
            }
            return checkPosition( inst );
        }
    },   
    /** The status for Sentries who are attacking! */
    ATTACKING {
        @Override SentryStatus update( SentryTrait inst ) {
               
            Entity myEntity = inst.getNPC().getEntity();

            if (    myEntity != null
                    && inst.attackTarget != null 
                    && !inst.attackTarget.isDead()
                    && inst.attackTarget.getWorld() == myEntity.getWorld() ) {
                Navigator navigator = inst.getNavigator();
                
                if  (   navigator.getEntityTarget() == null 
                        || navigator.getEntityTarget().getTarget() != inst.attackTarget ) {
                    navigator.setTarget( inst.attackTarget, true );
                    
                    NavigatorParameters params = navigator.getLocalParameters();
                    params.attackStrategy( inst.getMyAttack() );

                    double rangeSqrd = Utils.sqr( inst.getMyAttack().getApproxRange() );
                    params.attackRange( rangeSqrd );  
                    params.distanceMargin( rangeSqrd );
                }
            } 
            // somehow we failed to attack the chosen target, so lets clear it.           
            else inst.cancelAttack();
            
            return checkPosition( inst );
        }
    };

    /** Call this method to perform the activities needed for the current state. 
     *  @param inst - the SentryTrait instance to be updated.
     *  @return a new state when a change is needed. */
    abstract SentryStatus update( SentryTrait inst );
    
    /** Checks whether the sentry is too far away from their spawn point, the entity they are guarding, or the
     *  entity they are attacking.
     *  @return a new status for the Sentry if one is needed, otherwise the current status. */
    protected SentryStatus checkPosition( SentryTrait inst ) {
        
        Location myLocation = inst.getNPC().getStoredLocation();
        
        if ( inst.guardeeEntity != null ) {
            // Note to self:- inst.followDistance holds the square of the intended distance.
            if ( myLocation.distanceSquared( inst.guardeeEntity.getLocation() ) >= inst.followDistance ) {
                return SentryStatus.FOLLOWING;
            }           
        }
        else if ( myLocation.distanceSquared( inst.spawnLocation ) > Utils.sqr( inst.range ) ) {
            return SentryStatus.RETURNING_TO_SPAWNPOINT;
        } 
        
        switch ( this ) {
            case ATTACKING:
                if  (   inst.attackTarget == null 
                        || inst.attackTarget.isDead()
                        || inst.attackTarget.getWorld() != myLocation.getWorld()
                        || myLocation.distanceSquared( inst.attackTarget.getLocation() ) > Utils.sqr( inst.range ) ) {
                    inst.cancelAttack();
                    return SentryStatus.LOOKING;                   
                }
                break;
            case RETURNING_TO_SPAWNPOINT:
                if ( myLocation.distanceSquared( inst.spawnLocation ) > 1 ) break;
            case FOLLOWING: 
            case STUCK:
                inst.getNavigator().cancelNavigation();
                return SentryStatus.LOOKING;
            default:           
        }
        return this;
    }
    
    /** @return true if the navigator destination needs to be set, false if the distance is 
     *  too far - this method will have already teleported the npc. */
    protected boolean navigateOrTP( SentryTrait inst, Entity myEntity, Location loc ) {
        
        if ( loc.getWorld() != myEntity.getWorld() ) {
            if ( System.currentTimeMillis() > inst.reassesTime ) {
                String worldname = loc.getWorld().getName();
            
                if ( Utils.CanWarp( inst.guardeeEntity, worldname ) ) {
                    inst.ifMountedGetMount().teleport( loc, TeleportCause.PLUGIN );
                    return false;
                }
                Utils.sendMessage( inst.guardeeEntity, inst.getNPC().getName(), S.CANT_FOLLOW, worldname );
                inst.reassesTime = System.currentTimeMillis() + 5000;
            }
            return false;
        } 
        
        double distanceSqrd = myEntity.getLocation().distanceSquared( loc );
        
        if ( distanceSqrd < 2 ) return false;

        if ( distanceSqrd > 1024 ) {   // equals a distance > 32 blocks             
            inst.ifMountedGetMount().teleport( loc, TeleportCause.PLUGIN );
            return false;
        }           
        if ( myEntity.isInsideVehicle() ) 
            NMS.setHeadYaw( myEntity, myEntity.getVehicle().getLocation().getYaw() );  
        
        return true;
    }
    
    private static EnumSet<EntityType> flying = EnumSet.of( EntityType.ENDER_DRAGON, EntityType.BLAZE, EntityType.GHAST );
    boolean isFlying( EntityType type ) { return flying.contains( type ); }
    private static EnumSet<SentryStatus> deadOrDieing = EnumSet.of( DEAD, DIEING );
    boolean isDeadOrDieing() { return deadOrDieing.contains( this ); }
}
