package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class EventPVETarget extends AbstractTargetType {

    public EventPVETarget() { 
        super( 2 ); 
        targetString = "PvE";
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return !(entity instanceof Player);
    }
    @Override
    public boolean equals( Object o ) {
        return  o != null
                && o instanceof EventPVETarget;
    }
}
