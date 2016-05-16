package org.jabelpeeps.sentry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_9_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_9_R1.inventory.CraftItemStack;
/////////////////////////
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Horse;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Spider;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jabelpeeps.sentry.attackstrategies.CreeperAttackStrategy;
import org.jabelpeeps.sentry.attackstrategies.MountAttackStrategy;
import org.jabelpeeps.sentry.attackstrategies.SpiderAttackStrategy;
import org.jabelpeeps.sentry.pluginbridges.TownyBridge;

import net.aufdemrand.denizen.npc.traits.HealthTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;
import net.minecraft.server.v1_9_R1.EntityHuman;
import net.minecraft.server.v1_9_R1.EntityPotion;
import net.minecraft.server.v1_9_R1.Packet;
import net.minecraft.server.v1_9_R1.PacketPlayOutAnimation;

public class SentryInstance {

    Sentry sentry;

    Location _projTargetLostLoc;
    Location spawnLocation;

    int strength = 1;
    int armorValue = 0;
    int epCount = 0;
    int nightVision = 16;
    int respawnDelay = 10;
    int sentryRange = 10;
    int followDistance = 16;
    int mountID = -1;
    int warningRange = 0;

    float sentrySpeed = 1.0f;

    double attackRate = 2.0;
    double healRate = 0.0;
    double sentryWeight = 1.0;
    double sentryMaxHealth = 20.0;

    boolean killsDropInventory = true;
    boolean dropInventory = false;
    boolean targetable = true;
    boolean invincible = false;
    boolean loaded = false;
    boolean acceptsCriticals = true;
    boolean iWillRetaliate = true;
    boolean ignoreLOS = false;
    boolean mountCreated = false;

    private GiveUpStuckAction giveup = new GiveUpStuckAction( this );

    String greetingMsg = "";
    String warningMsg = "";

    private Map<Player, Long> warningsGiven = new HashMap<Player, Long>();
    Set<Player> _myDamamgers = new HashSet<Player>();

    LivingEntity guardEntity;
    LivingEntity attackTarget;
    String guardTarget;
    DamageCause causeOfDeath;

    Packet<?> healAnimation;

    Set<String> ignoreTargets = new HashSet<String>();
    Set<String> validTargets = new HashSet<String>();

    Set<String> _ignoreTargets = new HashSet<String>();
    Set<String> _validTargets = new HashSet<String>();

    // TODO why are we saving four instances of the system time?
    long isRespawnable = 0;
    long oktoFire = System.currentTimeMillis();
    long oktoheal = System.currentTimeMillis();
    long oktoreasses = System.currentTimeMillis();
    long okToTakedamage = 0;

    List<PotionEffect> weaponSpecialEffects;
    ItemStack potiontype;
    Random random = new Random();

    SentryStatus myStatus = SentryStatus.isSPAWNING;
    AttackType myAttacks;
    // public SentryTrait myTrait; // unused?
    public NPC myNPC;

    private int taskID = 0;

    public final int myID;
    private static int nextID;

    static {
        nextID = 0;
    }

    SentryInstance( Sentry plugin ) {
        myID = ++nextID;
        sentry = plugin;
        isRespawnable = System.currentTimeMillis();
    }

    public void initialize() {

        LivingEntity myEntity = getMyEntity();

        // check for illegal values
        if ( sentryWeight <= 0 )
            sentryWeight = 1.0;
        if ( attackRate > 30 )
            attackRate = 30.0;
        if ( sentryMaxHealth < 0 )
            sentryMaxHealth = 0;
        if ( sentryRange < 1 )
            sentryRange = 1;
        if ( sentryRange > 200 )
            sentryRange = 200;
        if ( sentryWeight <= 0 )
            sentryWeight = 1.0;
        if ( respawnDelay < -1 )
            respawnDelay = -1;
        if ( spawnLocation == null )
            spawnLocation = myEntity.getLocation();

        // Allow Denizen to handle the sentry's health if it is active.
        if ( Sentry.denizenActive ) {
            if ( myNPC.hasTrait( HealthTrait.class ) )
                myNPC.removeTrait( HealthTrait.class );
        }

        // disable citizens respawning, because Sentry doesn't always raise
        // EntityDeath
        myNPC.data().set( NPC.RESPAWN_DELAY_METADATA, -1 );

        setHealth( sentryMaxHealth );

        _myDamamgers.clear();
        myStatus = SentryStatus.isLOOKING;

        faceForward();

        healAnimation = new PacketPlayOutAnimation(
                ((CraftEntity) myEntity).getHandle(), 6 );

        if ( guardTarget == null )
            myNPC.teleport( spawnLocation, TeleportCause.PLUGIN );

        NavigatorParameters navigatorParams = myNPC.getNavigator()
                .getDefaultParameters();
        float myRange = navigatorParams.range();

        if ( myRange < sentryRange + 5 ) {
            myRange = sentryRange + 5;
        }

        myNPC.data().set( NPC.DEFAULT_PROTECTED_METADATA, false );
        myNPC.data().set( NPC.TARGETABLE_METADATA, targetable );

        navigatorParams.range( myRange );
        navigatorParams.stationaryTicks( 5 * 20 );
        navigatorParams.useNewPathfinder( false );

        if ( myEntity instanceof Creeper )
            navigatorParams.attackStrategy( new CreeperAttackStrategy() );
        else if ( myEntity instanceof Spider )
            navigatorParams.attackStrategy( new SpiderAttackStrategy() );

        // as far as I can see the initialise() method is only called from
        // SentryTrait.onSpawn(), which does a prior check that
        // the load() method has run, and the load method has a call to
        // processTargets(), so a second call isn't needed here.

        // TODO check this hasn't broken anything.
        // processTargets();

        updateAttackType();

        if ( taskID == 0 ) {
            taskID = sentry.getServer()
                           .getScheduler()
                           .scheduleSyncRepeatingTask( sentry, 
                                                       new SentryLogic(), 
                                                       40 + myNPC.getId(), 
                                                       Sentry.logicTicks );
        }
    }

    public void cancelRunnable() {
        if ( taskID != 0 )
            sentry.getServer().getScheduler().cancelTask( taskID );
    }

    public boolean hasTargetType( int type ) { return (targetFlags & type) == type;
    }

    public boolean hasIgnoreType( int type ) {
        return (ignoreFlags & type) == type;
    }

