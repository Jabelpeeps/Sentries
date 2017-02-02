package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;

import net.citizensnpcs.api.trait.trait.Owner;


public class OwnerTarget extends AbstractTargetType implements TargetType.Internal {

    private final UUID owner;
    
    public OwnerTarget( Owner ownerTrait ) {
        super( 10 );
        targetString = "Owner";
        prettyString = "The Owner of this NPC:- " + ownerTrait.getOwner();
        owner = ownerTrait.getOwnerId();
    } 
    
    @Override
    public boolean includes( LivingEntity entity ) {
        UUID uuid = entity.getUniqueId();
        
        return uuid != null && uuid.equals( owner );
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
