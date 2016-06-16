package org.jabelpeeps.sentries.pluginbridges;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.commands.SentriesComplexCommand;
import org.jabelpeeps.sentries.targets.AbstractTargetType;

import net.milkbowl.vault.permission.Permission;

public class VaultBridge extends PluginBridge {

    Map<SentryTrait, Set<String>> friends = new HashMap<>();
    Map<SentryTrait, Set<String>> enemies = new HashMap<>();

    private String activationMsg = "";
    static Permission perms = null;

    public VaultBridge( int flag ) { super( flag ); }

    @Override
    public String getPrefix() { return "GROUP"; }

    @Override
    public String getCommandHelp() { return "  Group:<GroupName> for a permission group."; }

    @Override
    public boolean activate() {

        RegisteredServiceProvider<Permission> permissionProvider = 
                Bukkit.getServicesManager().getRegistration( Permission.class );

        if ( permissionProvider != null ) {
            perms = permissionProvider.getProvider();

            if ( perms.hasGroupSupport() ) {

                String[] groups = perms.getGroups();

                if ( groups.length > 0 ) {
                    activationMsg = "Sucessfully interfaced with Vault: "
                            + groups.length
                            + " groups found. The GROUP: target will function.";
                    return true;
                }
                activationMsg = "Vault integration: No permission groups found.";
            }
            else
                activationMsg = "Vault integration: Permissions Provider does not support groups.";
        }
        else
            activationMsg = "Vault integration: No Permissions Provider is registered.";

        perms = null;
        return false;
    }

    @Override
    public String getActivationMessage() { return activationMsg; }

    @Override
    public boolean isTarget( LivingEntity entity, SentryTrait inst ) {

        if ( !enemies.containsKey( inst ) ) return false;

        return checkGroups( enemies.get( inst ), (Player) entity );
    }

    @Override
    public boolean isIgnoring( LivingEntity entity, SentryTrait inst ) {

        if ( !friends.containsKey( inst ) ) return false;

        return checkGroups( friends.get( inst ), (Player) entity );
    }

    private boolean checkGroups( Set<String> set, Player player ) {

        for ( String each : perms.getPlayerGroups( player.getWorld().getName(), player ) ) {
            if ( set.contains( each ) )
                return true;
        }
        for ( String each : perms.getPlayerGroups( (String) null, player ) ) {
            if ( set.contains( each ) )
                return true;
        }
        return false;
    }

    @Override
    public boolean isListed( SentryTrait inst, boolean asTarget ) {

        return (asTarget ? enemies.containsKey( inst )
                         : friends.containsKey( inst ));
    }

    @Override
    public boolean add( SentryTrait inst, String args ) {
        return false;
        // TODO Auto-generated method stub
        
    }

    @Override
    public String add( String target, SentryTrait inst, boolean asTarget ) {

        String targetGroup = CommandHandler.colon.split( target, 2 )[1];

        for ( String group : perms.getGroups() ) {

            if ( group.equalsIgnoreCase( targetGroup ) )
                return target.concat( addToList( inst, group, asTarget ) );
        }
        return "There is currently no Group matching ".concat( target );
    }

    private String addToList( SentryTrait inst, String group, boolean asTarget ) {
        Map<SentryTrait, Set<String>> map = asTarget ? enemies : friends;

        if ( !map.containsKey( inst ) )
            map.put( inst, new HashSet<String>() );

        if ( map.get( inst ).add( group ) )
            return String.join( " ", S.ADDED_TO_LIST, asTarget ? S.TARGETS : S.IGNORES );

        return String.join( " ", S.ALLREADY_ON_LIST, asTarget ? S.TARGETS : S.IGNORES );
    }

    @Override
    public String remove( String entity, SentryTrait inst, boolean fromTargets ) {

        if ( !isListed( inst, fromTargets ) ) {
            return String.join( " ", inst.getNPC().getName(), S.NOT_ANY,
                    "Groups added as", fromTargets ? S.TARGETS : S.IGNORES, S.YET );
        }
        String targetGroup = CommandHandler.colon.split( entity, 2 )[1];

        Map<SentryTrait, Set<String>> map = fromTargets ? enemies : friends;
        Set<String> groups = map.get( inst );

        for ( String group : groups ) {

            if (    group.equalsIgnoreCase( targetGroup )
                    && groups.remove( group ) ) {

                if ( groups.isEmpty() )
                    map.remove( inst );

                return String.join( " ", entity, S.REMOVED_FROM_LIST, fromTargets ? S.TARGETS : S.IGNORES );
            }
        }
        return String.join( " ", entity, S.NOT_FOUND_ON_LIST, fromTargets ? S.TARGETS : S.IGNORES );
    }
    
    public class GroupCommand implements SentriesComplexCommand {

        private String helpTxt;
        
        @Override
        public String getShortHelp() { return ""; }

        @Override
        public String getPerm() { return "sentry.groups"; }

        @Override
        public String getLongHelp() {

            if ( helpTxt == null )
                helpTxt = "";
            
            return helpTxt;
        }
        
        @Override
        public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
            // TODO Auto-generated method stub
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
            // TODO Auto-generated method stub
            return false;
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