    public boolean isIgnoring( LivingEntity aTarget ) {

        if ( aTarget == guardEntity )
            return true;
        if ( ignoreFlags == none )
            return false;
        if ( hasIgnoreType( all ) )
            return true;

        if ( CitizensAPI.getNPCRegistry().isNPC( aTarget ) ) {

            if ( hasIgnoreType( allnpcs ) )
                return true;

            NPC targetNpc = CitizensAPI.getNPCRegistry().getNPC( aTarget );

            if ( targetNpc != null ) {

                if ( hasIgnoreType( namednpcs )
                        && ignoresContain( "NPC:" + targetNpc.getName() ) )
                    return true;

                // As this is an NPC and we haven't decided whether to ignore it
                // yet, let check the ignores of the owner.
                return isIgnoring( (LivingEntity) sentry.getServer()
                        .getOfflinePlayer( targetNpc.getTrait( Owner.class )
                                .getOwnerId() ) );
            }
        }
        else if ( aTarget instanceof Player ) {

            if ( hasIgnoreType( allplayers ) )
                return true;

            Player player = (Player) aTarget;
            String name = player.getName();

            if ( hasIgnoreType( namedplayers )
                    && ignoresContain( "PLAYER:" + name ) )
                return true;

            if ( hasIgnoreType( owner ) && name.equalsIgnoreCase(
                    myNPC.getTrait( Owner.class ).getOwner() ) )
                return true;

            // if ( hasIgnoreType( permGroups ) &&
            // VaultBridge.checkGroups4Ignores( aTarget.getWorld(), player, this
            // ) )
            // return true;

            if ( hasIgnoreType( towny ) ) {

                String[] info = TownyBridge.getResidentTownyInfo( player );

                if ( info[1] != null && ignoresContain( "TOWN:" + info[1] ) )
                    return true;

                if ( info[0] != null && ignoresContain( "NATION:" + info[0] ) )
                    return true;
            }

            // if ( hasIgnoreType( faction ) ) {
            //
            // String factionName = FactionsBridge.getFactionsTag( player );
            //
            // if ( factionName != null
            // && ignoresContain( "FACTION:" + factionName ) )
            // return true;
            // }

            // if ( hasIgnoreType( war ) ) {
            //
            // String team = WarBridge.getWarTeam( player );
            //
            // if ( team != null
            // && ignoresContain( "WARTEAM:" + team ) )
            // return true;
            // }

            // if ( hasIgnoreType( mcTeams ) ) {
            //
            // String team = ScoreboardTeamsBridge.getMCTeamName( player );
            //
            // if ( team != null && ignoresContain( "TEAM:" + team ) )
            // return true;
            // }
            //
            // if ( hasIgnoreType( clans ) ) {
            //
            // String clan = SimpleClansBridge.getClan( player );
            //
            // if ( clan != null && ignoresContain( "CLAN:" + clan ) )
            // return true;
            // }
            for ( PluginBridge each : Sentry.activePlugins.values() ) {
                if ( hasIgnoreType( each.getBitFlag() )
                        && each.isIgnoring( player, this ) ) {
                    return true;
                }
            }
        }
        else if ( aTarget instanceof Monster && hasIgnoreType( allmonsters ) )
            return true;

        else if ( hasIgnoreType( namedentities )
                && ignoresContain( "ENTITY:" + aTarget.getType() ) )
            return true;

        return false;
    }

    public boolean isTarget( LivingEntity aTarget ) {

        if ( targetFlags == none || targetFlags == events )
            return false;

        if ( hasTargetType( all ) )
            return true;

        if ( CitizensAPI.getNPCRegistry().isNPC( aTarget ) ) {

            if ( hasTargetType( allnpcs ) )
                return true;

            NPC targetNpc = CitizensAPI.getNPCRegistry().getNPC( aTarget );

            String targetName = targetNpc.getName();

            if ( hasTargetType( namednpcs )
                    && targetsContain( "NPC:" + targetName ) )
                return true;

            // As we are checking an NPC and haven't decided whether to attack
            // it yet, lets check the owner.
            return isTarget( (LivingEntity) sentry.getServer().getOfflinePlayer(
                    targetNpc.getTrait( Owner.class ).getOwnerId() ) );
        }
        else if ( aTarget instanceof Player ) {

            if ( hasTargetType( allplayers ) )
                return true;

            Player player = (Player) aTarget;
            String name = player.getName();

            if ( hasTargetType( namedplayers )
                    && targetsContain( "PLAYER:" + name ) )
                return true;

            if ( targetsContain( "ENTITY:OWNER" ) && name.equalsIgnoreCase(
                    myNPC.getTrait( Owner.class ).getOwner() ) )
                return true;

            // if ( hasTargetType( permGroups ) &&
            // VaultBridge.checkGroups4Targets( player.getWorld(), player, this
            // ) )
            // return true;

            if ( hasTargetType( towny ) || hasTargetType( townyenemies ) ) {

                String[] info = TownyBridge.getResidentTownyInfo( player );

                if ( hasTargetType( towny ) && info[1] != null
                        && targetsContain( "TOWN:" + info[1] ) )
                    return true;

                if ( info[0] != null ) {

                    if ( hasTargetType( towny )
                            && targetsContain( "NATION:" + info[0] ) )
                        return true;

                    if ( hasTargetType( townyenemies ) )
                        for ( String each : NationsEnemies )
                            if ( TownyBridge.isNationEnemy( each, info[0] ) )
                                return true;
                }
            }

            // if ( hasTargetType( faction ) || hasTargetType( factionEnemies )
            // ) {
            //
            // String factionName = FactionsBridge.getFactionsTag( player );
            //
            // if ( factionName != null ) {
            //
            // if ( targetsContain( "FACTION:" + factionName ) )
            // return true;
            //
            // if ( hasTargetType( factionEnemies ) )
            // for ( String each : FactionEnemies )
            // if ( FactionsBridge.isFactionEnemy( each, factionName) )
            // return true;
            // }
            // }

            // if ( hasTargetType( war ) ) {
            //
            // String team = WarBridge.getWarTeam( player );
            //
            // if ( team != null && targetsContain( "WARTEAM:" + team ) )
            // return true;
            // }
            // if ( hasTargetType( mcTeams ) ) {
            //
            // String team = ScoreboardTeamsBridge.getMCTeamName( (Player)
            // aTarget );
            //
            // if ( team != null && targetsContain( "TEAM:" + team ) )
            // return true;
            // }
            // if ( hasTargetType( clans ) ) {
            //
            // String clan = SimpleClansBridge.getClan( player );
            //
            // if ( clan != null && targetsContain( "CLAN:" + clan ) )
            // return true;
            // }
            for ( PluginBridge each : Sentry.activePlugins.values() ) {
                if ( hasTargetType( each.getBitFlag() )
                        && each.isTarget( player, this ) ) {
                    return true;
                }
            }
        }
        else if ( aTarget instanceof Monster && hasTargetType( allmonsters ) )
            return true;

        else if ( hasTargetType( namedentities )
                && targetsContain( "ENTITY:" + aTarget.getType() ) )
            return true;

        return false;
    }

    /**
     * Checks whether the Set '_ignoreTargets' contains the supplied String.
     * 
     * @param theTarget
     *            - the string to check for.
     * @return true - if the string is found.
     */
    public boolean ignoresContain( String theTarget ) {
        return _ignoreTargets.contains( theTarget.toUpperCase().intern() );
    }

