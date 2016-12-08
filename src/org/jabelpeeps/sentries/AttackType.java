package org.jabelpeeps.sentries;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.WitherSkull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.citizensnpcs.api.ai.AttackStrategy;

@AllArgsConstructor
public enum AttackType implements AttackStrategy {
    // Columns:-  weapon held                 projectile       incendiary? lightning level?
    BOMBARDIER(   Material.EGG,               Egg.class ), 
    ARCHER(       Material.BOW,               Arrow.class ), 
    ICEMAGI(      Material.SNOW_BALL,         Snowball.class ), 
    PYRO1(        Material.REDSTONE_TORCH_ON, SmallFireball.class ), 
    PYRO2(        Material.TORCH,             SmallFireball.class, true, 0 ),
    PYRO3(        Material.BLAZE_ROD,         Fireball.class ), 
    STORMCALLER1( Material.PAPER,             ThrownPotion.class, false, 1 ), 
    STROMCALLER2( Material.BOOK,              ThrownPotion.class, false, 2 ), 
    STORMCALLER3( Material.BOOK_AND_QUILL,    ThrownPotion.class, false, 3 ), 
    WARLOCK1(     Material.ENDER_PEARL,       EnderPearl.class ), 
    WARLOCK2(     Material.SKULL_ITEM,        WitherSkull.class ),
    // warlock3( "Warlock3" ), // No default weapon in config.yml so disabled  for now.
    WITCHDOCTOR(  Material.SPLASH_POTION,     ThrownPotion.class ),
    CREEPER(      Material.SULPHUR,           null ),  
    BRAWLER(      Material.AIR,               null ) {
        // this override method is only used for the brawler instance.
        @Override
        public Material getWeapon( SentryTrait sentry ) {
            LivingEntity myEntity = sentry.getMyEntity();
            if ( myEntity == null ) return Material.AIR;
            
            if ( myEntity instanceof HumanEntity )
                return ((HumanEntity) myEntity).getInventory().getItemInMainHand().getType();
            
            return myEntity.getEquipment().getItemInMainHand().getType();
        }
    };
    @Getter private Material weapon;
    @Getter private Class<? extends Projectile> projectile;
    boolean incendiary;
    int lightningLevel;

    AttackType( Material w, Class<? extends Projectile> p ) {
        this( w, p, false, 0 );
    }

    // the argument for this method is only used in the override for the  'brawler' instance.
    public Material getWeapon( SentryTrait sentry ) {
        return weapon;
    }

    static Map<Material, AttackType> reverseSearch = new EnumMap<>( Material.class );
    static {
        updateMap();
    }

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
    public boolean handle( LivingEntity arg0, LivingEntity arg1 ) {
        SentryTrait inst = Util.getSentryTrait( arg0 );
        if ( inst == null ) return false;
        
        switch ( this ) {
            case ARCHER: // arrows
                
                break;
            case BOMBARDIER: // eggs
                
                break;
            case CREEPER: // no projectile, explodes
                
                break;
            case ICEMAGI: // snowballs
                
                break;
            case PYRO1: // smallfireball, non-incendiary
                
                break;
            case PYRO2: // smallfireball, incendiary
                
                break;
            case PYRO3: // fireball
                
                break;
            case STORMCALLER1: // thrownpotion, non-incendiary, Lightening 1 
                
                break;
            case STROMCALLER2: // thrownpotion, non-incendiary, Lightening 2
                
                break;
            case STORMCALLER3: // thrownpotion, non-incendiary, Lightening 3 (instant death)
                
                break;
            case WARLOCK1: // enderpearl
                
                break;
            case WARLOCK2: // witherskull
                
                break;
            case WITCHDOCTOR: // potions
                
                break;
            case BRAWLER: default:
                // no projectile attack, returning false will use the default melee attack
        }
        return false;
    }
}
