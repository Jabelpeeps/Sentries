package net.aufdemrand.sentry;

import org.bukkit.entity.Player;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;

public class FactionsUtils {

	static boolean isFactionEnemy( String world, String  faction1, String faction2 ) {
		
		if ( Sentry.factionsActive 
			&& !faction1.equalsIgnoreCase( faction2 ) ) {
			
				Faction f1 = FactionColl.get().getByName( faction1 );
				Faction f2 = FactionColl.get().getByName( faction2 );
	
				return f1.getRelationTo( f2 ) == Rel.ENEMY;
		}
		return false;
	}
	
	static  String getFactionsTag( Player player ) {
		
		if ( Sentry.factionsActive ) 
			return	MPlayer.get( player ).getFactionName();
		
		return null;
	}
}
