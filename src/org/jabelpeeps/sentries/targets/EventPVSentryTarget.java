package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;
import org.jabelpeeps.sentries.Util;


public class EventPVSentryTarget extends AbstractTargetType {

    public EventPVSentryTarget() { 
        super( 4 ); 
        targetString = "PvSentry";
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return Util.getSentryTrait( entity ) != null;
    }
    @Override
    public boolean equals( Object o ) {
        return  o != null
                && o instanceof EventPVSentryTarget;
    }
}
