package net.aufdemrand.sentry;

import java.text.DecimalFormat;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

public abstract class CommandHandler {
	
	/**
	 * Convenience method to check perms on Command usage.
	 * The method includes informing the player if they lack the required perms.
	 * @param command - The perm node to be checked.
	 * @param player - The sender of the command.
	 * @return true - if the player has the required permission.
	 */
	private static boolean checkCommandPerm( String command, CommandSender player ) {
		
		if ( player.hasPermission( command ) ) return true;
		
		player.sendMessage( ChatColor.RED + "You do not have permission for that command." );
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
		
		player.sendMessage( ChatColor.RED + "Use /sentry help for command reference." );
		return false;
	}
	
	/**
	 * Convenience method to send a formatted message to the player regarding the respawn status of the npc.
	 * 
	 * @param value - the number of seconds set as the respawn value.
	 * @param npc - the npc
	 * @param player - the player who sent the command.
	 */
	private static void respawnMessage( int value, NPC npc, CommandSender player ) {
		
		if ( value == 0 ) 
			player.sendMessage( ChatColor.GOLD + npc.getName() + " will not automatically respawn." );
		if ( value == -1 ) 
			player.sendMessage( ChatColor.GOLD + npc.getName() + " will be deleted upon death" );
		if ( value > 0 ) 
			player.sendMessage( ChatColor.GOLD + npc.getName() + " respawns after " + value + "s" );
	}
	
	/**
	 * Convenience method that removes single and double quotes from the ends of the supplied string.
	 * 
	 * @param input - the string to be parsed
	 * @return - the string without quotes
	 */
	private static String sanitiseString( String input ) {
			return input.replaceAll( "\"$", "" )
						.replaceAll( "^\"", "" )
						.replaceAll( "'$", "" )
						.replaceAll( "^'", "" );
	}
	
