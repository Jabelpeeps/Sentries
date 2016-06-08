package org.jabelpeeps.sentries.pluginbridges;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.commands.SentriesCommand;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;

public class ScoreboardTeamsBridge extends PluginBridge {

    Map<SentryTrait, Set<Team>> friends = new HashMap<>();
    Map<SentryTrait, Set<Team>> enemies = new HashMap<>();
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    SentriesCommand command = new ScoreboardTeamsCommand( this );

    public ScoreboardTeamsBridge( int flag ) { super( flag ); }

    @Override
    public boolean activate() {
        CommandHandler.addCommand( "scoreboard", command );
        return true; 
    }

    @Override
    public String getPrefix() { return "TEAM"; }

    @Override
    public String getActivationMessage() { return "MC Scoreboard Teams active, the TEAM: target will function"; }

    @Override
    public String getCommandHelp() { return "Team:<TeamName> for a Minecraft Scoreboard Team."; }

    @Override
    public boolean isTarget( LivingEntity entity, SentryTrait inst ) {

        if ( !enemies.containsKey( inst ) ) return false;

        return enemies.get( inst ).contains( scoreboard.getEntryTeam( entity.getName() ) );
    }

    @Override
    public boolean isIgnoring( LivingEntity entity, SentryTrait inst ) {

        if ( !friends.containsKey( inst ) ) return false;

        return friends.get( inst ).contains( scoreboard.getEntryTeam( entity.getName() ) );
    }

    @Override
    public String add( String target, SentryTrait inst, boolean asTarget ) {

        String targetTeam = CommandHandler.colon.split( target, 2 )[1];
        Set<Team> teams = scoreboard.getTeams();

        for ( Team team : teams ) {

            if ( team.getName().equalsIgnoreCase( targetTeam ) )
                return target.concat( addToList( inst, team, asTarget ) );
        }
        return "There is currently no Team matching ".concat( target );
    }

    private String addToList( SentryTrait inst, Team team, boolean asTarget ) {
        Map<SentryTrait, Set<Team>> map = asTarget ? enemies : friends;

        if ( !map.containsKey( inst ) )
            map.put( inst, new HashSet<Team>() );

        if ( map.get( inst ).add( team ) )
            return String.join( " ", S.ADDED_TO_LIST, asTarget ? S.TARGETS : S.IGNORES );

        return String.join( " ", S.ALLREADY_ON_LIST, asTarget ? S.TARGETS : S.IGNORES );
    }

    @Override
    public String remove( String entity, SentryTrait inst, boolean fromTargets ) {

        if ( !isListed( inst, fromTargets ) ) {
            return String.join( " ", inst.getNPC().getName(), S.NOT_ANY, "Teams added as ", 
                    fromTargets ? S.TARGETS : S.IGNORES, S.YET );
        }
        String targetTeam = CommandHandler.colon.split( entity, 2 )[1];

        Map<SentryTrait, Set<Team>> map = fromTargets ? enemies : friends;
        Set<Team> teams = map.get( inst );

        for ( Team team : teams ) {
            
            if (    team.getName().equalsIgnoreCase( targetTeam )
                    && teams.remove( team ) ) {

                if ( teams.isEmpty() )
                    map.remove( inst );

                return String.join( " ", entity, S.REMOVED_FROM_LIST, fromTargets ? S.TARGETS : S.IGNORES );
            }
        }
        return String.join( " ", entity, S.NOT_FOUND_ON_LIST, fromTargets ? S.TARGETS : S.IGNORES );
    }

    @Override
    public boolean isListed( SentryTrait inst, boolean asTarget ) {

        return ( asTarget ? enemies.containsKey( inst )
                          : friends.containsKey( inst ) );
    }
    
    public class ScoreboardTeamsCommand implements SentriesComplexCommand {
        
        private PluginBridge bridge;
        private String helpTxt;
        
        ScoreboardTeamsCommand( PluginBridge pb ) {
            bridge = pb;
        }
        
        @Override
        public boolean call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
            // TODO implement parsing of arguments to identify the intention of the sender.
            // TODO implement calling of methods in parent class to achieve target adding.
            // TODO don't forget to add TEAM:<TeamName> style tags to inst.validtargets or inst.ignoretargets
            // TODO remove "Non-Functional" message from getLongHelp()
            return false;
        }
        
        @Override
        public String getShortHelp() {
            return "manage scoreboard-defined targets";
        }

        @Override
        public String getLongHelp() {

            if ( helpTxt == null ) {
                helpTxt = String.join( "", Col.RED, "Non-functional, for testing only!", Col.RESET,
                        System.lineSeparator(), "do ", Col.GOLD, "/sentry scoreboard <add|remove> <target|ignore> <TeamName> ",
                        Col.RESET, "to have a sentry consider MC scoreboard membership when selecting targets.",
                        System.lineSeparator(), 
                        Col.GOLD, "  <TeamName> ", Col.RESET, "must be a currently existing scoreboard team." );
            }
            return helpTxt;
        }

        @Override
        public String getPerm() {
            return "sentry.scoreboardteams";
        }
    }
}
