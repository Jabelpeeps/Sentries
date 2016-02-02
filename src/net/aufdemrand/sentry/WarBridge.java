package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.tommytony.war.Team;

public class WarBridge extends PluginBridge {
	
	Map<SentryInstance, Set<Team>> friends = new HashMap<SentryInstance, Set<Team>>();
	Map<SentryInstance, Set<Team>> enemies = new HashMap<SentryInstance, Set<Team>>();
	
	WarBridge( int flag ) {
		super( flag );
	}
	
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
	public boolean isIgnoring( LivingEntity entity, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
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
}