    /**
     * Checks whether the Set '_validTargets' contains the supplied String.
     * 
     * @param theTarget
     *            - the string to check for.
     * @return true - if the string is found.
     */
    public boolean targetsContain( String theTarget ) {
        return _validTargets.contains( theTarget.toUpperCase().intern() );
    }

    public void deactivate() {
        sentry.getServer().getScheduler().cancelTask( taskID );
    }

    public void die( boolean runscripts, EntityDamageEvent.DamageCause cause ) {
        // most of the former contents of this method have been moved to the
        // SentryStatus state machine.

        if ( myStatus == SentryStatus.isDYING
                || myStatus == SentryStatus.isDEAD )
            return;

        causeOfDeath = cause;

        myStatus = SentryStatus.isDYING;

        if ( runscripts && Sentry.denizenActive )
            DenizenHook.sentryDeath( _myDamamgers, myNPC );
    }

    void faceEntity( Entity from, Entity at ) {

        if ( from.getWorld() != at.getWorld() )
            return;

        Location fromLoc = from.getLocation();
        Location atLoc = at.getLocation();

        double xDiff = atLoc.getX() - fromLoc.getX();
        double yDiff = atLoc.getY() - fromLoc.getY();
        double zDiff = atLoc.getZ() - fromLoc.getZ();

        double distanceXZSquared = xDiff * xDiff + zDiff * zDiff;
        double distanceY = Math.sqrt( distanceXZSquared + yDiff * yDiff );

        double yaw = Math.acos( xDiff / Math.sqrt( distanceXZSquared ) ) * 180
                / Math.PI;
        double pitch = (Math.acos( yDiff / distanceY ) * 180 / Math.PI) - 90;

        if ( zDiff < 0.0 ) {
            yaw = yaw + (Math.abs( 180 - yaw ) * 2);
        }
        NMS.look( from, (float) yaw - 90, (float) pitch );
    }

    private void faceForward() {
        LivingEntity myEntity = getMyEntity();
        NMS.look( myEntity, myEntity.getLocation().getYaw(), 0 );
    }

    void faceAlignWithVehicle() {
        LivingEntity myEntity = getMyEntity();
        NMS.look( myEntity, myEntity.getVehicle().getLocation().getYaw(), 0 );
    }

    public LivingEntity findTarget( Integer range ) {

        LivingEntity myEntity = getMyEntity();
        range += warningRange;

        List<Entity> entitiesInRange = myEntity.getNearbyEntities( range,
                range / 2, range );
        LivingEntity theTarget = null;
        Double distanceToBeat = 99999.0;

        for ( Entity aTarget : entitiesInRange ) {

            if ( !(aTarget instanceof LivingEntity) )
                continue;

            // find closest target
            if ( !isIgnoring( (LivingEntity) aTarget )
                    && isTarget( (LivingEntity) aTarget ) ) {

                // can i see it?
                double lightLevel = aTarget.getLocation().getBlock()
                        .getLightLevel();

                // sneaking cut light in half
                if ( aTarget instanceof Player
                        && ((Player) aTarget).isSneaking() )
                    lightLevel /= 2;

                // not too dark?
                if ( lightLevel >= (16 - nightVision) ) {

                    double dist = aTarget.getLocation()
                            .distance( myEntity.getLocation() );

                    if ( hasLOS( aTarget ) ) {

                        if ( warningRange > 0 && !warningMsg.isEmpty()
                                && myStatus == SentryStatus.isLOOKING
                                && aTarget instanceof Player
                                && dist > (range - warningRange) && !CitizensAPI
                                        .getNPCRegistry().isNPC( aTarget ) ) {

                            if ( !warningsGiven.containsKey( aTarget ) || System
                                    .currentTimeMillis() > warningsGiven
                                            .get( aTarget ) + 60000 ) {

                                Player player = (Player) aTarget;

                                player.sendMessage( Util.format( warningMsg,
                                        myNPC, player, null, null ) );

                                if ( !getNavigator().isNavigating() )
                                    faceEntity( myEntity, aTarget );

                                warningsGiven.put( player,
                                        System.currentTimeMillis() );
                            }
                        }
                        else if ( dist < distanceToBeat ) {
                            distanceToBeat = dist;
                            theTarget = (LivingEntity) aTarget;
                        }
                    }
                }
            }
            else if ( warningRange > 0 && !greetingMsg.isEmpty()
                    && myStatus == SentryStatus.isLOOKING
                    && aTarget instanceof Player
                    && !CitizensAPI.getNPCRegistry().isNPC( aTarget ) ) {

                if ( myEntity.hasLineOfSight( aTarget )
                        && (!warningsGiven.containsKey( aTarget )
                                || System.currentTimeMillis() > warningsGiven
                                        .get( aTarget ) + 60000) ) {

                    Player player = (Player) aTarget;

                    player.sendMessage( Util.format( greetingMsg, myNPC, player,
                            null, null ) );
                    faceEntity( myEntity, aTarget );

                    warningsGiven.put( player, System.currentTimeMillis() );
                }
            }
        }
        return theTarget;
    }

    public void draw( boolean on ) {
        // ((CraftLivingEntity) getMyEntity()).getHandle().b( on ); // TODO: 1.8
        // UPDATE - IS THIS CORRECT?
        // I don't think it was, let see what happens without it.
    }