	/** 
	 * Concatenates the supplied String[] starting at the position indicated.
	 * 
	 * @param startFrom - the starting position (zero-based)
	 * @param args - the String[] to be joined
	 * @return - the resulting String.
	 */
	private static String joinArgs( int startFrom, String[] args ) {
		String out = "";

		for ( int i = startFrom; i < args.length; i++ ) {
			out += " " + args[i];
		}
		return out.trim();
	}
//------------------------------------------------------------------------------------	
//------------------------------------------------------------------------------------
	/**
	 * The main method of this class, that parses the arguments and responds accordingly 
	 * 
	 * @param player
	 * @param inargs
	 * @param sentry
	 * @return - true if the command has been successfully handled.
	 */
	static boolean call( CommandSender player, String[] inargs, Sentry sentry ) {
		
		if ( !enoughArgs( 1, inargs, player ) ) return true;

//----------------------------------------------------- help command -----------------
		if ( inargs[0].equalsIgnoreCase( "help" ) ) {

			player.sendMessage(ChatColor.GOLD + "------- Sentry Commands -------");
			player.sendMessage(ChatColor.GOLD + "You can use /sentry (id) [command] [args] to perform any of these "
																	+ "commands on a sentry without having it selected.");			
			player.sendMessage(ChatColor.GOLD + "");
			
			if ( checkCommandPerm( "sentry.reload", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry reload");
				player.sendMessage(ChatColor.GOLD + "  reload the config.yml");
			}
			if ( checkCommandPerm( "sentry.target", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry target [add|remove] [target]");
				player.sendMessage(ChatColor.GOLD + "  Adds or removes a target to attack.");			
				player.sendMessage(ChatColor.GOLD + "/sentry target [list|clear]");
				player.sendMessage(ChatColor.GOLD + "  View or clear the target list..");
			}
			if ( checkCommandPerm( "sentry.ignore", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry ignore [add|remove] [target]");
				player.sendMessage(ChatColor.GOLD + "  Adds or removes a target to ignore.");			
				player.sendMessage(ChatColor.GOLD + "/sentry ignore [list|clear]");
				player.sendMessage(ChatColor.GOLD + "  View or clear the ignore list..");
			}
			if ( checkCommandPerm( "sentry.info", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry info");
				player.sendMessage(ChatColor.GOLD + "  View all Sentry attributes");
			}
			if ( checkCommandPerm( "sentry.equip", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry equip [item|none]");
				player.sendMessage(ChatColor.GOLD + "  Equip an item on the Sentry, or remove all equipment.");
			}
			if ( checkCommandPerm( "sentry.stats.speed", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry speed [0-1.5]");
				player.sendMessage(ChatColor.GOLD + "  Sets speed of the Sentry when attacking.");
			}
			if ( checkCommandPerm( "sentry.stats.health", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry health [1-2000000]");
				player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Health .");
			}
			if ( checkCommandPerm( "sentry.stats.armor", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry armor [0-2000000]");
				player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Armor.");
			}
			if ( checkCommandPerm( "sentry.stats.strength", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry strength [0-2000000]");
				player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Strength.");
			}
			if ( checkCommandPerm( "sentry.stats.attackrate", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry attackrate [0.0-30.0]");
				player.sendMessage(ChatColor.GOLD + "  Sets the time between the Sentry's projectile attacks.");
			}
			if ( checkCommandPerm( "sentry.stats.healrate", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry healrate [0.0-300.0]");
				player.sendMessage(ChatColor.GOLD + "  Sets the frequency the sentry will heal 1 point. 0 to disable.");
			}
			if ( checkCommandPerm( "sentry.stats.range", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry range [1-100]");
				player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's detection range.");
			}
			if ( checkCommandPerm( "sentry.stats.warningrange", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry warningrange [0-50]");
				player.sendMessage(ChatColor.GOLD + "  Sets the range, beyond the detection range, that the Sentry will warn targets.");
			}
			if ( checkCommandPerm( "sentry.stats.respawn", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry respawn [-1-2000000]");
				player.sendMessage(ChatColor.GOLD + "  Sets the number of seconds after death the Sentry will respawn.");
			}
			if ( checkCommandPerm( "sentry.stats.follow", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry follow [0-32]");
				player.sendMessage(ChatColor.GOLD + "  Sets the number of block away a bodyguard will follow. Default is 4");
			}
			if ( checkCommandPerm( "sentry.options.invincible", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry invincible");
				player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take no damage or knockback.");
			}
			if ( checkCommandPerm( "sentry.options.retaliate", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry retaliate");
				player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to always attack an attacker.");
			}
			if ( checkCommandPerm( "sentry.options.criticals", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry criticals");
				player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take critical hits and misses");
			}
			if ( checkCommandPerm( "sentry.options.drops", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry drops");
				player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to drop equipped items on death");
			}
			if ( checkCommandPerm( "sentry.options.killdrops", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry killdrops");
				player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the sentry's victims drop items and exp");
			}
			if ( checkCommandPerm( "sentry.options.mount", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry mount");
				player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the sentry rides a mount");
			}
			if ( checkCommandPerm( "sentry.options.targetable", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry targetable");
				player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the sentry is attacked by hostile mobs");
			}
			if ( checkCommandPerm( "sentry.spawn", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry spawn");
				player.sendMessage(ChatColor.GOLD + "  Set the sentry to respawn at its current location");
			}
			if ( checkCommandPerm( "sentry.warning", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry warning <text to use>");
				player.sendMessage(ChatColor.GOLD + "  Change the warning text. <NPC> and <PLAYER> can be used as placeholders");
			}
			if ( checkCommandPerm( "sentry.greeting", player ) ) {
				player.sendMessage(ChatColor.GOLD + "/sentry greeting <text to use>");
				player.sendMessage(ChatColor.GOLD + "  Change the greeting text. <NPC> and <PLAYER> can be used as placeholders");
			}
			return true;
		}
//---------------------------------------------------------- Debug Command --------------
		if ( inargs[0].equalsIgnoreCase( "debug" ) ) {
			
			if ( checkCommandPerm( "sentry.debug", player ) ) {

				Sentry.debug = !Sentry.debug;
				player.sendMessage( ChatColor.GREEN + "Debug is now: " + ( Sentry.debug ? "On"
																						: "Off" ) );
			}
			return true;
		}
//---------------------------------------------------------- Reload Command ------------
		if ( inargs[0].equalsIgnoreCase( "reload" ) ) {
			
			if ( checkCommandPerm( "sentry.reload", player ) ) {

				sentry.reloadMyConfig();
				player.sendMessage( ChatColor.GREEN + "reloaded Sentry/config.yml" );
			}
			return true;
		}
		
//-------------------------------------------------------------------------------------
		// the remaining commands all deal with npc's
		
		
		// did player specify an integer as the first argument?  
		// It will be checked later on to see if it is a valid npc id, for now we just store it and move on.		
		int npcid = Util.string2Int( inargs[0] );
		
		// i is set to 1 only if an int was found above.
//		int i = ( npcid > 0 ) ? 1 : 0;
		
		int nextArg = ( npcid > 0 ) ? 1 
									: 0;
// surely this is unnecessary?
//		
//		// create a new array of args that is re-based according to whether an npc id was found above.
//		String[] args = new String[ inargs.length - i ];
//
//		for ( int j = i; j < inargs.length; j++ ) {
//			args[ j - i ] = inargs[ j ];
//		}

		if ( !enoughArgs( 1 + nextArg, inargs, player ) ) return true;

		// hold the state of the third argument (if it holds a boolean value) in a field for later use.
		// this is held as an object not a primitive to allow for a third state - 'null'.
		Boolean set = null;
		
		if ( inargs.length == 2 + nextArg ) {
			if ( inargs[1 + nextArg].equalsIgnoreCase( "true" ) || inargs[1 + nextArg].equalsIgnoreCase( "on" ) ) 
					set = true;
			if ( inargs[1 + nextArg].equalsIgnoreCase( "false" ) || inargs[1 + nextArg].equalsIgnoreCase( "off" ) ) 
					set = false;
		}
		NPC thisNPC;
		
		// check to see whether the value saved earlier is an npc ID, and save a reference if so.
		if ( npcid == -1 ) {
			
			thisNPC = ((Citizens) sentry.pluginManager.getPlugin( "Citizens" ))
													  .getNPCSelector()
													  .getSelected( player );
			if ( thisNPC == null ) {
				player.sendMessage( ChatColor.RED + "You must provide an NPC #id "
												+ "or have an NPC selected to use this command" );
				return true;
			}	
			npcid = thisNPC.getId();
		} 
		else {
			thisNPC = CitizensAPI.getNPCRegistry().getById( npcid ); 
	
			if ( thisNPC == null ) {
				player.sendMessage( ChatColor.RED + "An NPC with #id " + npcid + " was not found" );
				return true;
			}
		}
		// We are now sure that thisNPC refers to a valid npc instance, and that npcid contains its id.
        // lets check that the specified npc has the sentry trait.
		
		if ( !thisNPC.hasTrait( SentryTrait.class ) ) {
			player.sendMessage( ChatColor.RED + "That command can only be used on a Sentry." );
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
				
				if ( !((Player) player).hasPermission( "citizens.admin" ) ) {
					// player is not an admin either.
					
					player.sendMessage( ChatColor.RED + "You must be the owner of this Sentry to execute commands." );
					return true;
				}
				if ( !thisNPC.getTrait( Owner.class ).getOwner().equalsIgnoreCase( "server" ) ) {
					// not server-owned NPC
					
					player.sendMessage( ChatColor.RED + "You, or the server, must be the owner of this Sentry to execute commands." );
					return true;
				}
			}
		} 
		
		// We now know that player is either the owner, or an admin with a server-owned npc.
		// lets save a reference to the SentryInstance of the npc before continuing.
		SentryInstance inst = thisNPC.getTrait( SentryTrait.class ).getInstance();
		
//------------------------------------------------------------ spawn command --------------		
		if ( inargs[nextArg].equalsIgnoreCase( "spawn" ) ) {
			
			if ( checkCommandPerm( "sentry.spawn", player ) ) {
			
				if ( thisNPC.getEntity() == null ) {
					player.sendMessage( ChatColor.RED + "Cannot set spawn while " +  thisNPC.getName()  + " is dead." );
					return true;
				}
				inst.spawnLocation = thisNPC.getEntity().getLocation();
				player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " will respawn at its present location." );
			}
			return true;
		}

//----------------------------------------------------------- invincible command ------------
		if ( inargs[nextArg].equalsIgnoreCase( "invincible" ) ) {
			
			if ( checkCommandPerm( "sentry.options.invincible", player ) ) {
				
				inst.invincible = ( set == null ) ? !inst.invincible 
											  	  : set;
				
				if ( inst.invincible )
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " now INVINCIBLE." );
				else 
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " now takes damage.." );
			}
			return true;
		}

//------------------------------------------------------------- retaliate command --------------
		if ( inargs[nextArg].equalsIgnoreCase( "retaliate" ) ) {
			
			if ( checkCommandPerm( "sentry.options.retaliate", player ) ) {
				
				inst.iWillRetaliate = ( set == null ) ? !inst.iWillRetaliate 
													  : set;
	
				if ( inst.iWillRetaliate ) 
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " will retalitate against all attackers." );
				else
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " will not retaliate." );
			}
			return true;
		}
		
//--------------------------------------------------------------- criticals command -------------		
		if ( inargs[nextArg].equalsIgnoreCase( "criticals" ) ) {
			
			if ( checkCommandPerm( "sentry.options.criticals", player ) ) {

				inst.acceptsCriticals = ( set == null ) ? !inst.acceptsCriticals
												 : set;
	
				if ( inst.acceptsCriticals ) 
					player.sendMessage( ChatColor.GREEN +thisNPC.getName() + " will take critical hits." );
				else
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " will take normal damage." );
			}
			return true;
		}
		
//-------------------------------------------------------------- drops command ----------------		
		if ( inargs[nextArg].equalsIgnoreCase( "drops" ) ) {
			
			if ( checkCommandPerm( "sentry.options.drops", player ) ) {
				
				inst.dropInventory = ( set == null ) ? !inst.dropInventory
													 : set;
	
				if ( inst.dropInventory )
					player.sendMessage( ChatColor.GREEN +  thisNPC.getName() + " will drop items" ); 
				else
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " will not drop items." );
			}
			return true;
		}
		
//--------------------------------------------------------------- killdrops command -------------
		if ( inargs[nextArg].equalsIgnoreCase( "killdrops" ) ) {
			
			if ( checkCommandPerm( "sentry.options.killdrops", player ) ) {

				inst.killsDropInventory =  ( set == null ) ? !inst.killsDropInventory 
														   : set;
	
				if ( inst.killsDropInventory )
					player.sendMessage( ChatColor.GREEN +  thisNPC.getName() + "'s kills will drop items or exp" );
				else
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + "'s kills will not drop items or exp." );
			}
			return true;
		}
		
//-----------------------------------------------------------------targetable command ------------
		if ( inargs[nextArg].equalsIgnoreCase( "targetable" ) ) {
			
			if ( checkCommandPerm( "sentry.options.targetable", player ) ) {

				inst.targetable = ( set == null ) ? !inst.targetable
												  : set;
				
				thisNPC.data().set( NPC.TARGETABLE_METADATA, inst.targetable );
	
				if ( inst.targetable ) 
					player.sendMessage( ChatColor.GREEN +  thisNPC.getName() + " will be targeted by mobs" );
				else
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " will not be targeted by mobs" );
			}
			return true;
		}	
//-------------------------------------------------------------------mount command -----------		
		if ( inargs[nextArg].equalsIgnoreCase( "mount" ) ) {
			
			if ( checkCommandPerm( "sentry.options.mount", player ) ) {
	
				set = ( set == null ) ? !inst.isMounted() 
									  : set;
				if ( set ) {
					player.sendMessage( ChatColor.GREEN +  thisNPC.getName() + " is now Mounted" );
					inst.createMount();
					inst.mount();
				}
				else {
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " is no longer Mounted" );
					if ( inst.isMounted() ) Util.removeMount( inst.mountID );	
					inst.mountID = -1;
				}
			}
			return true;
		}
//------------------------------------------------------------------guard command -------------	
		
		//TODO add help text for this command.
		if ( inargs[nextArg].equalsIgnoreCase( "guard" ) ) { 
			
			if ( checkCommandPerm( "sentry.guard", player ) ) {

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
//					String arg = "";
//					for ( int i = start + nextArg; i < inargs.length; i++ ) {
//						arg += " " + inargs[i];
//					}
//					arg = arg.trim();
	
					
					if ( !playersonly ){ 
						ok = inst.findGuardEntity( arg, false );
					}
	
					if ( !localonly ) {
						ok = inst.findGuardEntity( arg, true );
					}
	
					if ( ok )
						player.sendMessage( ChatColor.GREEN +  thisNPC.getName() + " is now guarding "+ arg );   
					else 
						player.sendMessage( ChatColor.RED +  thisNPC.getName() + " could not find " + arg + "." );   
					return true;
				}
				
				if ( inst.guardTarget == null )
					player.sendMessage( ChatColor.RED +  thisNPC.getName() + " is already set to guard its immediate area" );   	
				else
					player.sendMessage( ChatColor.GREEN +  thisNPC.getName() + " is now guarding its immediate area. " );
				
				inst.findGuardEntity( null, false );
			}
			return true;
		}
//---------------------------------------------------------------follow command -------------------
		if ( inargs[nextArg].equalsIgnoreCase( "follow" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.follow", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Follow Distance is " + inst.followDistance );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry follow [#]. Default is 4. " );
				}
				else {
					int dist = Util.string2Int( inargs[nextArg + 1] );
					if ( dist < 0 ) {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1]
														+ "' was not recognised as a valid number." );
						return true;
					}

					if ( dist > 32 ) dist = 32;
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " follow distance set to " + dist + "." );   
					inst.followDistance = dist * dist;
				}
			}
			return true;
		}
//----------------------------------------------------------------health command -------------------
		if ( inargs[nextArg].equalsIgnoreCase( "health" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.health", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Health is " + inst.sentryMaxHealth );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry health [#]   note: Typically players" );
					player.sendMessage( ChatColor.GOLD + "  have 20 HPs when fully healed" );
				}
				else {
					int HPs = Util.string2Int( inargs[nextArg + 1] );
					if ( HPs < 1 )  {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1]
														+ "' was not recognised as a valid number." );
						return true;
					}

					if ( HPs > 2000000 ) HPs = 2000000;
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " health set to " + HPs + "." );  
					inst.sentryMaxHealth = HPs;
					inst.setHealth( HPs );
				}
			}
			return true;
		}
//---------------------------------------------------------------armour command-----------------------
		if ( inargs[nextArg].equalsIgnoreCase( "armor" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.armor", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Armor is " + inst.armorValue );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry armor [#] " );
				}
				else {
					int armour = Util.string2Int( inargs[nextArg + 1] );
					if ( armour < 0 )  {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1]
														+ "' was not recognised as a valid number." );
						return true;
					}

					if ( armour > 2000000 ) armour = 2000000;
	
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " armor set to " + armour + "." );   
					inst.armorValue = armour;
				}
			}
			return true;
		}
//----------------------------------------------------------------strength command --------------		
		if ( inargs[nextArg].equalsIgnoreCase( "strength" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.strength", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Strength is " + inst.strength );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry strength # " );
					player.sendMessage( ChatColor.GOLD + "Note: At strength 0 the Sentry will do no damamge. " );
				}
				else {
					int strength = Util.string2Int( inargs[nextArg + 1] );
					if ( strength < 0 )  {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1]
														+ "' was not recognised as a valid number." );
						return true;
					}
					
