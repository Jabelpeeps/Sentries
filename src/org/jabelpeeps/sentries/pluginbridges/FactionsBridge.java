package org.jabelpeeps.sentries.pluginbridges;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;

public class FactionsBridge implements PluginBridge {

    private final static String PREFIX = "FACTIONS";
    private String commandHelp = String.join( "", "  using the ", Col.GOLD, "/sentry ", PREFIX.toLowerCase()," ... ", Col.RESET, "commands." );
    private SentriesComplexCommand command = new FactionsCommand();

    @Override
    public boolean activate() { 
        CommandHandler.addCommand( PREFIX.toLowerCase(), command );
        return true; 
        }

    @Override
    public String getPrefix() { return PREFIX; }

    @Override
    public String getActivationMessage() { return "Factions is active, the FACTION: target will function"; }

    @Override
    public String getCommandHelp() { return commandHelp; }

    @Override
    public void add( SentryTrait inst, String args ) {       
        command.call( null, null, inst, 0, Util.colon.split( args ) );
    }

    public class FactionsCommand implements SentriesComplexCommand {

        private String helpTxt;
        
        @Override
        public String getShortHelp() { return ""; }

        @Override
        public String getPerm() { return "sentry.factions"; }
        
        @Override
        public String getLongHelp() {

            if ( helpTxt == null ){
                StringJoiner joiner = new StringJoiner( System.lineSeparator() );
                
                joiner.add( String.join( "", "do ", Col.GOLD, "/sentry ", PREFIX.toLowerCase(), " <target|ignore|list|remove|join|leave|clearall> <Faction> ", 
                                                    Col.RESET, "where <Faction> is a valid current Faction name." ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "target ", Col.RESET, "to have a sentry attack members of <Faction>" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "ignore ", Col.RESET, "to have a sentry ignore members of <Faction>" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "list ", Col.RESET, "to display the current Factions target information." ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "remove ", Col.RESET, "to remove target or ignore for <Faction>" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "join ", Col.RESET, "to attack members of enemy factions (and ignore allies, and truce factions)." ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "leave ", Col.RESET, "to reverse a 'join' command." ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all Factions targets.") );
                
                helpTxt = joiner.toString();
            }         
            return helpTxt;
        }

        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

            String subCommand = args[nextArg + 1].toLowerCase();
            
            if ( S.LIST.equals( subCommand ) ) {
                StringJoiner joiner = new StringJoiner( ", " );
                
                inst.targets.stream().filter( t -> t instanceof AbstractFactionTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.RED, "Target: ", t.getTargetString().split( ":" )[2] ) ) );
                
                inst.ignores.stream().filter( t -> t instanceof AbstractFactionTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.GREEN, "Ignore: ", t.getTargetString().split( ":" )[2] ) ) );
                
