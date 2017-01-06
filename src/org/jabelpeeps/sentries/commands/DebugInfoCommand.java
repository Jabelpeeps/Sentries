package org.jabelpeeps.sentries.commands;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.TargetType;
import net.citizensnpcs.api.npc.NPC;

public class DebugInfoCommand implements SentriesComplexCommand {
    @Getter final String shortHelp = "view debug information for a sentry";
    @Getter final String longHelp = "Displays a page of internal information for a sentry.";
    @Getter final String perm = S.PERM_CITS_ADMIN;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

        StringJoiner joiner = new StringJoiner( System.lineSeparator() );
        NPC npc = inst.getNPC();

        joiner.add( String.join( "", Col.GOLD, "------- Debug Info for ", npcName, " (npcid - ",
                                    String.valueOf( inst.getNPC().getId() ), ") ", "------" ) );
               
        joiner.add( String.join( "", Col.BLUE, "Status: ", Col.WHITE, inst.myStatus.toString() ) );
        joiner.add( String.join( "", Col.BLUE, "Mounted: ", Col.WHITE, String.valueOf( inst.hasMount() ), 
                                 inst.hasMount() ? ( " (mountID = " + inst.mountID + ")" ) : "" ) );
        
        joiner.add( String.join( "", Col.BLUE, "StoredLocation: ", Col.WHITE, Utils.prettifyLocation( npc.getStoredLocation() ) ) );
        
        joiner.add( String.join( "", Col.BLUE, "Spawn Point: ", Col.WHITE, Utils.prettifyLocation( inst.spawnLocation ), 
                " (distance to:- ", inst.spawnLocation.getWorld() != npc.getStoredLocation().getWorld() 
                                    ? "not in current world)"
                                    : ( Utils.formatDbl( inst.spawnLocation.distance( npc.getStoredLocation() ) ) + " )" ) ) );

        Navigator navigator = inst.getNavigator();
        if ( navigator.isNavigating() ) {
            joiner.add( String.join( "", Col.BLUE, "PathFinding range: ", Col.WHITE, 
                                            String.valueOf( navigator.getDefaultParameters().range() ) ) );
            TargetType tt = navigator.getTargetType();
            joiner.add( String.join( "", Col.BLUE, "Currently Navigating to: ", Col.WHITE, tt.toString(), 
                             tt == TargetType.ENTITY ? navigator.getEntityTarget().getTarget().getName() : "", 
                                            " at ", Utils.prettifyLocation( navigator.getTargetAsLocation() ) ) );
        }
        sender.sendMessage( joiner.toString() );
    }
}

