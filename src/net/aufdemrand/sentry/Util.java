package net.aufdemrand.sentry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.LocaleI18n;

/**
 *  An abstract collection of useful static methods.
 */
public abstract class Util {
	/**
	 * This class appears to be tracing the source of a projectile travelling between two LivingEntity objects.
	 * 
	 * @param LivingEntity from
	 * @param LivingEntity to 
	 */
	public static Location getFireSource (LivingEntity from, LivingEntity to) {

		Location loco = from.getEyeLocation();
		
		Vector v = to.getEyeLocation().subtract( loco ).toVector();
		v = normalizeVector( v );
		v.multiply( 0.5 );

		Location loc = loco.add( v );

		return loc;
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
                if (   player.isPermissionSet( "sentry.bodyguard." + player.getWorld().getName() ) 
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
    /**
     * Returns the name of the material or item matching the supplied ID, or "Hand".
     * 
     * @param int MatID the ID to be named.
     */
	public static String getLocalItemName (int MatId) {
		if ( MatId == 0 ) 
		    return  "Hand";
		if ( MatId < 256 )
			return getMCBlock( MatId ).getName();
		else
		    return LocaleI18n.get( getMCItem( MatId ).getName() + ".name" );
	}

	//check for obfuscation change
	public static Item getMCItem (int id) {
		return Item.getById( id );
	}
	//check for obfuscation change
	public static Block getMCBlock (int id) {
		return Block.getById( id );
	}

	public static double hangtime (double launchAngle, double v, double elev, double g) {

		double a =  v * Math.sin( launchAngle );
		double b = -2 * g * elev;

		if ( Math.pow( a, 2 ) + b < 0 ){
			return 0;
		}
		return ( a + Math.sqrt( Math.pow( a, 2 ) + b ) )  /  g;
	}
	
	public static Double launchAngle (Location from, Location to, double v, double elev, double g) {

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
		else {
			//calc angle
			return Math.atan( ( v2 - Math.sqrt( v4 - derp ) ) / ( g * dist ) );
		}
	}

	public static String format (String input, NPC npc, CommandSender player, int item, String amount) {
	    
		if ( input == null ) return null;
		
		input = input.replace( "<NPC>", npc.getName() );
		input = input.replace( "<PLAYER>", player == null ? "" : player.getName() );
		input = input.replace( "<ITEM>", Util.getLocalItemName( item ) );
		input = input.replace( "<AMOUNT>", amount.toString() );
		input =	ChatColor.translateAlternateColorCodes( '&', input );
		
		return input;
	}

	public static Vector normalizeVector (Vector victor) {
	    
		double mag = Math.sqrt(   Math.pow( victor.getX(), 2 ) 
		                        + Math.pow( victor.getY(), 2 ) 
		                        + Math.pow( victor.getZ(), 2 ) ) ;
		                        
		if ( mag != 0 ) return victor.multiply( 1 / mag );
		
		return victor.multiply( 0 );
	}
}
