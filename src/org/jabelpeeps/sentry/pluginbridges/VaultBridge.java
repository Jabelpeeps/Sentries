package org.jabelpeeps.sentry.pluginbridges;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jabelpeeps.sentry.CommandHandler;
import org.jabelpeeps.sentry.PluginBridge;
import org.jabelpeeps.sentry.S;
import org.jabelpeeps.sentry.Sentry;
import org.jabelpeeps.sentry.SentryInstance;

import net.milkbowl.vault.permission.Permission;

public class VaultBridge extends PluginBridge {

    Map<SentryInstance, Set<String>> friends = new HashMap<SentryInstance, Set<String>>();
    Map<SentryInstance, Set<String>> enemies = new HashMap<SentryInstance, Set<String>>();

    private String activationMsg = "";
    static Permission perms = null;

    VaultBridge( int flag ) {
        super( flag );
    }

    @Override
    protected String getPrefix() {
        return "GROUP";
    }

    @Override
    protected String getCommandHelp() {
        return "Group:<GroupName> for a permission group.";
    }

    @Override
    protected boolean activate() {

        RegisteredServiceProvider<Permission> permissionProvider = Sentry
                .getSentry().getServer().getServicesManager()
                .getRegistration( Permission.class );

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
    protected String getActivationMessage() {
        return activationMsg;
    }

    @Override
    protected boolean isTarget( Player player, SentryInstance inst ) {

        if ( !enemies.containsKey( inst ) )
            return false;

        return checkGroups( enemies.get( inst ), player );
    }

    @Override
    protected boolean isIgnoring( Player player, SentryInstance inst ) {

        if ( !friends.containsKey( inst ) )
            return false;

        return checkGroups( friends.get( inst ), player );
    }

    private boolean checkGroups( Set<String> set, Player player ) {

        for ( String each : perms.getPlayerGroups( player.getWorld().getName(),
                player ) ) {
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
    protected boolean isListed( SentryInstance inst, boolean asTarget ) {

        return (asTarget ? enemies.containsKey( inst )
                : friends.containsKey( inst ));
    }

    @Override
    protected String add( String target, SentryInstance inst,
            boolean asTarget ) {

        String targetGroup = CommandHandler.colon.split( target, 2 )[1];

        for ( String group : perms.getGroups() ) {

            if ( group.equalsIgnoreCase( targetGroup ) )
                return target.concat( addToList( inst, group, asTarget ) );
        }
        return "There is currently no Group matching ".concat( target );
    }

    private String addToList( SentryInstance inst, String group,
            boolean asTarget ) {
        Map<SentryInstance, Set<String>> map = asTarget ? enemies : friends;

        if ( !map.containsKey( inst ) )
            map.put( inst, new HashSet<String>() );

        if ( map.get( inst ).add( group ) )
            return String.join( " ", S.ADDED_TO_LIST,
                    asTarget ? S.TARGETS : S.IGNORES );

        return String.join( " ", S.ALLREADY_ON_LIST,
                asTarget ? S.TARGETS : S.IGNORES );
    }

    @Override
    protected String remove( String entity, SentryInstance inst,
            boolean fromTargets ) {

        if ( !isListed( inst, fromTargets ) ) {
            return String.join( " ", inst.myNPC.getName(), S.NOT_ANY,
                    "Groups added as", fromTargets ? S.TARGETS : S.IGNORES,
                    S.YET );
        }
        String targetGroup = CommandHandler.colon.split( entity, 2 )[1];

        Map<SentryInstance, Set<String>> map = fromTargets ? enemies : friends;
        Set<String> groups = map.get( inst );

        for ( String group : groups ) {

            if ( group.equalsIgnoreCase( targetGroup )
                    && groups.remove( group ) ) {

                if ( groups.isEmpty() )
                    map.remove( inst );

                return String.join( " ", entity, S.REMOVED_FROM_LIST,
                        fromTargets ? S.TARGETS : S.IGNORES );
            }
        }
        return String.join( " ", entity, S.NOT_FOUND_ON_LIST,
                fromTargets ? S.TARGETS : S.IGNORES );
    }

    // static boolean checkGroups4Targets( World world, OfflinePlayer player,
    // SentryInstance inst ) {
    //
    // // check world permission groups & then global permission groups if
    // needed.
    // return checkGroups4Targets( perms.getPlayerGroups( world.getName(),
    // player ), inst )
    // || checkGroups4Targets( perms.getPlayerGroups( (String) null, player ),
    // inst );
    // }
    //
    // private static boolean checkGroups4Targets( String[] groups,
    // SentryInstance inst ) {
    //
    // if ( groups != null ) {
    // for ( String each : groups )
    // if ( inst.targetsContain( "GROUP:" + each ) )
    // return true;
    // }
    // return false;
    // }
    //
    // static boolean checkGroups4Ignores( World world, OfflinePlayer player,
    // SentryInstance inst ) {
    //
    // // check world permission groups & then global permission groups if
    // needed.
    // return checkGroups4Ignores( perms.getPlayerGroups( world.getName(),
    // player ), inst )
    // || checkGroups4Ignores( perms.getPlayerGroups( (String) null, player ),
    // inst );
    // }
    //
    // private static boolean checkGroups4Ignores( String[] groups,
    // SentryInstance inst ) {
    //
    // if ( groups != null ) {
    // for ( String each : groups )
    // if ( inst.ignoresContain( "GROUP:" + each ) )
    // return true;
    // }
    // return false;
    // }
}