					if ( strength > 2000000 ) strength = 2000000;	
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " strength set to " + strength+ "." );  
					inst.strength = strength;
				}
			}
			return true;
		}
//-----------------------------------------------------------------nightvision command---------		
		//TODO add help text for this command
		if ( inargs[nextArg].equalsIgnoreCase( "nightvision" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.nightvision", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Night Vision is " + inst.nightVision );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry nightvision [0-16] " );
					player.sendMessage( ChatColor.GOLD + "Usage: 0 = See nothing, 16 = See everything. " );
				}
				else {
					int vision = Util.string2Int( inargs[nextArg + 1] );
					if ( vision < 0 )  {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1]
														+ "' was not recognised as a valid number." );
						return true;
					}
					if ( vision > 16 ) vision = 16;
	
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " Night Vision set to " + vision+ "." );   
					inst.nightVision = vision;
				}
			}
			return true;
		}
//-----------------------------------------------------------respawn command------------------------
		if ( inargs[nextArg].equalsIgnoreCase( "respawn" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.respawn", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					respawnMessage( inst.respawnDelay, thisNPC, player );
					
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry respawn [-1 - 2000000] " );
					player.sendMessage( ChatColor.GOLD + "Usage: set to 0 to prevent automatic respawn" );
					player.sendMessage( ChatColor.GOLD + "Usage: set to -1 to *permanently* delete the Sentry on death." );
				}
				else {
					int respawn = Util.string2Int( inargs[nextArg + 1] );
					if ( respawn < -1 )  {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1] 
													+ "' was not recognised as a valid value." );
						return true;
					}
					
					if ( respawn > 2000000 ) respawn = 2000000;
					inst.respawnDelay = respawn;
					respawnMessage( inst.respawnDelay, thisNPC, player );
				}
			}
			return true;
		}
