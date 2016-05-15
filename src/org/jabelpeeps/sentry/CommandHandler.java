package org.jabelpeeps.sentry;

import java.text.DecimalFormat;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.CitizensPlugin;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Owner;

public abstract class CommandHandler {
	
	public static Pattern colon = Pattern.compile( ":" );
	static Pattern initialDoubleQuote = Pattern.compile( "^\"" );
	static Pattern endDoubleQuote = Pattern.compile( "\"$" );
	static Pattern initialSingleQuote = Pattern.compile( "^'" );
	static Pattern endSingleQuote = Pattern.compile( "'$" );
	
	static String guardCommandHelp;
	static String equipCommandHelp;
	static String targetCommandHelp;
	static String ignoreCommandHelp;
	static String mainHelpIntro;
	
	/**
	 * Convenience method to check perms on Command usage.
	 * The method includes informing the player if they lack the required perms.
	 * @param command - The perm node to be checked.
	 * @param player - The sender of the command.
	 * @return true - if the player has the required permission.
	 */
	private static boolean checkCommandPerm( String command, CommandSender player ) {
		
		if ( player.hasPermission( command ) ) return true;
		
		player.sendMessage( S.Col.RED.concat( S.ERROR_NO_COMMAND_PERM ) );
		return false;
	}
	
	/**
	 * Check that the String[] args contains enough arguments, directing the player on
	 * how to get help if false.
	 * 
	 * @param number - the number of required args
	 * @param args - the argument array
	 * @param player - the player who entered the command.
	 * @return true - args.length >= number
	 */
	private static boolean enoughArgs( int number, String[] args, CommandSender player ) {
		
		if ( args.length >= number ) return true;
		
		player.sendMessage( S.Col.RED.concat( S.GET_COMMAND_HELP ) );
		return false;
	}
	
	/**
	 * Convenience method to send a formatted message to the player regarding the respawn status of the npc.
	 * 
	 * @param value - the number of seconds set as the respawn value.
	 * @param npc - the npc
	 * @param player - the player who sent the command.
	 */
	private static void respawnCommancMessage( int value, NPC npc, CommandSender player ) {
		
		if ( value == 0 ) 
			player.sendMessage( String.join( "", S.Col.GOLD, npc.getName(), S.NPC_NO_AUTO_RESPAWN ) );
		if ( value == -1 ) 
			player.sendMessage( String.join( "", S.Col.GOLD, npc.getName(), S.NPC_DELETE_ON_DEATH ) );
		if ( value > 0 ) 
			player.sendMessage( String.join( "", S.Col.GOLD, npc.getName(), S.NPC_RESPAWN_AFTER,
																String.valueOf( value ), S.SECONDS ) );
	}
	
	/**
	 * Convenience method that removes single and double quotes from the ends of the supplied string.
	 * 
	 * @param input - the string to be parsed
	 * @return - the string without quotes
	 */
	private static String sanitiseString( String input ) {
		
		input = initialDoubleQuote.matcher( input ).replaceAll( "" );
		input = endDoubleQuote.matcher( input ).replaceAll( "" );
		input = initialSingleQuote.matcher( input ).replaceAll( "" );
		input = endSingleQuote.matcher( input ).replaceAll( "" );
		
		return input;
	}
	
	/** 
	 * Concatenates the supplied String[] starting at the position indicated.
	 * 
	 * @param startFrom - the starting position (zero-based)
	 * @param args - the String[] to be joined
	 * @return - the resulting String.
	 */
	private static String joinArgs( int startFrom, String[] args ) {

		StringJoiner joiner = new StringJoiner( " " );
		
		for ( int i = startFrom; i < args.length; i++ ) {
			joiner.add( args[i] );
		}
		return joiner.toString();
	}
	