    public void fire( LivingEntity theTarget ) {

        LivingEntity myEntity = getMyEntity();
        Class<? extends Projectile> myProjectile = myAttacks.getProjectile();
        Effect effect = null;

        double v = 34;
        double g = 20;

        boolean ballistics = true;

        if ( myProjectile == Arrow.class ) {
            effect = Effect.BOW_FIRE;
        }
        else if ( myProjectile == SmallFireball.class
                || myProjectile == Fireball.class
                || myProjectile == WitherSkull.class ) {
            effect = Effect.BLAZE_SHOOT;
            ballistics = false;
        }
        else if ( myProjectile == ThrownPotion.class ) {
            v = 21;
            g = 20;
        }
        else {
            v = 17.75;
            g = 13.5;
        }

        // calc shooting spot.
        Location myLocation = Util.getFireSource( myEntity, theTarget );
        Location targetsHeart = theTarget.getLocation().add( 0, .33, 0 );

        Vector test = targetsHeart.clone().subtract( myLocation ).toVector();

        double elev = test.getY();
        Double testAngle = Util.launchAngle( myLocation, targetsHeart, v, elev,
                g );

        if ( testAngle == null ) {
            clearTarget();
            return;
        }

        double hangtime = Util.hangtime( testAngle, v, elev, g );
        Vector targetVelocity = theTarget.getLocation()
                .subtract( _projTargetLostLoc ).toVector();

        targetVelocity.multiply( 20 / Sentry.logicTicks );

        Location to = Util.leadLocation( targetsHeart, targetVelocity,
                hangtime );
        Vector victor = to.clone().subtract( myLocation ).toVector();

        double dist = Math.sqrt(
                Math.pow( victor.getX(), 2 ) + Math.pow( victor.getZ(), 2 ) );
        elev = victor.getY();

        if ( dist == 0 )
            return;

        if ( !hasLOS( theTarget ) ) {
            clearTarget();
            return;
        }
        if ( myAttacks.lightningLevel > 0 ) {
            ballistics = false;
            effect = null;
        }

        if ( dist > sentryRange ) {
            clearTarget();
            return;
        }

        if ( ballistics ) {

            Double launchAngle = Util.launchAngle( myLocation, to, v, elev, g );

            if ( launchAngle == null ) {
                clearTarget();
                return;
            }

            // Apply angle
            victor.setY( Math.tan( launchAngle ) * dist );

            victor = Util.normalizeVector( victor );

            // Vector noise = Vector.getRandom();
            // noise = noise.multiply( 0.1 );

            // victor = victor.add(noise);

            if ( myProjectile == Arrow.class
                    || myProjectile == ThrownPotion.class )
                v = v + (1.188 * Math.pow( hangtime, 2 ));
            else
                v = v + (0.5 * Math.pow( hangtime, 2 ));

            v = v + (random.nextDouble() - 0.8) / 2;

            // apply power
            victor = victor.multiply( v / 20.0 );

        }
        switch ( myAttacks.lightningLevel ) {

            case 1:
                to.getWorld().strikeLightningEffect( to );
                theTarget.damage( getStrength(), myEntity );

            case 2:
                to.getWorld().strikeLightning( to );

            case 3:
                to.getWorld().strikeLightningEffect( to );
                theTarget.setHealth( 0 );

            default:
                // not lightning
                Projectile projectile;

                if ( myProjectile == ThrownPotion.class ) {
                    net.minecraft.server.v1_9_R1.World nmsWorld = ((CraftWorld) myEntity
                            .getWorld()).getHandle();
                    EntityPotion ent = new EntityPotion( nmsWorld,
                            myLocation.getX(), myLocation.getY(),
                            myLocation.getZ(),
                            CraftItemStack.asNMSCopy( potiontype ) );
                    nmsWorld.addEntity( ent );
                    projectile = (Projectile) ent.getBukkitEntity();
                }
                else if ( myProjectile == EnderPearl.class )
                    projectile = myEntity.launchProjectile( myProjectile );
                else
                    projectile = myEntity.getWorld().spawn( myLocation, myProjectile );

                if ( myProjectile == Fireball.class
                        || myProjectile == WitherSkull.class ) {
                    victor = victor.multiply( 1 / 1000000000 );
                }
                else if ( myProjectile == SmallFireball.class ) {

                    victor = victor.multiply( 1 / 1000000000 );
                    ((SmallFireball) projectile)
                            .setIsIncendiary( myAttacks.incendiary );

                    if ( !myAttacks.incendiary ) {
                        ((SmallFireball) projectile).setFireTicks( 0 );
                        ((SmallFireball) projectile).setYield( 0 );
                    }
                }

                // TODO why are we counting enderpearls?
                else if ( myProjectile == EnderPearl.class ) {
                    epCount++;
                    if ( epCount > Integer.MAX_VALUE - 1 )
                        epCount = 0;
                    if ( Sentry.debug )
                        Sentry.debugLog( epCount + "" );
                }

                sentry.arrows.add( projectile );
                projectile.setShooter( myEntity );
                projectile.setVelocity( victor );
        }

        if ( effect != null )
            myEntity.getWorld().playEffect( myEntity.getLocation(), effect,
                    null );

        faceEntity( getMyEntity(), theTarget );

        if ( myProjectile == Arrow.class )
            draw( false );
        else
            swingPlayerArm( myEntity );
    }

    private void swingPlayerArm( LivingEntity myEntity ) {
        if ( myEntity instanceof Player ) {
            PlayerAnimation.ARM_SWING.play( (Player) myEntity, 64 );
        }
    }

    public int getArmor() {

        if ( sentry.armorBuffs.isEmpty() )
            return armorValue;

        double mod = 0;

        if ( getMyEntity() instanceof Player ) {
            for ( ItemStack is : ((Player) getMyEntity()).getInventory()
                    .getArmorContents() ) {
                Material item = is.getType();

                if ( sentry.armorBuffs.containsKey( item ) )
                    mod += sentry.armorBuffs.get( item );
            }
        }
        return (int) (armorValue + mod);
    }

    public double getHealth() {
        if ( myNPC == null || getMyEntity() == null )
            return 0;

        return ((CraftLivingEntity) getMyEntity()).getHealth();
    }

    public float getSpeed() {
        if ( !myNPC.isSpawned() )
            return sentrySpeed;

        double mod = 0;
        LivingEntity myEntity = getMyEntity();

        if ( !sentry.speedBuffs.isEmpty() ) {

            if ( myEntity instanceof Player ) {
                for ( ItemStack stack : ((Player) myEntity).getInventory()
                        .getArmorContents() ) {
                    Material item = stack.getType();

                    if ( sentry.speedBuffs.containsKey( item ) )
                        mod += sentry.speedBuffs.get( item );
                }
            }
        }
        return (float) (sentrySpeed + mod)
                * (myEntity.isInsideVehicle() ? 2 : 1);
    }

    public int getStrength() {
        if ( sentry.strengthBuffs.isEmpty() )
            return strength;

        double mod = 0;
        LivingEntity myEntity = getMyEntity();

        if ( myEntity instanceof Player ) {

            Material item = ((Player) myEntity).getInventory()
                    .getItemInMainHand().getType();

            if ( sentry.strengthBuffs.containsKey( item ) ) {
                mod += sentry.strengthBuffs.get( item );
            }
        }
        return (int) (strength + mod);
    }

    static Set<AttackType> pyros = EnumSet.of( AttackType.pyro1, AttackType.pyro2, AttackType.pyro3 );
    static Set<AttackType> stormCallers = EnumSet.of( AttackType.sc1, AttackType.sc2, AttackType.sc3 );

    public boolean isPyromancer() { return pyros.contains( myAttacks ); }
    public boolean isPyromancer1() { return (myAttacks == AttackType.pyro1); }
    public boolean isStormcaller() { return stormCallers.contains( myAttacks ); }
    public boolean isWarlock1() { return (myAttacks == AttackType.warlock1); }
    public boolean isWitchDoctor() { return (myAttacks == AttackType.witchdoctor); }

