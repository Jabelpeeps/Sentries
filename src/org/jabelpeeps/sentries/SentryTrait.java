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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jabelpeeps.sentries.attackstrategies.CreeperAttackStrategy;
import org.jabelpeeps.sentries.attackstrategies.MountAttackStrategy;
import org.jabelpeeps.sentries.attackstrategies.SpiderAttackStrategy;

import net.aufdemrand.denizen.npc.traits.HealthTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.NPCDamageEvent;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.MemoryDataKey;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;

public class SentryTrait extends Trait {

    Sentries sentry;

    Location _projTargetLostLoc;
    Location spawnLocation;

    int strength, armour, epCount, nightVision, respawnDelay, sentryRange, followDistance, warningRange, mountID;
    int activeStrength, activeArmour;
    float speed, activeSpeed;
    double attackRate, healRate, sentryWeight, sentryMaxHealth;
    boolean killsDropInventory, dropInventory, targetable, invincible, loaded, acceptsCriticals, iWillRetaliate, ignoreLOS;

    static GiveUpStuckAction giveup = new GiveUpStuckAction();

    String greetingMsg = "", warningMsg = "";

    private Map<Player, Long> warningsGiven = new HashMap<>();
    Set<Player> _myDamamgers = new HashSet<>();

    LivingEntity guardeeEntity;
    LivingEntity attackTarget;
    String guardeeName;
    DamageCause causeOfDeath;

//   Packet<?> healAnimation;

    Set<TargetType> targets = new HashSet<>();
    Set<TargetType> ignores = new HashSet<>();
    
    Set<String> ignoreTargets = new HashSet<>();
    Set<String> validTargets = new HashSet<>();

    Set<String> _ignoreTargets = new HashSet<>();
    Set<String> _validTargets = new HashSet<>();

    long isRespawnable = System.currentTimeMillis();
    long oktoFire = System.currentTimeMillis();
    long oktoheal = System.currentTimeMillis();
    long oktoreasses = System.currentTimeMillis();
    long okToTakedamage = 0;

    List<PotionEffect> weaponSpecialEffects;
    ItemStack potionItem;
    Random random = new Random();

    SentryStatus myStatus = SentryStatus.NOT_SPAWNED;
    SentryStatus oldStatus;
    AttackType myAttack;

    private BukkitTask tickMe;

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

        armour = key.getInt( S.ARMOR, sentry.defaultIntegers.get( S.ARMOR ) );
        strength = key.getInt( S.STRENGTH, sentry.defaultIntegers.get( S.STRENGTH ) );
        sentryRange = key.getInt( S.RANGE, sentry.defaultIntegers.get( S.RANGE ) );
        respawnDelay = key.getInt( S.RESPAWN_DELAY, sentry.defaultIntegers.get( S.RESPAWN_DELAY ) );
        followDistance = key.getInt( S.FOLLOW_DISTANCE, sentry.defaultIntegers.get( S.FOLLOW_DISTANCE ) );
        warningRange = key.getInt( S.WARNING_RANGE, sentry.defaultIntegers.get( S.WARNING_RANGE ) );
        nightVision = key.getInt( S.NIGHT_VISION, sentry.defaultIntegers.get( S.NIGHT_VISION ) );
        mountID = key.getInt( S.MOUNTID, -1 );

        speed = (float) key.getDouble( S.SPEED, sentry.defaultDoubles.get( S.SPEED ) );
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

    @Override
    public void onSpawn() {
        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onSpawn()" );

        if ( !loaded ) {
            try {
                load( new MemoryDataKey() );

            } catch ( NPCLoadException e ) { e.printStackTrace(); }
        }
        
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
        faceForward();

//        healAnimation = new PacketPlayOutAnimation( NMS.getHandle( myEntity ), 6 );

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

        if ( tickMe == null ) {
            tickMe = new BukkitRunnable() {                
                    @SuppressWarnings( "synthetic-access" )
                    @Override
                    public void run() {                      
                        if ( Sentries.debug && oldStatus != myStatus ) {
                            Sentries.debugLog( npc.getName() + " is now:- " + myStatus.name() );
                            oldStatus = myStatus;
                        }
                        myStatus = myStatus.update( SentryTrait.this );                    
                    }
            }.runTaskTimer( sentry, 40 + myID, Sentries.logicTicks );
        }
    }

    @Override
    public void onRemove() {        
        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onRemove()" );

        cancelRunnable();
    }
    
    public void cancelRunnable() {
        if ( tickMe != null ) tickMe.cancel();;
    }
    