	private static String guardCommandHelp() {
		
		if ( guardCommandHelp == null ) {
			
			StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );
			
			joiner.add( String.join( "", S.Col.GOLD, "do '/sentry guard'", S.Col.RESET ) );
			joiner.add( "  to discover what a sentry is guarding" );
			joiner.add( String.join( "", S.Col.GOLD, "do '/sentry guard clear'", S.Col.RESET ) );
			joiner.add( "  to clear the player/npc being guarded" );
			joiner.add( String.join( "", S.Col.GOLD, "do '/sentry guard (-p/l) <EntityName>'", S.Col.RESET ) );
			joiner.add( "  to have a sentry guard a player, or another NPC" );
			joiner.add( String.join( "", S.Col.GOLD, "    -p ", S.Col.RESET, "-> only search player names" ) );
			joiner.add( String.join( "", S.Col.GOLD, "    -l ", S.Col.RESET, "-> only search local entities" ) );
			joiner.add( "      -> only use one of -p or -l (or omit)" );
			
			guardCommandHelp = joiner.toString();
		}
		return guardCommandHelp;
	}
	
	private static String equipCommandHelp() {
		
		if ( equipCommandHelp == null ) {
			
			StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );
			
			joiner.add( String.join( "", S.Col.GOLD, "do '/sentry equip <ItemName>'", S.Col.RESET ) );
			joiner.add( "  to give the named item to the sentry" );
			joiner.add( "  item names are the offical item names" );
			joiner.add( String.join( "", S.Col.GOLD, "do '/sentry equip clearall'", S.Col.RESET ) );
			joiner.add( "  to clear all equipment slots." );
			joiner.add( String.join( "", S.Col.GOLD, "do '/sentry equip clear <slot>'", S.Col.RESET ) );
			joiner.add( "  to clear the specified slot, where slot can be one of: hand, helmet, chestplate, leggings or boots." );
			
			equipCommandHelp = joiner.toString();
		}
		return equipCommandHelp;
	}

	private static String targetCommandHelp() {
		
		if ( targetCommandHelp == null ) {
			
			StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );
			
			joiner.add( String.join( "", S.Col.GOLD, S.HELP_COMMAND_TARGET, S.Col.RESET ) );
			joiner.add( String.join( " ", S.Col.GOLD, "", S.LIST, S.Col.RESET, S.HELP_LIST, S.TARGETS ) );
			joiner.add( String.join( " ", S.Col.GOLD, "", S.CLEAR, S.Col.RESET, S.HELP_CLEAR, S.TARGETS ) );
			joiner.add( String.join( " ", S.Col.GOLD, S.HELP_ADD_TYPE, S.Col.RESET, S.HELP_ADD ) );
			joiner.add( String.join( " ", S.Col.GOLD, S.HELP_REMOVE_TYPE, S.Col.RESET, S.HELP_REMOVE ) );
			joiner.add( S.HELP_ADD_REMOVE_TYPES );
			joiner.add( getAdditionalTargets() );
			
			targetCommandHelp = joiner.toString();
		}
		return targetCommandHelp;
	}
	
	private static String ignoreCommandHelp() {
		
		if ( ignoreCommandHelp == null ) {
			
			StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );
			
			joiner.add( String.join( "", S.Col.GOLD, S.HELP_COMMAND_IGNORE, S.Col.RESET ) );
			joiner.add( String.join( " ", S.Col.GOLD, "", S.LIST, S.Col.RESET, S.HELP_LIST, S.IGNORES ) );
			joiner.add( String.join( " ", S.Col.GOLD, "", S.CLEAR, S.Col.RESET, S.HELP_CLEAR, S.IGNORES ) );
			joiner.add( String.join( " ", S.Col.GOLD, S.HELP_ADD_TYPE, S.Col.RESET, S.HELP_ADD ) );
			joiner.add( String.join( " ", S.Col.GOLD, S.HELP_REMOVE_TYPE, S.Col.RESET, S.HELP_REMOVE ) );
			joiner.add( S.HELP_ADD_REMOVE_TYPES );
			joiner.add( getAdditionalTargets() );
			
			ignoreCommandHelp = joiner.toString();			
		}
		return ignoreCommandHelp;
	}
	
	/**
	 * iterates over the activated PluginBridges, polling each one for command help text.
	 * @return - the concatenated help Strings
	 */
	private static String getAdditionalTargets() {
		String outString = "";
		
		if ( !Sentry.activePlugins.isEmpty() ) {
			StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );
		
			joiner.add( "You may also use these additional types:- " );
			
			for ( PluginBridge each : Sentry.activePlugins.values() ) {
				joiner.add( each.getCommandHelp() );
			}
			outString = joiner.toString();
		}
		return outString;
	}
	
