package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.entity.Player;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;

public class FactionsBridge extends PluginBridge {
	
	Map<SentryInstance, Set<Faction>> friendlyFactions = new HashMap<SentryInstance, Set<Faction>>();
	Map<SentryInstance, Set<Faction>> rivalFactions = new HashMap<SentryInstance, Set<Faction>>();
	Faction myFaction;
	String commandHelp;
	
	FactionsBridge( int flag ) { super( flag ); }
	
	@Override
	boolean activate() { return true; }

	@Override
	String getPrefix() { return "FACTION"; }

	@Override
	String getActivationMessage() { return "Factions is active, the FACTION: target will function"; }

	@Override
	String getCommandHelp() { 
		
		if ( commandHelp == null ) {
			
			StringJoiner joiner = new StringJoiner( System.lineSeparator() );
			
			joiner.add( "Faction:<FactionName> for members of a faction." );
			joiner.add( "Faction:Join:<FactionName> have a sentry attack enemies of the named faction." );
			
			commandHelp = joiner.toString();
		}
		return commandHelp;
	}
	
	@Override
	boolean isTarget( Player player, SentryInstance inst ) {

		if ( !rivalFactions.containsKey( inst ) && myFaction == null ) return false;
		
		Faction target = MPlayer.get( player ).getFaction();
		
		if ( myFaction != null && myFaction.getRelationTo( target ) == Rel.ENEMY ) return true;
		
		return rivalFactions.get( inst ).contains( target );
	}

	@Override
	boolean isIgnoring( Player player, SentryInstance inst ) {
		
		if ( !friendlyFactions.containsKey( inst ) && myFaction == null ) return false;
		
		Faction ignore = MPlayer.get( player ).getFaction();
		
		if ( myFaction != null && myFaction.getRelationTo( ignore ) == Rel.ALLY ) return true;
		
		return friendlyFactions.get( inst ).contains( ignore );
	}

	@Override
	boolean isListed( SentryInstance inst, boolean asTarget ) {
		
		return myFaction != null || ( asTarget ? rivalFactions.containsKey( inst )
				  							   : friendlyFactions.containsKey( inst ) );
	}

	@Override
	String add( String target, SentryInstance inst, boolean asTarget ) {
		
		String[] input = CommandHandler.colon.split( target, 3 );
		
		if ( S.JOIN.equalsIgnoreCase( input[1] ) ) {
			for ( Faction faction : FactionColl.get().getAll() ) {
				if ( faction.getName().equalsIgnoreCase( input[2] ) ) {
					
					myFaction = faction;
					return String.join( " ", inst.myNPC.getName(), "has joined", faction.getName() );
				}
			}
		}
		for ( Faction faction : FactionColl.get().getAll() ) {
			
			if ( faction.getName().equalsIgnoreCase( input[1] ) ) 
				return String.join( " ", target, addToList( inst, faction, asTarget ) );
		}
		return "There is currently no Faction name matching ".concat( target );
	}
	
	private String addToList( SentryInstance inst, Faction faction, boolean asTarget ) {
		Map<SentryInstance, Set<Faction>> map = asTarget ? rivalFactions : friendlyFactions;
		
		if ( !map.containsKey( inst ) )
			map.put( inst, new HashSet<Faction>() );

		if ( map.get( inst ).add( faction ) )
			return String.join( " ", S.ADDED_TO_LIST, asTarget ? S.TARGETS : S.IGNORES );
		
		return String.join( " ", S.ALLREADY_ON_LIST, asTarget ? S.TARGETS : S.IGNORES );
	}
	
	@Override
	String remove( String entity, SentryInstance inst, boolean fromTargets ) {
		
		if ( !isListed( inst, fromTargets ) ) {
			return String.join( 
					" ", inst.myNPC.getName(), S.NOT_ANY, "Factions added as", fromTargets ? S.TARGETS : S.IGNORES , S.YET );
		}
		
		String[] input = CommandHandler.colon.split( entity, 3 );
		
		if ( S.JOIN.equalsIgnoreCase( input[1] ) ) {
			for ( Faction faction : FactionColl.get().getAll() ) {
				if ( faction.getName().equalsIgnoreCase( input[2] ) ) {
					
					String outMsg = String.join( " ", inst.myNPC.getName(), "has left", myFaction.getName() );
					myFaction = null;
					return outMsg;
				}
			}
		}
		Map<SentryInstance, Set<Faction>> map = fromTargets ? rivalFactions : friendlyFactions;
		Set<Faction> factions = map.get( inst );
	
		for ( Faction faction : factions ) {
			
			if ( faction.getName().equalsIgnoreCase( input[1] ) && factions.remove( faction ) ) {
				
				if ( factions.isEmpty() )
					map.remove( inst );
					
				return String.join( " ", entity, S.REMOVED_FROM_LIST, fromTargets ? S.TARGETS : S.IGNORES );
			}
		}
		return String.join( " ", entity, S.NOT_FOUND_ON_LIST, fromTargets ? S.TARGETS : S.IGNORES );
	}
}
