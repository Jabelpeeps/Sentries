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
import org.jabelpeeps.sentries.Utils;
import org.jabelpeeps.sentries.commands.SentriesCommand;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;
import org.jabelpeeps.sentries.targets.TargetType;

import lombok.Getter;
import net.milkbowl.vault.permission.Permission;

public class VaultBridge implements PluginBridge {

    final String prefix = "Group";
    @Getter private String activationMessage;
    protected static Permission perms;

    @Override
    public boolean activate() {

        RegisteredServiceProvider<Permission> permissionProvider = 
                Bukkit.getServicesManager().getRegistration( Permission.class );

        if ( permissionProvider == null ) {
            activationMessage = "Vault integration: No Permissions Provider is registered.";
            return false;
        }    
        perms = permissionProvider.getProvider();

        if ( !perms.hasGroupSupport() ) {
            activationMessage = "Vault integration: Permissions Provider does not support groups.";
            return false;
        }
        String[] groups = perms.getGroups();

        if ( groups.length == 0 ) {
            activationMessage = "Vault integration: No permission groups found.";
            perms = null;
            return false;
        }        
        activationMessage = "Sucessfully interfaced with Vault: " + groups.length
                            + " groups found. The GROUP: target will function.";
        
        CommandHandler.addCommand( prefix.toLowerCase(), new GroupCommand() );
        return true;
    }

    public class GroupCommand implements SentriesComplexCommand, SentriesCommand.Targetting {
        
        @Getter final String shortHelp = "to set permission group targets"; 
        @Getter final String perm = "sentry.groups";
        private String helpTxt;
        
        @Override
        public String getLongHelp() { 
            if ( helpTxt == null ) {
                helpTxt = Utils.join( "do ", Col.GOLD, "/sentry group <target|ignore|remove|list|clearall> <GroupName> ",
                        Col.RESET, "to have a sentry consider permission group membership when selecting targets.", System.lineSeparator(),
                        "  use ", Col.GOLD, "target ", Col.RESET, "to target players from <GroupName>", System.lineSeparator(),
                        "  use ", Col.GOLD, "ignore ", Col.RESET, "to ignore players from <GroupName>", System.lineSeparator(),
                        "  use ", Col.GOLD, "remove ", Col.RESET, "to remove <GroupName> as either a target or ignore", System.lineSeparator(),
                        "  use ", Col.GOLD, "list ", Col.RESET, "to list current group targets and ignores", System.lineSeparator(),
                        "  use ", Col.GOLD, "clearall ", Col.RESET, "to remove all perm group targets and ignores from a sentry.", 
                        System.lineSeparator(), Col.GOLD, "    <GroupName> ", Col.RESET, "must be a currently existing permission group." );
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
                
                inst.targets.stream().filter( t -> t instanceof GroupTarget )
                                     .forEach( t -> joiner.add( 
                                             Utils.join( Col.RED, "Target: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
                
                inst.ignores.stream().filter( t -> t instanceof GroupTarget )
                                     .forEach( t -> joiner.add( 
                                             Utils.join( Col.GREEN, "Ignore: ", Utils.colon.split( t.getTargetString() )[2] ) ) );
                
                if ( joiner.length() < 1 ) 
                    Utils.sendMessage( sender, Col.YELLOW, npcName, " has no group targets or ignores" );
                else
                    Utils.sendMessage( sender, Col.YELLOW, "Current Group targets are:-", Col.RESET, System.lineSeparator(), joiner.toString() );
                return;
            }
            
            if ( S.CLEARALL.equals( subCommand ) ) {                
                inst.targets.removeIf( t -> t instanceof GroupTarget );
                inst.ignores.removeIf( t -> t instanceof GroupTarget );
                
                Utils.sendMessage( sender, Col.GREEN, "All group targets cleared from ", npcName );
                inst.checkIfEmpty( sender );
                return;              
            }
            
            if ( args.length <= nextArg + 2 ) { 
                Utils.sendMessage( sender, S.ERROR, "Not enough arguments. ", Col.RESET, "Try /sentry help ", prefix.toLowerCase() );
                return;
            }
            String groupName = args[nextArg + 2];
            
            if ( !Arrays.asList( perms.getGroups() ).contains( groupName ) ) {
                Utils.sendMessage( sender, S.ERROR, "No Group was found matching:- ", args[nextArg + 2] );
                return;
            }
            
            if ( (S.REMOVE + S.TARGET + S.IGNORE).contains( subCommand ) ) {
                
                TargetType target = new GroupTarget( groupName );
                
                if ( S.REMOVE.equals( subCommand ) ) {
                    
                    if ( inst.targets.remove( target ) ) {
                        Utils.sendMessage( sender, Col.GREEN, groupName, " was removed from ", npcName, "'s list of targets." );
                        inst.checkIfEmpty( sender );
                    }
                    else if ( inst.ignores.remove( target ) ) {
                        Utils.sendMessage( sender, Col.GREEN, groupName, " was removed from ", npcName, "'s list of ignores." );
                        inst.checkIfEmpty( sender );
                    }
                    else 
                        Utils.sendMessage( sender, Col.RED, npcName, " was neither targeting nor ignoring ", groupName );                  
                    return;
                }
                target.setTargetString( String.join( ":", prefix, subCommand, groupName ) );
                
                if ( S.TARGET.equals( subCommand ) ) {
                    
                    if ( !inst.ignores.contains( target ) && inst.targets.add( target ) ) 
                        Utils.sendMessage( sender, Col.GREEN, "Group: ", groupName, " will be targeted by ", npcName );
                    else 
                        Utils.sendMessage( sender, Col.RED, groupName, S.ALREADY_LISTED, npcName );

                    if ( sender != null ) call( sender, npcName, inst, 0, "", S.LIST );
                    return;                
                }
                
                if ( S.IGNORE.equals( subCommand ) ) {
                    
                    if ( !inst.targets.contains( target ) && inst.ignores.add( target ) ) 
                        Utils.sendMessage( sender, Col.GREEN, "Group: ", groupName, " will be ignored by ", npcName );
                    else 
                        Utils.sendMessage( sender, Col.RED, groupName, S.ALREADY_LISTED, npcName );

                    if ( sender != null ) call( sender, npcName, inst, 0, "", S.LIST );
                    return;              
                }   
            }           
            Utils.sendMessage( sender, S.ERROR, " Sub-command not recognised!", Col.RESET, " please check ",
                    Col.GOLD, "/sentry help ", prefix.toLowerCase(), Col.RESET, " and try again." );            
        }       
    }
    
    public static class GroupTarget extends AbstractTargetType {
        
        private final String group;
        
        GroupTarget( String grp ) { 
            super( 50 );
            group = grp;
            prettyString = "Members of permission group:- " + grp;
        }
        @Override
        public boolean includes( LivingEntity entity ) { 
            return  entity instanceof Player 
                    && perms.playerInGroup( (Player) entity, group );
        }        
        @Override
        public boolean equals( Object o ) {
            return  o != null 
                    && o instanceof GroupTarget 
                    && ((GroupTarget) o).group.equals( group );           
        }         
        @Override
        public int hashCode() { return group.hashCode(); }
    }
}