//------------------------------------------------------------------------------------	
//------------------------------------------------------------------------------------
	/**
	 * The only accessible method of this class. It parses the arguments and responds accordingly.
	 * 
	 * @param player
	 * @param inargs
	 * @param sentry
	 * @return - true if the command has been successfully handled.
	 */
	static boolean call( CommandSender player, String[] inargs, Sentry sentry ) {
		
		if ( !enoughArgs( 1, inargs, player ) ) return true;

		//----------------------------------------------------- help command -----------------
		if ( S.HELP.equalsIgnoreCase( inargs[0] ) ) {
			
			if ( inargs.length > 1 ) {
				
				if ( checkCommandPerm( S.PERM_TARGET, player ) && S.TARGET.equalsIgnoreCase( inargs[1] ) ) {
					player.sendMessage( targetCommandHelp() );
				}
				else if ( checkCommandPerm( S.PERM_IGNORE, player ) && S.IGNORE.equalsIgnoreCase( inargs[1] ) ) {
					player.sendMessage( ignoreCommandHelp() );
				}
				else if ( checkCommandPerm( S.PERM_EQUIP, player ) && S.EQUIP.equalsIgnoreCase( inargs[1] ) ) {
					player.sendMessage( equipCommandHelp() );
				}
				else if ( checkCommandPerm( S.PERM_GUARD, player ) && S.GUARD.equalsIgnoreCase( inargs[1] ) ) {
					player.sendMessage( guardCommandHelp() );
				}
				else player.sendMessage( S.ERROR_NO_MORE_HELP );
			}
			else {
				if ( mainHelpIntro == null ) {
					
					StringJoiner joiner = new StringJoiner( System.lineSeparator() );
					
					joiner.add( String.join( "", S.Col.GOLD, "------- Sentry Commands -------", S.Col.RESET ) );
					joiner.add( String.join( " ", "You can use", S.Col.GOLD, "/sentry (#id) <command> [args]", 
							S.Col.RESET, "to perform commands on a sentry without having it selected." ) );
					joiner.add( String.join( " ", "If ... is shown do", S.Col.GOLD, "/sentry help <command>", 
							S.Col.RESET, "for further help" ) );
					joiner.add( "" );
					
					mainHelpIntro = joiner.toString();
				}
				player.sendMessage( mainHelpIntro );
				
				if ( checkCommandPerm( S.PERM_TARGET, player ) ) player.sendMessage( 
					String.join( " ", S.Col.GOLD, "/sentry target ...", S.Col.RESET, "set targets to attack." ) );
				if ( checkCommandPerm( S.PERM_IGNORE, player ) ) player.sendMessage( 
					String.join( " ", S.Col.GOLD, "/sentry ignore ...", S.Col.RESET, "set targets to ignore." ) );
				if ( checkCommandPerm( S.PERM_EQUIP, player ) ) player.sendMessage( 
					String.join( " ", S.Col.GOLD, "/sentry equip ...", S.Col.RESET, "set the equipment a sentry is using" ) );
				if ( checkCommandPerm( S.PERM_GUARD, player ) ) player.sendMessage( 
					String.join( " ", S.Col.GOLD, "/sentry guard ...", S.Col.RESET, "tell the sentry what to guard" ) );
				
				if ( checkCommandPerm( S.PERM_DEBUG, player ) ) player.sendMessage( 
					String.join( " ", S.Col.GOLD, "/sentry debug", S.Col.RESET, "- displays debug info on the console", 
								System.lineSeparator(), S.Col.RED, 
								"This option affects performance, do not turn it on unless you are using it!" ) );
				
				if ( checkCommandPerm( S.PERM_RELOAD, player ) )player.sendMessage( 
					String.join( " ", S.Col.GOLD, "/sentry reload", S.Col.RESET, "- Reloads the config file" ) );
				
				if ( checkCommandPerm( S.PERM_SPEED, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry speed [0-1.5]");
					player.sendMessage( "  Sets speed of the Sentry when attacking");
				}
				if ( checkCommandPerm( S.PERM_HEALTH, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry health [1-2000000]");
					player.sendMessage( "  Sets the Sentry's Health");
				}
				if ( checkCommandPerm( S.PERM_ARMOR, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry armor [0-2000000]");
					player.sendMessage( "  Sets the Sentry's Armor");
				}
				if ( checkCommandPerm( S.PERM_STRENGTH, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry strength [0-2000000]");
					player.sendMessage( "  Sets the Sentry's Strength");
				}
				if ( checkCommandPerm( S.PERM_ATTACK_RATE, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry attackrate [0.0-30.0]");
					player.sendMessage( "  Sets the time between the Sentry's projectile attacks");
				}
				if ( checkCommandPerm( S.PERM_HEAL_RATE, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry healrate [0.0-300.0]");
					player.sendMessage( "  Sets the frequency the sentry will heal 1 point. 0 to disable.");
				}
				if ( checkCommandPerm( S.PERM_RANGE, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry range [1-100]");
					player.sendMessage( "  Sets the Sentry's detection range");
				}
				if ( checkCommandPerm( S.PERM_WARNING_RANGE, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry warningrange [0-50]");
					player.sendMessage( "  Sets the range, beyond the detection range, that the Sentry will warn targets.");
				}
				if ( checkCommandPerm( S.PERM_NIGHTVISION, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry nightvision [0-16] " );
					player.sendMessage( "  0 = See nothing, 16 = See everything. " );
				}
				if ( checkCommandPerm( S.PERM_RESPAWN_DELAY, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry respawn [-1-2000000]");
					player.sendMessage( "  Sets the number of seconds after death the Sentry will respawn.");
				}
				if ( checkCommandPerm( S.PERM_FOLLOW_DIST, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry follow [0-32]");
					player.sendMessage( "  Sets the number of block away a bodyguard will follow. Default is 4");
				}
				if ( checkCommandPerm( S.PERM_INVINCIBLE, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry invincible");
					player.sendMessage( "  Toggle the Sentry to take no damage or knockback.");
				}
				if ( checkCommandPerm( S.PERM_RETALIATE, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry retaliate");
					player.sendMessage( "  Toggle the Sentry to always attack an attacker.");
				}
				if ( checkCommandPerm( S.PERM_CRITICAL_HITS, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry criticalhits");
					player.sendMessage( "  Toggle the Sentry to take critical hits and misses");
				}
				if ( checkCommandPerm( S.PERM_DROPS, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry drops");
					player.sendMessage( "  Toggle the Sentry to drop equipped items on death");
				}
				if ( checkCommandPerm( S.PERM_KILLDROPS, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry killdrops");
					player.sendMessage( "  Toggle whether or not the sentry's victims drop items and exp");
				}
				if ( checkCommandPerm( S.PERM_MOUNT, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry mount");
					player.sendMessage( "  Toggle whether or not the sentry rides a mount");
				}
				if ( checkCommandPerm( S.PERM_TARGETABLE, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry targetable");
					player.sendMessage( "  Toggle whether or not the sentry is attacked by hostile mobs");
				}
				if ( checkCommandPerm( S.PERM_SPAWN, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry spawn");
					player.sendMessage( "  Set the sentry to respawn at its current location");
				}
				if ( checkCommandPerm( S.PERM_WARNING, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry warning <text to use>");
					player.sendMessage( "  Change the warning text. <NPC> and <PLAYER> can be used as placeholders");
				}
				if ( checkCommandPerm( S.PERM_GREETING, player ) ) {
					player.sendMessage( S.Col.GOLD + "/sentry greeting <text to use>");
					player.sendMessage( "  Change the greeting text. <NPC> and <PLAYER> can be used as placeholders");
				}
				if ( checkCommandPerm( S.PERM_INFO, player ) ) player.sendMessage( 
					String.join( " ", S.Col.GOLD, "/sentry info", S.Col.RESET, "- View all attributes of a sentry NPC" ) );
			}
			return true;
		}
		//---------------------------------------------------------- Debug Command --------------
		if ( "debug".equalsIgnoreCase( inargs[0] ) ) {
			
			if ( checkCommandPerm( S.PERM_DEBUG, player ) ) {

				Sentry.debug = !Sentry.debug;
				player.sendMessage( S.Col.GREEN + "Debug is now: " + ( Sentry.debug ? S.ON
																					: S.OFF ) );
			}
			return true;
		}
		//---------------------------------------------------------- Reload Command ------------
		if ( "reload".equalsIgnoreCase( inargs[0] ) ) {
			
			if ( checkCommandPerm( S.PERM_RELOAD, player ) ) {

				sentry.reloadMyConfig();
				player.sendMessage( S.Col.GREEN + "reloaded Sentry's config.yml file" );
			}
			return true;
		}
		
		// the remaining commands all deal with npc's
		//-------------------------------------------------------------------------------------		
		// did player specify an integer as the first argument?  	
		int npcid = Util.string2Int( inargs[0] );
		
		int nextArg = ( npcid > 0 ) ? 1 
									: 0;

		if ( !enoughArgs( 1 + nextArg, inargs, player ) ) return true;

		NPC thisNPC;
		// check to see whether the value saved is an npc ID, and save a reference if so.
		if ( npcid == -1 ) {
			
			thisNPC = ((CitizensPlugin) CitizensAPI.getPlugin()).getDefaultNPCSelector().getSelected( player );
			
			if ( thisNPC == null ) {
				player.sendMessage( S.Col.RED.concat( S.ERROR_NO_NPC ) );
				return true;
			}	
			npcid = thisNPC.getId();
		} 
		else {
			thisNPC = CitizensAPI.getNPCRegistry().getById( npcid ); 
	
			if ( thisNPC == null ) {
				player.sendMessage( String.join( "", S.Col.RED, S.ERROR_ID_INVALID, String.valueOf( npcid ) ) );
				return true;
			}
		}
		// We are now sure that thisNPC is valid, and that npcid contains its id. 
		if ( !thisNPC.hasTrait( SentryTrait.class ) ) {
			player.sendMessage( S.Col.RED.concat( S.ERROR_NOT_SENTRY ) );
			return true;
		}
		// OK, we have a sentry to modify.
		
		// we need to check that the player sending the command has the authority to use it.
		// first lets be sure we are dealing with an actual player, not a player type npc.
		if ( player instanceof Player 
		  && !CitizensAPI.getNPCRegistry().isNPC( (Entity) player) ) {
			
			// TODO consider changing this section to allow admins to modify other players' npcs.

			if ( !thisNPC.getTrait( Owner.class ).getOwner().equalsIgnoreCase( player.getName() ) ) {
				// player is not owner of the npc
				
				if ( !((Player) player).hasPermission( S.PERM_CITS_ADMIN ) ) {
					// player is not an admin either.
					
					player.sendMessage( S.Col.RED.concat( "You must be the owner of this Sentry to execute commands." ) );
					return true;
				}
				if ( !thisNPC.getTrait( Owner.class ).getOwner().equalsIgnoreCase( "server" ) ) {
					// not server-owned NPC
					
					player.sendMessage( S.Col.RED.concat( "You, or the server, must be the owner of this Sentry to execute commands." ) );
					return true;
				}
			}
		} 
		
		// We now know that player is either the owner, or an admin with a server-owned npc.
		// lets save a reference to the SentryInstance of the npc before continuing.
		SentryInstance inst = thisNPC.getTrait( SentryTrait.class ).getInstance();
		String npcName = thisNPC.getName();
		
		// hold the state of the third argument (if it holds a boolean value) in a field for later use.
		// this is held as an object not a primitive to allow for a third state - 'null'.
		Boolean set = null;
		
		if ( inargs.length == 2 + nextArg ) {
			if ( S.TRUE.equalsIgnoreCase( inargs[1 + nextArg] ) || S.ON.equalsIgnoreCase( inargs[1 + nextArg] ) ) 
					set = true;
			if ( S.FALSE.equalsIgnoreCase( inargs[1 + nextArg] ) || S.OFF.equalsIgnoreCase( inargs[1 + nextArg] ) ) 
					set = false;
		}
		//------------------------------------------------------------ spawn command --------------		
		if ( S.SPAWN.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.SPAWN, player ) ) {
			
				if ( thisNPC.getEntity() == null ) {
					player.sendMessage( S.Col.RED.concat( "Cannot set spawn while a sentry is dead" ) );
					return true;
				}
				inst.spawnLocation = thisNPC.getEntity().getLocation();
				player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "will respawn at its present location" ) );
			}
			return true;
		}
		//----------------------------------------------------------- invincible command ------------
		if ( S.INVINCIBLE.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_INVINCIBLE, player ) ) {
				
				inst.invincible = ( set == null ) ? !inst.invincible : set;
				
				player.sendMessage( String.join( " ", S.Col.GREEN, npcName, 
												inst.invincible ? "is now INVINCIBLE" 
																: "now takes damage" ) );
			}
			return true;
		}
		//------------------------------------------------------------- retaliate command --------------
		if ( S.RETALIATE.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_RETALIATE, player ) ) {
				
				inst.iWillRetaliate = ( set == null ) ? !inst.iWillRetaliate : set;
	
				player.sendMessage( String.join( " ", S.Col.GREEN, npcName, 
												inst.iWillRetaliate ? "will retalitate against all attackers"
														            : "will not retaliate when attacked" ) );
			}
			return true;
		}
		//--------------------------------------------------------------- criticals command -------------		
		if ( S.CRITICAL_HITS.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_CRITICAL_HITS, player ) ) {

				inst.acceptsCriticals = ( set == null ) ? !inst.acceptsCriticals : set;
	
				player.sendMessage( String.join( " ", S.Col.GREEN, npcName,
												inst.acceptsCriticals ? "will take critical hits"
																	  : "will take normal damage" ) );
			}
			return true;
		}
		//-------------------------------------------------------------- drops command ----------------		
		if ( S.DROPS.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_DROPS, player ) ) {
				
				inst.dropInventory = ( set == null ) ? !inst.dropInventory : set;
	
				player.sendMessage( String.join( " ", S.Col.GREEN, npcName,
												inst.dropInventory ? "will drop items"
																   : "will not drop items" ) ); 
			}
			return true;
		}
		//--------------------------------------------------------------- killdrops command -------------
		if ( S.KILLS_DROP.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_KILLDROPS, player ) ) {

				inst.killsDropInventory = ( set == null ) ? !inst.killsDropInventory : set;
	
				player.sendMessage( String.join( "", S.Col.GREEN, npcName,
												inst.killsDropInventory ? "'s kills will drop items or exp" 
																		: "'s kills will not drop items or exp" ) );
			}
			return true;
		}
		//-----------------------------------------------------------------targetable command ------------
		if ( S.TARGETABLE.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_TARGETABLE, player ) ) {

				inst.targetable = ( set == null ) ? !inst.targetable : set;
				
				thisNPC.data().set( NPC.TARGETABLE_METADATA, inst.targetable );
	
				player.sendMessage( String.join( " ", S.Col.GREEN, npcName,
												inst.targetable ? "will be targeted by mobs"
																: "will not be targeted by mobs" ) );
			}
			return true;
		}	
		//-------------------------------------------------------------------mount command -----------		
		if ( S.MOUNT.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_MOUNT, player ) ) {
	
				set = ( set == null ) ? !inst.isMounted() : set;
				
				if ( set ) {
					inst.mount();
					player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is now Mounted" ) );
				}
				else {
					if ( inst.isMounted() ) 
						Util.removeMount( inst.mountID );	
					
					inst.mountID = -1;
					player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is no longer Mounted" ) );
				}
			}
			return true;
		}
		//------------------------------------------------------------------guard command -------------	
		
		//TODO add help text for this command.
		if ( S.GUARD.equalsIgnoreCase( inargs[nextArg] ) ) { 
			
			if ( checkCommandPerm( S.PERM_GUARD, player ) ) {

				if ( inargs.length > 1 + nextArg ) {
					
					boolean localonly = false;
					boolean playersonly = false;
					int start = 1;
					boolean ok = false;
	
					if ( inargs[nextArg + 1].equalsIgnoreCase( "-p" ) ) {
						start = 2;
						playersonly = true;
					}
	
					if ( inargs[nextArg + 1].equalsIgnoreCase( "-l" ) ) {
						start = 2;
						localonly = true;
					}
	
					String arg = joinArgs( start + nextArg, inargs );
					
					if ( !playersonly ){ 
						ok = inst.findGuardEntity( arg, false );
					}
	
					if ( !localonly ) {
						ok = inst.findGuardEntity( arg, true );
					}
	
					if ( ok )
						player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is now guarding", arg ) );   
					else 
						player.sendMessage( String.join( " ", S.Col.RED, npcName, "could not find", arg ) );   
					return true;
				}
				
				if ( S.CLEAR.equalsIgnoreCase( inargs[nextArg + 1] ) ) {
					inst.findGuardEntity( null, false );
				}
				
				if ( inst.guardTarget == null )
					player.sendMessage( S.Col.GREEN.concat( "Guarding: My Surroundings" ) );   	
				else if ( inst.guardEntity == null )
					player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is configured to guard",
							 inst.guardTarget, "but cannot find them at the moment" ) );
				else 
					player.sendMessage( String.join( " ", S.Col.BLUE, "Guarding:", inst.guardEntity.toString() ) );
					
				
			}
			return true;
		}
		//---------------------------------------------------------------follow command -------------------
		if ( S.FOLLOW.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_FOLLOW_DIST, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Follow Distance is ", 
												String.valueOf( inst.followDistance ) ) );
					player.sendMessage( S.Col.GOLD.concat( "Usage: /sentry follow [#]. Default is 4. " ) );
				}
				else {
					int dist = Util.string2Int( inargs[nextArg + 1] );
					if ( dist < 0 ) {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}

					if ( dist > 32 ) dist = 32;  
					inst.followDistance = dist * dist;
					player.sendMessage( 
							String.join( " ", S.Col.GREEN, npcName, "follow distance set to", String.valueOf( dist ) ) ); 
				}
			}
			return true;
		}
		//----------------------------------------------------------------health command -------------------
		if ( S.HEALTH.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_HEALTH, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( 
							String.join( "", S.Col.GOLD, npcName, "'s Health is ", String.valueOf( inst.sentryMaxHealth ) ) );
					player.sendMessage( S.Col.GOLD.concat( 
							"Usage: /sentry health [#]   note: Typically players have 20 HPs when fully healed" ) );
				}
				else {
					int HPs = Util.string2Int( inargs[nextArg + 1] );
					if ( HPs < 1 )  {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}

					if ( HPs > 2000000 ) HPs = 2000000; 
					inst.sentryMaxHealth = HPs;
					inst.setHealth( HPs );
					player.sendMessage( 
							String.join( " ", S.Col.GREEN, npcName, "health set to", String.valueOf( HPs ) ) ); 
				}
			}
			return true;
		}
		//---------------------------------------------------------------armour command-----------------------
		if ( S.ARMOR.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_ARMOR, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( 
							String.join( "", S.Col.GOLD, npcName, "'s Armor is ", String.valueOf( inst.armorValue ) ) );
					player.sendMessage( S.Col.GOLD.concat( "Usage: /sentry armor [#] " ) );
				}
				else {
					int armour = Util.string2Int( inargs[nextArg + 1] );
					if ( armour < 0 )  {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}

					if ( armour > 2000000 ) armour = 2000000;  
					inst.armorValue = armour;
					player.sendMessage( 
							String.join( " ", S.Col.GREEN, npcName, "armor set to", String.valueOf( armour ) ) ); 
				}
			}
			return true;
		}
		//----------------------------------------------------------------strength command --------------		
		if ( S.STRENGTH.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_STRENGTH, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( S.Col.GOLD + npcName + "'s Strength is " + inst.strength );
					player.sendMessage( S.Col.GOLD + "Usage: /sentry strength # " );
					player.sendMessage( S.Col.GOLD + "Note: At strength 0 the Sentry will do no damamge. " );
				}
				else {
					int strength = Util.string2Int( inargs[nextArg + 1] );
					if ( strength < 0 )  {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}
					
					if ( strength > 2000000 ) strength = 2000000; 
					inst.strength = strength;	
					player.sendMessage( 
							String.join( " ", S.Col.GREEN, npcName, "strength set to", String.valueOf( strength ) ) ); 
				}
			}
			return true;
		}
		//-----------------------------------------------------------------nightvision command---------		
		//TODO add help text for this command
		if ( S.NIGHT_VISION.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_NIGHTVISION, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( S.Col.GOLD + npcName + "'s Night Vision is " + inst.nightVision );
					player.sendMessage( S.Col.GOLD + "Usage: /sentry nightvision [0-16] " );
					player.sendMessage( S.Col.GOLD + "Usage: 0 = See nothing, 16 = See everything. " );
				}
				else {
					int vision = Util.string2Int( inargs[nextArg + 1] );
					if ( vision < 0 )  {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}
					if ( vision > 16 ) vision = 16;  
					inst.nightVision = vision;
					player.sendMessage( 
							String.join( " ", S.Col.GREEN, npcName, "Night Vision set to", String.valueOf( vision ) ) ); 
				}
			}
			return true;
		}
		//-----------------------------------------------------------respawn command------------------------
		if ( S.RESPAWN.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_RESPAWN_DELAY, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					respawnCommancMessage( inst.respawnDelay, thisNPC, player );
					
					player.sendMessage( S.Col.GOLD + "Usage: /sentry respawn [-1 - 2000000] " );
					player.sendMessage( S.Col.GOLD + "Usage: set to 0 to prevent automatic respawn" );
					player.sendMessage( S.Col.GOLD + "Usage: set to -1 to *permanently* delete the Sentry on death." );
				}
				else {
					int respawn = Util.string2Int( inargs[nextArg + 1] );
					if ( respawn < -1 )  {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}
					
					if ( respawn > 2000000 ) respawn = 2000000;
					inst.respawnDelay = respawn;
					respawnCommancMessage( inst.respawnDelay, thisNPC, player );
				}
			}
			return true;
		}
		//----------------------------------------------------------speed command--------------------------
		if ( S.SPEED.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_SPEED, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( S.Col.GOLD + npcName + "'s Speed is " + inst.sentrySpeed );
					player.sendMessage( S.Col.GOLD + "Usage: /sentry speed [0.0 - 2.0]" );
				}
				else {
					float speed = Util.string2Float( inargs[nextArg + 1] );
					if ( speed < 0.0 ) {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}

					if ( speed > 2.0 ) speed = 2.0f; 
					inst.sentrySpeed = speed;
					player.sendMessage( 
							String.join( " ", S.Col.GREEN, npcName, "speed set to", String.valueOf( speed ) ) );  
				}
			}
			return true;
		}
		//-----------------------------------------------------------attackrate command ---------		
		if ( S.ATTACK_RATE.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_ATTACK_RATE, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( S.Col.GOLD + npcName + "'s Projectile Attack Rate is " 
																		+ inst.attackRate + "seconds between shots." );
					player.sendMessage( S.Col.GOLD + "Usage: /sentry attackrate [0.0 - 30.0]");
				}
				else {
					double attackrate = Util.string2Double( inargs[nextArg + 1] );
					if ( attackrate < 0.0 ) {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}

					if ( attackrate > 30.0 ) attackrate = 30.0; 
					inst.attackRate = attackrate;
					player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Projectile Attack Rate set to", 
											String.valueOf( attackrate ) ) ); 
				}
			}
			return true;
		}
		//------------------------------------------------------------------healrate command-----------------
		if ( S.HEALRATE.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_HEAL_RATE, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( S.Col.GOLD + npcName + "'s Heal Rate is " + inst.healRate + "s" );
					player.sendMessage( S.Col.GOLD + "Usage: /sentry healrate [0.0 - 300.0]" );
					player.sendMessage( S.Col.GOLD + "Usage: Set to 0 to disable healing" );
				}
				else {
					double healrate = Util.string2Double( inargs[nextArg + 1] );
					if ( healrate < 0.0 ) {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}

					if ( healrate > 300.0 ) healrate = 300.0;  
					inst.healRate = healrate;
					player.sendMessage( 
							String.join( " ", S.Col.GREEN, npcName, "Heal Rate set to", String.valueOf( healrate ) ) ); 
				}
			}
			return true;
		}
		//-------------------------------------------------------------------range command-----------------
		if ( S.RANGE.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_RANGE, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( S.Col.GOLD + npcName + "'s Range is " + inst.sentryRange );
					player.sendMessage( S.Col.GOLD + "Usage: /sentry range [1 - 100]" );
				}
				else {
					int range = Util.string2Int( inargs[nextArg + 1] );
					if ( range < 1 ) {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}

					if ( range > 100 ) range = 100; 
					inst.sentryRange = range;
					player.sendMessage( 
							String.join( " ", S.Col.GREEN, npcName, "range set to", String.valueOf( range ) ) );  
				}
			}
			return true;
		}
		//----------------------------------------------------------------------warningrange command----------
		if ( S.WARNING_RANGE.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_WARNING_RANGE, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( S.Col.GOLD + npcName + "'s Warning Range is " + inst.warningRange );
					player.sendMessage( S.Col.GOLD + "Usage: /sentry warningrangee [0 - 50]" );
				}
				else {
					int range = Util.string2Int( inargs[nextArg + 1] );
					if ( range < 0 ) {
						player.sendMessage( String.join( "", S.ERROR, inargs[nextArg + 1], S.ERROR_NOT_NUMBER ) );
						return true;
					}

					if ( range > 50 ) range = 50; 
					inst.warningRange = range;
					player.sendMessage( 
							String.join( " ", S.Col.GREEN, npcName, "warning range set to", String.valueOf( range ) ) );  
				}
			}
			return true;
		}
		//--------------------------------------------------------------------------equip command-------------		
		if ( S.EQUIP.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_EQUIP, player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( equipCommandHelp() );
				}
				// TODO figure out why zombies and skele's are not included here.
				else if ( thisNPC.getEntity().getType() == EntityType.ENDERMAN 
							|| thisNPC.getEntity().getType() == EntityType.PLAYER ) {
						
						if ( "clearall".equalsIgnoreCase( inargs[nextArg + 1] ) ) {
							
							//remove equipment
							sentry.equip( thisNPC, inst, null );
							player.sendMessage( String.join( "", S.Col.YELLOW, npcName, "'s equipment cleared" ) ); 
						}
						else if ( S.CLEAR.equalsIgnoreCase( inargs[nextArg + 1] ) ) {
							
							for ( Entry<String, Integer> each : Sentry.equipmentSlots.entrySet() ) 
								
								if ( each.getKey().equalsIgnoreCase( inargs[nextArg + 2] ) ) 
									thisNPC.getTrait( Equipment.class ).set( each.getValue(), new ItemStack( Material.AIR ) );
						}
						else {
							Material mat = Util.getMaterial( inargs[nextArg + 1] );
							
							if ( mat == null ) {
								player.sendMessage( S.Col.RED.concat( "Could not equip: item name not recognised" ) ); 
								return true;
							}
							
							ItemStack item = new ItemStack( mat );
							
							if ( sentry.equip( thisNPC, inst, item ) ) 
								player.sendMessage( String.join( " ", S.Col.GREEN, "equipped", mat.toString(), "on", npcName ) ); 
							else 
								player.sendMessage( S.Col.RED.concat( "Could not equip: invalid mob type?" ) ); 
						}
				}
				else player.sendMessage( S.Col.RED.concat( "Could not equip: must be Player or Enderman type" ) );
			}
			return true;
		}
		//--------------------------------------------------------------------warning command-----------		
		if ( S.WARNING.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_WARNING, player ) ) {

				if ( inargs.length >= 2 + nextArg ) {
						
					String str = sanitiseString( joinArgs( 1 + nextArg, inargs ) );
					
					player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Warning message set to", S.Col.RESET, 
													ChatColor.translateAlternateColorCodes( '&', str ) ) );   
					inst.warningMsg = str;
				}
				else {
					player.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Warning Message is: ", S.Col.RESET, 
													ChatColor.translateAlternateColorCodes( '&', inst.warningMsg ) ) );  

					player.sendMessage( S.Col.GOLD.concat( "Usage: /sentry warning 'The Text to use'" ) );
				}
			}
			return true;
		}
		//--------------------------------------------------------------------greeting command----------
		if ( S.GREETING.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_GREETING, player ) ) {
			
				if ( inargs.length >= 2 + nextArg ) {
	
					String str = sanitiseString( joinArgs( 1 + nextArg, inargs ) );
					
					player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Greeting message set to", S.Col.RESET, 
												ChatColor.translateAlternateColorCodes( '&', str ) ) );    
					inst.greetingMsg = str;
				}
				else {
					player.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s Greeting Message is: ", S.Col.RESET, 
												ChatColor.translateAlternateColorCodes( '&', inst.greetingMsg ) ) );
					
					player.sendMessage( S.Col.GOLD.concat( "Usage: /sentry greeting 'The Text to use'" ) );
				}
			}
			return true;
		}
		//---------------------------------------------------------------------info command-----------
		if ( S.INFO.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_INFO, player ) ) {
				
				StringJoiner joiner = new StringJoiner( System.lineSeparator() );
				
				joiner.add( String.join( "", S.Col.GOLD, "------- Sentry Info for (", 
										String.valueOf( thisNPC.getId() ), ") ", npcName, "------" ) );
				
				joiner.add( String.join( " ", S.Col.RED, "[HP]:", S.Col.WHITE, String.valueOf( inst.getHealth() ), "/",
						 													String.valueOf( inst.sentryMaxHealth ) ) );
				joiner.add( String.join( " ", S.Col.RED, "[AP]:", S.Col.WHITE, String.valueOf( inst.getArmor() ) ) );
				joiner.add( String.join( " ", S.Col.RED, "[STR]:", S.Col.WHITE, String.valueOf( inst.getStrength() ) ) );
				joiner.add( String.join( " ", S.Col.RED, "[SPD]:", S.Col.WHITE, 
																new DecimalFormat( "#.0" ).format( inst.getSpeed() ) ) );
				joiner.add( String.join( " ", S.Col.RED, "[RNG]:", S.Col.WHITE, String.valueOf( inst.sentryRange ) ) ); 
				joiner.add( String.join( " ", S.Col.RED, "[ATK]:", S.Col.WHITE, String.valueOf( inst.attackRate ) ) );
				joiner.add( String.join( " ", S.Col.RED, "[VIS]:", S.Col.WHITE, String.valueOf( inst.nightVision ) ) );
				joiner.add( String.join( " ", S.Col.RED, "[HEAL]:", S.Col.WHITE, String.valueOf( inst.healRate ) ) );
				joiner.add( String.join( " ", S.Col.RED, "[WARN]:", S.Col.WHITE, String.valueOf( inst.warningRange ) ) );
				joiner.add( String.join( " ", S.Col.RED, "[FOL]:", S.Col.WHITE, String.valueOf( Math.sqrt( inst.followDistance ) ) ) );
				
				joiner.add( String.join( "", S.Col.GREEN, "Invincible: ", String.valueOf( inst.invincible ),
											   				"  Retaliate: ", String.valueOf( inst.iWillRetaliate ) ) );
				joiner.add( String.join( "", S.Col.GREEN, "Drops Items: ", String.valueOf( inst.dropInventory ),
											   			"  Critical Hits: ", String.valueOf( inst.acceptsCriticals ) ) );
				joiner.add( String.join( "", S.Col.GREEN, "Kills Drop Items: ", String.valueOf( inst.killsDropInventory ),
													"  Respawn Delay: ", String.valueOf( inst.respawnDelay ), "secs" ) );
				joiner.add( String.join( "", S.Col.BLUE, "Status: ", inst.myStatus.toString() ) );
				
				if ( inst.attackTarget == null ) 
					joiner.add( S.Col.BLUE.concat( "Current Target: None" ) );
				else 
					joiner.add( String.join( "", S.Col.BLUE, "Current Target: ", inst.attackTarget.toString() ) );
	
				if ( inst.guardEntity == null )
					joiner.add( S.Col.BLUE.concat( "Guarding: My Surroundings" ) );
				else 		
					joiner.add( String.join( "", S.Col.BLUE, "Guarding: ", inst.guardEntity.toString() ) );
				
				player.sendMessage( joiner.toString() );
			}
			return true;
		}
		//----------------------------------------------------------------target command---------
		if ( S.TARGET.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_TARGET, player ) ) {

				if ( S.LIST.equals( inargs[nextArg + 1] ) ) {
					player.sendMessage( String.join( "", S.Col.GREEN, "Targets: ", inst.validTargets.toString() ) );
					return true;
				}
				if ( S.CLEAR.equals( inargs[nextArg + 1] ) ) {
					
					inst.validTargets.clear();
					inst.targetFlags = 0;
					player.sendMessage( String.join( "", S.Col.GREEN, npcName, ": ALL Targets cleared" ) );
					return true;
				}
				if ( inargs.length > 2 + nextArg ) {
					
					player.sendMessage( parseTargetOrIgnore( inargs, nextArg, npcName, inst, true ) );
					return true;
				}
				player.sendMessage( targetCommandHelp() );
			}
		}
		//--------------------------------------------------------------------------ignore command-----------
		if ( S.IGNORE.equalsIgnoreCase( inargs[nextArg] ) ) {
			
			if ( checkCommandPerm( S.PERM_IGNORE, player ) ) {
				
				if ( S.LIST.equals( inargs[nextArg + 1] ) ) {
					player.sendMessage( String.join( " ", S.Col.GREEN, npcName, "Current Ignores:", inst.ignoreTargets.toString() ) );
					return true;
				}
				if ( S.CLEAR.equals( inargs[nextArg + 1] ) ) {
					
					inst.ignoreTargets.clear();
					inst.ignoreFlags = 0;
					inst.clearTarget();
					player.sendMessage( String.join( "", S.Col.GREEN, npcName, ": ALL Ignores cleared" ) );
					return true;
				}
				if ( inargs.length > 2 + nextArg ) {

					player.sendMessage( parseTargetOrIgnore( inargs, nextArg, npcName, inst, false ) );
					return true;
				}
				player.sendMessage( ignoreCommandHelp() );
			}
		}
		return false; 
	}
	
	private static String parseTargetOrIgnore( String[] inargs, int nextArg, String npcName, 
														SentryInstance inst, boolean forTargets ) {
		
		String[] typeArgs = new String[inargs.length - ( 2 + nextArg )];
		System.arraycopy( inargs, 2 + nextArg, typeArgs, 0, inargs.length - ( 2 + nextArg ) );
		
		if ( Sentry.debug ) Sentry.debugLog( "Target types list is:- " + joinArgs( 0, typeArgs ) );
		
		StringJoiner joiner = new StringJoiner( System.lineSeparator() );
		Set<String> setOfTargets = forTargets ? inst.validTargets : inst.ignoreTargets;
		
		if ( S.ADD.equalsIgnoreCase( inargs[nextArg + 1] ) ) {
	
			for ( String arg : typeArgs ) {
				String[] args = colon.split( arg, 2 );
				
				if ( args.length > 1 ) {
					
					boolean messageSent = false, opSucceeded = false;
					
					plugins:
					for ( PluginBridge each : Sentry.activePlugins.values() ) {
						if ( each.getPrefix().equalsIgnoreCase( args[0] ) ) {
	
							joiner.add( each.add( arg, inst, forTargets ) );
							messageSent = true;
							break plugins;
						}
					}
					if ( setOfTargets.add( arg.toUpperCase() ) )
							opSucceeded = true;
					
					if ( !messageSent ) {
						if ( opSucceeded ) 
							joiner.add( String.join( " ", S.Col.GREEN, npcName,
										  	forTargets ? "Target added. Now targeting:-" 
													   : "Ignore added. Now ignoring:-",
											setOfTargets.toString() ) );
						else 
							joiner.add( String.join( " ", S.Col.GREEN, arg, S.ALLREADY_ON_LIST,
															forTargets ? S.TARGETS : S.IGNORES ) );
					}
				}	
			}
		}
		else if ( S.REMOVE.equalsIgnoreCase( inargs[nextArg + 1] ) ) {
	
			for ( String arg : typeArgs ) {
				String[] args = colon.split( arg, 2 );
				if ( args.length > 1 ) {
	
					boolean messageSent = false, opSucceeded = false;
					
					plugins:
					for ( PluginBridge each : Sentry.activePlugins.values() ) {
						if ( each.getPrefix().equalsIgnoreCase( args[0] ) ) {
	
							joiner.add( each.remove( arg, inst, forTargets ) );
							messageSent = true;
							break plugins;
						}
					}
					if ( setOfTargets.remove( arg.toUpperCase() ) )
							opSucceeded = true;
					
					if ( !messageSent ) {
						if ( opSucceeded ) 
							joiner.add( String.join( " ", S.Col.GREEN, npcName,
									  		forTargets ? "Target removed. Now targeting:-" 
													   : "Ignore removed. Now ignoring:-",
											setOfTargets.toString() ) );
						else 
							joiner.add( String.join( " ", S.Col.GREEN, arg, S.NOT_FOUND_ON_LIST,
															forTargets ? S.TARGETS : S.IGNORES ) );
					}
				}
			}
		}
		
		if ( joiner.toString() == "" ) {
			joiner.add( "Arguments not recognised. Try '/sentry help'" );
		}
		else {
			inst.processTargetStrings( false );
			inst.clearTarget();
		}
		return joiner.toString();		
	}	
}
