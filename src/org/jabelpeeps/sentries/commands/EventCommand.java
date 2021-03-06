package org.jabelpeeps.sentries.commands;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;
import org.jabelpeeps.sentries.targets.EventPVETarget;
import org.jabelpeeps.sentries.targets.EventPVNPCTarget;
import org.jabelpeeps.sentries.targets.EventPVPTarget;
import org.jabelpeeps.sentries.targets.EventPVSentryTarget;
import org.jabelpeeps.sentries.targets.TargetType;

import lombok.Getter;


public class EventCommand implements SentriesComplexCommand, SentriesCommand.Targetting, SentriesCommand.Tabable {

    private String commandHelp;
    @Getter private String shortHelp = "set events a sentry should react to";
    @Getter private String perm = S.PERM_EVENT;

    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length <= nextArg + 1 ) {
            sender.sendMessage( getLongHelp() );
            return;
        }
        String subCommand = args[nextArg + 1].toLowerCase();
        
        if ( S.LIST.equals( subCommand ) ) {
            StringJoiner joiner = new StringJoiner( ", " );
            inst.events.forEach( t -> joiner.add( t.getTargetString() ) );
            
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s Events: ", joiner.toString() );
            return;
        }
        
        if ( S.CLEARALL.equals( subCommand ) ) {
            inst.events.clear();
            inst.cancelAttack();
            Utils.sendMessage( sender, Col.GREEN, npcName, ": ALL Events cleared" );
            return;
        }
        
        if ( (S.ADD + S.REMOVE).contains( subCommand ) ) {
            
            if ( args.length <= nextArg + 2 ) {
                Utils.sendMessage( sender, S.ERROR, "Missing argument!", Col.RESET, " try '/sentry help event'" );
                return;
            }
            TargetType target = null;
            String eventName = args[nextArg + 2].toLowerCase();
 
            if ( eventName.equals( "pvp" ) ) 
                target = new EventPVPTarget();
            else if ( eventName.equals( "pve" ) ) 
                target = new EventPVETarget();
            else if ( eventName.equals( "pvnpc" ) ) 
                target = new EventPVNPCTarget();
            else if ( eventName.equals( "pvsentry" ) )
                target = new EventPVSentryTarget();
            
            if ( target == null )
                Utils.sendMessage( sender, "The event name was not recognised" );
            else if ( S.ADD.equals( subCommand ) && inst.events.add( target ) )
                Utils.sendMessage( sender, "Event Added" );
            else if ( S.REMOVE.equals( subCommand ) && inst.events.remove( target ) )
                Utils.sendMessage( sender, "Event Removed" );  
        }
    }

    @Override
    public List<String> onTab( int nextArg, String[] args ) {
        if ( args.length == nextArg + 2 ) {
            List<String> tabs = Arrays.asList( S.ADD, S.REMOVE, S.LIST, S.CLEARALL );
            tabs.removeIf( t -> !t.startsWith( args[1 + nextArg].toLowerCase() ) );
            return tabs;
        }
        return null;
    }
    
    @Override
    public String getLongHelp() {
        if ( commandHelp == null ) {
            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry ", S.EVENT, " <add|remove|list|clearall> <EventType>", 
                                                Col.RESET, " to configure events for a sentry to respond to."  ) );
            joiner.add( Utils.join( Col.BOLD, "Events are overridden by ignores (if both are configured and apply).", Col.RESET ) );
            joiner.add( Utils.join( "  use ", Col.GOLD, S.ADD, Col.RESET, " to respond to <EventType>" ) );
            joiner.add( Utils.join( "  use ", Col.GOLD, S.REMOVE, Col.RESET, " to stop responding to <EventType>" ) );
            joiner.add( Utils.join( "  use ", Col.GOLD, S.LIST, Col.RESET, " to display current list of events" ) );
            joiner.add( Utils.join( "  use ", Col.GOLD, S.CLEARALL, Col.RESET, " to clear the ALL the current events" ) );
            joiner.add( Utils.join( Col.GOLD, Col.BOLD, "<EventType> ", Col.RESET, "can be any of the following:-") );
            joiner.add( Utils.join( Col.GOLD, "  PvP ", Col.RESET, "- a Player-vs-Player Event") );
            joiner.add( Utils.join( Col.GOLD, "  PvE ", Col.RESET, "- a Player-vs-Environment Event" ) );
            joiner.add( Utils.join( Col.GOLD, "  PvNPC ", Col.RESET, "- a Player-vs-NPC Event") );
            joiner.add( Utils.join( Col.GOLD, "  PvSentry ", Col.RESET, "- a Player-vs-Sentry Event") );
            joiner.add( Utils.join( "In all cases the sentry will respond by attacking ", Col.WHITE, "the Aggressor!", Col.RESET ) );

            commandHelp = joiner.toString();
        }
        return commandHelp;
    }
}
