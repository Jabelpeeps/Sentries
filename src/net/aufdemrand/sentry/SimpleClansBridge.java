package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;

public class SimpleClansBridge implements PluginBridge {
	
	Map<SentryInstance, String> friends = new HashMap<SentryInstance, String>();
	Map<SentryInstance, String> enemies = new HashMap<SentryInstance, String>();
	
	SimpleClansBridge() {}
	
	@Override
	public String getActivationMessage() {
		return "Registered with SimpleClans sucessfully, The CLAN: target will function";
	}

	@Override
	public boolean isTarget( LivingEntity entity, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isIgnored( LivingEntity entity, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void refreshLists() {
		// TODO Auto-generated method stub
	}

	public static String getClan( Player player ) {
		
		try {
			Clan clan = SimpleClans.getInstance().getClanManager().getClanByPlayerName( player.getName() );
			
			if ( clan != null ) 
				return clan.getName();
			
		} catch ( Exception e ) {
			Sentry.logger.info( "Error getting Clan " + e.getMessage() );
		}
		return null;
	}
}
