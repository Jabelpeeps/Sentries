package org.jabelpeeps.sentries;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Horse;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.targets.TargetType;

import net.aufdemrand.denizen.npc.traits.HealthTrait;
import net.citizensnpcs.api.ai.AttackStrategy;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDamageEvent;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.MemoryDataKey;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;

public class SentryTrait extends Trait {

    final Sentries sentry;
    public Location spawnLocation;

    public int strength, epCount, nightVision, respawnDelay, range, followDistance, voiceRange, mountID;
    public float speed;

    public double attackRate, healRate, armour, weight, maxHealth;
    public boolean killsDrop, dropInventory, targetable, invincible, iRetaliate, acceptsCriticals;
    
    boolean loaded;
    boolean ignoreLOS;

    static SentryStuckAction setStuckStatus = new SentryStuckAction();
    static AttackStrategy mountedAttack = new MountAttackStrategy();

    public String greetingMsg = "", warningMsg = "";

    private Map<Player, Long> warningsGiven = new HashMap<>();
    Set<Player> _myDamamgers = new HashSet<>();

    public LivingEntity guardeeEntity;
    public LivingEntity attackTarget;
    public String guardeeName;
    DamageCause causeOfDeath;
    Entity killer;

    public Set<TargetType> targets = new TreeSet<>();
    public Set<TargetType> ignores = new TreeSet<>();
    public Set<TargetType> events = new TreeSet<>();

    long respawnTime = System.currentTimeMillis();
//    long okToAttack = System.currentTimeMillis();
    long oktoheal = System.currentTimeMillis();
    long reassesTime = System.currentTimeMillis();
    public long okToTakedamage = 0;

    List<PotionEffect> weaponSpecialEffects;
    ItemStack potionItem;

    public SentryStatus myStatus = SentryStatus.NOT_SPAWNED;
    SentryStatus oldStatus;
    AttackType myAttack;

    private Integer tickMe;
    
    public SentryTrait() {
        super( "sentries" );
        sentry = (Sentries) Bukkit.getPluginManager().getPlugin( "Sentries" );
    }
    
    @SuppressWarnings( "unchecked" )
    @Override
    public void load( DataKey key ) throws NPCLoadException {
        if ( Sentries.debug ) Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] load() start" );
  
        if ( key.keyExists( "traits" ) ) 
            key = key.getRelative( "traits" );

        iRetaliate = key.getBoolean( S.CON_RETALIATION, sentry.defaultBooleans.get( S.CON_RETALIATION ) );
        invincible = key.getBoolean( S.CON_INVINCIBLE, sentry.defaultBooleans.get( S.CON_INVINCIBLE ) );
        dropInventory = key.getBoolean( S.CON_DROP_INV, sentry.defaultBooleans.get( S.CON_DROP_INV ) );
        acceptsCriticals = key.getBoolean( S.CON_CRIT_HITS, sentry.defaultBooleans.get( S.CON_CRIT_HITS ) );
        killsDrop = key.getBoolean( S.CON_KILLS_DROP, sentry.defaultBooleans.get( S.CON_KILLS_DROP ) );
        ignoreLOS = key.getBoolean( S.CON_IGNORE_LOS, sentry.defaultBooleans.get( S.CON_IGNORE_LOS ) );
        targetable = key.getBoolean( S.CON_MOBS_ATTACK, sentry.defaultBooleans.get( S.CON_MOBS_ATTACK ) );

        strength = key.getInt( S.CON_STRENGTH, sentry.defaultIntegers.get( S.CON_STRENGTH ) );
        range = key.getInt( S.CON_RANGE, sentry.defaultIntegers.get( S.CON_RANGE ) );
        respawnDelay = key.getInt( S.CON_RESPAWN_DELAY, sentry.defaultIntegers.get( S.CON_RESPAWN_DELAY ) );
        followDistance = key.getInt( S.CON_FOLLOW_DIST, sentry.defaultIntegers.get( S.CON_FOLLOW_DIST ) );
        voiceRange = key.getInt( S.CON_VOICE_RANGE, sentry.defaultIntegers.get( S.CON_VOICE_RANGE ) );
        nightVision = key.getInt( S.CON_NIGHT_VIS, sentry.defaultIntegers.get( S.CON_NIGHT_VIS ) );
        mountID = key.getInt( S.PERSIST_MOUNT, -1 );

