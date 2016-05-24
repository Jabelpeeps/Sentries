package org.jabelpeeps.sentries;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_9_R2.inventory.CraftItemStack;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jabelpeeps.sentries.attackstrategies.CreeperAttackStrategy;
import org.jabelpeeps.sentries.attackstrategies.MountAttackStrategy;
import org.jabelpeeps.sentries.attackstrategies.SpiderAttackStrategy;

import net.aufdemrand.denizen.npc.traits.HealthTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.MemoryDataKey;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;
//import net.minecraft.server.v1_9_R2.EntityHuman;
import net.minecraft.server.v1_9_R2.EntityPotion;
import net.minecraft.server.v1_9_R2.Packet;
import net.minecraft.server.v1_9_R2.PacketPlayOutAnimation;
import net.minecraft.server.v1_9_R2.World;

public class SentryTrait extends Trait {

    Sentries sentry;

    Location _projTargetLostLoc;
    Location spawnLocation;

    int strength;
    int armourValue;
    int epCount;
    int nightVision;
    int respawnDelay;
    int sentryRange;
    int followDistance;
    int mountID;
    int warningRange;

    float sentrySpeed;

    double attackRate;
    double healRate;
    double sentryWeight;
    double sentryMaxHealth;

    boolean killsDropInventory;
    boolean dropInventory;
    boolean targetable;
    boolean invincible;
    boolean loaded;
    boolean acceptsCriticals;
    boolean iWillRetaliate;
    boolean ignoreLOS;

    private GiveUpStuckAction giveup = new GiveUpStuckAction( this );

    String greetingMsg = "";
    String warningMsg = "";

    private Map<Player, Long> warningsGiven = new HashMap<Player, Long>();
    Set<Player> _myDamamgers = new HashSet<Player>();

    LivingEntity guardeeEntity;
    LivingEntity attackTarget;
    String guardeeName;
    DamageCause causeOfDeath;

    Packet<?> healAnimation;

    Set<String> ignoreTargets = new HashSet<String>();
    Set<String> validTargets = new HashSet<String>();

    Set<String> _ignoreTargets = new HashSet<String>();
    Set<String> _validTargets = new HashSet<String>();

    // TODO why are we saving four instances of the system time?
    long isRespawnable = System.currentTimeMillis();
    long oktoFire = System.currentTimeMillis();
    long oktoheal = System.currentTimeMillis();
    long oktoreasses = System.currentTimeMillis();
    long okToTakedamage = 0;

    List<PotionEffect> weaponSpecialEffects;
    ItemStack potiontype;
    Random random = new Random();

    SentryStatus myStatus = SentryStatus.isSPAWNING;
    AttackType myAttacks;

    private int taskID = 0;

    public final int myID;
    private static int nextID;

    static {
        nextID = 0;
    }
    
    public SentryTrait() {
        super( "sentry" );
        sentry = (Sentries) Bukkit.getPluginManager().getPlugin( "Sentries" );
        myID = ++nextID;
    }
    
    @SuppressWarnings( "unchecked" )
    @Override
    public void load( DataKey key ) throws NPCLoadException {

        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] load() start" );
  
        if ( key.keyExists( "traits" ) ) 
            key = key.getRelative( "traits" );

        iWillRetaliate = key.getBoolean( S.RETALIATE, sentry.defaultBooleans.get( S.RETALIATE ) );
        invincible = key.getBoolean( S.INVINCIBLE, sentry.defaultBooleans.get( S.INVINCIBLE ) );
        dropInventory = key.getBoolean( S.DROP_INVENTORY, sentry.defaultBooleans.get( S.DROP_INVENTORY ) );
        acceptsCriticals = key.getBoolean( S.CRITICAL_HITS, sentry.defaultBooleans.get( S.CRITICAL_HITS ) );
        killsDropInventory = key.getBoolean( S.KILLS_DROP, sentry.defaultBooleans.get( S.KILLS_DROP ) );
        ignoreLOS = key.getBoolean( S.IGNORE_LOS, sentry.defaultBooleans.get( S.IGNORE_LOS ) );
        targetable = key.getBoolean( S.TARGETABLE, sentry.defaultBooleans.get( S.TARGETABLE ) );

        armourValue = key.getInt( S.ARMOR, sentry.defaultIntegers.get( S.ARMOR ) );
        strength = key.getInt( S.STRENGTH, sentry.defaultIntegers.get( S.STRENGTH ) );
        sentryRange = key.getInt( S.RANGE, sentry.defaultIntegers.get( S.RANGE ) );
        respawnDelay = key.getInt( S.RESPAWN_DELAY, sentry.defaultIntegers.get( S.RESPAWN_DELAY ) );
        followDistance = key.getInt( S.FOLLOW_DISTANCE, sentry.defaultIntegers.get( S.FOLLOW_DISTANCE ) );
        warningRange = key.getInt( S.WARNING_RANGE, sentry.defaultIntegers.get( S.WARNING_RANGE ) );
        nightVision = key.getInt( S.NIGHT_VISION, sentry.defaultIntegers.get( S.NIGHT_VISION ) );
        mountID = key.getInt( S.MOUNTID, -1 );