//----------------------------------------------------------speed command--------------------------
		if ( inargs[nextArg].equalsIgnoreCase( "speed" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.speed", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Speed is " + inst.sentrySpeed );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry speed [0.0 - 2.0]" );
				}
				else {
					float speed = Util.string2Float( inargs[nextArg + 1] );
					if ( speed < 0.0 ) {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1] 
														+ "' was not recognised as a valid value." );
						return true;
					}

					if ( speed > 2.0 ) speed = 2.0f;
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " speed set to " + speed + "." );   
					inst.sentrySpeed = speed;
				}
			}
			return true;
		}
//-----------------------------------------------------------attackrate command ---------		
		if ( inargs[nextArg].equalsIgnoreCase( "attackrate" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.attackrate", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Projectile Attack Rate is " 
																		+ inst.attackRate + "seconds between shots." );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry attackrate [0.0 - 30.0]");
				}
				else {
					double attackrate = Util.string2Double( inargs[nextArg + 1] );
					if ( attackrate < 0.0 ) {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1] 
														+ "' was not recognised as a valid value." );
						return true;
					}

					if ( attackrate > 30.0 ) attackrate = 30.0;
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " Projectile Attack Rate set to " + attackrate + "." );  
					inst.attackRate = attackrate;
				}
			}
			return true;
		}