        armour = key.getDouble( S.CON_ARMOUR, sentry.defaultDoubles.get( S.CON_ARMOUR ) );
        speed = (float) key.getDouble( S.CON_SPEED, sentry.defaultDoubles.get( S.CON_SPEED ) );
        weight = key.getDouble( S.CON_WEIGHT, sentry.defaultDoubles.get( S.CON_WEIGHT ) );
        maxHealth = key.getDouble( S.CON_HEALTH, sentry.defaultDoubles.get( S.CON_HEALTH ) );
        attackRate = key.getDouble( S.CON_ARROW_RATE, sentry.defaultDoubles.get( S.CON_ARROW_RATE ) );
        healRate = key.getDouble( S.CON_HEAL_RATE, sentry.defaultDoubles.get( S.CON_HEAL_RATE ) );

        guardeeName = key.getString( S.PERSIST_GUARDEE, null );
        greetingMsg = key.getString( S.CON_GREETING, sentry.defaultGreeting );
        warningMsg = key.getString( S.CON_WARNING, sentry.defaultWarning );

        if ( key.keyExists( S.PERSIST_SPAWN ) ) {
            spawnLocation = new Location( Bukkit.getWorld( key.getString( "Spawn.world" ) ),
                                         key.getDouble( "Spawn.x" ), 
                                         key.getDouble( "Spawn.y" ),
                                         key.getDouble( "Spawn.z" ),
                                        (float) key.getDouble( "Spawn.yaw" ),
                                        (float) key.getDouble( "Spawn.pitch" ) );

            if ( spawnLocation.getWorld() == null )
                spawnLocation = null;
        }
        if ( guardeeName != null && guardeeName.isEmpty() )
            guardeeName = null;
        
        Set<String> validTargets = new HashSet<>();
        
        if ( key.getRaw( S.TARGETS ) != null )
            validTargets.addAll( (Set<String>) key.getRaw( S.TARGETS ) );
        else
            validTargets.addAll( sentry.defaultTargets );
        
        validTargets.parallelStream().forEach( t -> CommandHandler.getCommand( S.TARGET ).call( null, null, this, 0, "", "add", t ) );
        
        validTargets.parallelStream()
                    .filter( v -> targets.parallelStream()
                                         .noneMatch( t -> t.getTargetString().equalsIgnoreCase( v ) ) )
                    .forEach( e -> checkBridges( e ) );
        
        Set<String> ignoreTargets = new HashSet<>();
        
        if ( key.getRaw( S.IGNORES ) != null )
            ignoreTargets.addAll( (Set<String>) key.getRaw( S.IGNORES ) );
        else
            ignoreTargets.addAll( sentry.defaultIgnores );
        
        ignoreTargets.parallelStream().forEach( i -> CommandHandler.getCommand( S.IGNORE ).call( null, null, this, 0, "", "add", i ) ); 
        
        ignoreTargets.parallelStream()
                     .filter( v -> ignores.parallelStream()
                                          .noneMatch( i -> i.getTargetString().equalsIgnoreCase( v ) ) )
                     .forEach( e -> checkBridges( e ) );

        Set<String> eventTargets = new HashSet<>();
        
        if ( key.getRaw( S.EVENTS ) != null )
            eventTargets.addAll( (Set<String>) key.getRaw( S.EVENTS ) );
        
        eventTargets.parallelStream().forEach( e -> CommandHandler.getCommand( S.EVENT ).call( null, null, this, 0, "", "add", e ) );
        
        updateArmour();
        updateAttackType();
        