        sentrySpeed = (float) key.getDouble( S.SPEED, sentry.defaultDoubles.get( S.SPEED ) );
        sentryWeight = key.getDouble( S.WEIGHT, sentry.defaultDoubles.get( S.WEIGHT ) );
        sentryMaxHealth = key.getDouble( S.HEALTH, sentry.defaultDoubles.get( S.HEALTH ) );
        attackRate = key.getDouble( S.ATTACK_RATE, sentry.defaultDoubles.get( S.ATTACK_RATE ) );
        healRate = key.getDouble( S.HEALRATE, sentry.defaultDoubles.get( S.HEALRATE ) );

        guardeeName = key.getString( S.GUARD_TARGET, null );
        greetingMsg = key.getString( S.GREETING, sentry.defaultGreeting );
        warningMsg = key.getString( S.WARNING, sentry.defaultWarning );

        if ( key.keyExists( "Spawn" ) ) {
            try {
                spawnLocation = new Location( Bukkit.getWorld( key.getString( "Spawn.world" ) ),
                                             key.getDouble( "Spawn.x" ), 
                                             key.getDouble( "Spawn.y" ),
                                             key.getDouble( "Spawn.z" ),
                                            (float) key.getDouble( "Spawn.yaw" ),
                                            (float) key.getDouble( "Spawn.pitch" ) );

                if ( spawnLocation.getWorld() == null )
                    spawnLocation = null;
            } catch ( Exception e ) {
                e.printStackTrace();
                spawnLocation = null;
            }
        }
        if ( guardeeName != null && guardeeName.isEmpty() )
            guardeeName = null;

        if ( key.getRaw( S.TARGETS ) != null )
            validTargets.addAll( (Set<String>) key.getRaw( S.TARGETS ) );
        else
            validTargets.addAll( sentry.defaultTargets );

        if ( key.getRaw( S.IGNORES ) != null )
            ignoreTargets.addAll( (Set<String>) key.getRaw( S.IGNORES ) );
        else
            ignoreTargets.addAll( sentry.defaultIgnores );

        loaded = true;
        processTargetStrings();

        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] load() end" );
    }