    public void onDamage( EntityDamageByEntityEvent event ) {

        if ( myStatus == SentryStatus.isDYING || invincible ) return;

        if ( myNPC == null || !myNPC.isSpawned() ) return;

        if ( guardTarget != null && guardEntity == null ) return; // dont take damage when bodyguard target isnt around.

        if ( System.currentTimeMillis() < okToTakedamage + 500 ) return;

        okToTakedamage = System.currentTimeMillis();

        event.getEntity().setLastDamageCause( event );

        NPC npc = myNPC;
        LivingEntity attacker = null;
        Entity damager = event.getDamager();

        // Find the attacker
        if ( damager instanceof Projectile
                && ((Projectile) damager).getShooter() instanceof LivingEntity )
            attacker = (LivingEntity) ((Projectile) damager).getShooter();

        else if ( damager instanceof LivingEntity )
            attacker = (LivingEntity) damager;

        if ( Sentry.ignoreListIsInvincible && isIgnoring( attacker ) )
            return;

        if ( attacker != null && iWillRetaliate
                && (!(damager instanceof Projectile) || CitizensAPI
                        .getNPCRegistry().getNPC( attacker ) == null) ) {

            attackTarget = attacker;
            setTarget( attacker );
        }
        Hits hit = Hits.Hit;
        double damage = event.getDamage();

        if ( acceptsCriticals ) {

            hit = Hits.getHit();
            damage = Math.round( damage * hit.damageModifier );
        }
        int arm = getArmor();

        if ( damage > 0 ) {

            if ( attacker != null ) {
                // knockback
                npc.getEntity()
                        .setVelocity( attacker.getLocation().getDirection()
                                .multiply( 1.0 / (sentryWeight + (arm / 5)) ) );
            }
            // Apply armor
            damage -= arm;

            // there was damage before armor.
            if ( damage <= 0 ) {
                npc.getEntity().getWorld().playEffect(
                        npc.getEntity().getLocation(),
                        Effect.ZOMBIE_CHEW_IRON_DOOR, 1 );
                hit = Hits.Block;
            }
        }

        if ( attacker instanceof Player
                && !CitizensAPI.getNPCRegistry().isNPC( attacker ) ) {

            _myDamamgers.add( (Player) attacker );

            String msg = hit.message;

            if ( msg != null && !msg.isEmpty() ) {
                ((Player) attacker).sendMessage( 
                        Util.format( msg,
                                     npc,
                                     attacker, 
                                     ((Player) attacker).getInventory()
                                                        .getItemInMainHand()
                                                        .getType(),
                                     String.valueOf( damage ) ) );
            }
        }

        if ( damage > 0 ) {
            npc.getEntity().playEffect( EntityEffect.HURT );

            // is he dead?
            if ( getHealth() - damage <= 0 ) {

                // set the killer
                if ( damager instanceof HumanEntity )
                    ((CraftLivingEntity) getMyEntity()).getHandle().killer 
                                = (EntityHuman) ((CraftLivingEntity) damager).getHandle();

                die( true, event.getCause() );
            }
            else
                getMyEntity().damage( damage );
        }
    }

    public void onEnvironmentDamage( EntityDamageEvent event ) {

        if ( myStatus == SentryStatus.isDYING ) return;

        if ( myNPC == null || !myNPC.isSpawned() || invincible ) return;

        if ( guardTarget != null && guardEntity == null ) return; // dont take damage when bodyguard target isnt around.

        if ( System.currentTimeMillis() < okToTakedamage + 500 ) return;

        okToTakedamage = System.currentTimeMillis();

        LivingEntity myEntity = getMyEntity();
        double finaldamage = event.getDamage();
        DamageCause cause = event.getCause();

        myEntity.setLastDamageCause( event );

        if ( cause == DamageCause.CONTACT
                || cause == DamageCause.BLOCK_EXPLOSION ) {
            finaldamage -= getArmor();
        }

        if ( finaldamage > 0 ) {
            myEntity.playEffect( EntityEffect.HURT );

            if ( cause == DamageCause.FIRE ) {

                Navigator navigator = getNavigator();

                if ( !navigator.isNavigating() )
                    navigator.setTarget(
                            myEntity.getLocation().add( random.nextInt( 2 ) - 1,
                                    0, random.nextInt( 2 ) - 1 ) );
            }
            if ( getHealth() - finaldamage <= 0 )
                die( true, cause );
            else
                myEntity.damage( finaldamage );
        }
    }

    static final int none = 0;
    static final int all = 1;
    static final int allplayers = 2;
    static final int allnpcs = 4;
    static final int allmonsters = 8;
    static final int events = 16;
    static final int namedentities = 32;
    static final int namedplayers = 64;
    static final int namednpcs = 128;
    static final int owner = 256;

    static final int towny = 512;
    static final int townyenemies = 1024;
    // static final int permGroups = 2048;
    // static final int faction = 512;
    // static final int factionEnemies = 4096;
    // static final int war = 2048;
    // static final int clans = 4096;
    // static final int mcTeams = 16384*2;
    static final int bridges = 2048;

    int targetFlags = none;
    int ignoreFlags = none;

    List<String> NationsEnemies = new ArrayList<String>();
    // List<String> FactionEnemies = new ArrayList<String>();

    /**
     * Scans the strings lists "validTargets" & "ignoreTargets", and sets the
     * "targets" and "ignores" bit-filter int's accordingly.
     * <p>
     * Also adds the strings relating to any named entities to the corresponding
     * list that is prefixed with a "_" character.
     */
    void processTargets() {
        processTargetStrings( true );
    }