    @Override
    public void onDespawn() {
        if ( Sentries.debug )
            Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onDespawn()" );

        isRespawnable = System.currentTimeMillis() + respawnDelay * 1000;
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
        key.setDouble( S.SPEED, speed );
        key.setDouble( S.WEIGHT, sentryWeight );
        key.setDouble( S.HEALRATE, healRate );
        key.setInt( S.ARMOR, armour );
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
        new BukkitRunnable() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void run() {
                spawnLocation = npc.getEntity().getLocation();
            }
        }.runTaskLater( sentry, 10 );
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
                LivingEntity player = Bukkit.getPlayer( targetNpc.getTrait( Owner.class ).getOwnerId() );
                
                if ( player != null ) return isIgnoring( player );
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
            LivingEntity player = Bukkit.getPlayer( targetNpc.getTrait( Owner.class ).getOwnerId() );
            
            if ( player != null ) return isTarget( player );            
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

        if ( myStatus.isDeadOrDieing() ) return;

        causeOfDeath = cause;

        myStatus = SentryStatus.DIEING;

        if ( runscripts && Sentries.denizenActive )
            DenizenHook.sentryDeath( _myDamamgers, npc );
    }

    void faceEntity( Entity from, Entity at ) {        
        NMS.look( NMS.getHandle( from ), NMS.getHandle( at ) );
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
        
        NMS.getHandle( getMyEntity() ).b( on );
        // TODO: - IS THIS CORRECT?  What does it do?
    }

    public void fire( LivingEntity theTarget ) {

        LivingEntity myEntity = getMyEntity();
        Class<? extends Projectile> projectileClazz = myAttack.getProjectile();
        Effect effect = null;

        double v = 34;
        double g = 20;

        boolean ballistics = true;

        if ( projectileClazz == Arrow.class ) {
            effect = Effect.BOW_FIRE;
        }
        else if ( projectileClazz == SmallFireball.class
                || projectileClazz == Fireball.class
                || projectileClazz == WitherSkull.class ) {
            effect = Effect.BLAZE_SHOOT;
            ballistics = false;
        }
        else if ( projectileClazz == ThrownPotion.class ) {
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

        if ( dist == 0 ) return;

        if ( !hasLOS( theTarget ) ) {
            clearTarget();
            return;
        }
        if ( myAttack.lightningLevel > 0 ) {
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

            if ( projectileClazz == Arrow.class
                    || projectileClazz == ThrownPotion.class )
                v += (1.188 * Math.pow( hangtime, 2 ));
            else
                v += (0.5 * Math.pow( hangtime, 2 ));

            v += (random.nextDouble() - 0.8) / 2;

            // apply power
            victor = victor.multiply( v / 20.0 );

        }
        switch ( myAttack.lightningLevel ) {

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
                Projectile projectile = myEntity.getWorld().spawn( myLocation, projectileClazz );

                if (    projectileClazz == ThrownPotion.class 
                        && potionItem != null ) {
                    
                    projectile = myEntity.getWorld().spawn( myLocation, ThrownPotion.class );
                    ((ThrownPotion) projectile).setItem( potionItem.clone() );
                }
                else if (    projectileClazz == Fireball.class
                        || projectileClazz == WitherSkull.class ) {
                    victor = victor.multiply( 1 / 1000000000 );
                }
                else if ( projectileClazz == SmallFireball.class ) {

                    victor = victor.multiply( 1 / 1000000000 );
                    ((SmallFireball) projectile).setIsIncendiary( myAttack.incendiary );

                    if ( !myAttack.incendiary ) {
                        ((SmallFireball) projectile).setFireTicks( 0 );
                        ((SmallFireball) projectile).setYield( 0 );
                    }
                }
                else if ( projectileClazz == EnderPearl.class ) {

                    // TODO why are we counting enderpearls?
                    epCount++;
                    if ( epCount > Integer.MAX_VALUE - 1 )
                        epCount = 0;
                    
                    if ( Sentries.debug ) Sentries.debugLog( "epCount: " + String.valueOf( epCount ) );
                }

                if ( projectileClazz == Arrow.class ) sentry.arrows.add( projectile );
                
                projectile.setShooter( myEntity );
                
                if ( projectile instanceof Fireball ) 
                    ((Fireball) projectile).setDirection( victor );
                else
                    projectile.setVelocity( victor );
        }

        if ( effect != null )
            myEntity.getWorld().playEffect( myEntity.getLocation(), effect, null );

        faceEntity( getMyEntity(), theTarget );

        if ( projectileClazz == Arrow.class )
            draw( false );
        else if ( myEntity instanceof Player ) 
            PlayerAnimation.ARM_SWING.play( (Player) myEntity, 64 );
    }

    public double getHealth() {
        LivingEntity myEntity = getMyEntity();
        if ( npc == null || myEntity == null ) return 0;

        return myEntity.getHealth();
    }
    
    public int getArmor() {

        if ( sentry.armorBuffs.isEmpty() )
            return armour;

        double mod = 0;

        if ( getMyEntity() instanceof Player ) {
            for ( ItemStack is : ((Player) getMyEntity()).getInventory().getArmorContents() ) {
                Material item = is.getType();

                if ( sentry.armorBuffs.containsKey( item ) )
                    mod += sentry.armorBuffs.get( item );
            }
        }
        return (int) (armour + mod);
    }

    public float getSpeed() {

        LivingEntity myEntity = getMyEntity();
        
        if ( myEntity == null ) return speed;

        double mod = 0;
        if ( !sentry.speedBuffs.isEmpty() ) {

            if ( myEntity instanceof Player ) {
                for ( ItemStack stack : ((Player) myEntity).getInventory().getArmorContents() ) {
                    Material item = stack.getType();

                    if ( sentry.speedBuffs.containsKey( item ) )
                        mod += sentry.speedBuffs.get( item );
                }
            }
        }
        return (float) ( speed + mod ) * ( myEntity.isInsideVehicle() ? 2 : 1 );
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

    static Set<AttackType> pyros = EnumSet.range( AttackType.pyro1, AttackType.pyro3 );
    static Set<AttackType> stormCallers = EnumSet.range( AttackType.sc1, AttackType.sc3 );
    static Set<AttackType> notFlammable = EnumSet.range( AttackType.pyro1, AttackType.sc3 );

    public boolean isPyromancer() { return pyros.contains( myAttack ); }
    public boolean isPyromancer1() { return myAttack == AttackType.pyro1; }
    public boolean isStormcaller() { return stormCallers.contains( myAttack ); }
    public boolean isWarlock1() { return myAttack == AttackType.warlock1; }
    public boolean isWitchDoctor() { return myAttack == AttackType.witchdoctor; }
    public boolean isFlammable() { return !notFlammable.contains( myAttack ); }

   public void onEnvironmentDamage( NPCDamageEvent event ) {
        // not called for fall damage, or for lightning on stormcallers,
        // or for fire on pyromancers & stormcallers, or for poison on witchdoctors.
        
        if ( myStatus.isDeadOrDieing() ) return;

        if ( npc == null || !npc.isSpawned() || invincible ) return;

        if ( guardeeName != null && guardeeEntity == null ) return; 

        if ( System.currentTimeMillis() < okToTakedamage + 500 ) return;

        okToTakedamage = System.currentTimeMillis();

        LivingEntity myEntity = getMyEntity();
        double finaldamage = event.getDamage();
        DamageCause cause = event.getCause();

 //       myEntity.setLastDamageCause( event );

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

    void reMountMount() {
           
        if (    npc.isSpawned() 
                && !getMyEntity().isInsideVehicle() 
                && hasMount()
                && isMyChunkLoaded() )
            mount();
    }
    
    void tryToHeal() {
        
        if ( healRate > 0 && System.currentTimeMillis() > oktoheal ) {

            if ( getHealth() < sentryMaxHealth ) {

                double heal = 1;

                if ( healRate < 0.5 )
                    heal = (0.5 / healRate);

                setHealth( getHealth() + heal );
                LivingEntity myEntity = getMyEntity();
                
                // idk what this effect looks like, so lets see if it looks ok in-game.
                myEntity.getWorld().playEffect( myEntity.getLocation(), Effect.VILLAGER_PLANT_GROW, 100 );

//                if ( healAnimation != null )
//                    NMS.sendPacketNearby( null, getMyEntity().getLocation(), healAnimation );

                if ( getHealth() >= sentryMaxHealth )
                    _myDamamgers.clear();

            }
            oktoheal = (long) (System.currentTimeMillis() + healRate * 1000);
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

        if ( npc == null || name == null ) return false;

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

        LivingEntity myEntity = getMyEntity();

        if ( myEntity == null ) return;

        myEntity.setMaxHealth( sentryMaxHealth );

        if ( health > sentryMaxHealth ) health = sentryMaxHealth;

        myEntity.setHealth( health );
    }
    
    boolean equip( ItemStack newEquipment ) {

        Equipment equipment = npc.getTrait( Equipment.class );
        if ( equipment == null ) return false;
        // the npc's entity type does not support equipment.

        if ( newEquipment == null ) {

            for ( int i = 0; i < 5; i++ ) {

                if (    equipment.get( i ) != null
                        && equipment.get( i ).getType() != Material.AIR ) {
                    equipment.set( i, null );
                }
            }
            return true;
        }
        int slot = Sentries.getSlot( newEquipment.getType() );

        equipment.set( slot, newEquipment );

        if ( slot == 0 ) updateAttackType();

        return true;
    }

    /**
     * Updates the Attacktype reference in myAttack field, according to the
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

            myAttack = AttackType.find( weapon );

            if ( myAttack != AttackType.witchdoctor )
                item.setDurability( (short) 0 );
            
        }
        else if ( myEntity instanceof Skeleton ) myAttack = AttackType.archer;
        else if ( myEntity instanceof Ghast ) myAttack = AttackType.pyro3;
        else if ( myEntity instanceof Snowman ) myAttack = AttackType.magi;
        else if ( myEntity instanceof Wither ) myAttack = AttackType.warlock2;
        else if ( myEntity instanceof Witch ) myAttack = AttackType.witchdoctor;
        else if ( myEntity instanceof Creeper ) myAttack = AttackType.creeper;
        else if ( myEntity instanceof Blaze || myEntity instanceof EnderDragon ) myAttack = AttackType.pyro2;
        else myAttack = AttackType.brawler;

        if ( myAttack == AttackType.witchdoctor ) {
            if ( item == null ) {
                item = new ItemStack( Material.SPLASH_POTION, 1, (short) 16396 );
                // TODO send message to owner about equipping a proper potion.
            }
            potionItem = item;
            weaponSpecialEffects = null;
        }
        else {
            potionItem = null;
            weaponSpecialEffects = sentry.weaponEffects.get( weapon );
        }
    }

    /**
     * Clears the target of the Sentry's attack, and returns it to following/looking status.
     * 
     */
    void clearTarget() {
        
        myStatus = SentryStatus.is_A_Guard( this );
        attackTarget = null;
        _projTargetLostLoc = null;
        
        draw( false );
        
//        GoalController goalController = getGoalController();
//        Navigator navigator = getNavigator();
//        
//        if ( guardeeEntity == null ) {
//            // not a guard or entity to be guarded is not spawned.
//            navigator.cancelNavigation();
//
//            faceForward();
//
//            // allow new goals to be added.
//            if ( goalController.isPaused() )
//                goalController.setPaused( false );
//        }
//        else {
//            goalController.setPaused( true );
//
//            if (    navigator.getEntityTarget() == null 
//                    || navigator.getEntityTarget().getTarget() != guardeeEntity ) {
//                
//                LivingEntity myEntity = getMyEntity();
//
//                if (    myEntity != null 
//                        && guardeeEntity.getLocation().getWorld() != myEntity.getLocation().getWorld() ) {
//                    npc.despawn();
//                    npc.spawn( guardeeEntity.getLocation().add( 1, 0, 1 ) );
//                    return;
//                }
//                navigator.setTarget( guardeeEntity, false );
//                navigator.getLocalParameters().stationaryTicks( 3 * 20 );
//            }
//        }
        return;
    }

    public void setAttackTarget( LivingEntity theEntity ) {

        LivingEntity myEntity = getMyEntity();

        if ( myEntity == null || theEntity == myEntity || theEntity == guardeeEntity ) return;

        // don't attack when bodyguard target isn't around.
        if ( guardeeName != null && guardeeEntity == null )
//            theEntity = null; 
//
//        if ( theEntity == null ) {
//            // no target to be attacked
//            if ( Sentries.debug )
//                Sentries.debugLog( npc.getName() + " - Set Target Null? " );
//
//            clearTarget();
            return;
//        }
        attackTarget = theEntity;
        myStatus = SentryStatus.ATTACKING;
    }

    Navigator getNavigator() {
        return ifMountedGetMount().getNavigator();
    }

    GoalController getGoalController() {
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
    
    
    static class GiveUpStuckAction implements StuckAction {
        @Override
        public boolean run( NPC npc, Navigator navigator ) {

            if ( !npc.isSpawned() ) return false;

            Location target = navigator.getTargetAsLocation();
            Location present = npc.getEntity().getLocation();

            if ( target.getWorld() == present.getWorld()
                    && present.distanceSquared( target ) <= 4 ) {
                return true;
            }
            Util.getSentryTrait( npc ).clearTarget();
            return false;
        }
    }
}
