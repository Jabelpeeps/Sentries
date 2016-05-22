package org.jabelpeeps.sentry;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.EntityEffect;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

enum SentryStatus {

    isDEAD {

        @Override
        boolean update( SentryInstance inst ) {

            if ( System.currentTimeMillis() > inst.isRespawnable
                    && inst.respawnDelay > 0
                    && inst.spawnLocation.getWorld().isChunkLoaded(
                            inst.spawnLocation.getBlockX() >> 4,
                            inst.spawnLocation.getBlockZ() >> 4 ) ) {

                if ( Sentry.debug )
                    Sentry.debugLog( "respawning" + inst.getNPC().getName() );

                inst.myStatus = SentryStatus.isSPAWNING;

                if ( inst.guardEntity == null )
                    inst.getNPC().spawn( inst.spawnLocation.clone() );
                else
                    inst.getNPC().spawn(
                            inst.guardEntity.getLocation().add( 2, 0, 2 ) );

                return true;
            }
            return false;
        }
    },

    isDYING {

        @Override
        boolean update( SentryInstance inst ) {

            LivingEntity myEntity = inst.getMyEntity();

            inst.clearTarget();

            if ( Sentry.denizenActive ) {

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

                    if ( Sentry.debug )
                        Sentry.debugLog( "Running Denizen actions for " + inst.getNPC().getName() + " with killer: " + killer.toString() );

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
                myEntity.getLocation().getWorld()
                        .spawn( myEntity.getLocation(), ExperienceOrb.class )
                        .setExperience( Sentry.sentryEXP );

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

            if ( Sentry.dieLikePlayers )
                myEntity.setHealth( 0 );
            else
                Sentry.getSentry().getServer() // citizens will despawn it.
                        .getPluginManager()
                        .callEvent( new EntityDeathEvent( myEntity, items ) );

            if ( inst.respawnDelay == -1 ) {

                if ( inst.isMounted() )
                    Util.removeMount( inst.mountID );

                inst.cancelRunnable();
                inst.getNPC().destroy();
            }
            else
                inst.isRespawnable = System.currentTimeMillis()
                        + inst.respawnDelay * 1000;

            inst.myStatus = SentryStatus.isDEAD;
            return false;
        }
    },

    isSPAWNING {

        @Override
        boolean update( SentryInstance inst ) {
            // TODO Auto-generated method stub
            return false;
        }
    },
    isLOOKING {

        @Override
        boolean update( SentryInstance inst ) {
            // TODO Auto-generated method stub
            return false;
        }
    },
    isATTACKING {

        @Override
        boolean update( SentryInstance inst ) {
            // TODO Auto-generated method stub
            return false;
        }
    };

    abstract boolean update( SentryInstance inst );
}
