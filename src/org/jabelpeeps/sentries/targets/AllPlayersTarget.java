package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 *  A TargetType to encompass all true players, but not Player-type NPC's.
 */
public class AllPlayersTarget extends AbstractTargetType {

    AllPlayersTarget() { super( 2 ); }
    
    @Override
    public boolean includes( LivingEntity entity ) {
        if ( entity instanceof Player && !entity.hasMetadata( "NPC" ) ) return true;

        return false;
    }
    
    @Override
    public String getTargetString() { 
        return "PLAYER:ALL"; 
    }
    
    @Override
    public boolean equals( Object o ) {
        if ( o != null && o instanceof AllPlayersTarget ) return true;

        return false;
    }
    @Override
    public int hashCode() {
        return 1;
    }
}
