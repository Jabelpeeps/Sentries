package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class ScoreboardTeamsBridge implements PluginBridge {
	
	Map<SentryInstance, String> friends = new HashMap<SentryInstance, String>();
	Map<SentryInstance, String> enemies = new HashMap<SentryInstance, String>();
	
	ScoreboardTeamsBridge() {}
	
	@Override
	public String getActivationMessage() {
		return "Minecraft scoreboard teams target is active, the TEAM: target will function";
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

	public static String getMCTeamName( Player player ) {
		
		Team team = Sentry.getSentry()
						  .getServer()
						  .getScoreboardManager()
						  .getMainScoreboard()
						  .getEntryTeam( player.getName() );
		
		if ( team != null ) {
			return team.getName();
		}
		return null;
	}

}
