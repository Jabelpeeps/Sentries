package net.aufdemrand.sentry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.minecraft.server.v1_8_R3.LocaleI18n;

/**
 *  An abstract collection of useful static methods.
 */
public abstract class Util {
	
	/*
	 * Some strings to hold the names of external plugins in one location (in case of future changes to the names.)
	 */
	public static final String TOWNY = "Towny";
	public static final String FACTIONS = "Factions";
	public static final String WAR = "War";
	public static final String CLANS = "SimpleClans";
	public static final String SCORE = "ScoreboardTeams";
	public static final String VAULT = "Vault";
	public static final String CITIZENS = "Citizens";
	public static final String DENIZEN = "Denizen";
	
	/**
	 * This method appears to be tracing the source of a projectile travelling between two LivingEntity objects.
	 * 
	 * @param LivingEntity from
	 * @param LivingEntity to 
	 */
	public static Location getFireSource( LivingEntity from, LivingEntity to ) {

		Location loco = from.getEyeLocation();
		Vector victor = to.getEyeLocation().subtract( loco ).toVector();
		
		victor = normalizeVector( victor );
		victor.multiply( 0.5 );

		return loco.add( victor );
	}
	
	public static Vector normalizeVector ( Vector victor ) {
	    
		double mag = Math.sqrt(   Math.pow( victor.getX(), 2 ) 
		                        + Math.pow( victor.getY(), 2 ) 
		                        + Math.pow( victor.getZ(), 2 ) ) ;
		                        
		if ( mag != 0 ) 
			return victor.multiply( 1 / mag );
		
		return victor.multiply( 0 );
	}
	
	public static Location leadLocation (Location loc, Vector victor, double t) {

		return loc.clone().add( victor.clone().multiply( t ) );
	}

	public static void removeMount (int npcid) {
		
		NPC npc = CitizensAPI.getNPCRegistry().getById( npcid );
		
		if ( npc != null ) {
			if ( npc.getEntity() != null ) {
				npc.getEntity().setPassenger( null );
			}
			npc.destroy();
		}
	}
	
    /**
     * Strangely named method, as appears to only check permissions for a player (when passed in as
     * the Entity argument) - the sentry.bodyguard.<world-name> permission.
     * 
     * @param Entity entity this is checked to be an instance of Player, and then has perms checked
     * @param NPC bodyguard unused
     * 
     * @returns true if player has permission "sentry.bodyguard" for the current world.
     */ 
	public static boolean CanWarp (Entity entity, NPC bodyguyard) {

		if ( entity instanceof Player ) {
		    
		    Player player = (Player) entity;

			if (  player.hasPermission( "sentry.bodyguard.*" ) ) {
			    // all players have "*" perm by default.
				
                if  (  player.isPermissionSet( "sentry.bodyguard." + player.getWorld().getName() ) 
                    && !player.hasPermission( "sentry.bodyguard." + player.getWorld().getName() ) ) {
						//denied in this world.
						return false;
				}
				return true;
			}
			if ( player.hasPermission( "sentry.bodyguard." + player.getWorld().getName() ) ) {
				// no "*"" but specifically allowed this world.
				return true;
			}
		}
		return false;
	}

	public static double hangtime ( double launchAngle, double v, double elev, double g ) {

		double a =  v * Math.sin( launchAngle );
		double b = -2 * g * elev;

		if ( Math.pow( a, 2 ) + b < 0 ){
			return 0;
		}
		return ( a + Math.sqrt( Math.pow( a, 2 ) + b ) )  /  g;
	}
	
	public static Double launchAngle ( Location from, Location to, double v, double elev, double g ) {

		Vector victor = from.clone().subtract( to ).toVector();
		
		Double dist =  Math.sqrt( Math.pow( victor.getX(), 2 ) 
		                        + Math.pow( victor.getZ(), 2 ) );

		double v2 = Math.pow( v, 2 );
		double v4 = Math.pow( v, 4 );

		double derp =  g * ( g * Math.pow( dist, 2 ) + 2 * elev * v2 );

		//Check unhittable.
		if ( v4 < derp ) {
			//target unreachable
			// use this to fire at optimal max angle launchAngle = Math.atan( ( 2*g*elev + v2) / (2*g*elev + 2*v2));
			return null;
		}
		//calc angle
		return Math.atan( ( v2 - Math.sqrt( v4 - derp ) ) / ( g * dist ) );
	}
	/** 
	 * Reformat the supplied String, replacing the tags <NPC>, <PLAYER>, <ITEM>, & <AMOUNT> with the 
	 * names of the objects supplied as arguments, and translating any colour codes.
	 * 
	 * The method will return immediately if 'input' is null, and will remove the tags related to any 
	 * other arguments that are null objects. 
	 */
	public static String format( String input, NPC npc, CommandSender player, Material item, String amount ) {
	    
		if ( input == null ) return null;
		
		input = input.replace( "<NPC>", 
							 ( npc == null ) ? ""
									 		 : npc.getName() );
		input = input.replace( "<PLAYER>", 
							 ( player == null ) ? "" 
												: player.getName() );
		input = input.replace( "<ITEM>", 
							 ( item == null ) ? ""
											  : Util.getLocalItemName( item ) );
		input = input.replace( "<AMOUNT>", 
							 ( amount == null ) ? ""
									 			: amount );
		
		input =	ChatColor.translateAlternateColorCodes( '&', input );
		
		return input;
	}
	/**
     * Returns the name of the material or item matching the supplied ID, or "Hand".
     * 
     * @param int MatID the ID to be named.
     */
	static String getLocalItemName ( Material mat ) {
		
		if ( mat == null || mat == Material.AIR ) 
		    return  "Hand";
		
		if ( mat.isBlock() ) return mat.name();
		
		return LocaleI18n.get( mat.name() + ".name" );
	}

	/**
	 * method to convert String values to int's.<p>
	 * It catches any NumberFormatExceptions and returns -1.
	 * 
	 * @param value - the string to be converted
	 * @return the int value represented by the string.
	 */
	static int string2Int( String value ) {
		try {
			return Integer.parseInt( value );
		} catch ( NumberFormatException e ) {
			return -1;
		}
	}
	/**
	 * method to convert String values to floats.<p>
	 * It catches any NumberFormatExceptions and returns -1.
	 * 
	 * @param value - the string to be converted
	 * @return the float value represented by the string.
	 */
	static float string2Float( String value ) {
		try {
			return Float.parseFloat( value );
		} catch ( NumberFormatException e ) {
			return -1;
		}
	}
	/**
	 * method to convert String values to doubles.<p>
	 * It catches any NumberFormatExceptions and returns -1.
	 * 
	 * @param value - the string to be converted
	 * @return the double value represented by the string.
	 */
	static double string2Double( String value ) {
		try {
			return Double.parseDouble( value );
		} catch ( NumberFormatException e ) {
			return -1;
		}
	}

	/** New method with this name, now re-written to return Material values from the official enum. */
	static Material getMaterial( String materialName ) {
		
		if ( materialName == null ) return null;
	
		String[] args = materialName.toUpperCase().split( ":" );
	
		Material material = Material.getMaterial( args[0] );
	
		if ( material == null ) 
			throw new RuntimeException("Invalid Material name:" + materialName + ". Please check config.yml carefully.");
		
		return material;
	}
	
}