    void processTargetStrings( boolean onReload ) {

        if ( Sentry.debug )
            Sentry.debugLog( "processing targets for npc: " + myNPC.getName() );

        targetFlags = none;
        ignoreFlags = none;
        _ignoreTargets.clear();
        _validTargets.clear();
        NationsEnemies.clear();
        // FactionEnemies.clear();

        for ( String target : validTargets ) {

            if ( target.contains( "ENTITY:ALL" ) )
                targetFlags |= all;
            else if ( target.contains( "ENTITY:MONSTER" ) )
                targetFlags |= allmonsters;
            else if ( target.contains( "ENTITY:PLAYER" ) )
                targetFlags |= allplayers;
            else if ( target.contains( "ENTITY:NPC" ) )
                targetFlags |= allnpcs;

            else if ( !checkBridges( target, onReload, true ) ) {

                _validTargets.add( target );

                if ( target.contains( "NPC:" ) )
                    targetFlags |= namednpcs;
                else if ( target.contains( "EVENT:" ) )
                    targetFlags |= events;
                else if ( target.contains( "PLAYER:" ) )
                    targetFlags |= namedplayers;
                else if ( target.contains( "ENTITY:" ) )
                    targetFlags |= namedentities;

                // else if ( Sentry.activePlugins.containsKey( S.VAULT ) &&
                // target.contains( "GROUP:" ) )
                // targetFlags |= permGroups;
                // else if ( Sentry.activePlugins.containsKey( S.FACTIONS ) ) {
                // if ( target.contains( "FACTION:" ) )
                // targetFlags |= faction;
                // if ( target.contains( "FACTIONENEMIES:" ) ) {
                // targetFlags |= factionEnemies;
                // FactionEnemies.add( target.split( ":" )[1] );
                // }
                // }
                else if ( Sentry.activePlugins.containsKey( S.TOWNY ) ) {
                    if ( target.contains( "TOWN:" ) )
                        targetFlags |= towny;
                    if ( target.contains( "NATION:" ) )
                        targetFlags |= towny;
                    if ( target.contains( "NATIONENEMIES:" ) ) {
                        targetFlags |= townyenemies;
                        NationsEnemies.add( target.split( ":" )[1] );
                    }
                }
                // else if ( Sentry.activePlugins.containsKey( S.WAR ) &&
                // target.contains( "WARTEAM:" ) )
                // targetFlags |= war;
                // else if ( Sentry.activePlugins.containsKey( S.SCORE ) &&
                // target.contains( "TEAM:" ) )
                // targetFlags |= mcTeams;
                // else if ( Sentry.activePlugins.containsKey( S.CLANS ) &&
                // target.contains( "CLAN:" ) )
                // targetFlags |= clans;

            }
        }
        // end of 1st for loop

        for ( String ignore : ignoreTargets ) {
            if ( ignore.contains( "ENTITY:ALL" ) )
                ignoreFlags |= all;
            else if ( ignore.contains( "ENTITY:MONSTER" ) )
                ignoreFlags |= allmonsters;
            else if ( ignore.contains( "ENTITY:PLAYER" ) )
                ignoreFlags |= allplayers;
            else if ( ignore.contains( "ENTITY:NPC" ) )
                ignoreFlags |= allnpcs;
            else if ( ignore.contains( "ENTITY:OWNER" ) )
                ignoreFlags |= owner;

            else if ( !checkBridges( ignore, onReload, false ) ) {

                _ignoreTargets.add( ignore );

                if ( ignore.contains( "NPC:" ) )
                    ignoreFlags |= namednpcs;
                else if ( ignore.contains( "PLAYER:" ) )
                    ignoreFlags |= namedplayers;
                else if ( ignore.contains( "ENTITY:" ) )
                    ignoreFlags |= namedentities;

                // else if ( Sentry.activePlugins.containsKey( S.VAULT ) &&
                // ignore.contains( "GROUP:" ) )
                // ignoreFlags |= permGroups;
                // else if ( Sentry.activePlugins.containsKey( S.FACTIONS ) &&
                // ignore.contains( "FACTION:" ) )
                // ignoreFlags |= faction;
                else if ( Sentry.activePlugins.containsKey( S.TOWNY ) ) {
                    if ( ignore.contains( "TOWN:" )
                            || ignore.contains( "NATION:" ) )
                        ignoreFlags |= towny;
                }
                // else if ( Sentry.activePlugins.containsKey( S.WAR ) &&
                // ignore.contains( "WARTEAM:" ) )
                // ignoreFlags |= war;
                // else if ( Sentry.activePlugins.containsKey( S.SCORE ) &&
                // ignore.contains( "TEAM:" ) )
                // ignoreFlags |= mcTeams;
                // else if ( Sentry.activePlugins.containsKey( S.CLANS ) &&
                // ignore.contains( "CLAN:" ) )
                // ignoreFlags |= clans;

            }
        }
        if ( Sentry.debug )
            Sentry.debugLog( "Target flags: " + targetFlags + "  Ignore flags: "
                    + ignoreFlags );
    }

    private boolean checkBridges( String target, boolean onReload,
            boolean asTarget ) {

        for ( PluginBridge each : Sentry.activePlugins.values() ) {

            if ( target.contains( each.getPrefix().concat( ":" ) ) ) {

                if ( onReload )
                    each.add( target, this, asTarget );

                if ( each.isListed( this, asTarget ) ) {

                    if ( asTarget )
                        targetFlags |= each.getBitFlag();
                    else
                        ignoreFlags |= each.getBitFlag();
                }
                return true;
            }
        }
        return false;
    }

    private class SentryLogic implements Runnable {

        SentryLogic() {
        }