//    @Override
//    public void onAttach() {
//
//        if ( Sentries.debug )
//            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onAttach()" );
//    }

    @Override
    public void onSpawn() {
        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onSpawn()" );

        if ( !loaded ) {
            try {
                load( new MemoryDataKey() );

            } catch ( NPCLoadException e ) {
            }
        }
        initialize();
    }
    
    @Override
    public void onRemove() {
        
        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onRemove()" );

        cancelRunnable();
    }
    
    @Override
    public void onDespawn() {

        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onDespawn()" );

        isRespawnable = System.currentTimeMillis() + respawnDelay * 1000;
        myStatus = SentryStatus.isDEAD;
        dismount();
    }
    
    @Override
    public void save( DataKey key ) {

        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] save()" );
        
        key.setBoolean( S.RETALIATE, iWillRetaliate );
        key.setBoolean( S.INVINCIBLE, invincible );
        key.setBoolean( S.DROP_INVENTORY, dropInventory );
        key.setBoolean( S.KILLS_DROP, killsDropInventory );
        key.setBoolean( S.TARGETABLE, targetable );
        key.setInt( S.MOUNTID, mountID );
        key.setBoolean( S.CRITICAL_HITS, acceptsCriticals );
        key.setBoolean( S.IGNORE_LOS, ignoreLOS );
        key.setRaw( S.TARGETS, validTargets );
        key.setRaw( S.IGNORES, ignoreTargets );

        if ( spawnLocation != null ) {
            key.setDouble( "Spawn.x", spawnLocation.getX() );
            key.setDouble( "Spawn.y", spawnLocation.getY() );
            key.setDouble( "Spawn.z", spawnLocation.getZ() );
            key.setString( "Spawn.world", spawnLocation.getWorld().getName() );
            key.setDouble( "Spawn.yaw", spawnLocation.getYaw() );
            key.setDouble( "Spawn.pitch", spawnLocation.getPitch() );
        }

        key.setDouble( S.HEALTH, sentryMaxHealth );
        key.setInt( S.RANGE, sentryRange );
        key.setInt( S.RESPAWN_DELAY, respawnDelay );
        key.setDouble( S.SPEED, sentrySpeed );
        key.setDouble( S.WEIGHT, sentryWeight );
        key.setDouble( S.HEALRATE, healRate );
        key.setInt( S.ARMOR, armourValue );
        key.setInt( S.STRENGTH, strength );
        key.setInt( S.WARNING_RANGE, warningRange );
        key.setDouble( S.ATTACK_RATE, attackRate );
        key.setInt( S.NIGHT_VISION, nightVision );
        key.setInt( S.FOLLOW_DISTANCE, followDistance );

        if ( guardeeName != null )
            key.setString( S.GUARD_TARGET, guardeeName );
        else if ( key.keyExists( S.GUARD_TARGET ) )
            key.removeKey( S.GUARD_TARGET );

        key.setString( S.WARNING, warningMsg );
        key.setString( S.GREETING, greetingMsg );
    }
    
    @Override
    public void onCopy() {

        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onCopy()" );
 
        // the new npc is not in the new location immediately.
        // TODO is this really needed?
        final Runnable cloneInstance = new Runnable() {

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void run() {
                spawnLocation = npc.getEntity().getLocation();
            }
        };
        Bukkit.getScheduler().scheduleSyncDelayedTask( sentry, cloneInstance, 10 );
    }
    
    public void initialize() {

        LivingEntity myEntity = getMyEntity();

        // check for illegal values
        if ( sentryWeight <= 0 ) sentryWeight = 1.0;
        if ( attackRate > 30 ) attackRate = 30.0;
        if ( sentryMaxHealth < 0 ) sentryMaxHealth = 0;
        if ( sentryRange < 1 ) sentryRange = 1;
        if ( sentryRange > 200 ) sentryRange = 200;
        if ( sentryWeight <= 0 ) sentryWeight = 1.0;
        if ( respawnDelay < -1 ) respawnDelay = -1;
        if ( spawnLocation == null ) spawnLocation = myEntity.getLocation();

        // Allow Denizen to handle the sentry's health if it is active.
        if ( DenizenHook.sentryHealthByDenizen ) {
            if ( npc.hasTrait( HealthTrait.class ) )
                npc.removeTrait( HealthTrait.class );
        }

        // disable citizens respawning, because Sentries doesn't always raise EntityDeath
        npc.data().set( NPC.RESPAWN_DELAY_METADATA, -1 );

        setHealth( sentryMaxHealth );

        _myDamamgers.clear();
//        myStatus = SentryStatus.isLOOKING;

        faceForward();

        healAnimation = new PacketPlayOutAnimation( ((CraftEntity) myEntity).getHandle(), 6 );

        if ( guardeeName == null )
            npc.teleport( spawnLocation, TeleportCause.PLUGIN );

        NavigatorParameters navigatorParams = npc.getNavigator().getDefaultParameters();
        float myRange = navigatorParams.range();

        if ( myRange < sentryRange + 5 ) {
            myRange = sentryRange + 5;
        }

        npc.setProtected( false );
        npc.data().set( NPC.TARGETABLE_METADATA, targetable );

        navigatorParams.range( myRange );
        navigatorParams.stationaryTicks( 5 * 20 );
        navigatorParams.useNewPathfinder( false );

        if ( myEntity instanceof Creeper )
            navigatorParams.attackStrategy( new CreeperAttackStrategy() );
        else if ( myEntity instanceof Spider )
            navigatorParams.attackStrategy( new SpiderAttackStrategy() );

        // TODO check this hasn't broken anything.
        // processTargets();

        updateAttackType();

        if ( taskID == 0 ) {
            taskID = Bukkit.getScheduler()
                           .scheduleSyncRepeatingTask( sentry, 
                                                       this, 
                                                       40 + npc.getId(), 
                                                       Sentries.logicTicks );
        }
    }

    public void cancelRunnable() {
        if ( taskID != 0 )
            Bukkit.getScheduler().cancelTask( taskID );
    }

    public boolean hasTargetType( int type ) { 
        return (targetFlags & type) == type;
    }

    public boolean hasIgnoreType( int type ) {
        return (ignoreFlags & type) == type;
    }

    public boolean isIgnoring( LivingEntity aTarget ) {

        if ( aTarget == guardeeEntity ) return true;
        if ( ignoreFlags == none ) return false;
        if ( hasIgnoreType( all ) ) return true;

        if ( CitizensAPI.getNPCRegistry().isNPC( aTarget ) ) {

            if ( hasIgnoreType( allnpcs ) ) return true;

            NPC targetNpc = CitizensAPI.getNPCRegistry().getNPC( aTarget );

            if ( targetNpc != null ) {

                if (    hasIgnoreType( namednpcs )
                        && ignoresContain( "NPC:" + targetNpc.getName() ) )
                    return true;

                // As this is an NPC and we haven't decided whether to ignore it yet, let check the ignores of the owner.
                return isIgnoring( (LivingEntity) Bukkit.getOfflinePlayer( targetNpc.getTrait( Owner.class ).getOwnerId() ) );
            }
        }
        else if ( aTarget instanceof Player ) {

            if ( hasIgnoreType( allplayers ) ) return true;

            Player player = (Player) aTarget;
            String name = player.getName();

            if (    hasIgnoreType( namedplayers )
                    && ignoresContain( "PLAYER:" + name ) )
                return true;

            if (    hasIgnoreType( owner ) 
                    && name.equalsIgnoreCase( npc.getTrait( Owner.class ).getOwner() ) )
                return true;

            for ( PluginBridge each : Sentries.activePlugins.values() ) {
                
                if (    hasIgnoreType( each.getBitFlag() )
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

        if ( targetFlags == none || targetFlags == events ) return false;

        if ( hasTargetType( all ) ) return true;

        if ( CitizensAPI.getNPCRegistry().isNPC( aTarget ) ) {

            if ( hasTargetType( allnpcs ) ) return true;

            NPC targetNpc = CitizensAPI.getNPCRegistry().getNPC( aTarget );

            String targetName = targetNpc.getName();

            if (    hasTargetType( namednpcs )
                    && targetsContain( "NPC:" + targetName ) )
                return true;

            // As we are checking an NPC and haven't decided whether to attack it yet, lets check the owner.
            return isTarget( 
                    (LivingEntity) Bukkit.getOfflinePlayer( targetNpc.getTrait( Owner.class ).getOwnerId() ) );
        }
        
        else if ( aTarget instanceof Player ) {

            if ( hasTargetType( allplayers ) ) return true;

            Player player = (Player) aTarget;
            String name = player.getName();

            if (    hasTargetType( namedplayers )
                    && targetsContain( "PLAYER:" + name ) )
                return true;

            if (    targetsContain( "ENTITY:OWNER" ) 
                    && name.equalsIgnoreCase( npc.getTrait( Owner.class ).getOwner() ) )
                return true;

            for ( PluginBridge each : Sentries.activePlugins.values() ) {
                
                if (    hasTargetType( each.getBitFlag() )
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

    public void die( boolean runscripts, EntityDamageEvent.DamageCause cause ) {
        // most of the former contents of this method have been moved to the SentryStatus state machine.

        if (    myStatus == SentryStatus.isDYING
                || myStatus == SentryStatus.isDEAD )
            return;

        causeOfDeath = cause;

        myStatus = SentryStatus.isDYING;

        if ( runscripts && Sentries.denizenActive )
            DenizenHook.sentryDeath( _myDamamgers, npc );
    }

    void faceEntity( Entity from, Entity at ) {

        if ( from.getWorld() != at.getWorld() ) return;

        Location fromLoc = from.getLocation();
        Location atLoc = at.getLocation();

        double xDiff = atLoc.getX() - fromLoc.getX();
        double yDiff = atLoc.getY() - fromLoc.getY();
        double zDiff = atLoc.getZ() - fromLoc.getZ();

        double distanceXZSquared = xDiff * xDiff + zDiff * zDiff;
        double distanceY = Math.sqrt( distanceXZSquared + yDiff * yDiff );

        double yaw = Math.acos( xDiff / Math.sqrt( distanceXZSquared ) ) * 180 / Math.PI;
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

        List<Entity> entitiesInRange = myEntity.getNearbyEntities( range, range / 2, range );
        LivingEntity theTarget = null;
        Double distanceToBeat = 99999.0;

        for ( Entity aTarget : entitiesInRange ) {

            if ( !(aTarget instanceof LivingEntity) ) continue;

            // find closest target
            if (    !isIgnoring( (LivingEntity) aTarget )
                    && isTarget( (LivingEntity) aTarget ) ) {

                // can i see it?
                double lightLevel = aTarget.getLocation().getBlock().getLightLevel();

                // sneaking cut light in half
                if (    aTarget instanceof Player
                        && ((Player) aTarget).isSneaking() )
                    lightLevel /= 2;

                // not too dark?
                if ( lightLevel >= (16 - nightVision) ) {

                    double dist = aTarget.getLocation().distance( myEntity.getLocation() );

                    if ( hasLOS( aTarget ) ) {

                        if (    warningRange > 0 && !warningMsg.isEmpty()
                                && myStatus == SentryStatus.isLOOKING
                                && aTarget instanceof Player
                                && dist > (range - warningRange) 
                                && !CitizensAPI.getNPCRegistry().isNPC( aTarget ) ) {

                            if (    !warningsGiven.containsKey( aTarget ) 
                                    || System.currentTimeMillis() > warningsGiven.get( aTarget ) + 60000 ) {

                                Player player = (Player) aTarget;

                                player.sendMessage( Util.format( warningMsg, npc, player, null, null ) );

                                if ( !getNavigator().isNavigating() )
                                    faceEntity( myEntity, aTarget );

                                warningsGiven.put( player, System.currentTimeMillis() );
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

                if (    myEntity.hasLineOfSight( aTarget )
                        && (    !warningsGiven.containsKey( aTarget )
                                || System.currentTimeMillis() > warningsGiven .get( aTarget ) + 60000) ) {

                    Player player = (Player) aTarget;

                    player.sendMessage( Util.format( greetingMsg, npc, player, null, null ) );
                    faceEntity( myEntity, aTarget );

                    warningsGiven.put( player, System.currentTimeMillis() );
                }
            }
        }
        return theTarget;
    }

    public void draw( boolean on ) {
        ((CraftLivingEntity) getMyEntity()).getHandle().b( on ); 
        // TODO: - IS THIS CORRECT?  What does it do?
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
        Double testAngle = Util.launchAngle( myLocation, targetsHeart, v, elev, g );

        if ( testAngle == null ) {
            clearTarget();
            return;
        }

        double hangtime = Util.hangtime( testAngle, v, elev, g );
        Vector targetVelocity = theTarget.getLocation().subtract( _projTargetLostLoc ).toVector();

        targetVelocity.multiply( 20 / Sentries.logicTicks );

        Location to = Util.leadLocation( targetsHeart, targetVelocity, hangtime );
        Vector victor = to.clone().subtract( myLocation ).toVector();

        double dist = Math.sqrt( Math.pow( victor.getX(), 2 ) + Math.pow( victor.getZ(), 2 ) );
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
                v += (1.188 * Math.pow( hangtime, 2 ));
            else
                v += (0.5 * Math.pow( hangtime, 2 ));

            v += (random.nextDouble() - 0.8) / 2;

            // apply power
            victor = victor.multiply( v / 20.0 );

        }
        switch ( myAttacks.lightningLevel ) {

            case 1:
                to.getWorld().strikeLightningEffect( to );
                theTarget.damage( getStrength(), myEntity );
                break;
            case 2:
                to.getWorld().strikeLightning( to );
                break;
            case 3:
                to.getWorld().strikeLightningEffect( to );
                theTarget.setHealth( 0 );
                break;
            default:
                // not lightning
                Projectile projectile;

                if ( myProjectile == ThrownPotion.class ) {
                    World nmsWorld = ((CraftWorld) myEntity.getWorld()).getHandle();
                    EntityPotion ent = new EntityPotion( nmsWorld,
                                                         myLocation.getX(), 
                                                         myLocation.getY(),
                                                         myLocation.getZ(),
                                                         CraftItemStack.asNMSCopy( potiontype ) );
                    nmsWorld.addEntity( ent );
                    projectile = (Projectile) ent.getBukkitEntity();
                }
                else if ( myProjectile == EnderPearl.class )
                    projectile = myEntity.launchProjectile( myProjectile );
                else
                    projectile = myEntity.getWorld().spawn( myLocation, myProjectile );

                if (    myProjectile == Fireball.class
                        || myProjectile == WitherSkull.class ) {
                    victor = victor.multiply( 1 / 1000000000 );
                }
                else if ( myProjectile == SmallFireball.class ) {

                    victor = victor.multiply( 1 / 1000000000 );
                    ((SmallFireball) projectile).setIsIncendiary( myAttacks.incendiary );

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
                    if ( Sentries.debug )
                        Sentries.debugLog( epCount + "" );
                }

                sentry.arrows.add( projectile );
                projectile.setShooter( myEntity );
                projectile.setVelocity( victor );
        }

        if ( effect != null )
            myEntity.getWorld().playEffect( myEntity.getLocation(), effect, null );

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
            return armourValue;

        double mod = 0;

        if ( getMyEntity() instanceof Player ) {
            for ( ItemStack is : ((Player) getMyEntity()).getInventory().getArmorContents() ) {
                Material item = is.getType();

                if ( sentry.armorBuffs.containsKey( item ) )
                    mod += sentry.armorBuffs.get( item );
            }
        }
        return (int) (armourValue + mod);
    }

    public double getHealth() {
        if ( npc == null || getMyEntity() == null )
            return 0;

        return ((CraftLivingEntity) getMyEntity()).getHealth();
    }

    public float getSpeed() {
        if ( !npc.isSpawned() )
            return sentrySpeed;

        double mod = 0;
        LivingEntity myEntity = getMyEntity();

        if ( !sentry.speedBuffs.isEmpty() ) {

            if ( myEntity instanceof Player ) {
                for ( ItemStack stack : ((Player) myEntity).getInventory().getArmorContents() ) {
                    Material item = stack.getType();

                    if ( sentry.speedBuffs.containsKey( item ) )
                        mod += sentry.speedBuffs.get( item );
                }
            }
        }
        return (float) (sentrySpeed + mod) * (myEntity.isInsideVehicle() ? 2 : 1);
    }

    public int getStrength() {
        if ( sentry.strengthBuffs.isEmpty() )
            return strength;

        double mod = 0;
        LivingEntity myEntity = getMyEntity();

        if ( myEntity instanceof Player ) {

            Material item = ((Player) myEntity).getInventory().getItemInMainHand().getType();

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

        if ( npc == null || !npc.isSpawned() ) return;

        if ( guardeeName != null && guardeeEntity == null ) return;

        if ( System.currentTimeMillis() < okToTakedamage + 500 ) return;

        okToTakedamage = System.currentTimeMillis();

        event.getEntity().setLastDamageCause( event );

        LivingEntity attacker = null;
        Entity damager = event.getDamager();

        // Find the attacker
        if (    damager instanceof Projectile
                && ((Projectile) damager).getShooter() instanceof LivingEntity )
            attacker = (LivingEntity) ((Projectile) damager).getShooter();

        else if ( damager instanceof LivingEntity )
            attacker = (LivingEntity) damager;

        if (    attacker == null 
                || (    Sentries.ignoreListIsInvincible 
                        && isIgnoring( attacker ) ) ) {
            return;
        }    
        if (    iWillRetaliate
                && (    !(damager instanceof Projectile) 
                        || CitizensAPI.getNPCRegistry().getNPC( attacker ) == null) ) {

            attackTarget = attacker;
            setAttackTarget( attacker );
        }
        Hits hit = Hits.Hit;
        double damage = event.getDamage();

        if ( acceptsCriticals ) {

            hit = Hits.getHit();
            damage = Math.round( damage * hit.damageModifier );
        }
        
        int armour = getArmor();

        if ( damage > 0 ) {

            // knockback
            npc.getEntity().setVelocity( attacker.getLocation()
                                                 .getDirection()
                                                 .multiply( 1.0 / (sentryWeight + (armour / 5)) ) );
            // Apply armour
            damage -= armour;

            // there was damage before armour.
            if ( damage <= 0 ) {
                npc.getEntity().getWorld().playEffect( npc.getEntity().getLocation(),
                                                       Effect.ZOMBIE_CHEW_IRON_DOOR, 1 );
                hit = Hits.Block;
            }
        }

        if (    attacker instanceof Player
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
//                if ( damager instanceof HumanEntity ) 
//
//                    getMyEntity();
//                    ((CraftLivingEntity) getMyEntity()).getHandle().killer 
//                                = (EntityHuman) ((CraftLivingEntity) damager).getHandle();

                getMyEntity().damage( damage, attacker );
                die( true, event.getCause() );
            }
            else
                getMyEntity().damage( damage );
        }
    }

    public void onEnvironmentDamage( EntityDamageEvent event ) {
        // not called for fall damage, or for lightning on stormcallers,
        // or for fire on pyromancers & stormcallers, or for poison on witchdoctors.
        
        if ( myStatus == SentryStatus.isDYING ) return;

        if ( npc == null || !npc.isSpawned() || invincible ) return;

        if ( guardeeName != null && guardeeEntity == null ) return; 

        if ( System.currentTimeMillis() < okToTakedamage + 500 ) return;

        okToTakedamage = System.currentTimeMillis();

        LivingEntity myEntity = getMyEntity();
        double finaldamage = event.getDamage();
        DamageCause cause = event.getCause();

        myEntity.setLastDamageCause( event );

        if (    cause == DamageCause.CONTACT
                || cause == DamageCause.BLOCK_EXPLOSION ) {
            finaldamage -= getArmor();
        }

        if ( finaldamage > 0 ) {
            myEntity.playEffect( EntityEffect.HURT );

            if ( cause == DamageCause.FIRE ) {

                Navigator navigator = getNavigator();

                if ( !navigator.isNavigating() )
                    navigator.setTarget( myEntity.getLocation().add( 
                            random.nextInt( 2 ) - 1, 0, random.nextInt( 2 ) - 1 ) );
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

    static final int bridges = 512;

    int targetFlags = none;
    int ignoreFlags = none;


    /**
     * Convenience method that calls {@link#processTargetStrings( boolean )} with the arg 'true';
     */
    void processTargetStrings() {
        processTargetStrings( true );
    }
    /**
     * Scans the strings lists "validTargets" & "ignoreTargets", and sets the
     * "targets" and "ignores" bit-filter int's accordingly.
     * <p>
     * Also adds the strings relating to any named entities to the corresponding
     * list that is prefixed with a "_" character.
     * 
     * @param onReload - send true if the trait is being reloaded. false if the method is 
     *                      being called after adding a target.
     */
    void processTargetStrings( boolean onReload ) {

        if ( Sentries.debug )
            Sentries.debugLog( "processing targets for npc: " + npc.getName() );

        targetFlags = none;
        ignoreFlags = none;
        _ignoreTargets.clear();
        _validTargets.clear();

        for ( String target : validTargets ) {

            if (      target.contains( "ENTITY:ALL" ) ) targetFlags |= all;
            else if ( target.contains( "ENTITY:MONSTER" ) ) targetFlags |= allmonsters;
            else if ( target.contains( "ENTITY:PLAYER" ) ) targetFlags |= allplayers;
            else if ( target.contains( "ENTITY:NPC" ) ) targetFlags |= allnpcs;

            else if ( !checkBridges( target, onReload, true ) ) {

                _validTargets.add( target );

                if (      target.contains( "NPC:" ) ) targetFlags |= namednpcs;
                else if ( target.contains( "EVENT:" ) ) targetFlags |= events;
                else if ( target.contains( "PLAYER:" ) ) targetFlags |= namedplayers;
                else if ( target.contains( "ENTITY:" ) ) targetFlags |= namedentities;
             }
        }
        // end of 1st for loop
        
        for ( String ignore : ignoreTargets ) {
            if (      ignore.contains( "ENTITY:ALL" ) ) ignoreFlags |= all;
            else if ( ignore.contains( "ENTITY:MONSTER" ) ) ignoreFlags |= allmonsters;
            else if ( ignore.contains( "ENTITY:PLAYER" ) ) ignoreFlags |= allplayers;
            else if ( ignore.contains( "ENTITY:NPC" ) ) ignoreFlags |= allnpcs;
            else if ( ignore.contains( "ENTITY:OWNER" ) ) ignoreFlags |= owner;

            else if ( !checkBridges( ignore, onReload, false ) ) {

                _ignoreTargets.add( ignore );

                if (      ignore.contains( "NPC:" ) ) ignoreFlags |= namednpcs;
                else if ( ignore.contains( "PLAYER:" ) ) ignoreFlags |= namedplayers;
                else if ( ignore.contains( "ENTITY:" ) ) ignoreFlags |= namedentities;
             }
        }
        if ( Sentries.debug )
            Sentries.debugLog( "Target flags: " + targetFlags + "  Ignore flags: " + ignoreFlags );
    }

    private boolean checkBridges( String target, boolean onReload, boolean asTarget ) {

        for ( PluginBridge each : Sentries.activePlugins.values() ) {
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

    @Override
    public boolean isRunImplemented() { return true; }
    
    @Override
    public void run() {

        LivingEntity myEntity = getMyEntity();

        if ( myEntity == null ) myStatus = SentryStatus.isDEAD;
      
        if ( myStatus == SentryStatus.isDEAD || myStatus == SentryStatus.isDYING ) {
            myStatus.update( this );
            return;
        }
        if ( attackTarget != null ) setAttackTarget( attackTarget );

        if ( healRate > 0 && System.currentTimeMillis() > oktoheal ) {

            if ( getHealth() < sentryMaxHealth ) {

                double heal = 1;

                if ( healRate < 0.5 )
                    heal = (0.5 / healRate);

                setHealth( getHealth() + heal );

                if ( healAnimation != null )
                    NMS.sendPacketNearby( null, myEntity.getLocation(), healAnimation );

                if ( getHealth() >= sentryMaxHealth )
                    _myDamamgers.clear();

            }
            oktoheal = (long) (System.currentTimeMillis() + healRate * 1000);
        }

        if (    npc.isSpawned() 
                && !myEntity.isInsideVehicle() 
                && hasMount()
                && isMyChunkLoaded() )
            mount();

        if (    npc.isSpawned() 
                && (    myStatus == SentryStatus.isATTACKING 
                        || myStatus == SentryStatus.isLOOKING ) ) {

            myStatus.update( this );
        }
    }

    boolean isMyChunkLoaded() {

        LivingEntity myEntity = getMyEntity();

        if ( myEntity == null ) return false;

        Location npcLoc = myEntity.getLocation();

        return npcLoc.getWorld().isChunkLoaded( npcLoc.getBlockX() >> 4, npcLoc.getBlockZ() >> 4 );
    }

    /**
     * Searches for an Entity with a name that matches the provided String, and
     * if successful saves it in the field 'guardeeEntity' and the name in
     * 'guardeeName'
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

        if ( npc == null ) return false;

        if ( name == null ) {
            guardeeEntity = null;
            guardeeName = null;

            clearTarget();
            return true;
        }

        if ( onlyCheckAllPlayers ) {

            for ( Player player : Bukkit.getOnlinePlayers() ) {

                if ( name.equals( player.getName() ) ) {

                    guardeeEntity = player;
                    guardeeName = name;

                    clearTarget();
                    return true;
                }
            }
        }
        else {
            for ( Entity each : getMyEntity().getNearbyEntities( sentryRange, sentryRange, sentryRange ) ) {

                String ename = null;

                if ( each instanceof Player )
                    ename = ((Player) each).getName();

                else if ( each instanceof LivingEntity )
                    ename = ((LivingEntity) each).getCustomName();

                // if the entity for this loop isn't a player or living, move along...
                else continue;

                if ( ename == null ) continue;

                // name found! now is it the name we are looking for?
                if ( name.equals( ename ) ) {

                    guardeeEntity = (LivingEntity) each;
                    guardeeName = name;

                    clearTarget();
                    return true;
                }
            }
        }
        return false;
    }

    public void setHealth( double health ) {

        if ( npc == null ) return;

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
        
        myStatus = SentryStatus.isLOOKING;
        attackTarget = null;
        _projTargetLostLoc = null;
        
        draw( false );
        
        GoalController goalController = getGoalController();
        Navigator navigator = getNavigator();
        
        if ( guardeeEntity == null ) {
            // not a guard or entity to be guarded is not spawned.
            navigator.cancelNavigation();

            faceForward();

            // allow new goals to be added.
            if ( goalController.isPaused() )
                goalController.setPaused( false );
        }
        else {
            goalController.setPaused( true );

            if (    navigator.getEntityTarget() == null 
                    || navigator.getEntityTarget().getTarget() != guardeeEntity ) {
                
                LivingEntity myEntity = getMyEntity();

                if (    myEntity != null 
                        && guardeeEntity.getLocation().getWorld() != myEntity.getLocation().getWorld() ) {
                    npc.despawn();
                    npc.spawn( guardeeEntity.getLocation().add( 1, 0, 1 ) );
                    return;
                }
                navigator.setTarget( guardeeEntity, false );
                navigator.getLocalParameters().stationaryTicks( 3 * 20 );
            }
        }
        return;
    }

    public void setAttackTarget( LivingEntity theEntity ) {

        LivingEntity myEntity = getMyEntity();

        if ( myEntity == null || theEntity == myEntity ) return;

        if ( guardeeName != null && guardeeEntity == null )
            theEntity = null; // dont go aggro when bodyguard target isnt around.

        if ( theEntity == null ) {
            // no target to be attacked
            if ( Sentries.debug )
                Sentries.debugLog( npc.getName() + " - Set Target Null" );

            clearTarget();
            return;
        }

        if ( theEntity == guardeeEntity ) return;
        
        GoalController goalController = getGoalController();
        Navigator navigator = getNavigator();

        if ( !navigator.isNavigating() )
            faceEntity( myEntity, theEntity );

        attackTarget = theEntity;

        if ( myAttacks == AttackType.brawler ) {

            // check if the desired target is already the current destination.
            if (    navigator.getEntityTarget() != null
                    && navigator.getEntityTarget().getTarget() == theEntity )
                return;

            // pause goalcontroller to keep sentry focused on this attack
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
    
    //--------------------------------methods dealing with Mounts----------
    /** Returns true if mountID >= 0 */
    public boolean hasMount() { return mountID >= 0; }
 
    /** Returns the NPC with the current mountID or null if the id = -1 (the default) */
    // TODO convert to use uuid's
    NPC getMountNPC() {
        if ( hasMount() ) {
            return CitizensAPI.getNPCRegistry().getById( mountID );
        }
        return null;
    }

    NPC ifMountedGetMount() {

//        if ( Sentries.debug )
//            Sentries.debugLog( String.join( "", S.Col.RED, "ifMountedGetMount(): mountID = ", String.valueOf( mountID ) ) ) ;
        
        NPC mount = getMountNPC();
        
        if ( mount != null && mount.isSpawned() && getMyEntity().isInsideVehicle() ) {
            return mount;
        }
        return npc;
    }

    public void dismount() {

        LivingEntity myEntity = getMyEntity();
        
        if ( myEntity != null && myEntity.isInsideVehicle() ) {

            NPC mount = getMountNPC();

            if ( mount != null ) {
                myEntity.getVehicle().setPassenger( null );
                mount.despawn( DespawnReason.PENDING_RESPAWN );
            }
        }
    }

    public void mount() {
        if ( npc.isSpawned() ) {

            LivingEntity myEntity = getMyEntity();

            if ( myEntity.isInsideVehicle() )
                myEntity.getVehicle().setPassenger( null );

            NPC mount = getMountNPC();

            if ( mount == null || !mount.isSpawned() ) {

                mount = spawnMount();
            }

            if ( mount != null ) {

                if ( !mount.isSpawned() ) return; // dead mount

                mount.setProtected( false );

                NavigatorParameters mountParams = mount.getNavigator().getDefaultParameters();
                NavigatorParameters myParams = npc.getNavigator().getDefaultParameters();

                mountParams.attackStrategy( new MountAttackStrategy() );
                mountParams.useNewPathfinder( false );
                mountParams.speedModifier( myParams.speedModifier() * 2 );
                mountParams.range( myParams.range() + 5 );

                Entity ent = mount.getEntity();
                ent.setCustomNameVisible( false );
                ent.setPassenger( null );
                ent.setPassenger( myEntity );
            }
            else
                mountID = -1;
        }
    }

    /** 
     * Spawns and returns a mountNPC, creating a new NPC of type horse if the sentry does not already have a mount.
     * The method will do nothing and return null if the Sentries is not spawned.  */
    public NPC spawnMount() {
        if ( Sentries.debug ) Sentries.debugLog( "Creating mount for " + npc.getName() );

        if ( npc.isSpawned() ) {

            NPC mount = null;

            if ( hasMount() ) {
                mount = CitizensAPI.getNPCRegistry().getById( mountID );

                if ( mount != null )
                    mount.despawn();
                else
                    Sentries.logger.info( "Cannot find mount NPC " + mountID );
            }
            else {
                mount = CitizensAPI.getNPCRegistry().createNPC( EntityType.HORSE, npc.getName() + "_Mount" );
                mount.getTrait( MobType.class ).setType( EntityType.HORSE );
            }

            if ( mount == null ) {
                Sentries.logger.info( "Cannot create mount NPC!" );
            }
            else {
                mount.spawn( getMyEntity().getLocation() );
                mount.getTrait( Owner.class ).setOwner( npc.getTrait( Owner.class ).getOwner() );

                ((Horse) mount.getEntity()).getInventory().setSaddle( new ItemStack( Material.SADDLE ) );
                mountID = mount.getId();

                return mount;
            }
        }
        return null;
    }
    //------------------------------------------end of methods for mounts
    
    public boolean hasLOS( Entity other ) {

        LivingEntity myEntity = getMyEntity();
        
        if ( myEntity != null ) {
            return ignoreLOS || myEntity.hasLineOfSight( other );
        }
        return false;
    }

    /** Returns the entity of this NPC *only* if the NPC is spawned, and the entity is not dead. Otherwise returns null. */
    public LivingEntity getMyEntity() {
        
        Entity entity = npc.getEntity();
        
        if (    entity != null
                && !entity.isDead() ) {
            return (LivingEntity) entity;
        }
        return null;
    }


    @EventHandler
    public void onCitReload( CitizensReloadEvent event ) {

        cancelRunnable();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null || !(obj instanceof SentryTrait) ) return false;
        if ( ((SentryTrait) obj).myID == myID ) return true;
        return false;
    }

    @Override
    public int hashCode() { return myID; }
}
