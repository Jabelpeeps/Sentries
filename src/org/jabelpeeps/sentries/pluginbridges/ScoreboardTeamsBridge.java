package org.jabelpeeps.sentries.pluginbridges;

import java.util.StringJoiner;

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
import org.jabelpeeps.sentries.Util;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

public class ScoreboardTeamsBridge implements PluginBridge {

    final static String PREFIX = "SCOREBOARD";
    protected Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    private SentriesComplexCommand command = new ScoreboardTeamsCommand();
    private String commandHelp = String.join( "", "  using the ", Col.GOLD, "/sentry ", PREFIX.toLowerCase()," ... ", Col.RESET, "commands." ) ; 

    @Override
    public boolean activate() {
        CommandHandler.addCommand( PREFIX.toLowerCase(), command );
        return true; 
    }
    @Override
    public String getPrefix() { return PREFIX; }

    @Override
    public String getActivationMessage() { return "MC Scoreboard Teams active, the TEAM: target will function"; }

    @Override
    public String getCommandHelp() { return commandHelp; }

    @Override
    public void add( SentryTrait inst, String args ) {       
        command.call( null, null, inst, 0, Util.colon.split( args ) );
    }

    public class ScoreboardTeamsCommand implements SentriesComplexCommand {
        
        private String helpTxt = String.join( "", "do ", Col.GOLD, "/sentry scoreboard <target|ignore|remove|list|clearall> <TeamName> ",
                Col.RESET, "to have a sentry consider MC scoreboard membership when selecting targets.", System.lineSeparator(),
                "  use ", Col.GOLD, "target ", Col.RESET, "to target players from <TeamName>", System.lineSeparator(),
                "  use ", Col.GOLD, "ignore ", Col.RESET, "to ignore players from <TeamName>", System.lineSeparator(),
                "  use ", Col.GOLD, "remove ", Col.RESET, "to remove <TeamName> as either a target or ignore", System.lineSeparator(),
                "  use ", Col.GOLD, "list", Col.RESET, "to list the current targets and ignores", System.lineSeparator(),
                "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all scoreboard targets and ignores from the selected sentry.", 
                System.lineSeparator(), Col.GOLD, "    <TeamName> ", Col.RESET, "must be a currently existing scoreboard team." );
        
        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

            if ( args.length <= nextArg + 1 ) {
                Util.sendMessage( sender, getLongHelp() );
                return;
            }
            
            String subCommand = args[nextArg + 1].toLowerCase();

            if ( S.LIST.equals( subCommand ) ) {
                StringJoiner joiner = new StringJoiner( ", " );
                
                inst.targets.stream().filter( t -> t instanceof ScoreboardTeamsTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.RED, "Target: ", t.getTargetString().split( ":" )[2] ) ) );
                
                inst.ignores.stream().filter( t -> t instanceof ScoreboardTeamsTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.GREEN, "Ignore: ", t.getTargetString().split( ":" )[2] ) ) );
                
                if ( joiner.length() < 1 ) 
                    Util.sendMessage( sender, Col.YELLOW, npcName, " has no scoreboard targets or ignores" );
                else
                    Util.sendMessage( sender, Col.YELLOW, "Current Scoreboard Team targets are:-", Col.RESET, System.lineSeparator(), joiner.toString() );
                return;
            }
            
            if ( S.CLEARALL.equals( subCommand )  ) {                
                inst.targets.removeIf( t -> t instanceof ScoreboardTeamsTarget );
                inst.ignores.removeIf( t -> t instanceof ScoreboardTeamsTarget );
                
                Util.sendMessage( sender, Col.GREEN, "All Scoreboard Targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Util.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help ", PREFIX.toLowerCase() );
                return;
            }
            String teamName = args[nextArg + 2];
            Team team = scoreboard.getTeam( teamName );
            
            if ( team == null ) {
                Util.sendMessage( sender, S.ERROR, "No Team was found matching:- ", teamName );
                return;
            } 
            
            TargetType target = new ScoreboardTeamsTarget( team );
            
            if ( S.REMOVE.equals( subCommand ) ) {
                
                if ( inst.targets.remove( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, team.getName(), " was removed from ", npcName, "'s list of targets." );
                else if ( inst.ignores.remove( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, team.getName(), " was removed from ", npcName, "'s list of ignores." );
                else {
                    Util.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", team.getName() );
                    return;
                }
                inst.checkIfEmpty( sender );
                return;
            }
            
            target.setTargetString( String.join( ":", PREFIX, subCommand, teamName ) );
            
            if ( S.TARGET.equals( subCommand ) ) {
                
                if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, "Scoreboard Team: ", team.getName(), " will be targeted by ", npcName );
                else 
                    Util.sendMessage( sender, Col.RED, team.getName(), S.ALREADY_LISTED, npcName );
                
                return;
            }
            
            if ( S.IGNORE.equals( subCommand ) ) {
                
                if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, "Scoreboard Team: ", team.getName(), " will be ignored by ", npcName );
                else 
                    Util.sendMessage( sender, Col.RED, team.getName(), S.ALREADY_LISTED, npcName );
                
                return;            
            } 
            Util.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                      Col.GOLD, "/sentry help ", PREFIX.toLowerCase(), Col.RESET, " and try again." );            
        }
        
        @Override
        public String getShortHelp() { return "manage scoreboard-defined targets"; }

        @Override
        public String getPerm() { return "sentry.scoreboardteams"; }
        
        @Override
        public String getLongHelp() { return helpTxt; }
    }
    
    public class ScoreboardTeamsTarget extends AbstractTargetType {

        private final Team team;
        
        ScoreboardTeamsTarget( Team target ) { 
            super( 40 );
            team = target; 
        }    
        @Override
        public boolean includes( LivingEntity entity ) {
            return  scoreboard.getTeams().contains( team ) 
                    && team.hasEntry( entity.getName() );
        }       
        @Override
        public boolean equals( Object o ) {       
            return  o != null 
                    && o instanceof ScoreboardTeamsTarget 
                    && ((ScoreboardTeamsTarget) o).team.equals( team );            
        }
        @Override
        public int hashCode() { return team.hashCode(); }
    }
}
