package org.jabelpeeps.sentries.pluginbridges;

import java.util.Arrays;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

import net.milkbowl.vault.permission.Permission;

public class VaultBridge implements PluginBridge {

    protected final static String PREFIX = "GROUP";
    private String commandHelp = String.join( "", "  using the ", Col.GOLD, "/sentry ", PREFIX.toLowerCase()," ... ", Col.RESET, "commands." );
    private SentriesComplexCommand command = new GroupCommand();
    private String activationMsg;
    protected Permission perms;

    @Override
    public String getPrefix() { return PREFIX; }

    @Override
    public String getCommandHelp() { return commandHelp; }

    @Override
    public boolean activate() {

        RegisteredServiceProvider<Permission> permissionProvider = 
                Bukkit.getServicesManager().getRegistration( Permission.class );

        if ( permissionProvider == null ) {
            activationMsg = "Vault integration: No Permissions Provider is registered.";
            return false;
        }    
        perms = permissionProvider.getProvider();

        if ( !perms.hasGroupSupport() ) {
            activationMsg = "Vault integration: Permissions Provider does not support groups.";
            return false;
        }
        String[] groups = perms.getGroups();

        if ( groups.length == 0 ) {
            activationMsg = "Vault integration: No permission groups found.";
            perms = null;
            return false;
        }
        
        activationMsg = "Sucessfully interfaced with Vault: " + groups.length
                            + " groups found. The GROUP: target will function.";
        
        CommandHandler.addCommand( PREFIX.toLowerCase(), command );
        return true;
    }

    @Override
    public String getActivationMessage() { return activationMsg; }

    @Override
    public void add( SentryTrait inst, String args ) {       
        command.call( null, null, inst, 0, Util.colon.split( args ) );
    }
   
    public class GroupCommand implements SentriesComplexCommand {

        private String helpTxt = String.join( "", "do ", Col.GOLD, "/sentry group <target|ignore|remove|list|clearall> <GroupName> ",
                Col.RESET, "to have a sentry consider permission group membership when selecting targets.", System.lineSeparator(),
                "  use ", Col.GOLD, "target ", Col.RESET, "to target players from <GroupName>", System.lineSeparator(),
                "  use ", Col.GOLD, "ignore ", Col.RESET, "to ignore players from <GroupName>", System.lineSeparator(),
                "  use ", Col.GOLD, "remove ", Col.RESET, "to remove <GroupName> as either a target or ignore", System.lineSeparator(),
                "  use ", Col.GOLD, "list ", Col.RESET, "to list current group targets and ignores", System.lineSeparator(),
                "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all perm group targets and ignores from a sentry.", 
                System.lineSeparator(), Col.GOLD, "    <GroupName> ", Col.RESET, "must be a currently existing permission group." );
        
        @Override
        public String getShortHelp() { return "define targets by permission groups"; }

        @Override
        public String getPerm() { return "sentry.groups"; }

        @Override
        public String getLongHelp() { return helpTxt; }
        
        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {

            if ( args.length <= nextArg + 1 ) {
                Util.sendMessage( sender, getLongHelp() );
                return;
            }
            
            String subCommand = args[nextArg + 1].toLowerCase();
            
            if ( S.LIST.equals( subCommand ) ) {
                StringJoiner joiner = new StringJoiner( ", " );
                
                inst.targets.stream().filter( t -> t instanceof GroupTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.RED, "Target: ", t.getTargetString().split( ":" )[2] ) ) );
                
                inst.ignores.stream().filter( t -> t instanceof GroupTarget )
                                     .forEach( t -> joiner.add( String.join( "", Col.GREEN, "Ignore: ", t.getTargetString().split( ":" )[2] ) ) );
                
                if ( joiner.length() < 1 ) 
                    Util.sendMessage( sender, Col.YELLOW, npcName, " has no group targets or ignores" );
                else
                    Util.sendMessage( sender, Col.YELLOW, "Current Group targets are:-", Col.RESET, System.lineSeparator(), joiner.toString() );
                return;
            }
            
            if ( S.CLEARALL.equals( subCommand ) ) {                
                inst.targets.removeIf( t -> t instanceof GroupTarget );
                inst.ignores.removeIf( t -> t instanceof GroupTarget );
                
                Util.sendMessage( sender, Col.GREEN, "All group targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;              
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Util.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help ", PREFIX.toLowerCase() );
                return;
            }
            String groupName = args[nextArg + 2];
            
            if ( !Arrays.asList( perms.getGroups() ).contains( groupName ) ) {
                Util.sendMessage( sender, S.ERROR, "No Group was found matching:- ", args[nextArg + 2] );
                return;
            }
            
            if ( (S.REMOVE + S.TARGET + S.IGNORE).contains( subCommand ) ) {
                
                TargetType target = new GroupTarget( groupName );
                
                if ( S.REMOVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( target ) ) {
                        Util.sendMessage( sender, Col.GREEN, groupName, " was removed from ", npcName, "'s list of targets." );
                        inst.checkIfEmpty( sender );
                    }
                    else if ( inst.ignores.remove( target ) ) {
                        Util.sendMessage( sender, Col.GREEN, groupName, " was removed from ", npcName, "'s list of ignores." );
                        inst.checkIfEmpty( sender );
                    }
                    else 
                        Util.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", groupName );                  
                    return;
                }
                target.setTargetString( String.join( ":", PREFIX, subCommand, groupName ) );
                
                if ( S.TARGET.equals( subCommand ) ) {
                    
                    if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                        Util.sendMessage( sender, Col.GREEN, "Group: ", groupName, " will be targeted by ", npcName );
                    else 
                        Util.sendMessage( sender, Col.RED, groupName, S.ALREADY_LISTED, npcName );
         
                    return;                
                }
                
                if ( S.IGNORE.equals( subCommand ) ) {
                    
                    if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                        Util.sendMessage( sender, Col.GREEN, "Group: ", groupName, " will be ignored by ", npcName );
                    else 
                        Util.sendMessage( sender, Col.RED, groupName, S.ALREADY_LISTED, npcName );

                    return;              
                }   
            }           
            Util.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                    Col.GOLD, "/sentry help ", PREFIX.toLowerCase(), Col.RESET, " and try again." );            
        }       
    }
    
    public class GroupTarget extends AbstractTargetType {
        
        private String group;
        
        GroupTarget( String grp ) { 
            super( 50 );
            group = grp;
        }
        @Override
        public boolean includes( LivingEntity entity ) {            
            if ( !(entity instanceof Player) ) return false;
           
            return perms.playerInGroup( (Player) entity, group );
        }        
        @Override
        public boolean equals( Object o ) {
            if (    o != null 
                    && o instanceof GroupTarget 
                    && ((GroupTarget) o).group.equals( group ) )
                return true;
            
            return false;           
        }         
        @Override
        public int hashCode() { return group.hashCode(); }
    }
}
