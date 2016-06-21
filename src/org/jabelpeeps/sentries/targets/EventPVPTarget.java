package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class EventPVPTarget extends AbstractTargetType {

    public EventPVPTarget() { 
        super( 1 );
        targetString = "PvP";
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return entity instanceof Player
                && !entity.hasMetadata( "NPC" );
    }
    @Override
    public boolean equals( Object o ) {
        return  o != null
                && o instanceof EventPVPTarget;
    }
    @Override
    public int hashCode() { return 1; }
}
