package org.jabelpeeps.sentries;

import static org.bukkit.entity.EntityType.ARROW;
import static org.bukkit.entity.EntityType.EGG;
import static org.bukkit.entity.EntityType.ENDER_PEARL;
import static org.bukkit.entity.EntityType.FIREBALL;
import static org.bukkit.entity.EntityType.LINGERING_POTION;
import static org.bukkit.entity.EntityType.PRIMED_TNT;
import static org.bukkit.entity.EntityType.SMALL_FIREBALL;
import static org.bukkit.entity.EntityType.SNOWBALL;
import static org.bukkit.entity.EntityType.SPLASH_POTION;
import static org.bukkit.entity.EntityType.WITHER_SKULL;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.WitherSkull;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.citizensnpcs.api.ai.AttackStrategy;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;

@AllArgsConstructor
public enum AttackType implements AttackStrategy {
    // Columns:-  weapon held                 projectile        v  g     Effect?              damage  range
    ARCHER(       Material.BOW,               ARROW,            2, 0.05, Effect.BOW_FIRE,          6 ), 
    GRENADIER(    Material.TNT,               PRIMED_TNT,       1, 0.04, Effect.MOBSPAWNER_FLAMES, 4 ),
    BOMBARDIER(   Material.EGG,               EGG,              1, 0.03 ), 
    ICEMAGI(      Material.SNOW_BALL,         SNOWBALL,         1, 0.03 ), 
    PYRO1(        Material.REDSTONE_TORCH_ON, SMALL_FIREBALL,   2,       Effect.BLAZE_SHOOT,       5 ), 
    PYRO2(        Material.TORCH,             SMALL_FIREBALL,   2,       Effect.BLAZE_SHOOT,       5 ),
    PYRO3(        Material.BLAZE_ROD,         FIREBALL,         2,       Effect.BLAZE_SHOOT,       6 ), 
    STORMCALLER1( Material.PAPER,                                                                  5, 20 ),
    STROMCALLER2( Material.BOOK,                                                                  10, 20 ),
    STORMCALLER3( Material.BOOK_AND_QUILL,                                                         1, 20 ), 
    WARLOCK1(     Material.ENDER_PEARL,       ENDER_PEARL,      1, 0.03, Effect.ENDER_SIGNAL,     10 ), 
    WARLOCK2(     Material.SKULL_ITEM,        WITHER_SKULL,     2,       Effect.WITHER_SHOOT,      8 ),
    WITCHDOCTOR1( Material.SPLASH_POTION,     SPLASH_POTION,    1, 0.03 ),
    WITCHDOCTOR2( Material.LINGERING_POTION,  LINGERING_POTION, 1, 0.03 ),
    CREEPER(      Material.SULPHUR,                                                                4, 1.3 ),  
    BRAWLER(      Material.AIR,                                                                    1, 1.3 ); 
        
    @Getter private Material weapon;
    private final EntityType projectile;
    private final double v;
    private final double g;
    private final Effect effect;
    @Getter private int damage;
    @Getter private final double approxRange;
    
    static Map<Material, AttackType> reverseSearch = new EnumMap<>( Material.class );
    static { updateMap(); }

    /** Constructor for all values - including a calculated approximate range */
    AttackType( Material w, EntityType p, double V, double G, Effect e, int d ) { this( w, p, V, G, e, d, V * V / G ); }
    /** Constructor used by attacks which by default do no damage */
    AttackType( Material w, EntityType p, double V, double G ) { this( w, p, V, G, null, 0, V * V / G ); }
    /** Constructor used by fireballs - which aren't affected by gravity, and so can't have a calculated range */
    AttackType( Material w, EntityType p, double V, Effect e, int d ) { this( w, p, V, 0, e, d, 32 ); }
    /** Constructor for attacks which need a range to be specified */
    AttackType( Material w , int d, double r ) { this( w, null, 0, 0, null, d, r ); }

    /**
     * Quickly returns the appropriate AttackType by searching an EnumMap that
     * has the currently configured weapon Material values as its keys.
     * 
     * Searching for a key that is not present in the Map will return
     * 'AttackType.brawler'.
     * 
     * @param item
     *            - the weapon currently being used by the sentry
     * @return a reference to the appropriate AttackType instance.
     */
    static AttackType find( Material item ) {
        return reverseSearch.containsKey( item ) ? reverseSearch.get( item ) : AttackType.BRAWLER;
    }

