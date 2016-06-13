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
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;

public class ScoreboardTeamsBridge extends PluginBridge {

    private Map<SentryTrait, Set<Team>> friends = new HashMap<>();
    private Map<SentryTrait, Set<Team>> enemies = new HashMap<>();
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    private SentriesComplexCommand command = new ScoreboardTeamsCommand();
    private String commandHelp;

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
    public String getCommandHelp() {         
        if ( commandHelp == null ) {
            commandHelp = String.join( "", Col.GOLD, "  Team:<TeamName> ", Col.RESET, "for a Minecraft Scoreboard Team.", 
                    System.lineSeparator(), "    (or use the ", Col.GOLD, "/sentry scoreboard ... ", Col.RESET, "commands.)" ) ; 
        }       
        return commandHelp;
    }

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
    public boolean add( SentryTrait inst, String args ) {
        
        command.call( null, null, inst, 0, args);
        return true;
    }
    
    @Override
    public String add( String target, SentryTrait inst, boolean asTarget ) {

        Team team = scoreboard.getTeam( CommandHandler.colon.split( target, 2 )[1] );
        
        if ( team == null ) 
            return "There is currently no Team matching ".concat( target );
        
        Map<SentryTrait, Set<Team>> map = asTarget ? enemies : friends;

        if ( !map.containsKey( inst ) )
            map.put( inst, new HashSet<Team>() );

        if ( map.get( inst ).add( team ) )
            return String.join( " ", target, S.ADDED_TO_LIST, asTarget ? S.TARGETS : S.IGNORES );

        return String.join( " ", target, S.ALLREADY_ON_LIST, asTarget ? S.TARGETS : S.IGNORES );           
    }

    @Override
    public String remove( String entity, SentryTrait inst, boolean fromTargets ) {

        if ( !isListed( inst, fromTargets ) ) {
            return String.join( " ", inst.getNPC().getName(), S.NOT_ANY, "Teams added as ", fromTargets ? S.TARGETS : S.IGNORES, S.YET );
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
        
        private String helpTxt;
        
        @Override
        public boolean call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
  
            if ( args.length <= nextArg + 2 ) { 
                sender.sendMessage( String.join( "", S.ERROR, " Not enough arguments. ", Col.RESET, "Try /sentry help scoreboard" ) );
                return true;
            }
            
            if ( S.CLEARALL.equalsIgnoreCase( args[nextArg + 1] )  ) {               
                inst.targets.clear();
                inst.ignores.clear();  
                checkIfEmpty( sender, inst, npcName );
                return true;
            }
            
            Team team = scoreboard.getTeam( args[nextArg + 2] );
            
            if ( team == null ) {
                sender.sendMessage( String.join( "", S.ERROR, " No Team was found matching:- ", args[nextArg + 2] ) );
                return true;
            } 
            
            ScoreboardTeamsTarget target = new ScoreboardTeamsTarget( team );
            
            if ( S.REMOVE.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( inst.targets.remove( target ) ) 
                    sender.sendMessage( String.join( "", Col.GREEN, team.getName(), " was removed from ", npcName, "'s list of targets." ) );
                else if ( inst.ignores.remove( target ) ) 
                    sender.sendMessage( String.join( "", Col.GREEN, team.getName(), " was removed from ", npcName, "'s list of ignores." ) );
                else {
                    sender.sendMessage( String.join( "", Col.RED, npcName, " was neither targeting nor ignoring ", team.getName() ) );
                    return true;
                }
                checkIfEmpty( sender, inst, npcName );
                return true;
            }
            
            target.setTargetString( String.join( ":", getPrefix(), args[nextArg + 1], args[nextArg + 2] ) );
            
            if (    S.TARGET.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( !inst.ignores.contains( target ) && inst.targets.add( target ) && sender != null ) 
                    sender.sendMessage( String.join( "", Col.GREEN, "Scoreboard Team: ", team.getName(), " will be targeted by ", npcName ) );
                else if ( sender != null )
                    sender.sendMessage( String.join( "", Col.RED, team.getName(), " is already listed as either a target or ignore for ", npcName ) );
                
                return true;
            }
            
            if (    S.IGNORE.equalsIgnoreCase( args[nextArg + 1] ) 
                    || S.JOIN.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( !inst.targets.contains( target ) && inst.ignores.add( target ) && sender != null ) 
                    sender.sendMessage( String.join( "", Col.GREEN, "Scoreboard Team: ", team.getName(), " will be ignored by ", npcName ) );
                else if ( sender != null )
                    sender.sendMessage( String.join( "", Col.RED, team.getName(), " is already listed as either a target or ignore for ", npcName ) );
                
                return true;            
            } 
            if ( sender != null )
                sender.sendMessage( String.join( "", S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                                    Col.GOLD, "/sentry help scoreboard", Col.RESET, " and try again." ) );            
            return true;
        }
        
        private void checkIfEmpty ( CommandSender sender, SentryTrait inst, String npcName ) {
            if ( inst.targets.isEmpty() && inst.ignores.isEmpty() )
                sender.sendMessage( String.join( "", Col.YELLOW, npcName, " has no defined targets now." ) );
        }
        
        @Override
        public String getShortHelp() { return "manage scoreboard-defined targets"; }

        @Override
        public String getLongHelp() {

            if ( helpTxt == null ) {
                helpTxt = String.join( "", "do ", Col.GOLD, "/sentry scoreboard <target|ignore|remove|clearall> <TeamName> ",
                        Col.RESET, "to have a sentry consider MC scoreboard membership when selecting targets.", System.lineSeparator(),
                        "  use ", Col.GOLD, "target ", Col.RESET, "to target players from <TeamName>", System.lineSeparator(),
                        "  use ", Col.GOLD, "ignore ", Col.RESET, "to ignore players from <TeamName>", System.lineSeparator(),
                        "  use ", Col.GOLD, "remove ", Col.RESET, "to remove <TeamName> as either a target or ignore", System.lineSeparator(),
                        "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all scoreboard targets and ignores from the selected sentry.", 
                        System.lineSeparator(), Col.GOLD, "    <TeamName> ", Col.RESET, "must be a currently existing scoreboard team." );
            }
            return helpTxt;
        }
        @Override
        public String getPerm() { return "sentry.scoreboardteams"; }
    }
    
    public class ScoreboardTeamsTarget extends AbstractTargetType {

        private Team team;
        
        ScoreboardTeamsTarget( Team target ) { 
            super( 40 );
            team = target; 
        }
        
        @Override
        public boolean includes( LivingEntity entity ) {
            if ( !scoreboard.getTeams().contains( team ) ) return false;
            
            return team.hasEntry( entity.getName() );
        }
        
        @Override
        public boolean equals( Object o ) {
            if (    o != null 
                    && o instanceof ScoreboardTeamsTarget 
                    && ((ScoreboardTeamsTarget) o).team.equals( team ) ) 
                return true;
            
            return false;            
        }
        @Override
        public int hashCode() { return team.hashCode(); }
    }
}
