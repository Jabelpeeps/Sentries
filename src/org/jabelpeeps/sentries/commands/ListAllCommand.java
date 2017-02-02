package org.jabelpeeps.sentries.commands;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class ListAllCommand implements SentriesSimpleCommand {

    @Getter String shortHelp = "list all targets and ignores.";
    @Getter String perm = "";
    @Getter String longHelp = "Lists all targets, events and ignores for a sentry.";

    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst ) {
        if ( inst.checkIfEmpty( sender ) ) return;
        
        StringJoiner joiner = new StringJoiner( System.lineSeparator() );

        joiner.add( Utils.join( Col.GOLD, "-------- Target Listing for ", Col.WHITE, npcName, Col.GOLD, " --------" ) );
        
        joiner.add( Utils.join( Col.YELLOW, "Targets:-" ) );
        if ( inst.targets.isEmpty() )
            joiner.add( Utils.join( "No Targets set. Add some with the ", Col.GOLD, "/sentry target ", Col.RESET, "command" ) );
        else
            inst.targets.stream().forEach( t -> joiner.add( t.getPrettyString() ) );
        
        joiner.add( Utils.join( Col.YELLOW, "Ignores:-" ) );
        if ( inst.ignores.isEmpty() )
            joiner.add( Utils.join( "No Ignores set. Add some with the ", Col.GOLD, "/sentry ignore ", Col.RESET, "command" ) );
        else
            inst.ignores.stream().forEach( i -> joiner.add( i.getPrettyString() ) );
        
        joiner.add( Utils.join( Col.YELLOW, "Events:-" ) );
        if ( inst.events.isEmpty() )
            joiner.add( Utils.join( "No Events set. Add some with the ", Col.GOLD, "/sentry event ", Col.RESET, "command" ) );
        else
            inst.events.stream().forEach( e -> joiner.add( e.getPrettyString() ) );
    }
}
