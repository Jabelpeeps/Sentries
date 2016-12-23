package org.jabelpeeps.sentries;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.WitherSkull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.citizensnpcs.api.ai.AttackStrategy;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;

@AllArgsConstructor
public enum AttackType implements AttackStrategy {
    // Columns:-  weapon held                 projectile           v     g    Effect?           incendiary? 
    ARCHER(       Material.BOW,               Arrow.class,         34,   20,  Effect.BOW_FIRE ), 
    BOMBARDIER(   Material.EGG,               Egg.class,           17.5, 13.5 ), 
    ICEMAGI(      Material.SNOW_BALL,         Snowball.class,      17.5, 13.5 ), 
    PYRO1(        Material.REDSTONE_TORCH_ON, SmallFireball.class, 34,   20,  Effect.BLAZE_SHOOT ), 
    PYRO2(        Material.TORCH,             SmallFireball.class, 34,   20,  Effect.BLAZE_SHOOT, true ),
    PYRO3(        Material.BLAZE_ROD,         Fireball.class,      34,   20,  Effect.BLAZE_SHOOT ), 
    STORMCALLER1( Material.PAPER,             ThrownPotion.class,  21,   20 ), 
    STROMCALLER2( Material.BOOK,              ThrownPotion.class,  21,   20 ), 
    STORMCALLER3( Material.BOOK_AND_QUILL,    ThrownPotion.class,  21,   20 ), 
    WARLOCK1(     Material.ENDER_PEARL,       EnderPearl.class,    17.5, 13.5 ), 
    WARLOCK2(     Material.SKULL_ITEM,        WitherSkull.class,   34,   20,  Effect.WITHER_SHOOT ),
    // warlock3( "Warlock3" ), // No default weapon in config.yml so disabled  for now.
    WITCHDOCTOR(  Material.SPLASH_POTION,     ThrownPotion.class,  21,   20 ),
    CREEPER(      Material.SULPHUR ),  
    BRAWLER(      Material.AIR ); //{
        
//  This doesn't seem to be being used anymore....
//        @Override
//        public Material getWeapon( SentryTrait sentry ) {
//            LivingEntity myEntity = sentry.getMyEntity();
//            if ( myEntity == null ) return Material.AIR;
//            
//            if ( myEntity instanceof HumanEntity )
//                return ((HumanEntity) myEntity).getInventory().getItemInMainHand().getType();
//            
//            return myEntity.getEquipment().getItemInMainHand().getType();
//        }
 //   };
    
    @Getter private Material weapon;
    final Class<? extends Projectile> projectile;
    final double v;
    final double g;
    final Effect effect;
    final boolean incendiary;
    
    static Map<Material, AttackType> reverseSearch = new EnumMap<>( Material.class );
    static { updateMap(); }

    AttackType( Material w, Class<? extends Projectile> p, double V, double G ) { this( w, p, V, G, null, false ); }
    AttackType( Material w, Class<? extends Projectile> p, double V, double G, Effect e ) { this( w, p, V, G, e, false ); }
    AttackType( Material w ) { this( w, null, 0, 0, null, false ); }

    // the argument for this method is only used in the override for the  'BRAWLER' instance.
//    public Material getWeapon( SentryTrait sentry ) { return weapon; }

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
                attack.weapon = Material.getMaterial( config.getString( "AttackTypes." + attack.name() ) );
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
        for ( AttackType each : values() )
            reverseSearch.put( each.weapon, each );
    }

    @Override
    public boolean handle( LivingEntity myEntity, LivingEntity victim ) {
        SentryTrait inst = Util.getSentryTrait( myEntity );
        if ( inst == null ) return false;

        if ( System.currentTimeMillis() < inst.oktoFire ) return false;

        inst.oktoFire = (long) (System.currentTimeMillis() + inst.arrowRate * 1000.0);
 
        Location myLoc = myEntity.getLocation();
        World world = myEntity.getWorld();
        Location targetLoc = victim.getLocation();
        
        switch ( this ) {
            case BRAWLER: // returning false will use the default melee attack 
                return false;

            case CREEPER: 
                world.createExplosion( myLoc, 4F );
                inst.setHealth( 0 );
                return true;
                
            case STORMCALLER1: 
                world.strikeLightningEffect( targetLoc.add( 0, .33, 0 ) );
                victim.damage( inst.strength, myEntity );               
                break;
                
            case STROMCALLER2: 
                world.strikeLightning( targetLoc.add( 0, .33, 0 ) );                
                break;
                
            case STORMCALLER3: 
                world.strikeLightningEffect( targetLoc.add( 0, .33, 0 ) );
                victim.setHealth( 0 );
                break;
                
            case ARCHER: // arrows, ballistics   
            case BOMBARDIER: // eggs, ballistic
            case ICEMAGI: // snowballs, ballistic
            case WARLOCK1: // enderpearl, ballistics
            case WITCHDOCTOR: // potions, ballistic
                
                double range = Util.getRange( v, g, myLoc.getY() );
                if ( Math.min( range * range, inst.range * inst.range ) < myLoc.distanceSquared( targetLoc ) ) {
                    // can't hit target
                    inst.clearTarget();
                    inst.myStatus = SentryStatus.is_A_Guard( inst );
                    return true;
                }
                
                Projectile proj = world.spawn( myLoc, projectile );
                if  (   this == WITCHDOCTOR 
                        && inst.potionItem != null ) {
                    ((ThrownPotion) proj).setItem( inst.potionItem.clone() );
                }
                proj.setShooter( myEntity );
                proj.setVelocity( Util.getFiringVector( myLoc.toVector(), v, targetLoc.toVector(), g ) );
                break;

            case PYRO1: // smallfireball, non-incendiary
            case PYRO2: // smallfireball, incendiary
            case PYRO3: // fireball   
            case WARLOCK2: // witherskull (also a sub-class of fireball)
                Fireball fireball = (Fireball) world.spawn( myLoc, projectile );
                fireball.setIsIncendiary( incendiary );
                fireball.setShooter( myEntity );
                fireball.setDirection( targetLoc.toVector().subtract( myLoc.toVector() ) );       
                break;       
        }        
        NMS.look( myEntity, victim );
        
        if ( effect != null )
            world.playEffect( myLoc, effect, null );
        
        if ( myEntity instanceof Player ) 
            PlayerAnimation.ARM_SWING.play( (Player) myEntity, 64 ); 
        
        return true;
    }
}