    /**
     * Only updates the values held in each enum instance if 'UseCustomWeapons'
     * is set to true in the config.yml
     * 
     * @param config
     *            - the FileConfiguration instance to scan for the needed
     *            values.
     */
    static void loadWeapons( FileConfiguration config ) {
        if ( config.getBoolean( "UseCustomWeapons" ) ) {

            for ( AttackType attack : values() ) {                
                if ( attack == BRAWLER ) continue;
                try {
                    attack.weapon = Material.getMaterial( config.getString( "AttackTypes." + attack.name() ) );
                } catch ( NullPointerException e ) {
                    if ( Sentries.debug ) Sentries.debugLog( e.getMessage() );
                }
            }
            updateMap();
        }
    }

    /**
     * Seeds the Map used for find() Called either from the static initialiser,
     * or loadWeapons()
     */
    private static void updateMap() {
        reverseSearch.clear();
        for ( AttackType each : values() ) {              
            if ( each == BRAWLER ) continue;
            reverseSearch.put( each.weapon, each );
        }
    }

    @Override
    public boolean handle( LivingEntity myEntity, LivingEntity victim ) {
        final SentryTrait inst = Utils.getSentryTrait( myEntity );
        if ( inst == null ) return false;
 
        Location myLoc = myEntity.getEyeLocation();
        World world = myEntity.getWorld();
        Location targetLoc = this == GRENADIER ? victim.getLocation() : victim.getEyeLocation();   
        NMS.look( myEntity, victim );
        
        switch ( this ) {
            case BRAWLER:  return false;

            case CREEPER: 
                final BukkitScheduler sch = Bukkit.getScheduler();
                final int task = sch.scheduleSyncRepeatingTask( Sentries.plugin, 
                        
                    new Runnable() { 
                         int runcount = 0;
                         boolean outOfRange = false;
                         @Override public void run() {
                             if  (   outOfRange 
                                     || myLoc.distanceSquared( targetLoc ) > 50 
                                     || !inst.getNPC().isSpawned() ) {
                                 outOfRange = true;
                             }
                             else if ( ++runcount <= 3 ) {
                                 world.playSound( myLoc, Sound.ENTITY_CREEPER_PRIMED, 5, 1 );
                                 myEntity.playEffect( EntityEffect.HURT );
                             } 
                             else {
                                 world.createExplosion( myLoc.getX(), myLoc.getY(), myLoc.getZ(),
                                                          (float) inst.strength, false, false );
                                 inst.getNPC().despawn();
                                 inst.kill();
                             }
                         }
                     }, 0, 10 );
                
                sch.scheduleSyncDelayedTask( Sentries.plugin, () -> sch.cancelTask( task ), 35 );
                return true;
                
            case STORMCALLER1: 
            case STROMCALLER2: 
                world.strikeLightningEffect( targetLoc );
                victim.damage( inst.strength, myEntity );               
                break;
                
            case STORMCALLER3: 
                world.strikeLightningEffect( targetLoc );
                victim.setHealth( 0 );
                break;

            case GRENADIER: // TNT, ballistic, non-projectile EntityType

                if ( cantHit( inst, myLoc, targetLoc ) ) return true;
                
                Vector victor = getFiringVector( myLoc.toVector(), targetLoc.toVector() );
                if ( victor == null ) return true;
                if ( Sentries.debug ) 
                    Sentries.debugLog( "TNT Vector for " + myEntity.getName() + " is " + victor.toString() );
                
                TNTPrimed tnt = (TNTPrimed) world.spawn( myLoc, projectile.getEntityClass() );
                tnt.setGravity( true );
                tnt.setVelocity( victor );
                tnt.setFuseTicks( 40 );
                tnt.setIsIncendiary( false );
                tnt.setYield( (float) inst.strength );
                ThrownEntities.addTNT( tnt, myEntity );
                break;
                
            case ARCHER:       // arrows, ballistics   
            case BOMBARDIER:   // eggs, ballistic
            case ICEMAGI:      // snowballs, ballistic
            case WARLOCK1:     // enderpearl, ballistic
            case WITCHDOCTOR1: // splash potions, ballistic
            case WITCHDOCTOR2: // lingering potions, ballistic
                
                if ( cantHit( inst, myLoc, targetLoc ) ) return true;
                
                Vector vector = getFiringVector( myLoc.toVector(), targetLoc.toVector() );
                if ( vector == null ) return true;
                if ( Sentries.debug ) 
                    Sentries.debugLog( "Firing Vector for " + myEntity.getName() + " is " + vector.toString() );
                
                Projectile proj = (Projectile) world.spawn( myLoc, projectile.getEntityClass() );
              
                if  (   inst.isWitchDoctor()  
                        && inst.potionItem != null ) {
                    ((ThrownPotion) proj).setItem( inst.potionItem.clone() );
                } 
                if ( proj instanceof Egg ) ThrownEntities.addEgg( (Egg) proj, myEntity );
                
                proj.setShooter( myEntity );
                proj.setVelocity( vector );
                
                if ( this == AttackType.WARLOCK1 ) inst.epCount++;
                break;

            case PYRO1:    // smallfireball, non-incendiary
            case PYRO2:    // smallfireball, incendiary
            case PYRO3:    // fireball   
            case WARLOCK2: // witherskull (also a sub-class of fireball)
                
                Fireball fireball = (Fireball) world.spawn( myLoc, projectile.getEntityClass() );
                fireball.setIsIncendiary( this == PYRO2 );
                fireball.setShooter( myEntity );
                fireball.setDirection( targetLoc.toVector()
                                                .subtract( myLoc.toVector() )
                                                .normalize().multiply( v ) ); 
                
                if ( fireball instanceof WitherSkull ) ThrownEntities.addFireball( fireball, myEntity );
                
                if ( Sentries.debug ) 
                    Sentries.debugLog( "Fireball launched on vector:- " + fireball.getDirection() + 
                                        " with velocity of " + fireball.getVelocity() );
                break;       
        } 
        if ( effect != null )
            world.playEffect( myLoc, effect, null );
        
        if ( myEntity instanceof Player ) {
            final Player player = (Player) myEntity;
            
            if ( this != ARCHER ) 
                PlayerAnimation.ARM_SWING.play( player ); 
            else {
                PlayerAnimation.START_USE_MAINHAND_ITEM.play( player );
                
                Bukkit.getScheduler()
                      .runTaskLater( Sentries.plugin, () -> PlayerAnimation.STOP_USE_ITEM.play( player ), 10 );
            }
        }
        return true;
    }
    
