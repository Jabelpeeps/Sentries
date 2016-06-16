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

public class SimpleClansBridge extends PluginBridge {

    ClanManager clanManager = SimpleClans.getInstance().getClanManager();
    SentriesComplexCommand command = new ClansCommand();
    final static String PREFIX = "CLAN";

    public SimpleClansBridge( int flag ) { 
        super( flag ); 
        CommandHandler.addCommand( PREFIX.toLowerCase(), command );
    }

    @Override
    public boolean activate() { return true; }

    @Override
    public String getActivationMessage() { return "SimpleClans is active, The CLAN: target will function"; }

    @Override
    public String getCommandHelp() { return "Clan:<ClanName> for a SimpleClans Clan."; }

    @Override
    public String getPrefix() { return PREFIX; }

    @Override
    public boolean add( SentryTrait inst, String args ) {
        command.call( null, null, inst, 0, args);
        return true;  
    }

    public class ClansCommand implements SentriesComplexCommand {

        private String helpTxt;
        
        @Override
        public String getShortHelp() { return "define targets by clan membership"; }

        @Override
        public String getLongHelp() {

            if ( helpTxt == null ) {
                StringJoiner joiner = new StringJoiner( System.lineSeparator() );
                
                joiner.add( String.join( "", "do ", Col.GOLD, "/sentry clan <target|ignore|list|remove|join|leave> <ClanName> ", Col.RESET, 
                                                    "where <ClanName> is a valid current clan name, or tag." ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "target ", Col.RESET, "to have a sentry attack members of <ClanName>" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "ignore ", Col.RESET, "to have a sentry ignore members of <ClanName>" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "list ", Col.RESET, "to display the current clan target information." ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "remove ", Col.RESET, "to remove target or ignore for <ClanName>" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "join ", Col.RESET, "to attack members of rival clans (and ignore allies)" ) );
                joiner.add( String.join( "", "  use ", Col.GOLD, "leave ", Col.RESET, "to reverse a 'join' command." ) );
                
                helpTxt = joiner.toString();
            }                        
            return helpTxt;
        }

        @Override
        public String getPerm() { return "sentry.simpleclans"; }

        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
            
            if ( S.LIST.equalsIgnoreCase( args[nextArg + 1] ) ) {
                StringJoiner joiner = new StringJoiner( ", " );
                
                joiner.add( "Targets:" );
                inst.targets.stream().filter( t -> t instanceof ClanTarget )
                                     .forEach( t -> joiner.add( t.getTargetString() ) );
                
                joiner.add( "Ignores:" );
                inst.ignores.stream().filter( t -> t instanceof ClanTarget )
                                     .forEach( t -> joiner.add( t.getTargetString() ) );
                
                sender.sendMessage( joiner.toString() );
                return;
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Util.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help clan" );
                return;
            }
            
            Clan clan = clanManager.getClan( args[nextArg + 2] );
            
            if ( clan == null ) {
                Util.sendMessage( sender, S.ERROR, "No Clan was found matching:- ", args[nextArg + 2] );
                return;
            } 
            TargetType target = new ClanTarget( clan );
            
            if ( S.REMOVE.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( inst.targets.remove( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, clan.getName(), " was removed from ", npcName, "'s list of targets." );
                else if ( inst.ignores.remove( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, clan.getName(), " was removed from ", npcName, "'s list of ignores." );
                else {
                    Util.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", clan.getName() );
                    return;
                }
                inst.checkIfEmpty( sender );
                return;
            }
            
            if ( S.LEAVE.equalsIgnoreCase( args[nextArg + 1] ) ) {
                TargetType rivals = new ClanRivalsTarget( clan );
                TargetType allies = new ClanAlliesTarget( clan ); 
                
                if ( inst.targets.remove( rivals ) && inst.ignores.remove( allies ) )
                    Util.sendMessage( sender, Col.GREEN, npcName, " will no longer fight alongside ", clan.getName() );
                else
                    Util.sendMessage( sender, Col.RED, npcName, " never considered ", clan.getName(), " to be brothers in arms!" );
                
                return;                
            }
            
            if ( S.JOIN.equalsIgnoreCase( args[nextArg + 1] ) ) {
                TargetType rivals = new ClanRivalsTarget( clan );
                TargetType allies = new ClanAlliesTarget( clan ); 
                rivals.setTargetString( String.join( ":", PREFIX, args[nextArg + 1], args[nextArg + 2] ) );
                
                if ( inst.targets.add( rivals ) && inst.ignores.add( allies ) )
                    Util.sendMessage( sender, Col.GREEN, npcName, " will support ", clan.getName(), " in all things!" );
                return; 
            }

            target.setTargetString( String.join( ":", PREFIX, args[nextArg + 1], args[nextArg + 2] ) );
            
            if ( S.TARGET.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, "Clan: ", clan.getName(), " will be targeted by ", npcName );
                else 
                    Util.sendMessage( sender, Col.RED, clan.getName(), " is already listed as either a target or ignore for ", npcName );
     
                return;                
            }
            
            if ( S.IGNORE.equalsIgnoreCase( args[nextArg + 1] ) ) {
                
                if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                    Util.sendMessage( sender, Col.GREEN, "Clan: ", clan.getName(), " will be ignored by ", npcName );
                else 
                    Util.sendMessage( sender, Col.RED, clan.getName(), " is already listed as either a target or ignore for ", npcName );

                return;              
            }
            
            Util.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                    Col.GOLD, "/sentry help clan", Col.RESET, " and try again." );            
        }       
    }
    
    public class ClanTarget extends AbstractTargetType {
        
        protected Clan clan;

        ClanTarget( Clan myClan ) {
            super( 60 );
            clan = myClan; 
        }  
        
        protected ClanTarget( int i ) {
            super( i );
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
        @Override
        public int hashCode() { return clan.hashCode(); }
    }
    
    public class ClanAlliesTarget extends ClanTarget {
        
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
            return super.equals( o ) && o instanceof ClanAlliesTarget;          
        }       
    }
 
    public class ClanRivalsTarget extends ClanTarget {
        
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
            return super.equals( o ) && o instanceof ClanRivalsTarget;           
        } 
    }
}
