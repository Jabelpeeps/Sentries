package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class ScoreboardTeamsBridge extends PluginBridge {
	
	Map<SentryInstance, Set<Team>> friends = new HashMap<SentryInstance, Set<Team>>();
	Map<SentryInstance, Set<Team>> enemies = new HashMap<SentryInstance, Set<Team>>();
	
	ScoreboardTeamsBridge( int flag ) {
		super( flag );
	}
	
	@Override
	public boolean activate() {
		return true;
	}
	
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
	public boolean isIgnoring( LivingEntity entity, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void refreshLists( SentryInstance inst ) {
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
