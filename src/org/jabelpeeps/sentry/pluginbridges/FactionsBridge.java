package org.jabelpeeps.sentry.pluginbridges;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.entity.Player;
import org.jabelpeeps.sentry.CommandHandler;
import org.jabelpeeps.sentry.PluginBridge;
import org.jabelpeeps.sentry.S;
import org.jabelpeeps.sentry.SentryInstance;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;

public class FactionsBridge extends PluginBridge {

    Map<SentryInstance, Set<Faction>> friendlyFactions = new HashMap<SentryInstance, Set<Faction>>();
    Map<SentryInstance, Set<Faction>> rivalFactions = new HashMap<SentryInstance, Set<Faction>>();
    Map<SentryInstance, Faction> myFaction = new HashMap<SentryInstance, Faction>();
    String commandHelp;

    FactionsBridge( int flag ) {
        super( flag );
    }

    @Override
    public boolean activate() {
        return true;
    }

    @Override
    public String getPrefix() {
        return "FACTION";
    }

    @Override
    public String getActivationMessage() {
        return "Factions is active, the FACTION: target will function";
    }

    @Override
    public String getCommandHelp() {

        if ( commandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() );

            joiner.add( "Faction:<FactionName> for members of a faction." );
            joiner.add(
                    "Faction:Join:<FactionName> have a sentry attack enemies of the named faction." );

            commandHelp = joiner.toString();
        }
        return commandHelp;
    }

    @Override
    public boolean isTarget( Player player, SentryInstance inst ) {

        Faction target = MPlayer.get( player ).getFaction();

        if ( myFaction.containsKey( inst )
                && myFaction.get( inst ).getRelationTo( target ) == Rel.ENEMY )
            return true;

        if ( rivalFactions.containsKey( inst )
                && rivalFactions.get( inst ).contains( target ) )
            return true;

        return false;
    }

    @Override
    public boolean isIgnoring( Player player, SentryInstance inst ) {

        Faction ignore = MPlayer.get( player ).getFaction();

        if ( myFaction.containsKey( inst )
                && myFaction.get( inst ).getRelationTo( ignore ) == Rel.ALLY )
            return true;

        if ( friendlyFactions.containsKey( inst )
                && friendlyFactions.get( inst ).contains( ignore ) )
            return true;

        return false;
    }

    @Override
    public boolean isListed( SentryInstance inst, boolean asTarget ) {

        return asTarget ? rivalFactions.containsKey( inst )
                        : friendlyFactions.containsKey( inst );
    }

    @Override
    public String add( String target, SentryInstance inst, boolean asTarget ) {

        String[] input = CommandHandler.colon.split( target, 3 );

        if ( S.JOIN.equalsIgnoreCase( input[1] ) ) {

            for ( Faction faction : FactionColl.get().getAll() ) {

                if ( faction.getName().equalsIgnoreCase( input[2] ) ) {

                    myFaction.put( inst, faction );
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

        Map<SentryInstance, Set<Faction>> map = asTarget ? rivalFactions
                                                         : friendlyFactions;

        if ( !map.containsKey( inst ) )
            map.put( inst, new HashSet<Faction>() );

        if ( map.get( inst ).add( faction ) )
            return String.join( " ", S.ADDED_TO_LIST, asTarget ? S.TARGETS : S.IGNORES );

        return String.join( " ", S.ALLREADY_ON_LIST, asTarget ? S.TARGETS : S.IGNORES );
    }

    @Override
    public String remove( String entity, SentryInstance inst, boolean fromTargets ) {

        String[] input = CommandHandler.colon.split( entity, 3 );

        if ( S.JOIN.equalsIgnoreCase( input[1] ) ) {

            for ( Faction faction : FactionColl.get().getAll() ) {

                if ( faction.getName().equalsIgnoreCase( input[2] )
                        && myFaction.containsKey( inst )
                        && myFaction.get( inst ).getName().equalsIgnoreCase( input[2] ) ) {

                    myFaction.remove( inst );

                    return String.join( " ", inst.myNPC.getName(), "has left", input[2] );
                }
            }
        }

        if ( !isListed( inst, fromTargets ) ) {
            return String.join( " ", inst.myNPC.getName(), S.NOT_ANY,
                    "Factions added as", fromTargets ? S.TARGETS : S.IGNORES, S.YET );
        }

        Map<SentryInstance, Set<Faction>> map = fromTargets ? rivalFactions
                                                            : friendlyFactions;
        Set<Faction> factions = map.get( inst );

        for ( Faction faction : factions ) {

            if ( faction.getName().equalsIgnoreCase( input[1] )
                    && factions.remove( faction ) ) {

                if ( factions.isEmpty() )
                    map.remove( inst );

                return String.join( " ", entity, S.REMOVED_FROM_LIST, fromTargets ? S.TARGETS : S.IGNORES );
            }
        }
        return String.join( " ", entity, S.NOT_FOUND_ON_LIST, fromTargets ? S.TARGETS : S.IGNORES );
    }
}