        @SuppressWarnings( { "null" } )
        @Override
        public void run() {

            LivingEntity myEntity = getMyEntity();

            if ( myEntity == null ) {
                myStatus = SentryStatus.isDEAD;
            }
            if ( myStatus == SentryStatus.isDEAD
                    || myStatus == SentryStatus.isDYING ) {
                myStatus.update( SentryInstance.this );
                return;
            }
            if ( attackTarget != null ) {
                setTarget( attackTarget );
            }

            if ( healRate > 0 && System.currentTimeMillis() > oktoheal ) {

                if ( getHealth() < sentryMaxHealth ) {

                    double heal = 1;

                    if ( healRate < 0.5 )
                        heal = (0.5 / healRate);

                    setHealth( getHealth() + heal );

                    if ( healAnimation != null )
                        NMS.sendPacketNearby( null, myEntity.getLocation(),
                                healAnimation );

                    if ( getHealth() >= sentryMaxHealth )
                        _myDamamgers.clear();

                }
                oktoheal = (long) (System.currentTimeMillis()
                        + healRate * 1000);
            }

            if ( myNPC.isSpawned() && !myEntity.isInsideVehicle() && isMounted()
                    && isMyChunkLoaded() )
                mount();

            if ( myStatus == SentryStatus.isATTACKING && myNPC.isSpawned() ) {

                if ( !isMyChunkLoaded() ) {
                    clearTarget();
                    return;
                }

                if ( targetFlags > none && attackTarget == null
                        && System.currentTimeMillis() > oktoreasses ) {

                    LivingEntity target = findTarget( sentryRange );
                    setTarget( target );
                    oktoreasses = System.currentTimeMillis() + 3000;
                }

                if ( attackTarget != null && !attackTarget.isDead()
                        && attackTarget.getWorld() == myEntity.getLocation()
                                .getWorld() ) {

                    if ( myAttacks != AttackType.brawler ) {

                        if ( _projTargetLostLoc == null )
                            _projTargetLostLoc = attackTarget.getLocation();

                        if ( !getNavigator().isNavigating() )
                            faceEntity( myEntity, attackTarget );

                        if ( !getNavigator().isPaused() ) {
                            getNavigator().setPaused( true );
                            getNavigator().cancelNavigation();
                            getNavigator().setTarget(
                                    SentryInstance.this.attackTarget, true );
                        }

                        draw( true );

                        if ( System.currentTimeMillis() > oktoFire ) {

                            oktoFire = (long) (System.currentTimeMillis()
                                    + attackRate * 1000.0);
                            fire( attackTarget );
                        }

                        if ( attackTarget != null )
                            _projTargetLostLoc = attackTarget.getLocation();

                        return;
                    }
                    if ( isMounted() )
                        faceEntity( myEntity, attackTarget );

                    double dist = attackTarget.getLocation()
                            .distance( myEntity.getLocation() );
                    // block if in range
                    draw( dist < 3 );
                    // Did it get away?
                    if ( dist > sentryRange )
                        clearTarget();
                }
                else
                    clearTarget();
            }

            else if ( myStatus == SentryStatus.isLOOKING
                    && myNPC.isSpawned() ) {

                if ( getNavigator().isPaused() ) {
                    getNavigator().setPaused( false );
                }

                if ( myEntity.isInsideVehicle() == true )
                    faceAlignWithVehicle();

                if ( guardEntity instanceof Player
                        && !((Player) guardEntity).isOnline() ) {
                    guardEntity = null;
                }
                else if ( guardTarget != null && guardEntity == null
                        && findGuardEntity( guardTarget, false ) ) {
                    findGuardEntity( guardTarget, true );
                }

                if ( guardEntity != null ) {

                    Location npcLoc = myEntity.getLocation();
                    Location guardEntLoc = guardEntity.getLocation();

                    if ( guardEntLoc.getWorld() != npcLoc.getWorld()
                            || !isMyChunkLoaded() ) {

                        if ( Util.CanWarp( guardEntity, myNPC ) ) {
                            myNPC.despawn();
                            myNPC.spawn( guardEntLoc.add( 1, 0, 1 ) );
                        }
                        else {
                            ((Player) guardEntity).sendMessage( myNPC.getName()
                                    + S.CANT_FOLLOW
                                    + guardEntity.getWorld().getName() );
                            guardEntity = null;
                        }
                    }
                    else {
                        Navigator navigator = getNavigator();
                        boolean isNavigating = navigator.isNavigating();
                        double dist = npcLoc.distanceSquared( guardEntLoc );

                        if ( Sentry.debug )
                            Sentry.debugLog( myNPC.getName() + dist
                                    + isNavigating + " "
                                    + navigator.getEntityTarget() + " " );

                        if ( dist > 1024 ) {
                            myNPC.teleport( guardEntLoc.add( 1, 0, 1 ),
                                    TeleportCause.PLUGIN );
                        }
                        else if ( dist > followDistance && !isNavigating ) {
                            navigator.setTarget( guardEntity, false );
                            navigator.getLocalParameters()
                                    .stationaryTicks( 3 * 20 );
                        }
                        else if ( dist < followDistance && isNavigating ) {
                            navigator.cancelNavigation();
                        }
                    }
                }

                LivingEntity target = null;

                if ( targetFlags > none ) {
                    target = findTarget( sentryRange );
                }

                if ( target != null ) {
                    oktoreasses = System.currentTimeMillis() + 3000;
                    setTarget( target );
                }
            }
        }
    }

    boolean isMyChunkLoaded() {

        LivingEntity myEntity = getMyEntity();

        if ( myEntity == null )
            return false;

        Location npcLoc = myEntity.getLocation();

        return npcLoc.getWorld().isChunkLoaded( npcLoc.getBlockX() >> 4,
                npcLoc.getBlockZ() >> 4 );
    }

    /**
     * Searches for an Entity with a name that matches the provided String, and
     * if successful saves it in the field 'guardEntity' and the name in
     * 'guardTarget'
     * 
     * @param name
     *            - The name that you wish to search for.
     * @param onlyCheckAllPlayers
     *            - if true, the search is conducted on all online Players<br>
     * @param onlyCheckAllPlayers
     *            - if false, all LivingEntities within sentryRange are checked.
     * 
     * @return true if an Entity with the supplied name is found, otherwise
     *         returns false.
     */
    public boolean findGuardEntity( String name, boolean onlyCheckAllPlayers ) {

        if ( myNPC == null )
            return false;

        if ( name == null ) {
            guardEntity = null;
            guardTarget = null;

            clearTarget();
            return true;
        }

        if ( onlyCheckAllPlayers ) {

            for ( Player player : sentry.getServer().getOnlinePlayers() ) {

                if ( name.equals( player.getName() ) ) {

                    guardEntity = player;
                    guardTarget = name;

                    clearTarget();
                    return true;
                }
            }
        }
        else {
            for ( Entity each : getMyEntity().getNearbyEntities( sentryRange,
                    sentryRange, sentryRange ) ) {

                String ename = null;

                if ( each instanceof Player )
                    ename = ((Player) each).getName();

                else if ( each instanceof LivingEntity )
                    ename = ((LivingEntity) each).getCustomName();

                // if the entity for this loop isn't a player or living, move
                // along...
                else
                    continue;

                if ( ename == null )
                    continue;

                // name found! now is it the name we are looking for?
                if ( name.equals( ename ) ) {

                    guardEntity = (LivingEntity) each;
                    guardTarget = name;

                    clearTarget();
                    return true;
                }
            }
        }
        return false;
    }

    public void setHealth( double health ) {

        if ( myNPC == null ) return;

        LivingEntity myEntity = getMyEntity();

        if ( myEntity == null ) return;

        if ( ((CraftLivingEntity) myEntity).getMaxHealth() != sentryMaxHealth )
            myEntity.setMaxHealth( sentryMaxHealth );

        if ( health > sentryMaxHealth )
            health = sentryMaxHealth;

        myEntity.setHealth( health );
    }

    /**
     * Updates the Attacktype reference in myAttacks field, according to the
     * item being held by the NPC.
     * <p>
     * Also sets potion effects, and potion types if appropriate.
     */
    public void updateAttackType() {

        Material weapon = Material.AIR;
        ItemStack item = null;
        LivingEntity myEntity = getMyEntity();

        if ( myEntity instanceof HumanEntity ) {
            item = ((HumanEntity) myEntity).getInventory().getItemInMainHand();
            weapon = item.getType();

            myAttacks = AttackType.find( weapon );

            if ( myAttacks != AttackType.witchdoctor )
                item.setDurability( (short) 0 );
        }
        else if ( myEntity instanceof Skeleton ) myAttacks = AttackType.archer;
        else if ( myEntity instanceof Ghast ) myAttacks = AttackType.pyro3;
        else if ( myEntity instanceof Snowman ) myAttacks = AttackType.magi;
        else if ( myEntity instanceof Wither ) myAttacks = AttackType.warlock2;
        else if ( myEntity instanceof Witch ) myAttacks = AttackType.witchdoctor;
        else if ( myEntity instanceof Blaze || myEntity instanceof EnderDragon ) myAttacks = AttackType.pyro2;
        else myAttacks = AttackType.brawler;

        weaponSpecialEffects = sentry.weaponEffects.get( weapon );

        if ( myAttacks == AttackType.witchdoctor ) {
            if ( item == null ) {
                item = new ItemStack( Material.POTION, 1, (short) 16396 );
            }
            potiontype = item;
        }
    }

