package org.jabelpeeps.sentries.pluginbridges;

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

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

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

    private SentriesComplexCommand command = new TownyCommand();
    private String commandHelp = String.join( "", "  using the ", Col.GOLD, "/sentry towny ... ", Col.RESET, "commands." );

    public TownyBridge( int flag ) { super( flag ); }

    @Override
    public boolean activate() {
        CommandHandler.addCommand( "towny", command );
        return true; 
    }
    @Override
    public String getPrefix() { return "TOWNY"; }

    @Override
    public String getActivationMessage() { return "Detected Towny, the TOWNY: target will function"; }

    @Override
    public String getCommandHelp() { return commandHelp; }

    @Override
    public boolean add( SentryTrait inst, String args ) {
        command.call( null, null, inst, 0, args );
        return true;
    }
    
    public class TownyCommand implements SentriesComplexCommand {
        
        private String helpTxt;
        
        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
            
            if ( args.length <= nextArg + 2 ) { 
                Util.sendMessage( sender,  S.ERROR, " Not enough arguments. ", Col.RESET, "Try /sentry help towny" );
                return;
            }
            
            Town town = null;
            try {
                town = TownyUniverse.getDataSource().getTown( args[nextArg + 2] );
                
            } catch ( NotRegisteredException e ) {}
            
            if ( town == null ) {
                Util.sendMessage( sender, S.ERROR, " No Town was found matching:- ", args[nextArg + 2] );
                return;
            } 
            
            TargetType target = new TownyTarget( town, true ); 
            TargetType ignore = new TownyTarget( town, false ); 
            
            if ( S.LEAVE.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( inst.targets.remove( target ) && inst.ignores.remove( ignore ) )
                    Util.sendMessage( sender, Col.GREEN, npcName, " is no-longer a resident of ", town.getName() );
                else 
                    Util.sendMessage( sender, Col.YELLOW, npcName, " is unknown in ", town.getName() );
                
                return;
            }
            // we only need to set the targetString on one TownyTarget instance, as each command creates and removes two instances.
            target.setTargetString( String.join( ":", getPrefix(), S.JOIN, args[nextArg + 2] ) );
            
            if ( S.JOIN.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( inst.targets.add( target ) && inst.ignores.add( ignore ) )
                    Util.sendMessage( sender, Col.GREEN, npcName, " has settled in ", town.getName() );              
            }
        } 
        @Override
        public String getShortHelp() { return "have a sentry join a town"; }
        
        @Override
        public String getPerm() { return "sentry.towny"; }  

        @Override
        public String getLongHelp() {
            if ( helpTxt == null ) {
                helpTxt = String.join( "", "do ", Col.GOLD, "/sentry towny <join|leave> <TownName> ", Col.RESET, 
                        "to have a player-type sentry behave as though it were a town resident.  It will attack the members of enemy nations, and ignore allies.", 
                        System.lineSeparator(), "  use ", Col.GOLD, "join ", Col.RESET, "to join <TownName>", 
                        System.lineSeparator(), "  use ", Col.GOLD, "leave ", Col.RESET, "to leave <TownName>",
                        System.lineSeparator(), "  (", Col.GOLD, "<TownName> ", Col.RESET, "must be a valid Towny Town name." );
            }
            return helpTxt;
        }    
    }
    
    public class TownyTarget extends AbstractTargetType {

        private Town town;
        private boolean forEnemies;
        
        TownyTarget( Town target, boolean toAttack ) { 
            super( 55 );
            town = target; 
            forEnemies = toAttack; 
        }
        
        @Override
        public boolean includes( LivingEntity entity ) {
            try {
                Resident theStranger = TownyUniverse.getDataSource().getResident( entity.getName() );
                
                if ( forEnemies ) {               
                    return town.getNation().hasEnemy( theStranger.getTown().getNation() );
                }            
                return town.hasResident( theStranger );
                
            } catch ( NotRegisteredException e ) {
                if ( Sentries.debug ) {
                    Sentries.debugLog( "TownyTarget has thrown NotRegisteredException" );
                    e.printStackTrace();
                }
                return false;
            }      
        }
        
        @Override
        public boolean equals( Object o ) {
            if (    o != null 
                    && o instanceof TownyTarget
                    && ((TownyTarget)o).town.equals( town )) 
                return true;
            
            return false;            
        }
        @Override
        public int hashCode() { return town.hashCode(); }
    }
}
