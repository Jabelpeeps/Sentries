package org.jabelpeeps.sentries.commands;

import java.util.StringJoiner;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.AttackType;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.TargetType;
import net.citizensnpcs.api.npc.NPC;

public class DebugInfoCommand implements SentriesComplexCommand {
    
    @Getter final String shortHelp = "view a sentry's debug information";
    @Getter final String longHelp = "Displays a page of internal field values and other information for a sentry.";
    @Getter final String perm = S.PERM_CITS_ADMIN;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

        StringJoiner joiner = new StringJoiner( System.lineSeparator() );
        NPC npc = inst.getNPC();

        joiner.add( Utils.join( Col.GOLD, "------- Debug Info for ", npcName, " (npcid - ",
                                    String.valueOf( inst.getNPC().getId() ), ") ", "------" ) );
               
        joiner.add( Utils.join( Col.BLUE, "Status: ", Col.WHITE, inst.getMyStatus().toString() ) );
        joiner.add( Utils.join( Col.BLUE, "Mounted: ", Col.WHITE, String.valueOf( inst.hasMount() ), 
                                 inst.hasMount() ? ( " (mountID = " + inst.mountID + ")" ) : "" ) );
        AttackType attack = inst.getMyAttack();
        if ( attack != null )
            joiner.add( Utils.join( Col.BLUE, "AttackType: ", Col.WHITE, attack.name() ) );
        
        Location stored = npc.getStoredLocation();
        joiner.add( Utils.join( Col.BLUE, "StoredLocation: ", Col.WHITE, Utils.prettifyLocation( stored ) ) );
        
        if ( inst.spawnLocation != null )
            joiner.add( Utils.join( Col.BLUE, "Spawn Point: ", Col.WHITE, 
                                    Utils.prettifyLocation( inst.spawnLocation ), 
                                    distanceOf( stored, inst.spawnLocation ) ) );

        Navigator navigator = inst.getNavigator();
        if ( navigator.isNavigating() ) {
            joiner.add( Utils.join( Col.BLUE, "PathFinding range: ", Col.WHITE, 
                                            String.valueOf( navigator.getDefaultParameters().range() ) ) );
            TargetType tt = navigator.getTargetType();
            Location target = navigator.getTargetAsLocation();
            joiner.add( Utils.join( Col.BLUE, "Currently Navigating to: ", Col.WHITE, tt.toString(), 
                             ( tt == TargetType.ENTITY ? navigator.getEntityTarget().getTarget().getName() : "" ), 
                             " at ", Utils.prettifyLocation( target ), distanceOf( stored, target ) ) );
        }
        else joiner.add( Utils.join( Col.BLUE, "Not currently navigating" ) );
        
        sender.sendMessage( joiner.toString() );
    }
    private String distanceOf( Location from, Location to ) {
        return from.getWorld() != to.getWorld() 
                ? "(not in current world)"
                : Utils.join( " (at distance of:- ", Utils.formatDbl( from.distance( to ) ), " )" );
    }
}

