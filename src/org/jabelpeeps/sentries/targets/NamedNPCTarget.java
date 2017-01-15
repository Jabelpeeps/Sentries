package org.jabelpeeps.sentries.targets;

import java.util.UUID;

import org.bukkit.entity.LivingEntity;
import org.jabelpeeps.sentries.Sentries;


public class NamedNPCTarget extends AbstractTargetType implements TargetType.Internal {

    private final UUID uuid;
    
    public NamedNPCTarget( UUID NPCuuid ) { 
        super( 20 ); 
        uuid = NPCuuid;
        targetString = String.join( ":", "Named", "NPC", uuid.toString() ); 
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return  entity.hasMetadata( "NPC" ) 
                && uuid.equals( entity.getUniqueId() );
    }
    @Override
    public String getPrettyString() { 
        return "The NPC named:- " + Sentries.registry.getByUniqueId( uuid ).getName(); 
    }   
    @Override
    public boolean equals( Object o ) {
        return  o != null 
                && o instanceof NamedNPCTarget 
                && uuid.equals( ((NamedNPCTarget) o).uuid );
    }
    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
