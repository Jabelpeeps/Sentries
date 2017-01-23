package org.jabelpeeps.sentries.pluginbridges;

import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginTargetBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;
import org.jabelpeeps.sentries.commands.SentriesCommand;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

import lombok.Getter;

public class ScoreboardTeamsBridge implements PluginTargetBridge {

    @Getter final String prefix = "SCOREBOARD";
    @Getter final String activationMessage = "MC Scoreboard Teams active, the SCOREBOARD: target will function";
    protected static Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    private SentriesComplexCommand command = new ScoreboardTeamsCommand();
    @Getter private String commandHelp = 
            String.join( "", "  using the ", Col.GOLD, "/sentry ", prefix.toLowerCase()," ... ", Col.RESET, "commands." ) ; 

    @Override
    public boolean activate() {
        CommandHandler.addCommand( prefix.toLowerCase(), command );
        return true; 
    }

    public class ScoreboardTeamsCommand implements SentriesComplexCommand, SentriesCommand.Targetting {
        
        @Getter final String shortHelp = "manage scoreboard-defined targets";
        @Getter final String perm = "sentry.scoreboardteams";
        private String helpTxt; 
        @Override
        public String getLongHelp() { 
            if ( helpTxt == null ) {
                helpTxt = String.join( "", 
                        "do ", Col.GOLD, "/sentry scoreboard <target|ignore|remove|list|clearall> <TeamName> ", Col.RESET, 
                        "to have a sentry consider MC scoreboard membership when selecting targets.", System.lineSeparator(),
                        "  use ", Col.GOLD, "target ", Col.RESET, "to target players from <TeamName>", System.lineSeparator(),
                        "  use ", Col.GOLD, "ignore ", Col.RESET, "to ignore players from <TeamName>", System.lineSeparator(),
                        "  use ", Col.GOLD, "remove ", Col.RESET, "to remove <TeamName> as either a target or ignore", System.lineSeparator(),
                        "  use ", Col.GOLD, "list", Col.RESET, "to list the current targets and ignores", System.lineSeparator(),
                        "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all scoreboard targets and ignores from the selected sentry.", 
                        System.lineSeparator(), Col.GOLD, "    <TeamName> ", Col.RESET, "must be a currently existing scoreboard team." );
            }
            return helpTxt;
        }
        
        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

            if ( args.length <= nextArg + 1 ) {
                Utils.sendMessage( sender, getLongHelp() );
                return;
            }
            
            String subCommand = args[nextArg + 1].toLowerCase();

            if ( S.LIST.equals( subCommand ) ) {
                StringJoiner joiner = new StringJoiner( ", " );
                
                inst.targets.stream().filter( t -> t instanceof ScoreboardTeamsTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.RED, "Target: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
                
                inst.ignores.stream().filter( t -> t instanceof ScoreboardTeamsTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.GREEN, "Ignore: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
                
                if ( joiner.length() < 1 ) 
                    Utils.sendMessage( sender, Col.YELLOW, npcName, " has no scoreboard targets or ignores" );
                else
                    Utils.sendMessage( sender, Col.YELLOW, "Current Scoreboard Team targets are:-", Col.RESET, System.lineSeparator(), joiner.toString() );
                return;
            }
            
            if ( S.CLEARALL.equals( subCommand )  ) {                
                inst.targets.removeIf( t -> t instanceof ScoreboardTeamsTarget );
                inst.ignores.removeIf( t -> t instanceof ScoreboardTeamsTarget );
                
                Utils.sendMessage( sender, Col.GREEN, "All Scoreboard Targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Utils.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help ", prefix.toLowerCase() );
                return;
            }
            String teamName = args[nextArg + 2];
            Team team = scoreboard.getTeam( teamName );
            
            if ( team == null ) {
                Utils.sendMessage( sender, S.ERROR, "No Team was found matching:- ", teamName );
                return;
            } 
            
            TargetType target = new ScoreboardTeamsTarget( team );
            
            if ( S.REMOVE.equals( subCommand ) ) {
                
                if ( inst.targets.remove( target ) ) 
                    Utils.sendMessage( sender, Col.GREEN, team.getName(), " was removed from ", npcName, "'s list of targets." );
                else if ( inst.ignores.remove( target ) ) 
                    Utils.sendMessage( sender, Col.GREEN, team.getName(), " was removed from ", npcName, "'s list of ignores." );
                else {
                    Utils.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", team.getName() );
                    call( sender, npcName, inst, 0, "", S.LIST );
                    return;
                }
                inst.checkIfEmpty( sender );
                return;
            }
            
            target.setTargetString( String.join( ":", prefix, subCommand, teamName ) );
            
            if ( S.TARGET.equals( subCommand ) ) {
                
                if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                    Utils.sendMessage( sender, Col.GREEN, "Scoreboard Team: ", team.getName(), " will be targeted by ", npcName );
                else 
                    Utils.sendMessage( sender, Col.RED, team.getName(), S.ALREADY_LISTED, npcName );
                
                call( sender, npcName, inst, 0, "", S.LIST );
                return;
            }
            
            if ( S.IGNORE.equals( subCommand ) ) {
                
                if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                    Utils.sendMessage( sender, Col.GREEN, "Scoreboard Team: ", team.getName(), " will be ignored by ", npcName );
                else 
                    Utils.sendMessage( sender, Col.RED, team.getName(), S.ALREADY_LISTED, npcName );

                call( sender, npcName, inst, 0, "", S.LIST );
                return;            
            } 
            Utils.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                      Col.GOLD, "/sentry help ", prefix.toLowerCase(), Col.RESET, " and try again." );            
        }
    }
    
    public static class ScoreboardTeamsTarget extends AbstractTargetType {

        private final Team team;
        
        ScoreboardTeamsTarget( Team target ) { 
            super( 40 );
            team = target;
            prettyString = "The Scoreboard Team:- " + team.getDisplayName();
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