//------------------------------------------------------------------healrate command-----------------
		if ( inargs[nextArg].equalsIgnoreCase( "healrate" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.healrate", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Heal Rate is " + inst.healRate + "s" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry healrate [0.0 - 300.0]" );
					player.sendMessage( ChatColor.GOLD + "Usage: Set to 0 to disable healing" );
				}
				else {
					double healrate = Util.string2Double( inargs[nextArg + 1] );
					if ( healrate < 0.0 ) {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1] 
														+ "' was not recognised as a valid value." );
						return true;
					}

					if ( healrate > 300.0 ) healrate = 300.0;
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " Heal Rate set to " + healrate + "." );   
					inst.healRate = healrate;
				}
			}
			return true;
		}
//-------------------------------------------------------------------range command-----------------
		if ( inargs[nextArg].equalsIgnoreCase( "range" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.range", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Range is " + inst.sentryRange );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry range [1 - 100]" );
				}
				else {
					int range = Util.string2Int( inargs[nextArg + 1] );
					if ( range < 1 ) {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1] 
														+ "' was not recognised as a valid value." );
						return true;
					}

					if ( range > 100 ) range = 100;
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " range set to " + range + "." );   
					inst.sentryRange = range;
				}
			}
			return true;
		}
//----------------------------------------------------------------------warningrange command----------
		if ( inargs[nextArg].equalsIgnoreCase( "warningrange" ) ) {
			
			if ( checkCommandPerm( "sentry.stats.warningrange", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Warning Range is " + inst.warningRange );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry warningrangee [0 - 50]" );
				}
				else {
					int range = Util.string2Int( inargs[nextArg + 1] );
					if ( range < 0 ) {
						player.sendMessage( ChatColor.RED + "Error: '" + inargs[nextArg + 1] 
														+ "' was not recognised as a valid value." );
						return true;
					}

					if ( range > 50 ) range = 50;
					player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " warning range set to " + range + ".");   
					inst.warningRange = range;
				}
			}
			return true;
		}
