package org.jabelpeeps.sentries.pluginbridges;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyBridge implements PluginBridge {
    
    /*
     * Notes for self:
     * - Towny disables friendly fire between members of the same town, same nation, and nation allies. (by default)
     * - Towns cannot declare each other enemies, only nations. 
     * - only Nations can declare wars.
     * - one town can be a nation - size to enable is configurable.
     * 
     * (taken from http://towny.palmergames.com/towny/757-2/#How_Towny_Controls_PVP_Combat )
     */

    private SentriesComplexCommand command = new TownyCommand();
    private String commandHelp = String.join( "", "  using the ", Col.GOLD, "/sentry towny ... ", Col.RESET, "commands." );
    final static String PREFIX = "TOWNY";
    
    @Override
    public boolean activate() {
        CommandHandler.addCommand( PREFIX.toLowerCase(), command );
        return true; 
    }
    @Override
    public String getPrefix() { return PREFIX; }

    @Override
    public String getActivationMessage() { return "Detected Towny, the TOWNY: target will function"; }

    @Override
    public String getCommandHelp() { return commandHelp; }

    @Override
    public void add( SentryTrait inst, String args ) {       
        command.call( null, null, inst, 0, Util.colon.split( args ) );
    }
    
    public class TownyCommand implements SentriesComplexCommand {
        
        private String helpTxt = String.join( "", "do ", Col.GOLD, "/sentry towny <join|leave|info> <TownName> ", Col.RESET, 
                "to have a player-type sentry behave as though it were a town resident.  It will attack the members of enemy nations, and ignore allies.", 
                System.lineSeparator(), "  use ", Col.GOLD, "join ", Col.RESET, "to join <TownName>", 
                System.lineSeparator(), "  use ", Col.GOLD, "leave ", Col.RESET, "to leave <TownName>",
                System.lineSeparator(), "  (", Col.GOLD, "<TownName> ", Col.RESET, "must be a valid Towny Town name.",
                System.lineSeparator(), "  use ", Col.GOLD, "info ", Col.RESET, "to see which (if any) Town is currently configured.");
        
        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

            String subCommand = args[nextArg + 1].toLowerCase();
            
            if ( S.INFO.equals( subCommand ) ) {
                
                StringJoiner joiner = new StringJoiner( ", " );
                
                inst.targets.parallelStream().filter( t -> t instanceof TownyEnemyTarget )
                                             .forEach( t -> joiner.add( t.getTargetString().split( ":" )[2] ) );
                
                if ( joiner.length() < 3 )
                    Util.sendMessage( sender, Col.YELLOW, npcName, " is a member of ", joiner.toString() );
                else
                    Util.sendMessage( sender, Col.YELLOW, npcName, " has not settled in a town yet." );
                return;    
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Util.sendMessage( sender,  S.ERROR, " Not enough arguments. ", Col.RESET, "Try /sentry help towny" );
                return;
            }
            String townName = args[nextArg + 2];
            Town town = null;
            try {
                town = TownyUniverse.getDataSource().getTown( townName );
                
            } catch ( NotRegisteredException e ) {}
            
            if ( town == null ) {
                Util.sendMessage( sender, S.ERROR, " No Town was found matching:- ", townName );
                return;
            } 
            
            if ( (S.LEAVE + S.JOIN).contains( subCommand ) ) {
                
                TargetType enemies = new TownyEnemyTarget( town ); 
                TargetType friends = new TownyFriendTarget( town ); 
                
                if ( S.LEAVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( enemies ) && inst.ignores.remove( friends ) )
                        Util.sendMessage( sender, Col.GREEN, npcName, " is no-longer a resident of ", town.getName() );
                    else 
                        Util.sendMessage( sender, Col.YELLOW, npcName, " is unknown in ", town.getName() );
                    
                    return;
                }
                // we only need to set the targetString on one TargetType instance, as they are created and removed in pairs.
                enemies.setTargetString( String.join( ":", PREFIX, S.JOIN, townName ) );
                
                if ( S.JOIN.equals( subCommand ) 
                        && inst.targets.add( enemies ) 
                        && inst.ignores.add( friends ) ) {
                    Util.sendMessage( sender, Col.GREEN, npcName, " has settled in ", town.getName() ); 
                    return;
                }
            }         
            Util.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                            Col.GOLD, "/sentry help towny", Col.RESET, " and try again." ); 
        } 
        
        @Override
        public String getShortHelp() { return "have a sentry join a town"; }
        
        @Override
        public String getPerm() { return "sentry.towny"; }  

        @Override
        public String getLongHelp() { return helpTxt; }    
    }
    
    protected abstract class AbstractTownyTarget extends AbstractTargetType {

        protected Town town;
        protected TownyDataSource townyData = TownyUniverse.getDataSource();
        
        protected AbstractTownyTarget( int i ) { super( i ); }
        @Override
        public int hashCode() { return town.hashCode(); }
    }
    
    public class TownyEnemyTarget extends AbstractTownyTarget {
        
        TownyEnemyTarget( Town target ) { 
            super( 55 );
            town = target; 
        }     
        @Override
        public boolean includes( LivingEntity entity ) {
            try {
                return townyData.getResident( entity.getName() ).getTown().getNation().hasEnemy( town.getNation() );
             
            } catch ( NotRegisteredException e ) {
                if ( Sentries.debug ) {
                    Sentries.debugLog( "TownyEnemyTarget has thrown NotRegisteredException" );
                    e.printStackTrace();
                }
                return false;
            }      
        }      
        @Override
        public boolean equals( Object o ) {           
            return  o != null 
                    && o instanceof TownyEnemyTarget
                    && ((TownyEnemyTarget)o).town.equals( town );            
        }
    }
    
    public class TownyFriendTarget extends AbstractTownyTarget {
        
        TownyFriendTarget( Town target ) { 
            super( 56 );
            town = target; 
        }        
        @Override
        public boolean includes( LivingEntity entity ) {
            try {
                Resident resident = townyData.getResident( entity.getName() );
                
                return  town.hasResident( resident )
                        || resident.getTown().getNation().hasAlly( town.getNation() );
                
            } catch ( NotRegisteredException e ) {
                if ( Sentries.debug ) {
                    Sentries.debugLog( "TownyEnemyTarget has thrown NotRegisteredException" );
                    e.printStackTrace();
                }
                return false;
            }      
        }      
        @Override
        public boolean equals( Object o ) {
            return  o != null 
                    && o instanceof TownyFriendTarget
                    && ((TownyFriendTarget) o).town.equals( town );            
        }
    }
}
