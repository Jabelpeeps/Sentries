package org.jabelpeeps.sentry.pluginbridges;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.bukkit.entity.Player;
import org.jabelpeeps.sentry.PluginBridge;
import org.jabelpeeps.sentry.S;
import org.jabelpeeps.sentry.SentryTrait;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.utils.CombatUtil;

public class TownyBridge extends PluginBridge {
    
    /*
     * Notes for self:
     * - Towny disables friendly fire between members of the same town, same nation, and nation allies. (by default)
     * - Towns cannot declare each other enemies, only nations. 
     * - only Nations can declare wars.
     * - one town can be a nation - size to enable is configurable.
     * 
     * (taken from http://towny.palmergames.com/towny/757-2/#How_Towny_Controls_PVP_Combat )
     */

    Map<SentryTrait, Town> myTown = new HashMap<SentryTrait, Town>();
    String commandHelp;

    public TownyBridge( int flag ) { super( flag ); }

    @Override
    public boolean activate() { return true; }

    @Override
    public String getPrefix() { return "TOWNY"; }

    @Override
    public String getActivationMessage() { return "Detected Towny, the TOWNY: target will function"; }

    @Override
    public String getCommandHelp() {

        if ( commandHelp == null ) {
            StringJoiner joiner = new StringJoiner( System.lineSeparator() );

            joiner.add( "add/remove Towny:<town_name> to have a Sentry join/leave a town.");
            joiner.add( "The Sentry will then follow friendly fire restrictions, and attack national enemies.");

            commandHelp = joiner.toString();
        }
        return commandHelp;
    }

    @Override
    public boolean isTarget( Player player, SentryTrait inst ) {
        return !CombatUtil.preventDamageCall( inst.getMyEntity(), player );
    }

    @Override
    public boolean isIgnoring( Player player, SentryTrait inst ) {
        return CombatUtil.preventDamageCall( inst.getMyEntity(), player );
    }

    @Override
    public String add( String target, SentryTrait inst, boolean asTarget ) {
        
        Town town = null;
        String outstring = "";
        try {
            town = TownyUniverse.getDataSource().getTown( target );
        } catch ( NotRegisteredException e ) {}
        
        if ( town == null  ) {
            outstring = S.Col.RED.concat( "Town not found." );
        }
        else if ( isListed( inst, false ) ) {
            outstring = String.join( "", S.Col.GREEN, "The Sentry has left ", 
                    myTown.replace( inst, town ).getName(), " and joined ", target );
        }
        else {
            myTown.put( inst, town );
            outstring = String.join( "", S.Col.GREEN, "The Sentry has joined ", target ); 
        }
        return outstring;
    }

    @Override
    public String remove( String entity, SentryTrait inst, boolean fromTargets ) {
        String outstring = "";
        
        if (    !isListed( inst, false ) 
                || !myTown.get( inst ).getName().equalsIgnoreCase( entity ) ) {;
            outstring = String.join( "", S.Col.RED, inst.getNPC().getName(), " is not a member of ", entity );
        }
        else {
            myTown.remove( inst );
            outstring = String.join( "", S.Col.GREEN, inst.getNPC().getName(), " has left ", entity );
        }
        return outstring;
    }

    @Override
    public boolean isListed( SentryTrait inst, boolean asTarget ) {
        return myTown.containsKey( inst );
    }
}