//--------------------------------------------------------------------------equip command-------------		
		if ( inargs[nextArg].equalsIgnoreCase( "equip" ) ) {
			
			if ( checkCommandPerm( "sentry.equip", player ) ) {

				if ( inargs.length <= 1 + nextArg ) {
					player.sendMessage( ChatColor.RED + "You must specify a valid Item Name. "
														+ "Specify 'none' to remove all equipment." );
				}
				else if ( thisNPC.getEntity().getType() == EntityType.ENDERMAN 
							|| thisNPC.getEntity().getType() == EntityType.PLAYER ) {
						
					if ( inargs[nextArg + 1].equalsIgnoreCase( "none" ) ) {
						
						//remove equipment
						sentry.equip( thisNPC, inst, null );
						player.sendMessage( ChatColor.YELLOW +thisNPC.getName() + "'s equipment cleared." ); 
					}
					else {
						Material mat = Util.getMaterial( inargs[nextArg + 1] );
						
						if ( mat == null ) {
							player.sendMessage( ChatColor.RED + " Could not equip: unknown item name" ); 
							return true;
						}
						
						ItemStack item = new ItemStack( mat );
						
						if ( sentry.equip( thisNPC, inst, item ) ) 
							player.sendMessage( ChatColor.GREEN + " equipped " + mat.toString() + " on " + thisNPC.getName() ); 
						else 
							player.sendMessage( ChatColor.RED + " Could not equip: invalid mob type?" ); 
					}
				}
				else player.sendMessage( ChatColor.RED + " Could not equip: must be Player or Enderman type." );
			}
			return true;
		}
//--------------------------------------------------------------------warning command-----------		
		if ( inargs[nextArg].equalsIgnoreCase( "warning" ) ) {
			
			if ( checkCommandPerm( "sentry.warning", player ) ) {

				if ( inargs.length >= 2 + nextArg ) {
					
//					String arg = "";
//					for ( int i = 1 + nextArg; i < inargs.length; i++ ) {
//						arg += " " + inargs[i];
//					}
//					arg = arg.trim();
	
					String str = sanitiseString( joinArgs( 1 + nextArg, inargs ) );
					
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " warning message set to " 
										+ ChatColor.RESET + ChatColor.translateAlternateColorCodes( '&', str ) + "." );   
					inst.warningMsg = str;
				}
				else {
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Warning Message is: " 
										+ ChatColor.RESET + ChatColor.translateAlternateColorCodes( '&', inst.warningMsg ) );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry warning 'The Text to use'" );
				}
			}
			return true;
		}
