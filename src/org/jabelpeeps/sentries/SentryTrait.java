package org.jabelpeeps.sentries;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.targets.TargetType;

import lombok.Getter;
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
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;

public class SentryTrait extends Trait {

    final Sentries sentry;

    @Persist( S.PERSIST_SPAWN ) public Location spawnLocation;
    @Persist( S.PERSIST_MOUNT ) public int mountID = -1;
    @Persist( S.CON_NIGHT_VIS ) public int nightVision = Sentries.defIntegers.get( S.CON_NIGHT_VIS );
    @Persist( S.CON_RESPAWN_DELAY ) public int respawnDelay = Sentries.defIntegers.get( S.CON_RESPAWN_DELAY );
    @Persist( S.CON_RANGE ) public int range = Sentries.defIntegers.get( S.CON_RANGE );
    @Persist( S.CON_FOLLOW_DIST ) public int followDistance = Sentries.defIntegers.get( S.CON_FOLLOW_DIST );
    @Persist( S.CON_VOICE_RANGE ) public int voiceRange = Sentries.defIntegers.get( S.CON_VOICE_RANGE );
    
    @Persist( S.CON_SPEED ) public double speed = Sentries.defDoubles.get( S.CON_SPEED );
    @Persist( S.CON_STRENGTH ) public double strength = Sentries.defDoubles.get( S.CON_STRENGTH );
    @Persist( S.CON_ARROW_RATE ) public double attackRate = Sentries.defDoubles.get( S.CON_ARROW_RATE );
    @Persist( S.CON_HEAL_RATE ) public double healRate = Sentries.defDoubles.get( S.CON_HEAL_RATE );
    @Persist( S.CON_ARMOUR ) public double armour = Sentries.defDoubles.get( S.CON_ARMOUR );
    @Persist( S.CON_WEIGHT ) public double weight = Sentries.defDoubles.get( S.CON_WEIGHT );
    @Persist( S.CON_HEALTH ) public double maxHealth = Sentries.defDoubles.get( S.CON_HEALTH );
    
    @Persist( S.CON_USE_WEAPON_STRENGTH ) 
        public boolean strengthFromWeapon = Sentries.defBooleans.get( S.CON_USE_WEAPON_STRENGTH );
    @Persist( S.CON_KILLS_DROP ) public boolean killsDrop = Sentries.defBooleans.get( S.CON_KILLS_DROP );
    @Persist( S.CON_DROP_INV ) public boolean dropInventory = Sentries.defBooleans.get( S.CON_DROP_INV );
    @Persist( S.CON_MOBS_ATTACK ) public boolean targetable = Sentries.defBooleans.get( S.CON_MOBS_ATTACK );
    @Persist( S.CON_INVINCIBLE ) public boolean invincible = Sentries.defBooleans.get( S.CON_INVINCIBLE );
    @Persist( S.CON_RETALIATION ) public boolean iRetaliate = Sentries.defBooleans.get( S.CON_RETALIATION );
    @Persist( S.CON_CRIT_HITS ) public boolean acceptsCriticals = Sentries.defBooleans.get( S.CON_CRIT_HITS );
    @Persist( S.CON_IGNORE_LOS ) public boolean ignoreLOS = Sentries.defBooleans.get( S.CON_IGNORE_LOS );
    
    @Persist public UUID guardeeID;
    @Persist public String guardeeName;
    @Persist ItemStack potionItem;

    @Persist( S.CON_GREETING ) public String greetingMsg = Sentries.defaultGreeting;
    @Persist( S.CON_WARNING ) public String warningMsg = Sentries.defaultWarning;

    private Map<Player, Long> warningsGiven = new HashMap<>();
    Set<Player> myDamagers = new HashSet<>();
    List<PotionEffect> weaponSpecialEffects;

    public LivingEntity guardeeEntity, attackTarget;
    DamageCause causeOfDeath;
    Entity killer;

    public Set<TargetType> targets = new TreeSet<>();
    public Set<TargetType> ignores = new TreeSet<>();
    public Set<TargetType> events = new TreeSet<>();

    long respawnTime = System.currentTimeMillis();
    long oktoheal = System.currentTimeMillis();
    long reassesTime = System.currentTimeMillis();
    long okToTakedamage = 0;
    int epCount;

