package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;

public class FactionsBridge extends PluginBridge {
	
	Map<SentryInstance, Set<Faction>> friendlyFactions = new HashMap<SentryInstance, Set<Faction>>();
	Map<SentryInstance, Set<Faction>> rivalFactions = new HashMap<SentryInstance, Set<Faction>>();
	
	FactionsBridge( int flag ) {
		super( flag );
	}
	
	@Override
	public boolean activate() {
		return true;
	}
	
	@Override
	public String getActivationMessage() {
		return "Registered with Factions sucessfully, the FACTION: target will function";
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
	
	static boolean isFactionEnemy( String world, String faction1, String faction2 ) {
		
		if ( !faction1.equalsIgnoreCase( faction2 ) ) {
			
				Faction f1 = FactionColl.get().getByName( faction1 );
				Faction f2 = FactionColl.get().getByName( faction2 );
	
				return f1.getRelationTo( f2 ) == Rel.ENEMY;
		}
		return false;
	}
	
	static  String getFactionsTag( Player player ) {
		
		return	MPlayer.get( player ).getFactionName();
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
		return "FACTION";
	}

	@Override
	String getCommandHelp() {
		// TODO Auto-generated method stub
		return null;
	}
}
