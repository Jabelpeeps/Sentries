package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;


public class OwnerTarget extends AbstractTargetType {

    private UUID owner;
    
    protected OwnerTarget( UUID uuid ) { 
        super( 10 ); 
        owner = uuid;
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        UUID uuid = entity.getUniqueId();
        
        if ( uuid != null && uuid.equals( owner ) ) return true;
        
        return false;
    }

    @Override
    public boolean equals( Object o ) {
        if (    o != null 
                && o instanceof OwnerTarget 
                && owner.equals( ((OwnerTarget) o).owner ) ) 
            return true;
        
        return false;
    }

    @Override
    public int hashCode() {
        return owner.hashCode();
    }
}
