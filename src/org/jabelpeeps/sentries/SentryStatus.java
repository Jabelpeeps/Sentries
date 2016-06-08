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
public enum SentryStatus {

    /** Sentries with this status will have their death's handled, along with any drops, and then 
     * various items tidied up before having their status set to {@link SentryStatus#DEAD} */
    DIEING {

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
                    inst.dismount();

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

            if ( myEntity.isInsideVehicle() ) inst.faceAlignWithVehicle();

            if (    inst.guardeeEntity instanceof Player
                    && !((Player) inst.guardeeEntity).isOnline() ) {
                inst.guardeeEntity = null;
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
    
    /** Sentries with this status will search for possible targets, and be receptive to events 
     *  within their detection range. <p>  They will also heal whilst in this state. */
    LOOKING {

        @Override
        SentryStatus update( SentryTrait inst ) {
            
            inst.tryToHeal();
            
            LivingEntity target = null;

            // find and set a target to attack (if no current target)
            if (    inst.targetFlags > 0 
                    && inst.attackTarget == null
                    && System.currentTimeMillis() > inst.reassesTime ) {

                target = inst.findTarget( inst.range );
                
                if ( target != null ) {
                    inst.reassesTime = System.currentTimeMillis() + 3000;
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
                    inst.faceEntity( myEntity, inst.attackTarget );
                
                if ( inst.myAttack != AttackType.brawler ) {

                    if ( inst._projTargetLostLoc == null )
                        inst._projTargetLostLoc = inst.attackTarget.getLocation();

                    if ( !navigator.isPaused() ) {
                        navigator.setPaused( true );
                        navigator.cancelNavigation();
                        navigator.setTarget( inst.attackTarget, true );
                    }

                    inst.draw( true );

                    if ( System.currentTimeMillis() > inst.oktoFire ) {

                        inst.oktoFire = (long) (System.currentTimeMillis() + inst.arrowRate * 1000.0);
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
                        navigator.getLocalParameters().stuckAction( SentryTrait.giveup );
                        navigator.getLocalParameters().stationaryTicks( 5 * 20 );
                    }
                }
                inst.faceEntity( myEntity, inst.attackTarget );

                double dist = inst.attackTarget.getLocation().distanceSquared( myEntity.getLocation() );
                // block if in range
                inst.draw( dist < 9 );
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
     *  and {@link SentryStatus#LOOKING} for non-guards. */
    static SentryStatus is_A_Guard( SentryTrait inst ) {
        
        if ( inst.guardeeName == null )
            return SentryStatus.LOOKING;
        
        return SentryStatus.FOLLOWING;
    }
    boolean isDeadOrDieing() {
        return this == DEAD || this == DIEING;
    }
}
