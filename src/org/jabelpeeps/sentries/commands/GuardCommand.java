package org.jabelpeeps.sentries.commands;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryStatus;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class GuardCommand implements SentriesComplexCommand {

    private String guardCommandHelp;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length > nextArg + 1 ) {
            
            if ( S.CLEAR.equalsIgnoreCase( args[nextArg + 1] ) ) {
                inst.guardeeName = null;
                inst.guardeeEntity = null;
            }
            else {
                boolean checklocal = true, checkplayers = true;
                int start = 1;
                boolean ok = false;

                if ( args[nextArg + 1].equalsIgnoreCase( "-p" ) ) {
                    start = 2;
                    checklocal = false;
                }

                if ( args[nextArg + 1].equalsIgnoreCase( "-l" ) ) {
                    start = 2;
                    checkplayers = false;
                }

                String arg = Util.joinArgs( start + nextArg, args );

                if ( checkplayers ) ok = inst.findPlayerGuardEntity( arg );   
                if ( !ok && checklocal ) ok = inst.findOtherGuardEntity( arg );

                if ( ok ) {
                    Util.sendMessage( sender, Col.GREEN, npcName, " is now guarding ", arg );
                    inst.myStatus = SentryStatus.FOLLOWING;
                }
                else
                    Util.sendMessage( sender, Col.RED, npcName, " could not find ", arg );
                return;
            }
        }
        if ( inst.guardeeName == null )
            sender.sendMessage( Col.GREEN.concat( "Guarding: My Surroundings" ) );
        else if ( inst.guardeeEntity == null )
            Util.sendMessage( sender, Col.GREEN, npcName, " is configured to guard ", inst.guardeeName, " but cannot find them at the moment." );
        else
            Util.sendMessage( sender, Col.BLUE, "Guarding: ", inst.guardeeEntity.getName() );
    }

    @Override
    public String getShortHelp() { return "tell the sentry what to guard" ; }

    @Override
    public String getLongHelp() {
        
        if ( guardCommandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry guard", Col.RESET ) );
            joiner.add( "  to discover what a sentry is guarding" );
            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry guard clear", Col.RESET ) );
            joiner.add( "  to clear the player/npc being guarded" );
            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry guard (-p/l) <EntityName>", Col.RESET ) );
            joiner.add( "  to have a sentry guard a player, or another NPC" );
            joiner.add( String.join( "", Col.GOLD, "    -p ", Col.RESET, "-> only search player names" ) );
            joiner.add( String.join( "", Col.GOLD, "    -l ", Col.RESET, "-> only search local entities" ) );
            joiner.add( "    -> only use one of -p or -l (or omit)" );

            guardCommandHelp = joiner.toString();
        }
        return guardCommandHelp;
    }
    
    @Override
    public String getPerm() { return S.PERM_GUARD; }
}
