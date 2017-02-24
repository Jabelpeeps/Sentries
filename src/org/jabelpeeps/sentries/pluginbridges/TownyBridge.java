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
import org.jabelpeeps.sentries.Utils;
import org.jabelpeeps.sentries.commands.SentriesCommand;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import lombok.Getter;

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

    final String prefix = "Towny";
    @Getter final String activationMessage = "Detected Towny, the TOWNY: target will function";
    
    @Override
    public boolean activate() {
        CommandHandler.addCommand( prefix.toLowerCase(), new TownyCommand() );
        return true; 
    }
    
    public class TownyCommand implements SentriesComplexCommand, SentriesCommand.Targetting {
        
        @Getter final String shortHelp = "have a sentry join a town";
        @Getter final String perm = "sentry.towny";
        private String helpTxt;
        
        @Override
        public String getLongHelp() {
            if ( helpTxt == null ) {
                helpTxt = Utils.join(  
                        "do ", Col.GOLD, "/sentry towny <join|leave|info|clearall> <TownName> ", Col.RESET, 
                        System.lineSeparator(), "  where ", Col.GOLD, "<TownName> ", Col.RESET, "must be a valid Towny Town name.",
                        System.lineSeparator(), "  use ", Col.GOLD, "join ", Col.RESET, "to join <TownName>", 
                                                "  A player-type sentry will then behave as though it were a town resident.",
                                                "  It will attack the members of enemy nations, and ignore allies.", 
                        System.lineSeparator(), "  use ", Col.GOLD, "leave ", Col.RESET, "to leave <TownName>",
                        System.lineSeparator(), "  use ", Col.GOLD, "info ", Col.RESET, "to see which (if any) Town is currently configured.",
                        System.lineSeparator(), "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all Towny targets.",
                        System.lineSeparator(), "do  ", Col.GOLD, "/sentry towny <target|ignore|remove> <TownName> ", Col.RESET,
                                                " to add or remove legacy Towny targets & ignores" );
            }
            return helpTxt; 
        }  
        
        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

            if ( args.length <= nextArg + 1 ) {
                Utils.sendMessage( sender, getLongHelp() );
                return;
            }
            
            String subCommand = args[nextArg + 1].toLowerCase();
            
            if ( S.INFO.equals( subCommand ) ) {
                
                StringJoiner joiner = new StringJoiner( ", " );
                
                inst.targets.stream().filter( t -> t instanceof TownyEnemyTarget )
                                             .forEach( t -> joiner.add( Utils.colon.split( t.getTargetString() )[2] ) );
                
                if ( joiner.length() < 3 )
                    Utils.sendMessage( sender, Col.YELLOW, npcName, " has not settled in a town yet." );
                else
                    Utils.sendMessage( sender, Col.YELLOW, npcName, " is a member of these Towns:-", Col.RESET, 
                                                                                System.lineSeparator(), joiner.toString() );

                StringJoiner joiner2 = new StringJoiner( ", " );
                
                inst.targets.stream().filter( t -> t instanceof TownyTarget )
                                     .forEach( t -> joiner2.add( 
                                             Utils.join( Col.RED, "Target: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
                
                inst.ignores.stream().filter( t -> t instanceof TownyTarget )
                                     .forEach( t -> joiner2.add( 
                                             Utils.join( Col.GREEN, "Ignore: ", Utils.colon.split( t.getTargetString() )[2]) ) );

                if ( joiner2.length() > 3 ) 
                    Utils.sendMessage( sender, Col.YELLOW, "These legacy Towny targets are also active:- ", System.lineSeparator(),
                                        joiner2.toString(), Col.RESET, "You could consider removing these, and using ", 
                                        Col.GOLD, "/sentry towny join <TownName>", Col.RESET, 
                                        " instead for behaviour that is more 'player-like'." );
                return;    
            }
            
            if ( S.CLEARALL.equals( subCommand ) ) {                
                inst.targets.removeIf( t -> t instanceof AbstractTownyTarget );
                inst.ignores.removeIf( t -> t instanceof AbstractTownyTarget );
                
                Utils.sendMessage( sender, Col.GREEN, "All Towny targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;              
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Utils.sendMessage( sender,  S.ERROR, " Not enough arguments. ", Col.RESET, "Try /sentry help towny" );
                return;
            }
            String townName = args[nextArg + 2];
            Town town = null;
            try {
                town = TownyUniverse.getDataSource().getTown( townName );
                
            } catch ( NotRegisteredException e ) {}
            
            if ( town == null ) {
                Utils.sendMessage( sender, S.ERROR, " No Town was found matching:- ", townName );
                return;
            } 
            
            if ( (S.LEAVE + S.JOIN).contains( subCommand ) ) {
                
                TargetType enemies = new TownyEnemyTarget( town ); 
                TargetType friends = new TownyFriendTarget( town ); 
                
                if ( S.LEAVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( enemies ) && inst.ignores.remove( friends ) )
                        Utils.sendMessage( sender, Col.GREEN, npcName, " is no-longer a resident of ", town.getName() );
                    else {
                        Utils.sendMessage( sender, Col.YELLOW, npcName, " is unknown in ", town.getName() );
                        if ( sender != null ) call( sender, npcName, inst, 0, "", S.INFO );
                    }
                    return;
                }
                // we only need to set the targetString on one TargetType instance, as they are created and removed in pairs.
                enemies.setTargetString( String.join( ":", prefix, S.JOIN, townName ) );
                
                if ( S.JOIN.equals( subCommand ) 
                        && inst.targets.add( enemies ) 
                        && inst.ignores.add( friends ) ) {
                    Utils.sendMessage( sender, Col.GREEN, npcName, " has settled in ", town.getName() ); 
                    return;
                }
            }             
            if ( (S.REMOVE + S.TARGET + S.IGNORE).contains( subCommand ) ) {
                
                TargetType target = new TownyTarget( town );

                if ( S.REMOVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( target ) ) {
                        Utils.sendMessage( sender, Col.GREEN, town.getName(), " was removed from ", npcName, "'s list of targets." );
                        inst.checkIfEmpty( sender );
                    }
                    else if ( inst.ignores.remove( target ) ) {
                        Utils.sendMessage( sender, Col.GREEN, town.getName(), " was removed from ", npcName, "'s list of ignores." );
                        inst.checkIfEmpty( sender );
                    }
                    else {
                        Utils.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", town.getName() );
                        if ( sender != null ) call( sender, npcName, inst, 0, "", S.INFO );
                    }
                    return;
                }
                
                target.setTargetString( String.join( ":", prefix, subCommand, townName ) );
                
                if ( S.TARGET.equals( subCommand ) ) {
                    
                    if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                        Utils.sendMessage( sender, Col.GREEN, "Town: ", town.getName(), " will be targeted by ", npcName );
                    else 
                        Utils.sendMessage( sender, Col.RED, town.getName(), S.ALREADY_LISTED, npcName );

                    if ( sender != null ) call( sender, npcName, inst, 0, "", S.LIST );
                    return;  
                }
                
                if ( S.IGNORE.equals( subCommand ) ) {
                    
                    if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                        Utils.sendMessage( sender, Col.GREEN, "Town: ", town.getName(), " will be ignored by ", npcName );
                    else 
                        Utils.sendMessage( sender, Col.RED, town.getName(), S.ALREADY_LISTED, npcName );

                    if ( sender != null ) call( sender, npcName, inst, 0, "", S.LIST );
                    return; 
                }
            }        
            Utils.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                            Col.GOLD, "/sentry help towny", Col.RESET, " and try again." ); 
        }   
    }
    
    protected abstract static class AbstractTownyTarget extends AbstractTargetType {

        protected final Town town;
        protected static TownyDataSource townyData = TownyUniverse.getDataSource();
        
        protected AbstractTownyTarget( int i, Town t ) { super( i ); town = t; }
        @Override
        public int hashCode() { return town.hashCode(); }
    }
    
    public static class TownyTarget extends AbstractTownyTarget {

        protected TownyTarget( Town t ) { super( 57, t ); }

        @Override
        public boolean includes( LivingEntity entity ) {
            try {
                Resident resident = townyData.getResident( entity.getName() );
                
                return  town.hasResident( resident );
             
            } catch ( NotRegisteredException e ) {
                if ( Sentries.debug ) {
                    Sentries.debugLog( "TownyTarget has thrown NotRegisteredException" );
                }
                return false;
            } 
        }
        @Override
        public boolean equals( Object o ) {           
            return  o != null 
                    && o instanceof TownyTarget
                    && ((TownyTarget)o).town.equals( town );   
        }
    }
    
    public static class TownyEnemyTarget extends AbstractTownyTarget {
        
        protected TownyEnemyTarget( Town target ) { super( 55, target ); }     
        @Override
        public boolean includes( LivingEntity entity ) {
            try {
                Resident resident = townyData.getResident( entity.getName() );
                
                return  !town.hasResident( resident )
                        && resident.getTown().getNation().hasEnemy( town.getNation() );
             
            } catch ( NotRegisteredException e ) {
                if ( Sentries.debug ) {
                    Sentries.debugLog( "TownyEnemyTarget has thrown NotRegisteredException" );
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
    
    public static class TownyFriendTarget extends AbstractTownyTarget {
        
        protected TownyFriendTarget( Town target ) { super( 56, target ); }        
        @Override
        public boolean includes( LivingEntity entity ) {
            try {
                Resident resident = townyData.getResident( entity.getName() );
                
                return  town.hasResident( resident )
                        || resident.getTown().getNation().hasAlly( town.getNation() );
                
            } catch ( NotRegisteredException e ) {
                if ( Sentries.debug ) {
                    Sentries.debugLog( "TownyFriendTarget has thrown NotRegisteredException" );
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
