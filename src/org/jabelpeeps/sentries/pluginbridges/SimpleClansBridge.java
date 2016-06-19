package org.jabelpeeps.sentries.pluginbridges;

import java.util.StringJoiner;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;

public class SimpleClansBridge implements PluginBridge {

    final static String PREFIX = "CLAN";
    private String commandHelp = String.join( "", "  using the ", Col.GOLD, "/sentry ", PREFIX.toLowerCase()," ... ", Col.RESET, "commands." );
    protected ClanManager clanManager = SimpleClans.getInstance().getClanManager();
    private SentriesComplexCommand command = new ClansCommand();

    @Override
    public boolean activate() { 
        CommandHandler.addCommand( PREFIX.toLowerCase(), command );
        return true; 
        }

    @Override
    public String getActivationMessage() { return "SimpleClans is active, The CLAN: target will function"; }

    @Override
    public String getCommandHelp() { return commandHelp; }

    @Override
    public String getPrefix() { return PREFIX; }

    @Override
    public void add( SentryTrait inst, String args ) {
        command.call( null, null, inst, 0, Util.colon.split( args ) );
    }

    public class ClansCommand implements SentriesComplexCommand {

        private String helpTxt;
        
        @Override
        public String getShortHelp() { return "define targets by clan membership"; }

        @Override
        public String getPerm() { return "sentry.simpleclans"; }

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
                Util.sendMessage( sender, getLongHelp() );
                return;
            }
            
            String subCommand = args[nextArg + 1].toLowerCase();
            
            if ( S.LIST.equals( subCommand ) ) {
                StringJoiner joiner = new StringJoiner( ", " );
                
                inst.targets.stream().filter( t -> t instanceof ClanTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.RED, "Target: ", t.getTargetString().split( ":" )[2] ) ) );
                
                inst.ignores.stream().filter( t -> t instanceof ClanTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.GREEN, "Ignore: ", t.getTargetString().split( ":" )[2] ) ) );
                
                if ( joiner.length() < 1 ) 
                    Util.sendMessage( sender, Col.YELLOW, npcName, " has no Clan targets or ignores" );
                else
                    sender.sendMessage( joiner.toString() );
                return;
            }
            
            if ( S.CLEARALL.equals( subCommand ) ) {                
                inst.targets.removeIf( t -> t instanceof ClanTarget );
                inst.ignores.removeIf( t -> t instanceof ClanTarget );
                
                Util.sendMessage( sender, Col.GREEN, "All Clan Targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;              
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Util.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help ", PREFIX.toLowerCase() );
                return;
            }
            
            Clan clan = clanManager.getClan( args[nextArg + 2] );
            
            if ( clan == null ) {
                Util.sendMessage( sender, S.ERROR, "No Clan was found matching:- ", args[nextArg + 2] );
                return;
            } 
            
            if ( (S.REMOVE + S.TARGET + S.IGNORE).contains( subCommand ) ) {
                
                TargetType target = new ClanTarget( clan );
                
                if ( S.REMOVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( target ) ) {
                        Util.sendMessage( sender, Col.GREEN, clan.getName(), " was removed from ", npcName, "'s list of targets." );
                        inst.checkIfEmpty( sender );
                    }
                    else if ( inst.ignores.remove( target ) ) {
                        Util.sendMessage( sender, Col.GREEN, clan.getName(), " was removed from ", npcName, "'s list of ignores." );
                        inst.checkIfEmpty( sender );
                    }
                    else 
                        Util.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", clan.getName() );                  
                    return;
                }
                target.setTargetString( String.join( ":", PREFIX, subCommand, args[nextArg + 2] ) );
                
                if ( S.TARGET.equals( subCommand ) ) {
                    
                    if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                        Util.sendMessage( sender, Col.GREEN, "Clan: ", clan.getName(), " will be targeted by ", npcName );
                    else 
                        Util.sendMessage( sender, Col.RED, clan.getName(), S.ALREADY_LISTED, npcName );
         
                    return;                
                }
                
                if ( S.IGNORE.equals( subCommand ) ) {
                    
                    if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                        Util.sendMessage( sender, Col.GREEN, "Clan: ", clan.getName(), " will be ignored by ", npcName );
                    else 
                        Util.sendMessage( sender, Col.RED, clan.getName(), S.ALREADY_LISTED, npcName );

                    return;              
                }   
            }
            if ( (S.LEAVE + S.JOIN).contains( subCommand ) ) {
                
                TargetType rivals = new ClanRivalsTarget( clan );
                TargetType allies = new ClanAlliesTarget( clan ); 
                
                if ( S.LEAVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( rivals ) && inst.ignores.remove( allies ) )
                        Util.sendMessage( sender, Col.GREEN, npcName, " will no longer fight alongside ", clan.getName() );
                    else
                        Util.sendMessage( sender, Col.RED, npcName, " never considered ", clan.getName(), " to be brothers in arms!" );
                    
                    inst.checkIfEmpty( sender );
                    return;                
                } 
                
                if ( S.JOIN.equals( subCommand ) ) {                
                    rivals.setTargetString( String.join( ":", PREFIX, subCommand, args[nextArg + 2] ) );
                    
                    if ( inst.targets.add( rivals ) && inst.ignores.add( allies ) )
                        Util.sendMessage( sender, Col.GREEN, npcName, " will support ", clan.getName(), " in all things!" );
                    return; 
                }
            }            
            Util.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                                        Col.GOLD, "/sentry help ", PREFIX.toLowerCase(), Col.RESET, " and try again." );            
        }       
    }
    
    protected abstract class AbstractClanTarget extends AbstractTargetType {

        protected Clan clan;
        
        protected AbstractClanTarget( int i ) {
            super( i );
        }       
        @Override
        public int hashCode() { return clan.hashCode(); }       
    }
    
    public class ClanTarget extends AbstractClanTarget {

        ClanTarget( Clan myClan ) {
            super( 60 );
            clan = myClan; 
        }  

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
    
    public class ClanAlliesTarget extends AbstractClanTarget {
        
        ClanAlliesTarget( Clan myClan ) {
            super( 61 );
            clan = myClan; 
        } 
        
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
 
    public class ClanRivalsTarget extends AbstractClanTarget {
        
        ClanRivalsTarget( Clan myClan ) {
            super( 62 );
            clan = myClan; 
        } 
        
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
