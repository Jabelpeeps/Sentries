package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;

public class WarBridge implements PluginBridge {
	
	Map<SentryInstance, String> friends = new HashMap<SentryInstance, String>();
	Map<SentryInstance, String> enemies = new HashMap<SentryInstance, String>();
	
	WarBridge() {}

	@Override
	public boolean activate() {
		return true;
	}
	
	@Override
	public String getActivationMessage() {
		return "Registered with War sucessfully, The WARTEAM: target will function";
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
	public void refreshAllLists() {
		// TODO Auto-generated method stub
	}

	@Override
	public void refreshLists( SentryInstance inst ) {
		// TODO Auto-generated method stub
		
	}
	
	public static String getWarTeam( Player player ) {
			
		Team warTeam = Team.getTeamByPlayerName( player.getName() );
		
		if ( warTeam != null ) 
			return warTeam.getName();
	
		Sentry.logger.info( "Error getting Team" );

		return null;
	}
}