    private boolean cantHit( SentryTrait inst, Location myLoc, Location targetLoc ) {
        
        if ( Sentries.debug ) 
            Sentries.debugLog( this.toString() + ": Max range for " + inst.getNPC().getName() + 
                                    " is " + Utils.formatDbl( approxRange ) );
        if ( Utils.sqr( Math.min( approxRange, inst.range ) ) < myLoc.distanceSquared( targetLoc ) ) {
            // can't hit target
            inst.cancelAttack();
            return true;
        } 
        return false;
    }

    /** 
      * Solve firing angles for a ballistic projectile with speed and gravity to hit a fixed position.
     *
     * @param myLoc - point projectile will fire from
     * @param v - scalar speed of projectile
     * @param targetLoc - point projectile is trying to hit
     * @param g - force of gravity, positive down
    
     *
     * @return the low-angle Vector to hit the target.
     */
     private Vector getFiringVector( Vector myLoc, Vector targetLoc ) {
    
         Vector diff = targetLoc.subtract( myLoc );
         double y = diff.getY() * -1;
         double groundDistSqrd = diff.setY( 0 ).lengthSquared(); 
         double v2 = Utils.sqr( v );
         double root = Utils.sqr( v2 ) - g * ( g * groundDistSqrd + 2 * y * v2 );
    
         // No solution
         if ( root < 0 ) return null;
    
         double lowAng = Math.atan2( v2 - Math.sqrt( root ), g * Math.sqrt( groundDistSqrd ) );
    
         return diff.normalize().multiply( Math.cos( lowAng ) )
                                .multiply( v )
                                .setY( Math.sin( lowAng ) * v * -1 );
     }
}
