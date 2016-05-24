package org.jabelpeeps.sentries.pluginbridges;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;
import org.jabelpeeps.sentries.CommandHandler;
import org.jabelpeeps.sentries.PluginBridge;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.SentryTrait;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;

public class SimpleClansBridge extends PluginBridge {

    Map<SentryTrait, Set<Clan>> allies = new HashMap<SentryTrait, Set<Clan>>();
    Map<SentryTrait, Set<Clan>> rivals = new HashMap<SentryTrait, Set<Clan>>();
    ClanManager clanManager = SimpleClans.getInstance().getClanManager();

    public SimpleClansBridge( int flag ) { super( flag ); }

    @Override
    public boolean activate() { return true; }

    @Override
    public String getActivationMessage() { return "SimpleClans is active, The CLAN: target will function"; }

    @Override
    public String getCommandHelp() { return "Clan:<ClanName> for a SimpleClans Clan."; }

    @Override
    public String getPrefix() { return "CLAN"; }

    @Override
    public boolean isTarget( Player player, SentryTrait inst ) {

        if ( !rivals.containsKey( inst ) ) return false;

        return rivals.get( inst ).contains( clanManager.getClanByPlayerName( player.getName() ) );
    }

    @Override
    public boolean isIgnoring( Player player, SentryTrait inst ) {

        if ( !allies.containsKey( inst ) ) return false;

        return allies.get( inst ).contains( clanManager.getClanByPlayerName( player.getName() ) );
    }

    @Override
    public String add( String target, SentryTrait inst, boolean asTarget ) {

        String targetClan = CommandHandler.colon.split( target, 2 )[1];

        for ( Clan clan : clanManager.getClans() ) {

            if ( clan.getName().equalsIgnoreCase( targetClan ) )
                return target.concat( addToList( inst, clan, asTarget ) );
        }
        return "There is currently no Clan matching ".concat( target );
    }

    private String addToList( SentryTrait inst, Clan clan, boolean asTarget ) {
        
        Map<SentryTrait, Set<Clan>> map = asTarget ? rivals : allies;

        if ( !map.containsKey( inst ) )
            map.put( inst, new HashSet<Clan>() );

        if ( map.get( inst ).add( clan ) )
            return String.join( " ", S.ADDED_TO_LIST, asTarget ? S.TARGETS : S.IGNORES );

        return String.join( " ", S.ALLREADY_ON_LIST, asTarget ? S.TARGETS : S.IGNORES );
    }

    @Override
    public String remove( String entity, SentryTrait inst, boolean fromTargets ) {

        if ( !isListed( inst, fromTargets ) ) {
            return String.join( " ", inst.getNPC().getName(), S.NOT_ANY,
                    "Clans added as", fromTargets ? S.TARGETS : S.IGNORES, S.YET );
        }
        String targetClan = CommandHandler.colon.split( entity, 2 )[1];

        Map<SentryTrait, Set<Clan>> map = fromTargets ? rivals : allies;
        Set<Clan> clans = map.get( inst );

        for ( Clan clan : clans ) {

            if ( clan.getName().equalsIgnoreCase( targetClan )
                    && clans.remove( clan ) ) {

                if ( clans.isEmpty() )
                    map.remove( inst );

                return String.join( " ", entity, S.REMOVED_FROM_LIST, fromTargets ? S.TARGETS : S.IGNORES );
            }
        }
        return String.join( " ", entity, S.NOT_FOUND_ON_LIST, fromTargets ? S.TARGETS : S.IGNORES );
    }

    @Override
    public boolean isListed( SentryTrait inst, boolean asTarget ) {

        return (asTarget ? rivals.containsKey( inst )
                         : allies.containsKey( inst ));
    }
}
