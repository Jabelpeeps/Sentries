package net.aufdemrand.sentry;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.WitherSkull;

enum AttackType {
	// Columns: "name"			Material in hand			Projectile class	incendiary?	lightning?	level?	
	brawler( 	"Brawler", 		Material.AIR,				null) {
		@Override
		Material getWeapon( SentryInstance sentry ) {
			
			return ( (HumanEntity) sentry.getMyEntity() ).getInventory().getItemInHand().getType();
		}
	},
	bombardier( "Bombardier", 	Material.EGG, 				Egg.class ),
	archer( 	"Archer", 		Material.BOW, 				Arrow.class ),
	magi( 		"IceMagi", 		Material.SNOW_BALL,         Snowball.class ),
	pyro1( 		"Pyro1", 		Material.REDSTONE_TORCH_ON, SmallFireball.class ),
	pyro2( 		"Pyro2", 		Material.TORCH, 			SmallFireball.class, true,	false,	0 ),
	pyro3( 		"Pyro3", 		Material.BLAZE_ROD,			Fireball.class ),
	sc1( 		"StormCaller1", Material.PAPER,				ThrownPotion.class,	 false,	true,	1 ),
	sc2( 		"StormCaller2", Material.BOOK,				ThrownPotion.class,  false,	true,	2 ),
	sc3( 		"StormCaller3", Material.BOOK_AND_QUILL,	ThrownPotion.class,  false, true,	3 ),
	warlock1( 	"Warlock1", 	Material.ENDER_PEARL,		EnderPearl.class ),
	warlock2( 	"Warlock2", 	Material.SKULL_ITEM,		WitherSkull.class ),
//	warlock3( "Warlock3" ),  // No default weapon in config.yml 
//  NB if re-adding, remember to uncomment lines from SentryInstance.updateWeapon() 
	witchdoctor( "WitchDoctor", Material.POTION,			ThrownPotion.class );
	
	// The strings used for the names must correspond with the names used in the config file.
	String name;
	private Material weapon;
	private Class<? extends Projectile> projectile;
	boolean incendiary;
	boolean lightning;
	int lightningLevel;
	
	static Map<Material, AttackType> reverseSearch = new EnumMap<Material, AttackType>(Material.class);
	
	static {
		for ( AttackType each : values() ) 
			reverseSearch.put( each.weapon, each );
	}
	AttackType( String n, Material w, Class<? extends Projectile> p ) {
		this( n, w, p, false, false, 0);
	}
	AttackType( String n, Material w, Class<? extends Projectile> p, boolean i, boolean l, int ll ) {
		name = n;
		weapon = w;
		projectile = p;
		incendiary = i;
		lightning = l;
		lightningLevel = ll;
	}
	
	// the argument for this method is only used in the override for the 'brawler' instance.
	Material getWeapon( SentryInstance sentry ) {
		return weapon;
	}
	Class<? extends Projectile> getProjectile() {
		return projectile;
	}
	
	/** Method to get the appropriate AttackType instance
	 * @param item - the weapon currently being used by the sentry */
	static AttackType findType( Material item ) {
		return reverseSearch.get( item );
	}
	
	/** Only updates the values held in each enum instance if 'UseCustomWeapons' is set to true in the config.yml
	 * @param config - the FileConfiguration instance to scan for the needed values. */
	static void loadWeapons( FileConfiguration config ) {
		
		if ( config.getBoolean("UseCustomWeapons") ) {
			
			for ( AttackType attack : values() ) {
				if ( attack == brawler ) continue;
				
				attack.weapon = Sentry.getMaterial( config.getString( "AttackTypes." + attack.name ) );
			}
		}
	}
}