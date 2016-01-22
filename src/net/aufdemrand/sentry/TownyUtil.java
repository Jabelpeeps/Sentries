package net.aufdemrand.sentry;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyUtil {
	
	static String[] getResidentTownyInfo( Player player ) {
		
		String[] info = { null, null };

		try {
			Resident resident = TownyUniverse.getDataSource().getResident( player.getName() );
			
			if ( resident.hasTown() ) {
				
				Town town = resident.getTown();
				
				info[1] = town.getName();
				
				if ( town.hasNation() ) {
					info[0] = town.getNation().getName();
				}
			}			
		} catch ( NotRegisteredException e ) { }

		return info;
	}
	
	static boolean isNationEnemy( String Nation1, String Nation2 ) {
		
		if  (  Sentry.townyActive 
			&& !Nation1.equalsIgnoreCase( Nation2 ) 
			&& TownyUniverse.getDataSource().hasNation( Nation1 )
			&& TownyUniverse.getDataSource().hasNation( Nation2 ) ) {
			
				try {
					Nation theNation1 = TownyUniverse.getDataSource().getNation( Nation1 );
					Nation theNation2 = TownyUniverse.getDataSource().getNation( Nation2 );
	
					if ( theNation1.hasEnemy( theNation2 ) || theNation2.hasEnemy( theNation1 ) ) 
							return true;
		
				} catch ( NotRegisteredException e ) {}
		}
		return false;
	}
	
	static String getNationNameForLocation( Location loc ) {
		
		if ( Sentry.townyActive ) {
			
			TownBlock townBlock = TownyUniverse.getTownBlock( loc );
			
			try {	
				if ( townBlock != null 
				  && townBlock.getTown().hasNation() ) {
					
					return townBlock.getTown().getNation().getName();
				}
			} catch ( NotRegisteredException e ) { }
		}
		return null;
	}
}
