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
	String getCommandText() {
		return "Team";
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
	String getCommandHelp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	boolean add( String target, SentryInstance inst, boolean asTarget ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	boolean remove( String entity, SentryInstance inst, boolean fromTargets ) {
		// TODO Auto-generated method stub
		return false;
	}

}
