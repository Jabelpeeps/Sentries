package org.jabelpeeps.sentry.pluginbridges;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;
import org.jabelpeeps.sentry.CommandHandler;
import org.jabelpeeps.sentry.PluginBridge;
import org.jabelpeeps.sentry.S;
import org.jabelpeeps.sentry.SentryInstance;

import com.tommytony.war.Team;
import com.tommytony.war.War;
import com.tommytony.war.Warzone;

public class WarBridge extends PluginBridge {

    Map<SentryInstance, Set<Team>> friends = new HashMap<SentryInstance, Set<Team>>();
    Map<SentryInstance, Set<Team>> enemies = new HashMap<SentryInstance, Set<Team>>();

    WarBridge( int flag ) {
        super( flag );
    }

    @Override
    public boolean activate() {
        return true;
    }

    @Override
    public String getPrefix() {
        return "WAR";
    }

    @Override
    public String getActivationMessage() {
        return "War is active, The WAR: target will function";
    }

    @Override
    public String getCommandHelp() {
        return "War:<TeamName> for a War Team.";
    }

    @Override
    public boolean isTarget( Player player, SentryInstance inst ) {

        if ( !enemies.containsKey( inst ) )
            return false;

        return enemies.get( inst )
                .contains( Team.getTeamByPlayerName( player.getName() ) );
    }

    @Override
    public boolean isIgnoring( Player player, SentryInstance inst ) {

        if ( !friends.containsKey( inst ) )
            return false;

        return friends.get( inst )
                .contains( Team.getTeamByPlayerName( player.getName() ) );
    }

    @Override
    public String add( String target, SentryInstance inst, boolean asTarget ) {

        String targetTeam = CommandHandler.colon.split( target, 2 )[1];
        List<Warzone> zones = War.war.getWarzones();
        Set<Team> teams = new HashSet<Team>();

        for ( Warzone zone : zones ) {
            teams.addAll( zone.getTeams() );
        }

        for ( Team team : teams ) {

            if ( team.getName().equalsIgnoreCase( targetTeam ) )
                return target.concat( addToList( inst, team, asTarget ) );
        }
        return "There is currently no Team matching ".concat( target );
    }

    private String addToList( SentryInstance inst, Team team,
            boolean asTarget ) {
        Map<SentryInstance, Set<Team>> map = asTarget ? enemies : friends;

        if ( !map.containsKey( inst ) )
            map.put( inst, new HashSet<Team>() );

        if ( map.get( inst ).add( team ) )
            return String.join( " ", S.ADDED_TO_LIST,
                    asTarget ? S.TARGETS : S.IGNORES );

        return String.join( " ", S.ALLREADY_ON_LIST,
                asTarget ? S.TARGETS : S.IGNORES );
    }

    @Override
    public String remove( String entity, SentryInstance inst, boolean fromTargets ) {

        if ( !isListed( inst, fromTargets ) ) {
            return String.join( " ", inst.myNPC.getName(), S.NOT_ANY,
                    "Teams added as", fromTargets ? S.TARGETS : S.IGNORES,
                    S.YET );
        }
        String targetTeam = CommandHandler.colon.split( entity, 2 )[1];

        Map<SentryInstance, Set<Team>> map = fromTargets ? enemies : friends;
        Set<Team> teams = map.get( inst );

        for ( Team team : teams ) {
            if ( team.getName().equalsIgnoreCase( targetTeam )
                    && teams.remove( team ) ) {

                if ( teams.isEmpty() )
                    map.remove( inst );

                return String.join( " ", entity, S.REMOVED_FROM_LIST,
                        fromTargets ? S.TARGETS : S.IGNORES );
            }
        }
        return String.join( " ", entity, S.NOT_FOUND_ON_LIST,
                fromTargets ? S.TARGETS : S.IGNORES );
    }

    @Override
    public boolean isListed( SentryInstance inst, boolean asTarget ) {

        return (asTarget ? enemies.containsKey( inst )
                : friends.containsKey( inst ));
    }
}
