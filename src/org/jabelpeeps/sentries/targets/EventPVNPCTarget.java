package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;


public class EventPVNPCTarget extends AbstractTargetType {

    public EventPVNPCTarget() { 
        super( 3 ); 
        targetString = "PvNPC";
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return entity.hasMetadata( "NPC" );
    }
    @Override
    public boolean equals( Object o ) {
        return  o != null
                && o instanceof EventPVNPCTarget;
    }
}
