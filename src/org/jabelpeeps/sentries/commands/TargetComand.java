package org.jabelpeeps.sentries.commands;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.TargetType;


public class TargetComand implements SentriesComplexCommand {

    private String targetCommandHelp;

    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
                
        if ( args.length <= nextArg + 1 ) {
            sender.sendMessage( getLongHelp() );
            return true;
        }
        if ( S.LIST.equals( args[nextArg + 1] ) ) {
            StringJoiner joiner = new StringJoiner( ", " );
            
            for ( TargetType each : inst.targets ) {
                joiner.add( each.getTargetString() );
            }
            joiner.add( inst.validTargets.toString() );
            
            sender.sendMessage( String.join( "", Col.GREEN, "Targets: ", joiner.toString() ) );
            return true;
        }
        if ( S.CLEARALL.equals( args[nextArg + 1] ) ) {
            inst.targets.clear();
            inst.validTargets.clear();
            inst.targetFlags = 0;
            inst.clearTarget();
            sender.sendMessage( String.join( "", Col.GREEN, npcName, ": ALL Targets cleared" ) );
            return true;
        }
        if ( args.length > 2 + nextArg ) {
            sender.sendMessage( CommandHandler.parseTargetOrIgnore( args, nextArg, npcName, inst, true ) );
            return true;
        }                
        return false;
    }

    @Override
    public String getShortHelp() {
        return "set targets to attack.";
    }

    @Override
    public String getLongHelp() {
        
        if ( targetCommandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( String.join( "", Col.GOLD, "do '/sentry target <option>' where <option> is:-", Col.RESET ) );
            joiner.add( String.join( " ", Col.GOLD, "", S.LIST, Col.RESET, S.HELP_LIST, S.TARGETS ) );
            joiner.add( String.join( " ", Col.GOLD, "", S.CLEARALL, Col.RESET, S.HELP_CLEAR, S.TARGETS ) );
            joiner.add( String.join( " ", Col.GOLD, S.HELP_ADD_TYPE, Col.RESET, S.HELP_ADD ) );
            joiner.add( String.join( " ", Col.GOLD, S.HELP_REMOVE_TYPE, Col.RESET, S.HELP_REMOVE ) );
            joiner.add( S.HELP_ADD_REMOVE_TYPES );
            joiner.add( PluginBridge.getAdditionalTargets() );

            targetCommandHelp = joiner.toString();
        }       
        return targetCommandHelp;
    }

    @Override
    public String getPerm() {
        return S.PERM_TARGET;
    }
}
