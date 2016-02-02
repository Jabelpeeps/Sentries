package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;

public class SimpleClansBridge extends PluginBridge {
	
	Map<SentryInstance, Set<Clan>> allies = new HashMap<SentryInstance, Set<Clan>>();
	Map<SentryInstance, Set<Clan>> rivals = new HashMap<SentryInstance, Set<Clan>>();
	
	SimpleClansBridge( int flag ) {
		super( flag );
	}
	
	@Override
	public boolean activate() {
		return true;
	}
	
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
	public boolean isIgnoring( LivingEntity entity, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void refreshLists( SentryInstance inst ) {
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

	@Override
	public boolean addTarget( String target, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addIgnore( String target, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	String getCommandText() {
		return "CLAN";
	}

	@Override
	String getCommandHelp() {
		// TODO Auto-generated method stub
		return null;
	}
}