    public SentryStatus myStatus = SentryStatus.NOT_SPAWNED;
    SentryStatus oldStatus;
    @Getter private AttackType myAttack;
    private Integer tickMe;

    final static AttackStrategy mountedAttack = ( attacker, bukkitTarget ) -> {

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
    };
    
    final static StuckAction setStuckStatus = ( npc, navigator ) -> {

        if ( !npc.isSpawned() ) return false;
        SentryTrait inst = Utils.getSentryTrait( npc );
        
        if ( inst == null && navigator.getLocalParameters().attackStrategy() == mountedAttack )
            inst = Utils.getSentryTrait( npc.getEntity().getPassenger() );
        
        if ( inst != null )
            inst.myStatus = SentryStatus.STUCK;
        
        return false;
    };
    
    public SentryTrait() {
        super( "sentries" );
        sentry = (Sentries) Bukkit.getPluginManager().getPlugin( "Sentries" );
    }
    
    @SuppressWarnings( "unchecked" )
    @Override
    public void load( DataKey key ) throws NPCLoadException {
        if ( Sentries.debug ) Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] load() start" );
  
        if ( key.keyExists( "traits" ) ) key = key.getRelative( "traits" );

        String guardee = key.getString( S.PERSIST_GUARDEE, null );
        if ( guardee != null ) guardeeName = guardee;

        Set<String> validTargets = new HashSet<>();
        
        if ( key.getRaw( S.TARGETS ) != null )
            validTargets.addAll( (Set<String>) key.getRaw( S.TARGETS ) );
        else
            validTargets.addAll( Sentries.defaultTargets );
        
        validTargets.parallelStream().filter( s -> !CommandHandler.callCommand( this, Utils.colon.split( s ) ) )
        // the second callCommand() is only used if the first is unsuccessful.
                                     .forEach( t -> CommandHandler.callCommand( this, S.TARGET, "add", t ) );
        
        Set<String> ignoreTargets = new HashSet<>();
        
        if ( key.getRaw( S.IGNORES ) != null )
            ignoreTargets.addAll( (Set<String>) key.getRaw( S.IGNORES ) );
        else
            ignoreTargets.addAll( Sentries.defaultIgnores );
        
        ignoreTargets.parallelStream().filter( s -> !CommandHandler.callCommand( this, Utils.colon.split( s ) ) )
                                      .forEach( i -> CommandHandler.callCommand( this, S.IGNORE, "add", i ) ); 

        Set<String> eventTargets = new HashSet<>();
        
        if ( key.getRaw( S.EVENTS ) != null )
            eventTargets.addAll( (Set<String>) key.getRaw( S.EVENTS ) );
        
