package org.jabelpeeps.sentries.pluginbridges;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginTargetBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;
import org.jabelpeeps.sentries.commands.SentriesCommand;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;

import lombok.Getter;

public class FactionsBridge implements PluginTargetBridge {

    @Getter final String prefix = "FACTIONS";
    @Getter private String activationMessage = "Factions is active, the FACTION: target will function";
    @Getter private String commandHelp = 
            Utils.join( "  using the ", Col.GOLD, "/sentry ", prefix.toLowerCase()," ... ", Col.RESET, "commands." );

    @Override
    public boolean activate() { 
        CommandHandler.addCommand( prefix.toLowerCase(), new FactionsCommand() );
        return true; 
    }

    public class FactionsCommand implements SentriesComplexCommand, SentriesCommand.Targetting {

        @Getter final String shortHelp = "manage targets based on Factions";
        @Getter final String perm = "sentry.factions";
        private String helpTxt;
        
        @Override
        public String getLongHelp() {

            if ( helpTxt == null ){
                StringJoiner joiner = new StringJoiner( System.lineSeparator() );
                
                joiner.add( Utils.join( "do ", Col.GOLD, "/sentry ", prefix.toLowerCase(), 
                                            " <target|ignore|list|remove|join|leave|clearall> <Faction> ", Col.RESET, 
                                            "where <Faction> is a valid current Faction name." ) );
                joiner.add( Utils.join( "  use ", Col.GOLD, "target ", Col.RESET, "to have a sentry attack members of <Faction>" ) );
                joiner.add( Utils.join( "  use ", Col.GOLD, "ignore ", Col.RESET, "to have a sentry ignore members of <Faction>" ) );
                joiner.add( Utils.join( "  use ", Col.GOLD, "list ", Col.RESET, "to display the current Factions target information." ) );
                joiner.add( Utils.join( "  use ", Col.GOLD, "remove ", Col.RESET, "to remove target or ignore for <Faction>" ) );
                joiner.add( Utils.join( "  use ", Col.GOLD, "join ", Col.RESET, "to attack members of enemy factions (and ignore allies, and truce factions)." ) );
                joiner.add( Utils.join( "  use ", Col.GOLD, "leave ", Col.RESET, "to reverse a 'join' command." ) );
                joiner.add( Utils.join( "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all Factions targets.") );
                
                helpTxt = joiner.toString();
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
            
            if ( S.LIST.equals( subCommand ) ) {
                StringJoiner joiner = new StringJoiner( ", " );
                
                inst.targets.stream()
                            .filter( t -> t instanceof FactionTarget )
                            .forEach( t -> joiner.add( Utils.join( Col.RED, "Target: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
                
                inst.ignores.stream()
                            .filter( t -> t instanceof FactionTarget )
                            .forEach( t -> joiner.add( Utils.join( Col.GREEN, "Ignore: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
 
                inst.targets.stream()
                            .filter( t -> t instanceof FactionRivalsTarget )
                            .forEach( t -> joiner.add( Utils.join( Col.BLUE, "Member of: ", Utils.colon.split( t.getTargetString() )[2] ) ) );

                if ( joiner.length() < 1 ) 
                    Utils.sendMessage( sender, Col.YELLOW, npcName, " has no Factions targets or ignores" );
                else
                    Utils.sendMessage( sender, Col.YELLOW, "Current Factions targets are:-", Col.RESET, System.lineSeparator(), joiner.toString() );
                return;
            }
            
            if ( S.CLEARALL.equals( subCommand ) ) {                
                inst.targets.removeIf( t -> t instanceof AbstractFactionTarget );
                inst.ignores.removeIf( t -> t instanceof AbstractFactionTarget );
                
                Utils.sendMessage( sender, Col.GREEN, "All Factions targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;              
            }
            
            if ( args.length <= nextArg + 2 ) {
                Utils.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help factions" );
                return;    
            }
            String factionName = args[nextArg + 2];
            Faction faction = FactionColl.get().getByName( factionName );
            
            if ( faction == null ) {
                Utils.sendMessage( sender, S.ERROR, "No Faction was found matching:- ", factionName );
                return;    
            }
          
            if ( (S.REMOVE + S.TARGET + S.IGNORE).contains( subCommand ) ) {
                
                TargetType target = new FactionTarget( faction );

                if ( S.REMOVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( target ) ) {
                        Utils.sendMessage( sender, Col.GREEN, faction.getName(), " was removed from ", npcName, "'s list of targets." );
                        inst.checkIfEmpty( sender );
                    }
                    else if ( inst.ignores.remove( target ) ) {
                        Utils.sendMessage( sender, Col.GREEN, faction.getName(), " was removed from ", npcName, "'s list of ignores." );
                        inst.checkIfEmpty( sender );
                    }
                    else {
                        Utils.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", faction.getName() );
                        if ( sender != null ) call( sender, npcName, inst, 0, "", S.LIST );
                    }
                    return;
                }
                
                target.setTargetString( String.join( ":", prefix, subCommand, factionName ) );
                
                if ( S.TARGET.equals( subCommand ) ) {
                    
                    if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                        Utils.sendMessage( sender, Col.GREEN, "Faction: ", faction.getName(), " will be targeted by ", npcName );
                    else 
                        Utils.sendMessage( sender, Col.RED, faction.getName(), S.ALREADY_LISTED, npcName );

                    if ( sender != null ) call( sender, npcName, inst, 0, "", S.LIST );
                    return;  
                }
                
                if ( S.IGNORE.equals( subCommand ) ) {
                    
                    if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                        Utils.sendMessage( sender, Col.GREEN, "Faction: ", faction.getName(), " will be ignored by ", npcName );
                    else 
                        Utils.sendMessage( sender, Col.RED, faction.getName(), S.ALREADY_LISTED, npcName );

                    if ( sender != null ) call( sender, npcName, inst, 0, "", S.LIST );
                    return; 
                }
            }
            if ( (S.LEAVE + S.JOIN).contains( subCommand ) ) {
                
                TargetType rivals = new FactionRivalsTarget( faction );
                TargetType allies = new FactionAlliesTarget( faction );
                
                if ( S.LEAVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( rivals ) && inst.ignores.remove( allies ) )
                        Utils.sendMessage( sender, Col.GREEN, npcName, " will no longer fight alongside ", faction.getName() );
                    else {
                        Utils.sendMessage( sender, Col.RED, npcName, " never considered ", faction.getName(), " to be brothers in arms!" );
                        if ( sender != null ) call( sender, npcName, inst, 0, "", S.LIST );
                    }
                    inst.checkIfEmpty( sender );
                    return;
                } 
                
                if ( S.JOIN.equals( subCommand ) ) {
                    rivals.setTargetString( String.join( ":", prefix, subCommand, factionName ) );
                    
                    if ( inst.targets.add( rivals ) && inst.ignores.add( allies ) )
                        Utils.sendMessage( sender, Col.GREEN, npcName, " will support ", faction.getName(), " in all battles!" );
                    return;
                }
            }
            Utils.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                    Col.GOLD, "/sentry help factions", Col.RESET, " and try again." );            
        }       
    }
    
    protected static abstract class AbstractFactionTarget extends AbstractTargetType {
        
        protected final Faction faction;

        protected AbstractFactionTarget( int i, Faction f ) { 
            super( i );
            faction = f;
        }
        @Override
        public int hashCode() { return faction.hashCode(); }
    }
    
    public static class FactionTarget extends AbstractFactionTarget {

        FactionTarget( Faction f ) { 
            super( 57, f ); 
            prettyString = "Members of faction:- " + f.getName();
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
    
    public static class FactionRivalsTarget extends AbstractFactionTarget {
               
        FactionRivalsTarget( Faction f ) { 
            super( 56, f ); 
            prettyString = "Rivals of faction:- " + f.getName();
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
    
   public static class FactionAlliesTarget extends AbstractFactionTarget {
        
        FactionAlliesTarget( Faction f ) { 
            super( 55, f ); 
            prettyString = "Allies of faction:- " + f.getName();
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
