package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;

public class FactionsBridge implements PluginBridge {
	
	Map<SentryInstance, String> friends = new HashMap<SentryInstance, String>();
	Map<SentryInstance, String> enemies = new HashMap<SentryInstance, String>();
	
	FactionsBridge() {}
	
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
	public boolean isIgnored( LivingEntity entity, SentryInstance inst ) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void refreshLists() {
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
}
