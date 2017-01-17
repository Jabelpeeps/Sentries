package org.jabelpeeps.sentries.pluginbridges;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
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

import lombok.Getter;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;

public class SimpleClansBridge implements PluginTargetBridge {

    @Getter final String prefix = "CLAN";
    @Getter final String activationMessage = "SimpleClans is active, The CLAN: target will function";
    @Getter private String commandHelp = 
            String.join( "", "  using the ", Col.GOLD, "/sentry ", prefix.toLowerCase()," ... ", Col.RESET, "commands." );
    protected static ClanManager clanManager = SimpleClans.getInstance().getClanManager();
    private SentriesComplexCommand command = new ClansCommand();

    @Override
    public boolean activate() { 
        CommandHandler.addCommand( prefix.toLowerCase(), command );
        return true; 
    }

    public class ClansCommand implements SentriesComplexCommand, SentriesCommand.Targetting {

        @Getter final String shortHelp = "define targets by clan membership";
        @Getter final String perm = "sentry.simpleclans";
        private String helpTxt;
        
        @Override
        public String getLongHelp() {
            if ( helpTxt == null ) {
                StringJoiner joiner = new StringJoiner( System.lineSeparator() );
                
                joiner.add( String.join( "", "do ", Col.GOLD, "/sentry clan <target|ignore|list|remove|join|leave|clearall> <ClanName> ", Col.RESET, 
                                                    "where <ClanName> is a valid current clan name, or tag." ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "target ", Col.RESET, "to have a sentry attack members of <ClanName>" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "ignore ", Col.RESET, "to have a sentry ignore members of <ClanName>" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "list ", Col.RESET, "to display the current clan target information." ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "remove ", Col.RESET, "to remove target or ignore for <ClanName>" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "join ", Col.RESET, "to attack members of rival clans (and ignore allies)" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "leave ", Col.RESET, "to reverse a 'join' command." ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all Clan targets from a sentry." ) );
                
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
                
                inst.targets.stream().filter( t -> t instanceof ClanTarget )
                                     .forEach( t -> joiner.add( 
                                             String.join( "", Col.RED, "Target: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
                
                inst.ignores.stream().filter( t -> t instanceof ClanTarget )
                                     .forEach( t -> joiner.add( 
                                             String.join( "", Col.GREEN, "Ignore: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
                
                inst.targets.stream().filter( t -> t instanceof ClanRivalsTarget )
                                     .forEach( t -> joiner.add( 
                                             String.join( "", Col.BLUE, "Member of: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
                
                if ( joiner.length() < 1 ) 
                    Utils.sendMessage( sender, Col.YELLOW, npcName, " has no Clan targets or ignores" );
                else
                    Utils.sendMessage( sender, Col.YELLOW, "Current Clan targets are:-", Col.RESET, System.lineSeparator(), joiner.toString() );
                return;
            }
            
            if ( S.CLEARALL.equals( subCommand ) ) {                
                inst.targets.removeIf( t -> t instanceof ClanTarget );
                inst.ignores.removeIf( t -> t instanceof ClanTarget );
                
                Utils.sendMessage( sender, Col.GREEN, "All Clan Targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;              
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Utils.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help ", prefix.toLowerCase() );
                return;
            }
            
            Clan clan = clanManager.getClan( args[nextArg + 2] );
            
            if ( clan == null ) {
                Utils.sendMessage( sender, S.ERROR, "No Clan was found matching:- ", args[nextArg + 2] );
                return;
            } 
            
            if ( (S.REMOVE + S.TARGET + S.IGNORE).contains( subCommand ) ) {
                
                TargetType target = new ClanTarget( clan );
                
                if ( S.REMOVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( target ) ) {
                        Utils.sendMessage( sender, Col.GREEN, clan.getName(), " was removed from ", npcName, "'s list of targets." );
                        inst.checkIfEmpty( sender );
                    }
                    else if ( inst.ignores.remove( target ) ) {
                        Utils.sendMessage( sender, Col.GREEN, clan.getName(), " was removed from ", npcName, "'s list of ignores." );
                        inst.checkIfEmpty( sender );
                    }
                    else 
                        Utils.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", clan.getName() );                  
                    return;
                }
                target.setTargetString( String.join( ":", prefix, subCommand, args[nextArg + 2] ) );
                
                if ( S.TARGET.equals( subCommand ) ) {
                    
                    if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                        Utils.sendMessage( sender, Col.GREEN, "Clan: ", clan.getName(), " will be targeted by ", npcName );
                    else 
                        Utils.sendMessage( sender, Col.RED, clan.getName(), S.ALREADY_LISTED, npcName );

                    call( sender, npcName, inst, 0, "", S.LIST );
                    return;                
                }
                
                if ( S.IGNORE.equals( subCommand ) ) {
                    
                    if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                        Utils.sendMessage( sender, Col.GREEN, "Clan: ", clan.getName(), " will be ignored by ", npcName );
                    else 
                        Utils.sendMessage( sender, Col.RED, clan.getName(), S.ALREADY_LISTED, npcName );

                    call( sender, npcName, inst, 0, "", S.LIST );
                    return;              
                }   
            }
            if ( (S.LEAVE + S.JOIN).contains( subCommand ) ) {
                
                TargetType rivals = new ClanRivalsTarget( clan );
                TargetType allies = new ClanAlliesTarget( clan ); 
                
                if ( S.LEAVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( rivals ) && inst.ignores.remove( allies ) )
                        Utils.sendMessage( sender, Col.GREEN, npcName, " will no longer fight alongside ", clan.getName() );
                    else
                        Utils.sendMessage( sender, Col.RED, npcName, " never considered ", clan.getName(), " to be brothers in arms!" );
                    
                    inst.checkIfEmpty( sender );
                    return;                
                } 
                
                if ( S.JOIN.equals( subCommand ) ) {                
                    rivals.setTargetString( String.join( ":", prefix, subCommand, args[nextArg + 2] ) );
                    
                    if ( inst.targets.add( rivals ) && inst.ignores.add( allies ) )
                        Utils.sendMessage( sender, Col.GREEN, npcName, " will support ", clan.getName(), " in all things!" );
                    return; 
                }
            }            
            Utils.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                        Col.GOLD, "/sentry help ", prefix.toLowerCase(), Col.RESET, " and try again." );            
        }       
    }
    
    protected static abstract class AbstractClanTarget extends AbstractTargetType {

        protected final Clan clan;
        
        protected AbstractClanTarget( int i, Clan c ) {
            super( i );
            clan = c;
        }       
        @Override
        public int hashCode() { return clan.hashCode(); }       
    }
    
    public static class ClanTarget extends AbstractClanTarget {

        ClanTarget( Clan myClan ) { super( 60, myClan ); }  

        @Override
        public boolean includes( LivingEntity entity ) {            
            Clan check = clanManager.getClanByPlayerUniqueId( entity.getUniqueId() );
            
            return check != null && check.equals( clan );
        }
        @Override
        public boolean equals( Object o ) {
            return  o != null
                    && o instanceof ClanTarget
                    && ((ClanTarget) o).clan.equals( clan );           
        }
    }
    
    public static class ClanAlliesTarget extends AbstractClanTarget {
        
        ClanAlliesTarget( Clan myClan ) { super( 61, myClan );} 
        
        @Override
        public boolean includes( LivingEntity entity ) {            
            Clan check = clanManager.getClanByPlayerUniqueId( entity.getUniqueId() );
            
            return check != null && clan.isAlly( check.getTag() );
        }  
        @Override
        public boolean equals( Object o ) {
            return  o != null
                    && o instanceof ClanAlliesTarget
                    && ((ClanAlliesTarget) o).clan.equals( clan );        
        }       
    }
 
    public static class ClanRivalsTarget extends AbstractClanTarget {
        
        ClanRivalsTarget( Clan myClan ) { super( 62, myClan ); } 
        
        @Override
        public boolean includes( LivingEntity entity ) {           
            Clan check = clanManager.getClanByPlayerUniqueId( entity.getUniqueId() );

            return check != null 
                    && (    clan.isRival( check.getTag() ) 
                            || clan.isWarring( check ) );
        }
        @Override
        public boolean equals( Object o ) {           
            return  o != null
                    && o instanceof ClanRivalsTarget
                    && ((ClanRivalsTarget) o).clan.equals( clan );         
        } 
    }
}