//--------------------------------------------------------------------greeting command----------
		if ( inargs[nextArg].equalsIgnoreCase( "greeting" ) ) {
			
			if ( checkCommandPerm( "sentry.greeting", player ) ) {
			
				if ( inargs.length >= 2 + nextArg ) {
	
//					String arg = "";
//					for ( int i = 1 + nextArg; i < inargs.length; i++ ) {
//						arg += " " + inargs[i];
//					}
//					arg = arg.trim();
	
					String str = sanitiseString( joinArgs( 1 + nextArg, inargs ) );
					
					player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Greeting message set to " 
										+ ChatColor.RESET + ChatColor.translateAlternateColorCodes( '&', str ) + "." );   
					inst.greetingMsg = str;
				}
				else{
					player.sendMessage( ChatColor.GOLD + thisNPC.getName() + "'s Greeting Message is: " 
										+ ChatColor.RESET + ChatColor.translateAlternateColorCodes( '&', inst.greetingMsg ) );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry greeting 'The Text to use'");
				}
			}
			return true;
		}
//---------------------------------------------------------------------info command-----------
		if ( inargs[nextArg].equalsIgnoreCase( "info" ) ) {
			
			if ( checkCommandPerm( "sentry.info", player ) ) {

				player.sendMessage( ChatColor.GOLD + "------- Sentry Info for (" + thisNPC.getId() + ") " 
																				 + thisNPC.getName() + "------" );
				player.sendMessage( ChatColor.RED + "[HP]:" + ChatColor.WHITE + inst.getHealth() + "/" + inst.sentryMaxHealth + 
									ChatColor.RED + " [AP]:" + ChatColor.WHITE + inst.getArmor() +
									ChatColor.RED + " [STR]:" + ChatColor.WHITE + inst.getStrength() + 
									ChatColor.RED + " [SPD]:" + ChatColor.WHITE + new DecimalFormat( "#.0" ).format( inst.getSpeed() ) +
									ChatColor.RED + " [RNG]:" + ChatColor.WHITE + inst.sentryRange + 
									ChatColor.RED + " [ATK]:" + ChatColor.WHITE + inst.attackRate + 
									ChatColor.RED + " [VIS]:" + ChatColor.WHITE + inst.nightVision +
									ChatColor.RED + " [HEAL]:" + ChatColor.WHITE + inst.healRate + 
									ChatColor.RED + " [WARN]:" + ChatColor.WHITE + inst.warningRange + 
									ChatColor.RED + " [FOL]:" + ChatColor.WHITE + Math.sqrt( inst.followDistance ) );
				player.sendMessage( ChatColor.GREEN + "Invincible: " + inst.invincible 
													+ "  Retaliate: " + inst.iWillRetaliate );
				player.sendMessage( ChatColor.GREEN + "Drops Items: " + inst.dropInventory 
													+ "  Critical Hits: " + inst.acceptsCriticals );
				player.sendMessage( ChatColor.GREEN + "Kills Drop Items: "+ inst.killsDropInventory 
													+ "  Respawn Delay: " + inst.respawnDelay + "s" );
				player.sendMessage( ChatColor.BLUE 	+ "Status: " + inst.myStatus);
				
				if ( inst.meleeTarget != null ) 
					player.sendMessage( ChatColor.BLUE + "Target: " + inst.meleeTarget.toString() );
				else if ( inst.projectileTarget != null ) 
					player.sendMessage( ChatColor.BLUE + "Target: " + inst.projectileTarget.toString() );
				else 
					player.sendMessage( ChatColor.BLUE + "Target: Nothing" );
	
				if ( inst.getGuardTarget() == null )
					player.sendMessage( ChatColor.BLUE + "Guarding: My Surroundings" );
				else 		
					player.sendMessage( ChatColor.BLUE + "Guarding: " + inst.getGuardTarget().toString() );
			}
			return true;
		}
