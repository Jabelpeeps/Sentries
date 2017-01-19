package org.jabelpeeps.sentries.commands;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class GuardCommand implements SentriesComplexCommand {

    private String guardCommandHelp;
    @Getter private String shortHelp = "tell the sentry what to guard"; 
    @Getter private String perm = S.PERM_GUARD;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length > nextArg + 1 ) {
            
            if ( S.CLEAR.equalsIgnoreCase( args[nextArg + 1] ) ) {
                inst.guardeeID = null;
                inst.guardeeName = null;
                inst.guardeeEntity = null;
            }
            else if ( "me".equalsIgnoreCase( args[nextArg +1] ) ) {
                if ( sender instanceof Player ) {
                    Player player = (Player) sender;
                    inst.guardeeEntity = player;
                    inst.guardeeID = player.getUniqueId();
                    inst.guardeeName = player.getName();
                }
                else Utils.sendMessage( sender, S.ERROR, "That command can only be used by Players" );
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

                String arg = Utils.joinArgs( start + nextArg, args );

                if ( checkplayers ) ok = inst.findPlayerGuardEntity( arg );   
                if ( !ok && checklocal ) ok = inst.findOtherGuardEntity( arg );

                if ( ok )
                    Utils.sendMessage( sender, Col.GREEN, npcName, " is now guarding ", arg );
                else
                    Utils.sendMessage( sender, Col.RED, npcName, " could not find ", arg );
                return;
            }
        }
        if ( inst.guardeeEntity != null )
            Utils.sendMessage( sender, Col.BLUE, "Guarding: ", inst.guardeeEntity.getName() );
        else if ( inst.guardeeName != null && !inst.guardeeName.isEmpty() )
            Utils.sendMessage( sender, Col.GREEN, npcName, " is configured to guard ", inst.guardeeName, 
                                                                   " but cannot find them at the moment." );
        else Utils.sendMessage( sender, Col.GREEN, "Guarding: My Surroundings" );
    }

    @Override
    public String getLongHelp() {        
        if ( guardCommandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry guard ", Col.RESET, "to discover what a sentry is guarding" ) );
            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry guard clear ", Col.RESET, "to clear the player/npc being guarded" ) );
            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry guard me ", Col.RESET, "to have a sentry guard you" ) );
            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry guard (-p/l) <EntityName> ", Col.RESET, 
                                                    "to have a sentry guard a player, or another NPC" ) );
            joiner.add( Utils.join( Col.GOLD, "    -p ", Col.RESET, "-> only search player names" ) );
            joiner.add( Utils.join( Col.GOLD, "    -l ", Col.RESET, "-> only search local entities" ) );
            joiner.add( "    -> only use one of -p or -l (or omit)" );

            guardCommandHelp = joiner.toString();
        }
        return guardCommandHelp;
    }
}
