package org.jabelpeeps.sentries;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.WitherSkull;

import lombok.AllArgsConstructor;
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
    BRAWLER(      Material.AIR ) {
        @Override
        public Material getWeapon( SentryTrait sentry ) {
            LivingEntity myEntity = sentry.getMyEntity();
            if ( myEntity == null ) return Material.AIR;
            
            if ( myEntity instanceof HumanEntity )
                return ((HumanEntity) myEntity).getInventory().getItemInMainHand().getType();
            
            return myEntity.getEquipment().getItemInMainHand().getType();
        }
    };
    
    private Material weapon;
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
    public Material getWeapon( SentryTrait sentry ) { return weapon; }

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
        return reverseSearch.containsKey( item ) ? reverseSearch.get( item )
                                                 : AttackType.BRAWLER;
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
        
        NMS.look( myEntity, victim );
        
        if ( effect != null )
            myEntity.getWorld().playEffect( myEntity.getLocation(), effect, null );
        
        if ( myEntity instanceof Player ) 
            PlayerAnimation.ARM_SWING.play( (Player) myEntity, 64 ); 
        
        Location loc = victim.getLocation();
        
        switch ( this ) {
            case ARCHER: // arrows, ballistics   

                break;
                
            case BOMBARDIER: // eggs, ballistic

                break;
                
            case ICEMAGI: // snowballs, ballistic

                break;
                
            case PYRO1: // smallfireball, non-incendiary

                break;
                
            case PYRO2: // smallfireball, incendiary

                break;
                
            case PYRO3: // fireball   

                break;
                
            case STORMCALLER1: // thrownpotion, non-incendiary 
                loc.getWorld().strikeLightningEffect( loc.add( 0, .33, 0 ) );
                victim.damage( inst.strength, myEntity );               
                break;
                
            case STROMCALLER2: // thrownpotion, non-incendiary
                loc.getWorld().strikeLightning( loc.add( 0, .33, 0 ) );                
                break;
                
            case STORMCALLER3: // thrownpotion, non-incendiary
                loc.getWorld().strikeLightningEffect( loc.add( 0, .33, 0 ) );
                victim.setHealth( 0 );
                break;
                
            case WARLOCK1: // enderpearl, ballistics

                break;
                
            case WARLOCK2: // witherskull

                break;
                
            case WITCHDOCTOR: // potions, ballistic

                break;
                
            case CREEPER: // no projectile, explodes
                // TODO
                break;
                
            case BRAWLER: default:
                return false;
                // as there is no projectile attack, returning false will use the default melee attack
        }
        return true;
    }
}