                if ( joiner.length() < 1 ) 
                    Util.sendMessage( sender, Col.YELLOW, npcName, " has no Factions targets or ignores" );
                else
                    sender.sendMessage( joiner.toString() );
                return;
            }
            
            if ( S.CLEARALL.equals( subCommand ) ) {                
                inst.targets.removeIf( t -> t instanceof AbstractFactionTarget );
                inst.ignores.removeIf( t -> t instanceof AbstractFactionTarget );
                
                Util.sendMessage( sender, Col.GREEN, "All Factions targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;              
            }
            
            if ( args.length <= nextArg + 2 ) {
                Util.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help factions" );
                return;    
            }
            String factionName = args[nextArg + 2];
            Faction faction = FactionColl.get().getByName( factionName );
            
            if ( faction == null ) {
                Util.sendMessage( sender, S.ERROR, "No Faction was found matching:- ", factionName );
                return;    
            }
          
            if ( (S.REMOVE + S.TARGET + S.IGNORE).contains( subCommand ) ) {
                
                TargetType target = new FactionTarget( faction );

                if ( S.REMOVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( target ) ) {
                        Util.sendMessage( sender, Col.GREEN, faction.getName(), " was removed from ", npcName, "'s list of targets." );
                        inst.checkIfEmpty( sender );
                    }
                    else if ( inst.ignores.remove( target ) ) {
                        Util.sendMessage( sender, Col.GREEN, faction.getName(), " was removed from ", npcName, "'s list of ignores." );
                        inst.checkIfEmpty( sender );
                    }
                    else
                        Util.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", faction.getName() );
                    return;
                }
                
                target.setTargetString( String.join( ":", PREFIX, subCommand, factionName ) );
                
                if ( S.TARGET.equals( subCommand ) ) {
                    
                    if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                        Util.sendMessage( sender, Col.GREEN, "Faction: ", faction.getName(), " will be targeted by ", npcName );
                    else 
                        Util.sendMessage( sender, Col.RED, faction.getName(), S.ALREADY_LISTED, npcName );
         
                    return;  
                }
                
                if ( S.IGNORE.equals( subCommand ) ) {
                    
                    if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                        Util.sendMessage( sender, Col.GREEN, "Faction: ", faction.getName(), " will be ignored by ", npcName );
                    else 
                        Util.sendMessage( sender, Col.RED, faction.getName(), S.ALREADY_LISTED, npcName );

                    return; 
                }
            }
            if ( (S.LEAVE + S.JOIN).contains( subCommand ) ) {
                
                TargetType rivals = new FactionRivalsTarget( faction );
                TargetType allies = new FactionAlliesTarget( faction );
                
                if ( S.LEAVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( rivals ) && inst.ignores.remove( allies ) )
                        Util.sendMessage( sender, Col.GREEN, npcName, " will no longer fight alongside ", faction.getName() );
                    else
                        Util.sendMessage( sender, Col.RED, npcName, " never considered ", faction.getName(), " to be brothers in arms!" );
                    
                    inst.checkIfEmpty( sender );
                    return;
                } 
                
                if ( S.JOIN.equals( subCommand ) ) {
                    rivals.setTargetString( String.join( ":", PREFIX, subCommand, args[nextArg + 2] ) );
                    
                    if ( inst.targets.add( rivals ) && inst.ignores.add( allies ) )
                        Util.sendMessage( sender, Col.GREEN, npcName, " will support ", faction.getName(), " in all battles!" );
                    return;
                }
            }
            Util.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                    Col.GOLD, "/sentry help factions", Col.RESET, " and try again." );            
        }       
    }
    
    protected abstract class AbstractFactionTarget extends AbstractTargetType {
        
        protected Faction faction;

        protected AbstractFactionTarget( int i ) { 
            super( i );
        }
        @Override
        public int hashCode() { return faction.hashCode(); }
    }
    
    public class FactionTarget extends AbstractFactionTarget {

        FactionTarget( Faction f ) {
            super( 57 );
            faction = f;
        }

        @Override
        public boolean includes( LivingEntity entity ) {            
            if ( !(entity instanceof Player) ) return false;
            
            MPlayer player = MPlayer.get( entity );
            
            return player != null && faction.equals( player.getFaction() );
        }

        @Override
        public boolean equals( Object o ) {
            return  o != null
                    && o instanceof FactionTarget
                    && ((FactionTarget) o).faction.equals( faction );
        }        
    }
    
    public class FactionRivalsTarget extends AbstractFactionTarget {
               
        FactionRivalsTarget(  Faction f ) {
            super( 56 );
            faction = f;
        }
        @Override
        public boolean includes( LivingEntity entity ) {          
            if ( !(entity instanceof Player) ) return false;
            
            MPlayer player = MPlayer.get( entity );      
            
            return  player != null
                    && (    faction.getRelationTo( player ) == Rel.ENEMY 
                            || player.getFaction().getRelationTo( faction ) == Rel.ENEMY );    
        }     
        @Override
        public boolean equals( Object o ) {          
            return  o != null
                    && o instanceof FactionRivalsTarget
                    && ((FactionRivalsTarget) o).faction.equals( faction );           
        }  
   }
    
   public class FactionAlliesTarget extends AbstractFactionTarget {
        
        FactionAlliesTarget(  Faction f ) {
            super( 55 );
            faction = f;
        }
        @Override
        public boolean includes( LivingEntity entity ) {           
            if ( !(entity instanceof Player) ) return false;
            
            MPlayer player = MPlayer.get( entity );     
            
            return  player != null 
                    && faction.getRelationTo( player ).isFriend();
        }        
        @Override
        public boolean equals( Object o ) {           
            return  o != null
                    && o instanceof FactionAlliesTarget
                    && ((FactionAlliesTarget) o).faction.equals( faction );           
        }  
    }
}
