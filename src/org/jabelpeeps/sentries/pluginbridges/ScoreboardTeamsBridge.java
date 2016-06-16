package org.jabelpeeps.sentries.pluginbridges;

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

public class ScoreboardTeamsBridge extends PluginBridge {

    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    private SentriesComplexCommand command = new ScoreboardTeamsCommand();
    private String commandHelp = String.join( "", "  using the ", Col.GOLD, "/sentry scoreboard ... ", Col.RESET, "commands." ) ; 
    final static String PREFIX = "TEAM";

    public ScoreboardTeamsBridge( int flag ) { super( flag ); }

    @Override
    public boolean activate() {
        CommandHandler.addCommand( "scoreboard", command );
        return true; 
    }
    @Override
    public String getPrefix() { return PREFIX; }

    @Override
    public String getActivationMessage() { return "MC Scoreboard Teams active, the TEAM: target will function"; }

    @Override
    public String getCommandHelp() { return commandHelp; }

    @Override
    public boolean add( SentryTrait inst, String args ) {       
        command.call( null, null, inst, 0, args);
        return true;
    }

    public class ScoreboardTeamsCommand implements SentriesComplexCommand {
        
        private String helpTxt;
        
        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
  
            if ( S.CLEARALL.equalsIgnoreCase( args[nextArg + 1] )  ) {                
                inst.targets.removeIf( t -> t instanceof ScoreboardTeamsTarget );
                inst.ignores.removeIf( t -> t instanceof ScoreboardTeamsTarget );
                
                Util.sendMessage( sender, Col.GREEN, "All Scoreboard Targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Util.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help scoreboard" );
                return;
            }
            
            Team team = scoreboard.getTeam( args[nextArg + 2] );
            
            if ( team == null ) {
                Util.sendMessage( sender, S.ERROR, "No Team was found matching:- ", args[nextArg + 2] );
                return;
            } 
            
            TargetType target = new ScoreboardTeamsTarget( team );
            
            if ( S.REMOVE.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
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
            
            target.setTargetString( String.join( ":", PREFIX, args[nextArg + 1], args[nextArg + 2] ) );
            
            if ( S.TARGET.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, "Scoreboard Team: ", team.getName(), " will be targeted by ", npcName );
                else 
                    Util.sendMessage( sender, Col.RED, team.getName(), " is already listed as either a target or ignore for ", npcName );
                
                return;
            }
            
            if ( S.IGNORE.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, "Scoreboard Team: ", team.getName(), " will be ignored by ", npcName );
                else 
                    Util.sendMessage( sender, Col.RED, team.getName(), " is already listed as either a target or ignore for ", npcName );
                
                return;            
            } 
            Util.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                      Col.GOLD, "/sentry help scoreboard", Col.RESET, " and try again." );            
        }
        
        @Override
        public String getShortHelp() { return "manage scoreboard-defined targets"; }

        @Override
        public String getPerm() { return "sentry.scoreboardteams"; }
        
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
