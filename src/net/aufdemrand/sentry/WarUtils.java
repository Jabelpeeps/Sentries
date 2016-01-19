package net.aufdemrand.sentry;

import org.bukkit.entity.Player;

import com.tommytony.war.Team;

public class WarUtils {
	
	public static String getWarTeam( Player player ) {
			
		Team warTeam = Team.getTeamByPlayerName( player.getName() );
		
		if ( warTeam != null ) 
			return warTeam.getName();
	
		Sentry.logger.info( "Error getting Team " );

		return null;
	}
}
