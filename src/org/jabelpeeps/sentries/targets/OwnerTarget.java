package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;


public class OwnerTarget extends AbstractTargetType implements TargetType.Internal {

    private UUID owner;
    
    public OwnerTarget( UUID uuid ) { 
        super( 10 ); 
        owner = uuid;
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        UUID uuid = entity.getUniqueId();
        
        return uuid != null && uuid.equals( owner );
    } 
    @Override
    public String getTargetString() { 
        return "Owner:" + owner.toString(); 
    }   
    @Override
    public boolean equals( Object o ) {
        return  o != null 
                && o instanceof OwnerTarget 
                && owner.equals( ((OwnerTarget) o).owner );
    }
    @Override
    public int hashCode() {
        return owner.hashCode();
    }
}