        eventTargets.parallelStream().forEach( e -> CommandHandler.callCommand( this, S.EVENT, "add", e ) );     
    }

    @Override
    public void onSpawn() {
        if ( Sentries.debug ) Sentries.debugLog( npc.getName() + ":[" + npc.getId() + "] onSpawn()" );
     
        LivingEntity myEntity = getMyEntity();

        // check for illegal values
        if ( weight <= 0 ) weight = 1.0;
        if ( attackRate > 30 ) attackRate = 30.0;
        if ( maxHealth < 1 ) maxHealth = 1;
        if ( range < 1 ) range = 1;
        if ( range > 100 ) range = 100;
        if ( respawnDelay < -1 ) respawnDelay = -1;
        if ( spawnLocation == null ) onCopy();

        // Allow Denizen to handle the sentry's health if it is active.
        if (    DenizenHook.sentryHealthByDenizen 
                && npc.hasTrait( HealthTrait.class ) )
            npc.removeTrait( HealthTrait.class );

        // disable citizens respawning, because Sentries doesn't always raise EntityDeath
        npc.data().set( NPC.RESPAWN_DELAY_METADATA, -1 );

        myEntity.getAttribute( Attribute.GENERIC_MAX_HEALTH ).setBaseValue( maxHealth );
        setHealth( maxHealth );
        
        myDamagers.clear();
        NMS.look( myEntity, myEntity.getLocation().getYaw(), 0 );

        npc.setProtected( false );
        npc.data().set( NPC.TARGETABLE_METADATA, targetable );
        
        NavigatorParameters navigatorParams = npc.getNavigator().getDefaultParameters();

        navigatorParams.useNewPathfinder( true );
        navigatorParams.stuckAction( setStuckStatus );
        navigatorParams.speedModifier( (float) speed );
        navigatorParams.attackDelayTicks( (int) (attackRate * 20) );

        updateArmour();
        updateAttackType();
        checkForGuardee();
       
        if ( tickMe == null ) {
            tickMe = Bukkit.getScheduler().scheduleSyncRepeatingTask( sentry, 
                    () -> {     
                            myStatus = myStatus.update( SentryTrait.this );             
                            if ( Sentries.debug && oldStatus != myStatus ) {
                                Sentries.debugLog( npc.getName() + " is now:- " + myStatus.name() );
                                oldStatus = myStatus;
                            }                   
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
            DenizenHook.sentryDeath( myDamagers, npc );
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

        Set<String> ignoreTargets = new HashSet<>();
        Set<String> validTargets = new HashSet<>();
        Set<String> eventTargets = new HashSet<>();
        
        targets.forEach( s -> validTargets.add( s.getTargetString() ) );
        ignores.forEach( s -> ignoreTargets.add( s.getTargetString() ) ); 
        events.forEach( e -> eventTargets.add( e.getTargetString() ) );
        
        key.setRaw( S.TARGETS, validTargets );
        key.setRaw( S.IGNORES, ignoreTargets );
        key.setRaw( S.EVENTS, eventTargets );
    }

    @Override
    public void onCopy() {
        Bukkit.getScheduler().runTaskLater( sentry, () -> spawnLocation = npc.getStoredLocation(), 10 );
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

        LivingEntity theTarget = null;
        Double distanceToBeat = 99999.0;

        for ( Entity aTarget : myEntity.getNearbyEntities( combinedRange, combinedRange / 2, combinedRange ) ) {

            if  (   !(aTarget instanceof LivingEntity) 
                    || !hasLOS( aTarget ) ) 
                continue;

            if (    !isIgnoring( (LivingEntity) aTarget )
                    && isTarget( (LivingEntity) aTarget ) ) {

                double lightLevel = aTarget.getLocation().getBlock().getLightLevel();

                if (    aTarget instanceof Player
                        && ((Player) aTarget).isSneaking() )
                    lightLevel /= 2;

                if ( lightLevel >= ( 16 - nightVision ) ) {
                    
                    double dist = aTarget.getLocation().distance( myLoc );

                    if (    dist > range 
                            && !warningMsg.isEmpty()
                            && checkSpeech( aTarget ) ) {

                        Player player = (Player) aTarget;
                        player.sendMessage( Utils.format( warningMsg, npc, player, null, null ) );
                        warningsGiven.put( player, System.currentTimeMillis() );
                        if ( !getNavigator().isNavigating() ) Util.faceEntity( myEntity, aTarget );
                    }
                    else if ( dist < distanceToBeat ) {
                        distanceToBeat = dist;
                        theTarget = (LivingEntity) aTarget;
                    }
                }
            }
            else if (  !greetingMsg.isEmpty()
                    && checkSpeech( aTarget ) ) {

                Player player = (Player) aTarget;
                player.sendMessage( Utils.format( greetingMsg, npc, player, null, null ) );
                warningsGiven.put( player, System.currentTimeMillis() );
                Util.faceEntity( myEntity, player );
            }
        }
        return theTarget;
    }

    private boolean checkSpeech( Entity aTarget ) {
        return voiceRange > 0 
                && aTarget instanceof Player
                && !aTarget.hasMetadata( "NPC" ) 
                && (    !warningsGiven.containsKey( aTarget ) 
                        || System.currentTimeMillis() > warningsGiven.get( aTarget ) + 60000 );
    }
    
    public double getHealth() {
        LivingEntity myEntity = getMyEntity();
        if ( myEntity == null ) return 0;

        return myEntity.getHealth();
    }
    
    /** Sets the Sentry's status to DEAD, and sets the respawnTime field according to the value of respawnDelay */
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
        return Sentries.useNewArmourCalc ? finaldamage - Math.abs( finaldamage * armour ) 
                                         : Math.min( finaldamage - Math.abs( armour ), 0 );
    }
   
    /**
     * recalculated the NPC's armour value from the currently equipped armour.
     * @return true if the NPC is wearing armour, false if not (or no armour values are configured).
     */
    public boolean updateArmour() {
        if ( Sentries.armorValues.isEmpty() ) {
            Sentries.logger.log( Level.WARNING, "ERROR: no armour values have been loaded from config." );
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
                if ( Sentries.armorValues.containsKey( item ) )
                    armour -= Sentries.armorValues.get( item );
            }
            if ( !Sentries.useNewArmourCalc ) armour *= 10;
            
            return armourWorn;
        }
        return false;
    }
    
    public float getSpeed() {

        LivingEntity myEntity = getMyEntity();
        
        if ( myEntity == null ) return (float) speed;

        double mod = 0;
        if ( !Sentries.speedBuffs.isEmpty() ) {

            if ( myEntity instanceof Player ) {
                for ( ItemStack stack : ((Player) myEntity).getInventory().getArmorContents() ) {
                    Material item = stack.getType();

                    if ( Sentries.speedBuffs.containsKey( item ) )
                        mod += Sentries.speedBuffs.get( item );
                }
            }
        }
        return (float) ( speed + mod ) * ( myEntity.isInsideVehicle() ? 2 : 1 );
    }

    public boolean updateStrength() {
        if ( Sentries.weaponStrengths.isEmpty() ) {
            Sentries.logger.log( Level.WARNING, "ERROR: no weapon strengths have been loaded from config." );
            return false;
        }
        if ( strengthFromWeapon ) { 
            if ( myAttack != AttackType.BRAWLER ) {
                strength = myAttack.getDamage();
                return true;
            }
            LivingEntity myEntity = getMyEntity();
            Material item = null;
            
            if ( myEntity instanceof Player )
                item = ((Player) myEntity).getInventory().getItemInMainHand().getType();  
            else 
                item = myEntity.getEquipment().getItemInMainHand().getType();
            
            if ( item != null && Sentries.weaponStrengths.containsKey( item ) ) {
                strength = Sentries.weaponStrengths.get( item );
                return true;
            }
            strength = 1;
        }
        return false;
    }

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
    boolean isMyChunkLoaded() { return Util.isLoaded( npc.getStoredLocation() ); }
    
    /** 
     * Checks whether sufficient time has passed since the last healing, and if so restores
     * health according to the configured healrate.
     */
    void tryToHeal() {
        
        if ( healRate > 0 && System.currentTimeMillis() > oktoheal ) {

            LivingEntity myEntity = getMyEntity();
            if ( myEntity == null ) return;
            
            double health = getHealth();
            if ( health < maxHealth - 1 ) {
                myEntity.setHealth( health + 1 );    
            }
            else if ( health < maxHealth ) {
                myEntity.setHealth( maxHealth );
                myDamagers.clear();
            }
            else return;
            
            myEntity.getWorld().spawnParticle( Particle.HEART, myEntity.getEyeLocation().subtract( 0, 0.5, 0 ), 5 );           
            oktoheal = (long) ( System.currentTimeMillis() + ( healRate * 1000 ) );
        }
    }

    /** Checks if the configured guardee is online (in the case of players), or spawned (in the case of NPC's). */
    public void checkForGuardee() {
        if ( guardeeID != null ) {
        
            Player player = Bukkit.getPlayer( guardeeID );
            if ( player != null && player.isOnline() )
                guardeeEntity = player;
            
            NPC guardeeNPC = Sentries.registry.getByUniqueId( guardeeID );
            if ( guardeeNPC != null && guardeeNPC.isSpawned() ) 
                guardeeEntity = (LivingEntity) guardeeNPC.getEntity();
            
            if ( guardeeEntity != null )
                guardeeName = guardeeEntity.getName();
        }
        else if ( guardeeName != null && !guardeeName.isEmpty() ) {
            
            Player player = Bukkit.getPlayer( guardeeName );
            if ( player != null && player.isOnline() )
                guardeeEntity = player;
            
            for ( NPC each : Sentries.registry ) {
                if ( each.getName().equalsIgnoreCase( guardeeName ) && each.isSpawned() ) {
                    guardeeEntity = (LivingEntity) each.getEntity();
                    break;
                }
            }
            if ( guardeeEntity != null )
                guardeeID = guardeeEntity.getUniqueId();
        }
    }
    /** Checks whether the supplied player is the player that this sentry is configured to guard,
     *  and stores its entity in guardeeEntity if so. */
    public void checkForGuardee( Player joined ) {
        if ( guardeeID != null ) {
            if ( guardeeID.equals( joined.getUniqueId() ) ) {
                guardeeEntity = joined;
                guardeeName = joined.getName();
            }
        }
        else if ( guardeeName != null && !guardeeName.isEmpty() ) {
            if ( guardeeName.equalsIgnoreCase( joined.getName() ) ) {
                guardeeEntity = joined;
                guardeeID = joined.getUniqueId();
            }
        }
    }
    /** Checks if the supplied NPC is the configured guardee for this sentry, and stores its 
     *  entity in guardeeEntity if so. */
    public void checkForGuardee( NPC spawned ) {
        if ( guardeeID != null ) {
            if ( guardeeID.equals( spawned.getUniqueId() ) ) {
                guardeeEntity = (LivingEntity) spawned.getEntity();
                guardeeName = spawned.getName();
            }
        }
        else if ( guardeeName != null && !guardeeName.isEmpty() ) {
            if ( guardeeName.equalsIgnoreCase( spawned.getName() ) ) {
                guardeeEntity = (LivingEntity) spawned.getEntity();
                guardeeID = spawned.getUniqueId();
            }
        }
    }
    /**
     * Searches all online players for one with a name that matches the provided String, and
     * if successful saves it in the field 'guardeeEntity', their name in 'guardeeName', and
     * their UUID in 'guardeeID'.
     * 
     * @param name - The name that you wish to search for.
     * @return true if an Entity with the supplied name is found, otherwise returns false.
     */
    public boolean findPlayerGuardEntity( String name ) { 
        if ( npc == null || name == null ) return false;

        for ( Player player : Bukkit.getOnlinePlayers() ) {

            if ( name.equalsIgnoreCase( player.getName() ) ) {

                guardeeID = player.getUniqueId();
                guardeeEntity = player;
                guardeeName = name;
                return true;
            }
        }
        return false;
    }
    /**
     * Searches all LivingEntities within range for one with a name that matches the provided String, and
     * if successful saves it in the field 'guardeeEntity' and the name in, their name in 'guardeeName', and
     * their UUID in 'guardeeID'.
     * 
     * @param name - The name that you wish to search for. 
     * @return true if an Entity with the supplied name is found, otherwise returns false.
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

                guardeeID = each.getUniqueId();
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
            weaponSpecialEffects = Sentries.weaponEffects.get( weapon );
        }
        NavigatorParameters params = npc.getNavigator().getDefaultParameters();
        params.attackStrategy( myAttack );
        
        if ( myAttack == AttackType.BRAWLER || myAttack == AttackType.CREEPER )
            params.attackRange( 1.75 );
        else
            params.attackRange( 100 );
        
        updateStrength();
    }

    /**
     *  Cancels the current navigation (including targetted attacks) and 
     *  clears the held reference for the target. <p>
     *  Note: this method does not change the sentry's status.
     */
    public void cancelAttack() {
        getNavigator().cancelNavigation();
        attackTarget = null; 
    }

    public void checkIfEmpty ( CommandSender sender ) {
        if ( targets.isEmpty() && events.isEmpty() )
            Utils.sendMessage( sender, Col.YELLOW, npc.getName(), " now has no defined targets." );
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

            if ( mount == null ) {
                mount = Sentries.registry.createNPC( EntityType.HORSE, npc.getName() + "_Mount" );
                mount.getTrait( MobType.class ).setType( EntityType.HORSE );
                mount.getTrait( Owner.class ).setOwner( npc.getTrait( Owner.class ).getOwner() );
                ((Horse) mount.getEntity()).getInventory().setSaddle( new ItemStack( Material.SADDLE ) );
                mountID = mount.getId();
                mount.setProtected( false );
            }
            else if ( !mount.isSpawned() ) {
                mount.despawn( DespawnReason.PENDING_RESPAWN );
            }
            mount.spawn( getMyEntity().getLocation() );

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
}
