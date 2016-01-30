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
		
		if ( player.hasPermission( command ) ) 
			return true;
		
		player.sendMessage( ChatColor.RED + "You do not have permission for that command." );
		return false;
	}
	
	/** The main method of this class, that parses the arguments and responds accordingly */
	static boolean call( CommandSender player, String[] inargs, Sentry sentry ) {
		
		// send short help message if no arguments provided.
		if ( inargs.length < 1 ) {
			player.sendMessage( ChatColor.RED + "Use /sentry help for command reference." );
			return true;
		}
		
		// did player specify an integer as the first argument?  
		// It will be checked later on to see if it is a valid npc id, for now we just store it and move on.		
		int npcid = Util.string2Int( inargs[0] );
		
		// i is set to 1 only if an int was found above.
		int i = ( npcid > 0 ) ? 1 : 0;

		// create a new array of args that is re-based according to whether an npc id was found above.
		String[] args = new String[ inargs.length - i ];

		for ( int j = i; j < inargs.length; j++ ) {
			args[ j - i ] = inargs[ j ];
		}

        // send short help message if no other arguments provided.
		if ( args.length < 1 ) {
			player.sendMessage( ChatColor.RED + "Use /sentry help for command reference." );
			return true;
		}

		// hold the state of the third argument (if present) in a boolean for later use.
		Boolean set = null;
		if ( args.length == 2 ) {
			if ( args[1].equalsIgnoreCase( "true" ) ) set = true;
			if ( args[1].equalsIgnoreCase( "false" ) ) set = false;
		}

//----------------------------------------------------- help command -----------------
		if ( args[0].equalsIgnoreCase( "help" ) ) {

			player.sendMessage(ChatColor.GOLD + "------- Sentry Commands -------");
			player.sendMessage(ChatColor.GOLD + "You can use /sentry (id) [command] [args] to perform any of these commands on a sentry without having it selected.");			
			player.sendMessage(ChatColor.GOLD + "");
			player.sendMessage(ChatColor.GOLD + "/sentry reload");
			player.sendMessage(ChatColor.GOLD + "  reload the config.yml");
			player.sendMessage(ChatColor.GOLD + "/sentry target [add|remove] [target]");
			player.sendMessage(ChatColor.GOLD + "  Adds or removes a target to attack.");
			player.sendMessage(ChatColor.GOLD + "/sentry target [list|clear]");
			player.sendMessage(ChatColor.GOLD + "  View or clear the target list..");
			player.sendMessage(ChatColor.GOLD + "/sentry ignore [add|remove] [target]");
			player.sendMessage(ChatColor.GOLD + "  Adds or removes a target to ignore.");
			player.sendMessage(ChatColor.GOLD + "/sentry ignore [list|clear]");
			player.sendMessage(ChatColor.GOLD + "  View or clear the ignore list..");
			player.sendMessage(ChatColor.GOLD + "/sentry info");
			player.sendMessage(ChatColor.GOLD + "  View all Sentry attributes");
			player.sendMessage(ChatColor.GOLD + "/sentry equip [item|none]");
			player.sendMessage(ChatColor.GOLD + "  Equip an item on the Sentry, or remove all equipment.");
			player.sendMessage(ChatColor.GOLD + "/sentry speed [0-1.5]");
			player.sendMessage(ChatColor.GOLD + "  Sets speed of the Sentry when attacking.");
			player.sendMessage(ChatColor.GOLD + "/sentry health [1-2000000]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Health .");
			player.sendMessage(ChatColor.GOLD + "/sentry armor [0-2000000]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Armor.");
			player.sendMessage(ChatColor.GOLD + "/sentry strength [0-2000000]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Strength.");
			player.sendMessage(ChatColor.GOLD + "/sentry attackrate [0.0-30.0]");
			player.sendMessage(ChatColor.GOLD + "  Sets the time between the Sentry's projectile attacks.");
			player.sendMessage(ChatColor.GOLD + "/sentry healrate [0.0-300.0]");
			player.sendMessage(ChatColor.GOLD + "  Sets the frequency the sentry will heal 1 point. 0 to disable.");
			player.sendMessage(ChatColor.GOLD + "/sentry range [1-100]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's detection range.");
			player.sendMessage(ChatColor.GOLD + "/sentry warningrange [0-50]");
			player.sendMessage(ChatColor.GOLD + "  Sets the range, beyond the detection range, that the Sentry will warn targets.");
			player.sendMessage(ChatColor.GOLD + "/sentry respawn [-1-2000000]");
			player.sendMessage(ChatColor.GOLD + "  Sets the number of seconds after death the Sentry will respawn.");
			player.sendMessage(ChatColor.GOLD + "/sentry follow [0-32]");
			player.sendMessage(ChatColor.GOLD + "  Sets the number of block away a bodyguard will follow. Default is 4");
			player.sendMessage(ChatColor.GOLD + "/sentry invincible");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take no damage or knockback.");
			player.sendMessage(ChatColor.GOLD + "/sentry retaliate");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to always attack an attacker.");
			player.sendMessage(ChatColor.GOLD + "/sentry criticals");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take critical hits and misses");
			player.sendMessage(ChatColor.GOLD + "/sentry drops");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to drop equipped items on death");
			player.sendMessage(ChatColor.GOLD + "/sentry killdrops");
			player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the sentry's victims drop items and exp");
			player.sendMessage(ChatColor.GOLD + "/sentry mount");
			player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the sentry rides a mount");
			player.sendMessage(ChatColor.GOLD + "/sentry targetable");
			player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the sentry is attacked by hostile mobs");
			player.sendMessage(ChatColor.GOLD + "/sentry spawn");
			player.sendMessage(ChatColor.GOLD + "  Set the sentry to respawn at its current location");
			player.sendMessage(ChatColor.GOLD + "/sentry warning 'The Test to use'");
			player.sendMessage(ChatColor.GOLD + "  Change the warning text. <NPC> and <PLAYER> can be used as placeholders");
			player.sendMessage(ChatColor.GOLD + "/sentry greeting 'The text to use'");
			player.sendMessage(ChatColor.GOLD + "  Change the greeting text. <NPC> and <PLAYER> can be used as placeholders");
			return true;
		}
//---------------------------------------------------------- Debug Command --------------
		if ( args[0].equalsIgnoreCase( "debug" ) ) {
			
			// TODO make sure this perm node exists in plugin.yml
			if ( checkCommandPerm( "sentry.debug", player) ) {

				Sentry.debug = !Sentry.debug;
				player.sendMessage( ChatColor.GREEN + "Debug is now: " + Sentry.debug );
			}
			return true;
		}
//---------------------------------------------------------- Reload Command ------------
		if ( args[0].equalsIgnoreCase( "reload" ) ) {
			
			if ( checkCommandPerm( "sentry.reload", player) ) {

				sentry.reloadMyConfig();
				player.sendMessage( ChatColor.GREEN + "reloaded Sentry/config.yml" );
			}
			return true;
		}
		
//-------------------------------------------------------------------------------------
		// the remaining commands all deal with npc's so lets check whether we have one selected,
		// and that we have permission to modify it.
		
		NPC thisNPC;
		
		// check to see whether an integer was provided as the first argument, and therefore was saved early.
		if ( npcid == -1 ) {
			// -1 is the unmodified value of npcid, therefore no int argument, lets attempt to use the selected npc (if any).
			
			thisNPC = ( (Citizens) sentry.pluginManager.getPlugin( "Citizens" ) ).getNPCSelector()
																		  .getSelected( player );
			// send message and return if null returned above.
			if ( thisNPC == null ) {
				player.sendMessage( ChatColor.RED + "You must have a NPC selected to use this command" );
				return true;
			}	
			npcid = thisNPC.getId();
			
		} 
		else {
			// an integer argument was provided, its time to see if it is a valid npcid.
			thisNPC = CitizensAPI.getNPCRegistry().getById( npcid ); 
	
			// send message and return if null returned above.
			if ( thisNPC == null ) {
				player.sendMessage( ChatColor.RED + "NPC with id " + npcid + " not found" );
				return true;
			}
		}
		// We are now sure that thisNPC refers to a valid npc instance, and that npcid contains its id.
        // lets check that the specified npc has the sentry trait.
		
		if ( !thisNPC.hasTrait( SentryTrait.class ) ) {
			player.sendMessage( ChatColor.RED + "That command must be performed on a Sentry!" );
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
		if ( args[0].equalsIgnoreCase( "spawn" ) ) {
			
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
		if ( args[0].equalsIgnoreCase( "invincible" ) ) {
			
			if ( checkCommandPerm( "sentry.options.invincible", player ) ) {
				
				// check if the boolean 'set' is null and toggles the state if so, otherwise it uses the value in set.
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
		if ( args[0].equalsIgnoreCase( "retaliate" ) ) {
			
			if ( checkCommandPerm( "sentry.options.retaliate", player ) ) {
				
				// check if the boolean 'set' is null and toggles the state if so, otherwise it uses the value in set.
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
		if ( args[0].equalsIgnoreCase( "criticals" ) ) {
			
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
		if ( args[0].equalsIgnoreCase( "drops" ) ) {
			
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
		if ( args[0].equalsIgnoreCase( "killdrops" ) ) {
			
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
		if ( args[0].equalsIgnoreCase( "targetable" ) ) {
			
			if ( checkCommandPerm( "sentry.options.targetable", player) ) {

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
		
		else if (args[0].equalsIgnoreCase("mount")) {
			if ( !checkCommandPerm( "sentry.options.mount", player) ) return true;

			set = set ==null? !inst.isMounted() : set;

			if (set){
				player.sendMessage(ChatColor.GREEN +  thisNPC.getName() + " is now Mounted");
				inst.createMount();
				inst.mount();
			}
			else {
				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " is no longer Mounted");
				if(inst.isMounted()) Util.removeMount(inst.mountID);	
				inst.mountID = -1;
			}
			return true;
		}
		
		else if (args[0].equalsIgnoreCase("guard")) {
			if ( !checkCommandPerm( "sentry.guard", player) ) return true;
			
			boolean localonly = false;
			boolean playersonly = false;
			int start = 1;

			if (args.length > 1) {

				if (args[1].equalsIgnoreCase("-p")){
					start = 2;
					playersonly = true;
				}

				if (args[1].equalsIgnoreCase("-l")){
					start = 2;
					localonly = true;
				}

				String arg = "";
				for (i=start;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();

				boolean ok = false;

				if(!playersonly){
					ok = inst.findGuardEntity(arg, false);
				}

				if(!localonly){
					ok = inst.findGuardEntity(arg, true);
				}

				if (ok) {
					player.sendMessage(ChatColor.GREEN +  thisNPC.getName() + " is now guarding "+ arg );   
				}
				else {
					player.sendMessage(ChatColor.RED +  thisNPC.getName() + " could not find " + arg + ".");   
				}
				
			}
			else {
				if (inst.guardTarget == null){
					player.sendMessage(ChatColor.RED +  thisNPC.getName() + " is already set to guard its immediate area" );   	
				}
				else{
					player.sendMessage(ChatColor.GREEN +  thisNPC.getName() + " is now guarding its immediate area. " );
				}
				inst.findGuardEntity(null, false);

			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("follow")) {
			if ( !checkCommandPerm( "sentry.stats.follow", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Follow Distance is " + inst.followDistance);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry follow [#]. Default is 4. ");
			}
			else {
				int HPs = Integer.valueOf(args[1]);
				if (HPs > 32) HPs = 32;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " follow distance set to " + HPs + ".");   
				inst.followDistance = HPs * HPs;
			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("health")) {
			if ( !checkCommandPerm( "sentry.stats.health", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Health is " + inst.sentryMaxHealth);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry health [#]   note: Typically players");
				player.sendMessage(ChatColor.GOLD + "  have 20 HPs when fully healed");
			}
			else {
				int HPs = Integer.valueOf(args[1]);
				if (HPs > 2000000) HPs = 2000000;
				if (HPs <1)  HPs =1;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " health set to " + HPs + ".");  
				inst.sentryMaxHealth = HPs;
				inst.setHealth(HPs);
			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("armor")) {
			if ( !checkCommandPerm( "sentry.stats.armor", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Armor is " + inst.armorValue);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry armor [#] ");
			}
			else {
				int HPs = Integer.valueOf(args[1]);
				if (HPs > 2000000) HPs = 2000000;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " armor set to " + HPs + ".");   
				inst.armorValue = HPs;
			}
			return true;
		}
		
		else if (args[0].equalsIgnoreCase("strength")) {
			if ( !checkCommandPerm( "sentry.stats.strength", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Strength is " + inst.strength);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry strength # ");
				player.sendMessage(ChatColor.GOLD + "Note: At Strength 0 the Sentry will do no damamge. ");
			}
			else {
				int HPs = Integer.valueOf(args[1]);
				if (HPs > 2000000) HPs = 2000000;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " strength set to " + HPs+ ".");  
				inst.strength = HPs;
			}
			return true;
		}
		
		else if (args[0].equalsIgnoreCase("nightvision")) {
			if ( !checkCommandPerm( "sentry.stats.nightvision", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Night Vision is " + inst.nightVision);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry nightvision [0-16] ");
				player.sendMessage(ChatColor.GOLD + "Usage: 0 = See nothing, 16 = See everything. ");
			}
			else {
				int HPs = Integer.valueOf(args[1]);
				if (HPs > 16) HPs = 16;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Night Vision set to " + HPs+ ".");   
				inst.nightVision = HPs;
			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("respawn")) {
			if ( !checkCommandPerm( "sentry.stats.respawn", player) ) return true;

			if (args.length <= 1) {
				if(inst.respawnDelay == 0  ) player.sendMessage(ChatColor.GOLD + thisNPC.getName() + " will not automatically respawn.");
				if(inst.respawnDelay == -1 ) player.sendMessage(ChatColor.GOLD + thisNPC.getName() + " will be deleted upon death");
				if(inst.respawnDelay > 0 ) player.sendMessage(ChatColor.GOLD + thisNPC.getName() + " respawns after " + inst.respawnDelay + "s");

				player.sendMessage(ChatColor.GOLD + "Usage: /sentry respawn [-1 - 2000000] ");
				player.sendMessage(ChatColor.GOLD + "Usage: set to 0 to prevent automatic respawn");
				player.sendMessage(ChatColor.GOLD + "Usage: set to -1 to *permanently* delete the Sentry on death.");
			}
			else {
				int HPs = Integer.valueOf(args[1]);
				if (HPs > 2000000) HPs = 2000000;
				if (HPs <-1)  HPs =-1;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " now respawns after " + HPs+ "s.");   
				inst.respawnDelay = HPs;
			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("speed")) {
			if ( !checkCommandPerm( "sentry.stats.speed", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Speed is " + inst.sentrySpeed);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry speed [0.0 - 2.0]");
			}
			else {
				Float HPs = Float.valueOf(args[1]);
				if (HPs > 2.0) HPs = 2.0f;
				if (HPs <0.0)  HPs =0f;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " speed set to " + HPs + ".");   
				inst.sentrySpeed = HPs;
			}
			return true;
		}
		
		else if (args[0].equalsIgnoreCase("attackrate")) {
			if ( !checkCommandPerm( "sentry.stats.attackrate", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Projectile Attack Rate is " + inst.attackRate + "s between shots." );
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry attackrate [0.0 - 30.0]");
			}
			else {
				Double HPs = Double.valueOf(args[1]);
				if (HPs > 30.0) HPs = 30.0;
				if (HPs < 0.0)  HPs = 0.0;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Projectile Attack Rate set to " + HPs + ".");  
				inst.attackRate = HPs;
			}
			return true;
		}
		
		else if (args[0].equalsIgnoreCase("healrate")) {
			if ( !checkCommandPerm( "sentry.stats.healrate", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Heal Rate is " + inst.healRate + "s" );
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry healrate [0.0 - 300.0]");
				player.sendMessage(ChatColor.GOLD + "Usage: Set to 0 to disable healing");
			}
			else {
				Double HPs = Double.valueOf(args[1]);
				if (HPs > 300.0) HPs = 300.0;
				if (HPs < 0.0)  HPs = 0.0;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Heal Rate set to " + HPs + ".");   
				inst.healRate = HPs;
			}
			return true;
		}
		
		else if (args[0].equalsIgnoreCase("range")) {
			if ( !checkCommandPerm( "sentry.stats.range", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Range is " + inst.sentryRange);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry range [1 - 100]");
			}
			else {
				Integer HPs = Integer.valueOf(args[1]);
				if (HPs > 100) HPs = 100;
				if (HPs <1)  HPs =1;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " range set to " + HPs + ".");   
				inst.sentryRange = HPs;
			}
			return true;
		}
		
		else if (args[0].equalsIgnoreCase("warningrange")) {
			if ( !checkCommandPerm( "sentry.stats.warningrange", player) ) return true;

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Warning Range is " + inst.warningRange);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry warningrangee [0 - 50]");
			}
			else {
				Integer HPs = Integer.valueOf(args[1]);
				if (HPs > 50) HPs = 50;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " warning range set to " + HPs + ".");   
				inst.warningRange = HPs;
			}
			return true;
		}
		
		else if ( args[0].equalsIgnoreCase( "equip" ) ) {
			if ( !checkCommandPerm( "sentry.equip", player ) ) return true;

			if ( args.length <= 1 ) {
				player.sendMessage( ChatColor.RED + "You must specify a Item ID or Name. or specify 'none' to remove all equipment." );
			}
			else {
				if ( thisNPC.getEntity().getType() == EntityType.ENDERMAN || thisNPC.getEntity().getType() == EntityType.PLAYER ) {
					
					if ( args[1].equalsIgnoreCase( "none" ) ) {
						
						//remove equipment
						sentry.equip( thisNPC, null );
						inst.UpdateWeapon();
						player.sendMessage( ChatColor.YELLOW +thisNPC.getName() + "'s equipment cleared." ); 
					}
					else {
						Material mat = Util.getMaterial( args[1] );
						
						if ( mat == null ) {
							player.sendMessage(ChatColor.RED +" Could not equip: unknown item name"); 
							return true;
						}
						
						ItemStack is = new ItemStack(mat);
						
						if ( sentry.equip( thisNPC, is ) ) {
							inst.UpdateWeapon();
							player.sendMessage(ChatColor.GREEN +" equipped " + mat.toString() + " on "+ thisNPC.getName()); 
						}
						else player.sendMessage(ChatColor.RED +" Could not equip: invalid mob type?"); 
						
						
					}
				}
				else player.sendMessage(ChatColor.RED +" Could not equip: must be Player or Enderman type");
			}
			return true;
		}
		
		else if (args[0].equalsIgnoreCase("warning")) {
			if ( !checkCommandPerm( "sentry.warning", player) ) return true;

			if (args.length >=2) {
				String arg = "";
				for (i=1;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();

				String str = arg.replaceAll("\"$", "").replaceAll("^\"", "").replaceAll("'$", "").replaceAll("^'", "");
				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " warning message set to " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&',str) + ".");   
				inst.warningMsg = str;
			}
			else{
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Warning Message is: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&',inst.warningMsg));
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry warning 'The Text to use'");
			}
			return true;
		}
		
		else if (args[0].equalsIgnoreCase("greeting")) {
			if ( !checkCommandPerm( "sentry.greeting", player) ) return true;
			
			if (args.length >=2) {

				String arg = "";
				for (i=1;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();

				String str = arg.replaceAll("\"$", "").replaceAll("^\"", "").replaceAll("'$", "").replaceAll("^'", "");
				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Greeting message set to "+ ChatColor.RESET  + ChatColor.translateAlternateColorCodes('&',str) + ".");   
				inst.greetingMsg = str;
			}
			else{
				player.sendMessage(ChatColor.GOLD + thisNPC.getName() + "'s Greeting Message is: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&',inst.greetingMsg));
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry greeting 'The Text to use'");
			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("info")) {
			if ( !checkCommandPerm( "sentry.info", player) ) return true;

			player.sendMessage( ChatColor.GOLD + "------- Sentry Info for (" + thisNPC.getId() + ") " 
																			 + thisNPC.getName() + "------");
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
												+ "  Retaliate: " + inst.iWillRetaliate);
			player.sendMessage( ChatColor.GREEN + "Drops Items: " + inst.dropInventory 
												+ "  Critical Hits: " + inst.acceptsCriticals);
			player.sendMessage( ChatColor.GREEN + "Kills Drop Items: "+ inst.killsDropInventory 
												+ "  Respawn Delay: " + inst.respawnDelay + "s");
			player.sendMessage( ChatColor.BLUE 	+ "Status: " + inst.myStatus);
			
			if ( inst.meleeTarget != null ) 
				player.sendMessage( ChatColor.BLUE + "Target: " + inst.meleeTarget.toString() );
			else if ( inst.projectileTarget != null ) 
				player.sendMessage( ChatColor.BLUE + "Target: " + inst.projectileTarget.toString() );
			else 
				player.sendMessage( ChatColor.BLUE + "Target: Nothing" );

			if ( inst.getGuardTarget() == null )
				player.sendMessage( ChatColor.BLUE + "Guarding: My Surroundings");
			else 		
				player.sendMessage( ChatColor.BLUE + "Guarding: " + inst.getGuardTarget().toString() );

			return true;
		}

		else if (args[0].equalsIgnoreCase("target")) {
			if ( !checkCommandPerm( "sentry.target", player) ) return true;

			if (args.length<2 ){
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add [entity:Name] or [player:Name] or [group:Name] or [entity:monster] or [entity:player]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove [target]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target list");
				return true;
			}
			String arg = "";
			for ( i = 2; i < args.length; i++ ) {
				arg += " " + args[i];
			}
			arg = arg.trim();

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

			if ( args[1].equals( "add" ) && arg.length() > 0 && arg.split(":").length > 1 ) {

				if ( !inst.targetsContain( arg.toUpperCase() ) ) 
						inst.validTargets.add( arg.toUpperCase() );
				inst.processTargets();
				inst.clearTarget();
				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Target added. Now targeting " + inst.validTargets.toString());
				return true;
			}

			else if ( args[1].equals( "remove" ) && arg.length() > 0 && arg.split(":").length > 1 ) {

				inst.validTargets.remove( arg.toUpperCase() );
				inst.processTargets();
				inst.clearTarget();
				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Targets removed. Now targeting " + inst.validTargets.toString());
				return true;
			}

			else if ( args[1].equals( "clear" ) ) {

				inst.validTargets.clear();
				inst.processTargets();
				inst.clearTarget();
				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Targets cleared.");
				return true;
			}
			
			else if ( args[1].equals( "list" ) ) {
				player.sendMessage(ChatColor.GREEN + "Targets: " + 	inst.validTargets.toString());
				return true;
			}

			else {
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target list");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add type:name");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove type:name");
				player.sendMessage(ChatColor.GOLD + "type:name can be any of the following: entity:MobName entity:monster entity:player entity:all player:PlayerName group:GroupName town:TownName nation:NationName faction:FactionName");

				return true;
			}
		}

		else if (args[0].equalsIgnoreCase("ignore")) {
			if ( !checkCommandPerm( "sentry.ignore", player) ) return true;

			if (args.length<2 ){
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore list");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore clear");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore add type:name");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore remove type:name");
				player.sendMessage(ChatColor.GOLD + "type:name can be any of the following: entity:MobName entity:monster entity:player entity:all player:PlayerName group:GroupName town:TownName nation:NationName faction:FactionName");

				return true;
			}
			String arg = "";
			for (i=2;i<args.length;i++){
				arg += " " + args[i];
			}
			arg = arg.trim();

			if (args[1].equals("add") && arg.length() > 0 && arg.split(":").length>1) {
				if (!inst.ignoresContain(arg.toUpperCase()))	inst.ignoreTargets.add(arg.toUpperCase());
				inst.processTargets();
				inst.setTarget(null, false);
				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Ignore added. Now ignoring " + inst.ignoreTargets.toString());
				return true;
			}

			else if (args[1].equals("remove") && arg.length() > 0 && arg.split(":").length>1) {

				inst.ignoreTargets.remove(arg.toUpperCase());
				inst.processTargets();
				inst.setTarget(null, false);
				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Ignore removed. Now ignoring " + inst.ignoreTargets.toString());
				return true;
			}

			else if (args[1].equals("clear")) {

				inst.ignoreTargets.clear();
				inst.processTargets();
				inst.setTarget(null, false);
				player.sendMessage(ChatColor.GREEN + thisNPC.getName() + " Ignore cleared.");
				return true;
			}
			
			else if (args[1].equals("list")) {

				player.sendMessage(ChatColor.GREEN + "Ignores: " + inst.ignoreTargets.toString());
				return true;
			}

			else {

				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore add [ENTITY:Name] or [PLAYER:Name] or [GROUP:Name] or [ENTITY:MONSTER]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore remove [ENTITY:Name] or [PLAYER:Name] or [GROUP:Name] or [ENTITY:MONSTER]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore clear");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore list");
				return true;
			}
		}
		return false;
	}
}
