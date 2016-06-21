package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 *  A TargetType to encompass all true players, but not Player-type NPC's.
 */
public class AllPlayersTarget extends AbstractTargetType implements TargetType.Internal {

    public AllPlayersTarget() { super( 2 ); }
    
    @Override
    public boolean includes( LivingEntity entity ) {
        return entity instanceof Player && !entity.hasMetadata( "NPC" );
    }   
    @Override
    public String getTargetString() { 
        return "All:Players"; 
    }
    @Override
    public boolean equals( Object o ) {
        return o != null && o instanceof AllPlayersTarget;
    }
    @Override
    public int hashCode() {
        return 1;
    }
}