//----------------------------------------------------------------target command---------
		if ( inargs[nextArg].equalsIgnoreCase( "target" ) ) {
			
			if ( checkCommandPerm( "sentry.target", player ) ) {

				if ( inargs.length < 2 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry target add [entity:Name] or [player:Name] or "
																+ "[group:Name] or [entity:monster] or [entity:player]" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry target remove [target]" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry target clear" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry target list" );
					return true;
				}
				
				String arg = joinArgs( 2 + nextArg, inargs );
//				String arg = "";
//				for ( int i = 2 + nextArg; i < inargs.length; i++ ) {
//					arg += " " + inargs[i];
//				}
//				arg = arg.trim();
	
				if ( arg.equalsIgnoreCase( "nationenemies" ) && inst.myNPC.isSpawned() ) {
					String natname = TownyBridge.getNationNameForLocation( inst.myNPC.getEntity().getLocation() );
					if ( natname != null ) {
						arg += ":" + natname;
					}
					else 	{
						player.sendMessage( ChatColor.RED + "Could not get Nation for this NPC's location" );
						return true;
					}
				}
				if ( inargs[nextArg + 1].equals( "add" ) && arg.length() > 0 && arg.split( ":" ).length > 1 ) {
	
					inst.validTargets.add( arg.toUpperCase() );
					inst.processTargets();
					inst.clearTarget();
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " Target added. Now targeting " 
																				+ inst.validTargets.toString() );
				}
				else if ( inargs[nextArg + 1].equals( "remove" ) && arg.length() > 0 && arg.split( ":" ).length > 1 ) {
					
					inst.validTargets.remove( arg.toUpperCase() );
					inst.processTargets();
					inst.clearTarget();
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " Targets removed. Now targeting " 
																				+ inst.validTargets.toString() );
				}
				else if ( inargs[nextArg + 1].equals( "clear" ) ) {
					
					inst.validTargets.clear();
					inst.processTargets();
					inst.clearTarget();
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " Targets cleared." );
				}
				else if ( inargs[nextArg + 1].equals( "list" ) ) {
					player.sendMessage( ChatColor.GREEN + "Targets: " + inst.validTargets.toString() );
				}
				else {
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry target list" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry target clear" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry target add type:name" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry target remove type:name" );
					player.sendMessage( ChatColor.GOLD + "type:name can be any of the following: entity:MobName entity:monster "
													+ "entity:player entity:all player:PlayerName group:GroupName town:TownName "
													+ "nation:NationName faction:FactionName" );
				}
			}
			return true;
		}
//--------------------------------------------------------------------------ignore command-----------
		if ( inargs[nextArg].equalsIgnoreCase( "ignore" ) ) {
			
			if ( checkCommandPerm( "sentry.ignore", player ) ) {

				if ( inargs.length < 2 + nextArg ) {
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry ignore list" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry ignore clear" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry ignore add type:name" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry ignore remove type:name" );
					player.sendMessage( ChatColor.GOLD + "type:name can be any of the following: entity:MobName "
													+ "entity:monster entity:player entity:all player:PlayerName "
													+ "group:GroupName town:TownName nation:NationName faction:FactionName");
					return true;
				}
				String arg = joinArgs( 2 + nextArg, inargs );
//				String arg = "";
//				for ( int i = 2 + nextArg; i < inargs.length; i++ ) {
//					arg += " " + inargs[i];
//				}
//				arg = arg.trim();
	
				if ( inargs[nextArg + 1].equals( "add" ) && arg.length() > 0 && arg.split( ":" ).length > 1 ) {
					
					inst.ignoreTargets.add( arg.toUpperCase() );
					inst.processTargets();
					inst.clearTarget();
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " Ignore added. Now ignoring " 
																			+ inst.ignoreTargets.toString() );
				}
				else if ( inargs[nextArg + 1].equals( "remove" ) && arg.length() > 0 && arg.split( ":" ).length > 1 ) {
	
					inst.ignoreTargets.remove( arg.toUpperCase() );
					inst.processTargets();
					inst.clearTarget();
					player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Ignore removed. Now ignoring " 
																			+ inst.ignoreTargets.toString());
				}
				else if ( inargs[nextArg + 1].equals( "clear" ) ) {
	
					inst.ignoreTargets.clear();
					inst.processTargets();
					inst.clearTarget();
					player.sendMessage( ChatColor.GREEN + thisNPC.getName() + " Ignore cleared." );
				}
				else if ( inargs[nextArg + 1].equals( "list" ) ) {
	
					player.sendMessage( ChatColor.GREEN + "Ignores: " + inst.ignoreTargets.toString() );
				}
				else {
	
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry ignore add [ENTITY:Name] or [PLAYER:Name] or "
																					+ "[GROUP:Name] or [ENTITY:MONSTER]" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry ignore remove [ENTITY:Name] or [PLAYER:Name] or "
																					+ "[GROUP:Name] or [ENTITY:MONSTER]" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry ignore clear" );
					player.sendMessage( ChatColor.GOLD + "Usage: /sentry ignore list" );
				}
			}
			return true;
		}
		return false;
	}
}
