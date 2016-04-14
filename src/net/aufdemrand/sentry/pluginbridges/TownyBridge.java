package net.aufdemrand.sentry.pluginbridges;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import net.aufdemrand.sentry.PluginBridge;
import net.aufdemrand.sentry.SentryInstance;

public class TownyBridge  extends PluginBridge {
	
	Map<SentryInstance, Set<Town>> friendlyTowns = new HashMap<SentryInstance, Set<Town>>();
	Map<SentryInstance, Set<Town>> rivalTowns = new HashMap<SentryInstance, Set<Town>>();
	
	Map<SentryInstance, Set<Nation>> friendlyNations = new HashMap<SentryInstance, Set<Nation>>();
	Map<SentryInstance, Set<Nation>> rivalNations = new HashMap<SentryInstance, Set<Nation>>();
	
	Map<SentryInstance, Town> myTown = new HashMap<SentryInstance, Town>();
	Map<SentryInstance, Nation> myNation = new HashMap<SentryInstance, Nation>();
	String commandHelp;
	
	TownyBridge( int flag ) { super( flag ); }
	
	@Override
	protected boolean activate() { return true; }
	
	@Override
	protected String getPrefix() { return "TOWNY"; }

	@Override
	protected String getActivationMessage() { return "Registered with Towny sucessfully, the TOWNY: target will function"; }

	@Override
	protected String getCommandHelp() {
		
		if ( commandHelp == null ) {
			StringJoiner joiner = new StringJoiner( System.lineSeparator() );
			
			joiner.add( "Towny:Town:<TownName> for residents of the named Town." );
			joiner.add( "Towny:Nation:<NationName> for residents of the named Nation." );
			joiner.add( "The following are valid for targets only:-" );
			joiner.add( "Towny:TownEnemies for enemies of the Town the sentry is in." );
			joiner.add( "Towny:NationEnemies for enemies of the Nation the sentry is in." );
			
			commandHelp = joiner.toString();
		}
		return commandHelp;
	}

	@Override
	protected boolean isTarget( Player player, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isIgnoring( Player player, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	} 
	
	public static String[] getResidentTownyInfo( Player player ) {
		
		String[] info = { null, null };

		try {
			Resident resident = TownyUniverse.getDataSource().getResident( player.getName() );
			
			if ( resident.hasTown() ) {
				
				Town town = resident.getTown();
				
				info[1] = town.getName();
				
				if ( town.hasNation() ) {
					info[0] = town.getNation().getName();
				}
			}			
		} catch ( NotRegisteredException e ) { }

		return info;
	}
	
	public static boolean isNationEnemy( String Nation1, String Nation2 ) {
		
		if  (  !Nation1.equalsIgnoreCase( Nation2 ) 
			&& TownyUniverse.getDataSource().hasNation( Nation1 )
			&& TownyUniverse.getDataSource().hasNation( Nation2 ) ) {
			
				try {
					Nation theNation1 = TownyUniverse.getDataSource().getNation( Nation1 );
					Nation theNation2 = TownyUniverse.getDataSource().getNation( Nation2 );
	
					if ( theNation1.hasEnemy( theNation2 ) || theNation2.hasEnemy( theNation1 ) ) 
							return true;
		
				} catch ( NotRegisteredException e ) {}
		}
		return false;
	}
	
	static String getNationNameForLocation( Location loc ) {
		
		TownBlock townBlock = TownyUniverse.getTownBlock( loc );
		
		try {	
			if ( townBlock != null 
			  && townBlock.hasTown()
			  && townBlock.getTown().hasNation() ) {
				
				return townBlock.getTown().getNation().getName();
			}
		} catch ( NotRegisteredException e ) { }
	
		return null;
	}

	@Override
	protected String add( String target, SentryInstance inst, boolean asTarget ) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String remove( String entity, SentryInstance inst, boolean fromTargets ) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean isListed( SentryInstance inst, boolean asTarget ) {
		// TODO Auto-generated method stub
		return false;
	}
	
	// TODO implement this function in the TownyBridge.
	// if ( arg.equalsIgnoreCase( "nationenemies" ) && inst.myNPC.isSpawned() ) {
	// String natname = TownyBridge.getNationNameForLocation( inst.myNPC.getEntity().getLocation() );
	// if ( natname != null ) {
	// arg += ":" + natname;
	// }
	// else 	{
	// player.sendMessage( ChatColor.RED + "Could not get Nation for this NPC's location" );
	// return true;
	// }
	// }
			
}