    /**
     * convenience method to reduce repetition - calls setTarget( null, false )
     * <p>
     * will hopefully be replaced with a better method at some point.
     */
    void clearTarget() {
        // TODO replace with a better method.
        setTarget( null );
    }

    public void setTarget( LivingEntity theEntity ) {

        LivingEntity myEntity = getMyEntity();

        if ( myEntity == null || theEntity == myEntity )
            return;

        if ( guardTarget != null && guardEntity == null )
            theEntity = null; // dont go aggro when bodyguard target isnt
                              // around.

        if ( theEntity == null ) {

            if ( Sentry.debug )
                Sentry.debugLog( myNPC.getName() + "- Set Target Null" );

            myStatus = SentryStatus.isLOOKING;
            attackTarget = null;
            _projTargetLostLoc = null;
        }

        if ( myNPC == null || !myNPC.isSpawned() )
            return;

        GoalController goalController = getGoalController();
        Navigator navigator = getNavigator();

        if ( theEntity == null ) {
            // no hostile target

            draw( false );

            if ( guardEntity == null ) {
                // not a guard
                navigator.cancelNavigation();

                faceForward();

                if ( goalController.isPaused() )
                    goalController.setPaused( false );

            }
            else {
                goalController.setPaused( true );

                if ( navigator.getEntityTarget() == null || navigator
                        .getEntityTarget().getTarget() != guardEntity ) {

                    if ( guardEntity.getLocation().getWorld() != myEntity
                            .getLocation().getWorld() ) {
                        myNPC.despawn();
                        myNPC.spawn( guardEntity.getLocation().add( 1, 0, 1 ) );
                        return;
                    }
                    navigator.setTarget( guardEntity, false );
                    navigator.getLocalParameters().stationaryTicks( 3 * 20 );
                }
            }
            return;
        }

        if ( theEntity == guardEntity )
            return;

        if ( !getNavigator().isNavigating() )
            faceEntity( myEntity, theEntity );

        attackTarget = theEntity;

        if ( myAttacks == AttackType.brawler ) {

            if ( navigator.getEntityTarget() != null
                    && navigator.getEntityTarget().getTarget() == theEntity )
                return;

            if ( !goalController.isPaused() )
                goalController.setPaused( true );

            navigator.setTarget( theEntity, true );
            navigator.getLocalParameters().speedModifier( getSpeed() );
            navigator.getLocalParameters().stuckAction( giveup );
            navigator.getLocalParameters().stationaryTicks( 5 * 20 );
        }
    }

    Navigator getNavigator() {
        return ifMountedGetMount().getNavigator();
    }

    private GoalController getGoalController() {
        return ifMountedGetMount().getDefaultGoalController();
    }

    private NPC ifMountedGetMount() {

        NPC npc = getMountNPC();

        if ( npc == null || !npc.isSpawned() )
            npc = myNPC;

        return npc;
    }

    public void dismount() {
        // get off and despawn the horse.
        if ( myNPC.isSpawned() && getMyEntity().isInsideVehicle() ) {

            NPC mount = getMountNPC();

            if ( mount != null ) {
                getMyEntity().getVehicle().setPassenger( null );
                mount.despawn( DespawnReason.PLUGIN );
            }
        }
    }

    public void mount() {
        if ( myNPC.isSpawned() ) {

            LivingEntity myEntity = getMyEntity();

            if ( myEntity.isInsideVehicle() )
                myEntity.getVehicle().setPassenger( null );

            NPC mount = getMountNPC();

            if ( mount == null || (!mount.isSpawned() && !mountCreated) ) {

                mount = createMount();
            }

            if ( mount != null ) {
                mountCreated = true;

                if ( !mount.isSpawned() )
                    return; // dead mount

                mount.data().set( NPC.DEFAULT_PROTECTED_METADATA, false );

                NavigatorParameters mountParams = mount.getNavigator()
                        .getDefaultParameters();
                NavigatorParameters myParams = myNPC.getNavigator()
                        .getDefaultParameters();

                mountParams.attackStrategy( new MountAttackStrategy() );
                mountParams.useNewPathfinder( false );
                mountParams.speedModifier( myParams.speedModifier() * 2 );
                mountParams.range( myParams.range() + 5 );

                ((CraftLivingEntity) mount.getEntity())
                        .setCustomNameVisible( false );
                mount.getEntity().setPassenger( null );
                mount.getEntity().setPassenger( myEntity );
            }
            else
                mountID = -1;
        }
    }

    public NPC createMount() {
        if ( Sentry.debug )
            Sentry.debugLog( "Creating mount for " + myNPC.getName() );

        if ( myNPC.isSpawned() ) {

            if ( getMyEntity() == null )
                Sentry.logger.info(
                        "why is this spawned but bukkit entity is null???" );

            NPC mount = null;

            if ( isMounted() ) {
                mount = CitizensAPI.getNPCRegistry().getById( mountID );

                if ( mount != null )
                    mount.despawn();
                else
                    Sentry.logger.info( "Cannot find mount NPC " + mountID );
            }
            else {
                mount = CitizensAPI.getNPCRegistry().createNPC(
                        EntityType.HORSE, myNPC.getName() + "_Mount" );
                mount.getTrait( MobType.class ).setType( EntityType.HORSE );
            }

            if ( mount == null ) {
                Sentry.logger.info( "Cannot create mount NPC!" );
            }
            else {
                mount.spawn( getMyEntity().getLocation() );

                mount.getTrait( Owner.class )
                        .setOwner( myNPC.getTrait( Owner.class ).getOwner() );

                ((Horse) mount.getEntity()).getInventory()
                        .setSaddle( new ItemStack( Material.SADDLE ) );

                mountID = mount.getId();

                return mount;
            }
        }
        return null;
    }

    public boolean hasLOS( Entity other ) {

        if ( !myNPC.isSpawned() )
            return false;
        if ( ignoreLOS )
            return true;

        return getMyEntity().hasLineOfSight( other );
    }

    public LivingEntity getMyEntity() {
        if ( myNPC == null || myNPC.getEntity() == null
                || myNPC.getEntity().isDead() )
            return null;

        if ( !(myNPC.getEntity() instanceof LivingEntity) ) {
            Sentry.logger.info( "Sentry " + myNPC.getName()
                    + " is not a living entity! Errors inbound...." );
            return null;
        }
        return (LivingEntity) myNPC.getEntity();
    }

    protected NPC getMountNPC() {
        if ( isMounted() && CitizensAPI.hasImplementation() ) {

            return CitizensAPI.getNPCRegistry().getById( mountID );
        }
        return null;
    }

    public boolean isMounted() { return mountID >= 0; }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null || !(obj instanceof SentryInstance) )
            return false;
        if ( ((SentryInstance) obj).myID == myID )
            return true;
        return false;
    }

    @Override
    public int hashCode() { return myID; }
}