        loaded = true;      
        if ( Sentries.debug ) {      
            Sentries.debugLog( "validTargets: " + validTargets.toString() );
            Sentries.debugLog( "ignoreTargets: " + ignoreTargets.toString() );
            Sentries.debugLog( "eventTargets: " + eventTargets.toString() );
        }
        
    }
    
    private void checkBridges( String target ) {
        if ( Sentries.debug ) Sentries.debugLog( "checkBridges() called with: " + target );
        
        Sentries.activePlugins.parallelStream()
                              .filter( p -> p instanceof PluginTargetBridge )
                              .map( p -> (PluginTargetBridge) p )
                              .filter( p -> target.contains( p.getPrefix() ) )
                              .forEach( b -> b.add( this, target ) );
    }
    
    @Override
    public void onSpawn() {
        if ( Sentries.debug ) Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onSpawn()" );

        if ( !loaded ) {
            try {
                load( new MemoryDataKey() );

            } catch ( NPCLoadException e ) { e.printStackTrace(); }
        }
        
        LivingEntity myEntity = getMyEntity();

        // check for illegal values
        if ( weight <= 0 ) weight = 1.0;
        if ( attackRate > 30 ) attackRate = 30.0;
        if ( maxHealth < 1 ) maxHealth = 1;
        if ( range < 1 ) range = 1;
        if ( range > 200 ) range = 200;
        if ( respawnDelay < -1 ) respawnDelay = -1;
        if ( spawnLocation == null ) spawnLocation = myEntity.getLocation();

        // Allow Denizen to handle the sentry's health if it is active.
        if (    DenizenHook.sentryHealthByDenizen 
                && npc.hasTrait( HealthTrait.class ) )
            npc.removeTrait( HealthTrait.class );

        // disable citizens respawning, because Sentries doesn't always raise EntityDeath
        npc.data().set( NPC.RESPAWN_DELAY_METADATA, -1 );

        myEntity.getAttribute( Attribute.GENERIC_MAX_HEALTH ).setBaseValue( maxHealth );
        setHealth( maxHealth );
        
        _myDamamgers.clear();
        NMS.look( myEntity, myEntity.getLocation().getYaw(), 0 );

        if ( guardeeName == null )
            npc.teleport( spawnLocation, TeleportCause.PLUGIN );

        NavigatorParameters navigatorParams = npc.getNavigator().getDefaultParameters();

        npc.setProtected( false );
        npc.data().set( NPC.TARGETABLE_METADATA, targetable );

        navigatorParams.stationaryTicks( 60 ); // = 3 seconds
        navigatorParams.useNewPathfinder( true );
        navigatorParams.stuckAction( setStuckStatus );
        navigatorParams.baseSpeed( speed );
        navigatorParams.attackDelayTicks( (int) (attackRate * 20) );

        updateAttackType();
       
        if ( tickMe == null ) {
            tickMe = Bukkit.getScheduler().scheduleSyncRepeatingTask( sentry, 
                    () -> {                  
                                if ( Sentries.debug && oldStatus != myStatus ) {
                                    Sentries.debugLog( npc.getName() + " is now:- " + myStatus.name() );
                                    oldStatus = myStatus;
                                }
                                myStatus = myStatus.update( SentryTrait.this );                    
                            }, 40, Sentries.logicTicks );
        }
    }

    @Override
    public void onRemove() {        
        if ( Sentries.debug ) Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onRemove()" );

        if ( hasMount() ) Utils.removeMount( mountID );       
        cancelRunnable();
    }
    
    public void cancelRunnable() {
        if ( tickMe != null ) Bukkit.getScheduler().cancelTask( tickMe );
    }
    
    public void die( boolean runscripts, NPCDamageEvent event ) {

        if ( myStatus.isDeadOrDieing() ) return;
        
        myStatus = SentryStatus.DIEING;       
        respawnTime = System.currentTimeMillis() + respawnDelay * 1000;
        causeOfDeath = event.getCause();
        
        if ( event instanceof NPCDamageByEntityEvent )
            killer = ((NPCDamageByEntityEvent) event).getDamager();
        
        if ( runscripts && Sentries.denizenActive )
            DenizenHook.sentryDeath( _myDamamgers, npc );
    }

    @Override
    public void onDespawn() {
        if ( Sentries.debug ) Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onDespawn()" );

        dismount();
        myStatus = SentryStatus.NOT_SPAWNED;
    }
    
    @Override
    public void save( DataKey key ) {
        if ( Sentries.debug ) Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] save()" );
        
        key.setBoolean( S.CON_RETALIATION, iRetaliate );
        key.setBoolean( S.CON_INVINCIBLE, invincible );
        key.setBoolean( S.CON_DROP_INV, dropInventory );
        key.setBoolean( S.CON_KILLS_DROP, killsDrop );
        key.setBoolean( S.CON_MOBS_ATTACK, targetable );
        key.setInt( S.PERSIST_MOUNT, mountID );
        key.setBoolean( S.CON_CRIT_HITS, acceptsCriticals );
        key.setBoolean( S.CON_IGNORE_LOS, ignoreLOS );

        Set<String> ignoreTargets = new HashSet<>();
        Set<String> validTargets = new HashSet<>();
        Set<String> eventTargets = new HashSet<>();
        
        targets.forEach( s -> validTargets.add( s.getTargetString() ) );
        ignores.forEach( s -> ignoreTargets.add( s.getTargetString() ) ); 
        events.forEach( e -> eventTargets.add( e.getTargetString() ) );
        
        if ( Sentries.debug ) {
            Sentries.debugLog( "validTargets: " + validTargets.toString() + System.lineSeparator() +
                                "ignoreTargets: " + ignoreTargets.toString() + System.lineSeparator() +
                                "eventTargets: " + eventTargets.toString() );
        }
        
        key.setRaw( S.TARGETS, validTargets );
        key.setRaw( S.IGNORES, ignoreTargets );
        key.setRaw( S.EVENTS, eventTargets );

        if ( spawnLocation != null ) {
            key.setDouble( "Spawn.x", spawnLocation.getX() );
            key.setDouble( "Spawn.y", spawnLocation.getY() );
            key.setDouble( "Spawn.z", spawnLocation.getZ() );
            key.setString( "Spawn.world", spawnLocation.getWorld().getName() );
            key.setDouble( "Spawn.yaw", spawnLocation.getYaw() );
            key.setDouble( "Spawn.pitch", spawnLocation.getPitch() );
        }

        key.setDouble( S.CON_HEALTH, maxHealth );
        key.setInt( S.CON_RANGE, range );
        key.setInt( S.CON_RESPAWN_DELAY, respawnDelay );
        key.setDouble( S.CON_SPEED, speed );
        key.setDouble( S.CON_WEIGHT, weight );
        key.setDouble( S.CON_HEAL_RATE, healRate );
        key.setDouble( S.CON_ARMOUR, armour );
        key.setInt( S.CON_STRENGTH, strength );
        key.setInt( S.CON_VOICE_RANGE, voiceRange );
        key.setDouble( S.CON_ARROW_RATE, attackRate );
        key.setInt( S.CON_NIGHT_VIS, nightVision );
        key.setInt( S.CON_FOLLOW_DIST, followDistance );

        if ( guardeeName != null )
            key.setString( S.PERSIST_GUARDEE, guardeeName );
        else if ( key.keyExists( S.PERSIST_GUARDEE ) )
            key.removeKey( S.PERSIST_GUARDEE );

        key.setString( S.CON_WARNING, warningMsg );
        key.setString( S.CON_GREETING, greetingMsg );
    }

    @Override
    public void onCopy() {
        Bukkit.getScheduler().runTaskLater( sentry, () -> spawnLocation = npc.getEntity().getLocation(), 10 );
    }

    public boolean isIgnoring( LivingEntity aTarget ) {
        if ( aTarget == guardeeEntity ) return true;      
        if ( ignores.parallelStream().anyMatch( t -> t.includes( aTarget ) ) ) return true;
        
        if ( aTarget.hasMetadata("NPC") ) {

            LivingEntity player = Bukkit.getPlayer( Sentries.registry.getNPC( aTarget ).getTrait( Owner.class ).getOwnerId() );
            
            if ( player != null ) return isIgnoring( player );           
        }
        return false;
    }

    public boolean isTarget( LivingEntity aTarget ) {
        if ( targets.parallelStream().anyMatch( t -> t.includes( aTarget ) ) ) return true;
               
        if ( aTarget.hasMetadata("NPC") ) {

            // As we are checking an NPC and haven't decided whether to attack it yet, lets check the owner.
            LivingEntity player = Bukkit.getPlayer( Sentries.registry.getNPC( aTarget ).getTrait( Owner.class ).getOwnerId() );
            
            if ( player != null ) return isTarget( player );            
        }
        return false;
    }

    public LivingEntity findTarget() {

        LivingEntity myEntity = getMyEntity();
        int combinedRange = range + voiceRange;
        Location myLoc = myEntity.getLocation();

        List<Entity> entitiesInRange = myEntity.getNearbyEntities( combinedRange, combinedRange / 2, combinedRange );
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
                if ( lightLevel >= ( 16 - nightVision ) ) {

                    double dist = aTarget.getLocation().distance( myLoc );

                    if ( hasLOS( aTarget ) ) {

                        if (    voiceRange > 0 
                                && !warningMsg.isEmpty()
                                && dist > range 
                                && aTarget instanceof Player
                                && !aTarget.hasMetadata( "NPC" ) ) {

                            if (    !warningsGiven.containsKey( aTarget ) 
                                    || System.currentTimeMillis() > warningsGiven.get( aTarget ) + 60000 ) {

                                Player player = (Player) aTarget;

                                player.sendMessage( Utils.format( warningMsg, npc, player, null, null ) );

                                if ( !getNavigator().isNavigating() )
                                    NMS.look( myEntity, aTarget );

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
            else if ( voiceRange > 0 
                    && !greetingMsg.isEmpty()
                    && aTarget instanceof Player
                    && !aTarget.hasMetadata( "NPC" ) ) {

                if (    hasLOS( aTarget )
                        && (    !warningsGiven.containsKey( aTarget )
                                || System.currentTimeMillis() > warningsGiven.get( aTarget ) + 60000) ) {

                    Player player = (Player) aTarget;

                    player.sendMessage( Utils.format( greetingMsg, npc, player, null, null ) );
                    NMS.look( myEntity, aTarget );

                    warningsGiven.put( player, System.currentTimeMillis() );
                }
            }
        }
        return theTarget;
    }

    public double getHealth() {
        LivingEntity myEntity = getMyEntity();
        if ( myEntity == null ) return 0;

        return myEntity.getHealth();
    }
    public void kill() {
        respawnTime = System.currentTimeMillis() + respawnDelay * 1000;
        myStatus = SentryStatus.DEAD;
    }
    
    /**
     * Calculates the damage to inflict on a sentry in the light of the current Armour settings.
     * It does not actually inflict the damage on the NPC.
     * @param finaldamage
     * @return the amount of the damage.
     */
    public double getFinalDamage( double finaldamage ) {
        return Sentries.useNewArmourCalc ? Math.abs( finaldamage * armour ) : Math.min( finaldamage - armour, 0 );
    }
   
    /**
     * recalculated the NPC's armour value from the currently equipped armour.
     * @return true if the NPC is wearing armour, false if not (or no armour values are configured).
     */
    public boolean updateArmour() {
        if ( sentry.armorValues.isEmpty() ) {
            Sentries.logger.log( Level.WARNING, "ERROR: no armour vales have been loaded from config." );
            return false;
        }
        if ( armour < 0 ) { // values less than 0 indicate a calculated value that needs refreshing
            armour = 0;
            LivingEntity myEntity = getMyEntity();
            ItemStack[] myArmour;  
            
            if ( myEntity instanceof Player )
                myArmour = ((Player) myEntity).getInventory().getArmorContents();
            else
                myArmour = myEntity.getEquipment().getArmorContents();
            
            boolean armourWorn = false;
            for ( ItemStack is : myArmour ) {
                Material item = is.getType();
                armourWorn = true;
                if ( sentry.armorValues.containsKey( item ) )
                    armour -= sentry.armorValues.get( item );
            }
            if ( !Sentries.useNewArmourCalc ) armour *= 10;
            
            return armourWorn;
        }
        return false;
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

//    public int getStrength() {
//        if ( sentry.strengthBuffs.isEmpty() ) return strength;
//
//        double mod = 0;
//        LivingEntity myEntity = getMyEntity();
//
//        if ( myEntity instanceof Player ) {
//
//            Material item = ((Player) myEntity).getInventory().getItemInMainHand().getType();
//
//            if ( sentry.strengthBuffs.containsKey( item ) ) {
//                mod += sentry.strengthBuffs.get( item );
//            }
//        }
//        return (int) (strength + mod);
//    }

    private static Set<AttackType> pyros = EnumSet.range( AttackType.PYRO1, AttackType.PYRO3 );
    private static Set<AttackType> stormCallers = EnumSet.range( AttackType.STORMCALLER1, AttackType.STORMCALLER3 );
    private static Set<AttackType> notFlammable = EnumSet.range( AttackType.PYRO1, AttackType.STORMCALLER3 );
    private static Set<AttackType> lightsFires = EnumSet.of( AttackType.PYRO1, AttackType.STROMCALLER2 );

    public boolean isPyromancer() { return pyros.contains( myAttack ); }
    public boolean isStormcaller() { return stormCallers.contains( myAttack ); }
    public boolean isWarlock1() { return myAttack == AttackType.WARLOCK1; }
    public boolean isWitchDoctor() { return myAttack == AttackType.WITCHDOCTOR; }
    public boolean isNotFlammable() { return notFlammable.contains( myAttack ); }
    public boolean lightsFires() { return lightsFires.contains( myAttack ); }

    /** 
     * Checks whether sufficient time has passed since the last healing, and if so restores
     * health according to the configured healrate.
     */
    void tryToHeal() {
        
        if ( healRate > 0 && System.currentTimeMillis() > oktoheal ) {

            double health = getHealth();
            if ( health < maxHealth ) {

                LivingEntity myEntity = getMyEntity();
                if ( myEntity == null ) return;
                myEntity.setHealth( health + 1 );
                
                // idk what this effect looks like, so lets see if it looks ok in-game.
                myEntity.getWorld().spawnParticle( Particle.HEART, myEntity.getLocation(), 5 );

                if ( getHealth() >= maxHealth )
                    _myDamamgers.clear();
            }
            oktoheal = (long) ( System.currentTimeMillis() + ( healRate * 1000 ) );
        }
    }
    
    boolean isMyChunkLoaded() {
        LivingEntity myEntity = getMyEntity();
        if ( myEntity == null ) return false;

        return Util.isLoaded( myEntity.getLocation() );
    }

    /**
     * Searches all online players for one with a name that matches the provided String, and
     * if successful saves it in the field 'guardeeEntity' and the name in
     * 'guardeeName'
     * 
     * @param name - The name that you wish to search for.
     * @return true if an Entity with the supplied name is found, otherwise
     *         returns false.
     */
    public boolean findPlayerGuardEntity( String name ) { 
        if ( npc == null || name == null ) return false;

        for ( Player player : Bukkit.getOnlinePlayers() ) {

            if ( name.equals( player.getName() ) ) {

                guardeeEntity = player;
                guardeeName = name;
                return true;
            }
        }
        return false;
    }
    /**
     * Searches all LivingEntities within range for one with a name that matches the provided String, and
     * if successful saves it in the field 'guardeeEntity' and the name in
     * 'guardeeName'
     * 
     * @param name - The name that you wish to search for. 
     * @return true if an Entity with the supplied name is found, otherwise
     *         returns false.
     */
    public boolean findOtherGuardEntity( String name ) {
        if ( npc == null || name == null ) return false;
        LivingEntity myEntity = getMyEntity();
        if ( myEntity == null ) return false;

        for ( Entity each : myEntity.getNearbyEntities( range, range, range ) ) {

            String ename = null;

            if ( each instanceof Player )
                ename = ((Player) each).getName();

            else if ( each instanceof LivingEntity )
                ename = ((LivingEntity) each).getCustomName();

            // if the entity for this loop isn't a player or living, move along...
            else continue;

            if ( ename != null && name.equals( ename ) ) {

                guardeeEntity = (LivingEntity) each;
                guardeeName = name;
                return true;
            }
        }
        return false;
    }

    public void setHealth( double health ) {
        LivingEntity myEntity = getMyEntity();
        if ( myEntity == null ) return;

        myEntity.setHealth( health > maxHealth ? maxHealth : health );
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

        myEntity.getEquipment();
        if ( myEntity instanceof HumanEntity ) {
            item = ((HumanEntity) myEntity).getInventory().getItemInMainHand();
            weapon = item.getType();

            myAttack = AttackType.find( weapon );

            if ( myAttack != AttackType.WITCHDOCTOR )
                item.setDurability( (short) 0 );           
        }
        else if ( myEntity instanceof Skeleton ) myAttack = AttackType.ARCHER;
        else if ( myEntity instanceof Ghast ) myAttack = AttackType.PYRO3;
        else if ( myEntity instanceof Snowman ) myAttack = AttackType.ICEMAGI;
        else if ( myEntity instanceof Wither ) myAttack = AttackType.WARLOCK2;
        else if ( myEntity instanceof Witch ) myAttack = AttackType.WITCHDOCTOR;
        else if ( myEntity instanceof Creeper ) myAttack = AttackType.CREEPER;
        else if ( myEntity instanceof Blaze || myEntity instanceof EnderDragon ) myAttack = AttackType.PYRO2;
        else myAttack = AttackType.BRAWLER;

        if ( myAttack == AttackType.WITCHDOCTOR ) {
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
        NavigatorParameters params = npc.getNavigator().getDefaultParameters();
        params.attackStrategy( myAttack );
        
        if ( myAttack == AttackType.BRAWLER || myAttack == AttackType.CREEPER )
            params.attackRange( 1.75 );
        else
            params.attackRange( 10 );
    }

    /**
     *  Cancels the current navigation (including targetted attacks) and 
     *  clears the held reference for the target. 
     */
    public void clearTarget() {

        getNavigator().cancelNavigation();
        attackTarget = null; 
    }

    public void checkIfEmpty ( CommandSender sender ) {
        if ( targets.isEmpty() && ignores.isEmpty() )
            sender.sendMessage( String.join( "", Col.YELLOW, npc.getName(), " now has no defined targets." ) );
    }
    
    boolean setAttackTarget( LivingEntity theEntity ) {
        LivingEntity myEntity = getMyEntity();
        if ( myEntity == null || theEntity == myEntity || theEntity == guardeeEntity ) return false;
        // don't attack when bodyguard target isn't around.
        if ( guardeeName != null && guardeeEntity == null ) return false;

        attackTarget = theEntity;
        myStatus = SentryStatus.ATTACKING;
        return true;
    }

    public Navigator getNavigator() {
        return ifMountedGetMount().getNavigator();
    }

    //--------------------------------methods dealing with Mounts----------
    /** Returns true if mountID >= 0 */
    public boolean hasMount() { return mountID >= 0; }
 
    /** Returns the NPC with the current mountID or null if the id = -1 (the default) */
    // TODO convert to use uuid's
    NPC getMountNPC() {
        return hasMount() ? Sentries.registry.getById( mountID ) : null;
    }

    NPC ifMountedGetMount() {
       
        NPC mount = getMountNPC();
        
        if ( mount != null && mount.isSpawned() && getMyEntity().isInsideVehicle() ) {
            return mount;
        }
        return npc;
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

                mountParams.attackStrategy( mountedAttack );
                mountParams.useNewPathfinder( true );
                mountParams.stuckAction( setStuckStatus );
                mountParams.speedModifier( myParams.speedModifier() * 2 );
                Utils.copyNavParams( myParams, mountParams );

                Entity ent = mount.getEntity();
                ent.setCustomNameVisible( false );
                ent.setPassenger( null );
                ent.setPassenger( myEntity );
            }
        }
    }

    
    /** 
     * Spawns and returns a mountNPC, creating a new NPC of type horse if the sentry does not already have a mount.
     * The method will do nothing and return null if the Sentry is not spawned.  */
    NPC spawnMount() {
        if ( Sentries.debug ) Sentries.debugLog( "Creating mount for " + npc.getName() );

        if ( npc.isSpawned() ) {

            NPC mount = null;

            if ( hasMount() ) {
                mount = Sentries.registry.getById( mountID );

                if ( mount != null )
                    mount.despawn();
                else
                    Sentries.logger.info( "Cannot find mount NPC " + mountID );
            }
            else {
                mount = Sentries.registry.createNPC( EntityType.HORSE, npc.getName() + "_Mount" );
                mount.getTrait( MobType.class ).setType( EntityType.HORSE );
            }

            if ( mount == null ) {
                Sentries.logger.info( "Cannot create mount NPC!" );
                mountID = -1;
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

    public void dismount() {

        LivingEntity myEntity = getMyEntity();
        
        if ( myEntity != null && myEntity.isInsideVehicle() ) {

            NPC mount = getMountNPC();

            if ( mount != null ) {
                Utils.copyNavParams( mount.getNavigator().getDefaultParameters(), npc.getNavigator().getDefaultParameters() );
                myEntity.getVehicle().setPassenger( null );
                mount.despawn( DespawnReason.PENDING_RESPAWN );
            }
        }
    }

    void reMountMount() {
           
        if (    npc.isSpawned() 
                && !getMyEntity().isInsideVehicle() 
                && hasMount()
                && isMyChunkLoaded() )
            mount();
    }
    
    //------------------------------------------end of methods for mounts
    
    public boolean hasLOS( Entity other ) {
        if ( ignoreLOS ) return true;
        
        LivingEntity myEntity = getMyEntity();
        
        if ( myEntity != null ) {
            return myEntity.hasLineOfSight( other );
        }
        return false;
    }

    /** Returns the entity of this NPC *only* if the NPC is spawned. Otherwise returns null. */
    public LivingEntity getMyEntity() {
        return (LivingEntity) npc.getEntity();
    }

    @EventHandler
    public void onCitReload( CitizensReloadEvent event ) {
        cancelRunnable();
    }    
    
    static class SentryStuckAction implements StuckAction {
        @Override
        public boolean run( NPC npc, Navigator navigator ) {

            SentryTrait inst = Utils.getSentryTrait( npc );
            
            if ( inst == null && navigator.getLocalParameters().attackStrategy() == mountedAttack ) 
                inst = Utils.getSentryTrait( npc.getEntity().getPassenger() );
            
            if ( inst != null )
                inst.myStatus = SentryStatus.STUCK;
            
            return false;
        }
    }
    
    static class MountAttackStrategy implements AttackStrategy {
        // make the rider attack when in range.
        @Override
        public boolean handle( LivingEntity attacker, LivingEntity bukkitTarget ) {

            if ( attacker == bukkitTarget ) return true;

            Entity passenger = attacker.getPassenger();

            if ( passenger != null ) {
                return Sentries.registry.getNPC( passenger )
                                        .getNavigator()
                                        .getLocalParameters()
                                        .attackStrategy()
                                        .handle( (LivingEntity) passenger, bukkitTarget );
            }
            return false;
        }
    }
}
