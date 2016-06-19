package org.jabelpeeps.sentries.pluginbridges;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

import com.tommytony.war.Team;
import com.tommytony.war.War;

public class WarBridge implements PluginBridge {

    final static String PREFIX = "WAR";
    private String commandHelp = String.join( "", "  using the ", Col.GOLD, "/sentry ", PREFIX.toLowerCase()," ... ", Col.RESET, "commands." ) ; 
    private SentriesComplexCommand command = new WarTeamCommand();

    @Override
    public boolean activate() { 
        CommandHandler.addCommand( PREFIX.toLowerCase(), command );
        return true; 
    }
    @Override
    public String getPrefix() { return PREFIX; }

    @Override
    public String getActivationMessage() { return "War is active, The WAR: target will function"; }

    @Override
    public String getCommandHelp() { return commandHelp; }

    
    @Override
    public void add( SentryTrait inst, String args ) {     
        command.call( null, null, inst, 0, Util.colon.split( args ) );
    }

    public class WarTeamCommand implements SentriesComplexCommand {

        private String helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", PREFIX.toLowerCase(), " <target|ignore|remove|list|clearall> <WarTeam> ",
                Col.RESET, "to have a sentry consider War Team membership when selecting targets.", System.lineSeparator(),
                "  use ", Col.GOLD, "target ", Col.RESET, "to target players from <WarTeam>", System.lineSeparator(),
                "  use ", Col.GOLD, "ignore ", Col.RESET, "to ignore players from <WarTeam>", System.lineSeparator(),
                "  use ", Col.GOLD, "remove ", Col.RESET, "to remove <TeamName> as either a target or ignore", System.lineSeparator(),
                "  use ", Col.GOLD, "list", Col.RESET, "to list the current targets and ignores", System.lineSeparator(),
                "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all War team targets and ignores from the selected sentry.", 
                System.lineSeparator(), Col.GOLD, "    <WarTeam> ", Col.RESET, "must be a currently existing War Team." );
        
        @Override
        public String getShortHelp() { return "define targets according to War Team"; }

        @Override
        public String getLongHelp() { return helpTxt; }

        @Override
        public String getPerm() { return "sentry.warteam"; }

        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

            if ( args.length <= nextArg + 1 ) {
                Util.sendMessage( sender, getLongHelp() );
                return;
            }
            
            String subCommand = args[nextArg + 1].toLowerCase();

            if ( S.LIST.equals( subCommand ) ) {
                StringJoiner joiner = new StringJoiner( ", " );
                
                inst.targets.stream().filter( t -> t instanceof WarTeamTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.RED, "Target: ", t.getTargetString().split( ":" )[2] ) ) );
                
                inst.ignores.stream().filter( t -> t instanceof WarTeamTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.GREEN, "Ignore: ", t.getTargetString().split( ":" )[2] ) ) );
                
                if ( joiner.length() < 1 ) 
                    Util.sendMessage( sender, Col.YELLOW, npcName, " has no scoreboard targets or ignores" );
                else
                    Util.sendMessage( sender, Col.YELLOW, "Current War Team targets are:-", Col.RESET, System.lineSeparator(), joiner.toString() );
                return;
            }
            
            if ( S.CLEARALL.equals( subCommand )  ) {                
                inst.targets.removeIf( t -> t instanceof WarTeamTarget );
                inst.ignores.removeIf( t -> t instanceof WarTeamTarget );
                
                Util.sendMessage( sender, Col.GREEN, "All War Team Targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Util.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help ", PREFIX.toLowerCase() );
                return;
            }
            String teamName = args[nextArg + 2];
            
            Team team = War.war.getEnabledWarzones()
                               .parallelStream()
                               .filter( z -> z.getTeams()
                                              .parallelStream()
                                              .anyMatch( t -> t.getName()
                                                               .equalsIgnoreCase( teamName ) ) )
                               .findAny()
                               .get()
                               .getTeams()
                               .parallelStream()
                               .filter( t -> t.getName()
                                              .equalsIgnoreCase( teamName ) )
                               .findAny()
                               .get();
            
            if ( team == null ) {
                Util.sendMessage( sender, S.ERROR, "No Team was found matching:- ", teamName );
                return;
            } 
            
            TargetType target = new WarTeamTarget( team );
            
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
                    Util.sendMessage( sender, Col.GREEN, "War Team: ", team.getName(), " will be targeted by ", npcName );
                else 
                    Util.sendMessage( sender, Col.RED, team.getName(), S.ALREADY_LISTED, npcName );
                
                return;
            }
            
            if ( S.IGNORE.equals( subCommand ) ) {
                
                if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, "War Team: ", team.getName(), " will be ignored by ", npcName );
                else 
                    Util.sendMessage( sender, Col.RED, team.getName(), S.ALREADY_LISTED, npcName );
                
                return;            
            } 
            Util.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                      Col.GOLD, "/sentry help ", PREFIX.toLowerCase(), Col.RESET, " and try again." );   
        }       
    }
    
    public class WarTeamTarget extends AbstractTargetType {
        
        private Team team;

        WarTeamTarget( Team t ) { 
            super( 80 );
            team = t; 
        }
       
        @Override
        public boolean includes( LivingEntity entity ) {
            
            if ( !(entity instanceof Player) ) return false;
           
            return team.getPlayers().contains( entity );
        }
        
        @Override
        public boolean equals( Object o ) {       
            return  o != null 
                    && o instanceof WarTeamTarget 
                    && ((WarTeamTarget) o).team.equals( team );           
        }       
        @Override
        public int hashCode() { return team.hashCode(); }
    }
}
