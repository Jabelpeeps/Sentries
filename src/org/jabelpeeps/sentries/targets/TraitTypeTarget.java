package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.Trait;


public class TraitTypeTarget extends AbstractTargetType implements TargetType.Internal {

    private final Class<? extends Trait> trait;
    
    public TraitTypeTarget( String traitName, Class<? extends Trait> traitClass ) {
        super( 5 );
        trait = traitClass;
        targetString = "Trait:" + traitName;
    }
    @Override
    public boolean includes( LivingEntity entity ) {
        return entity.hasMetadata( "NPC" ) 
                && CitizensAPI.getNPCRegistry().getNPC( entity ).hasTrait( trait );
    }
    @Override
    public boolean equals( Object o ) {
        return  o != null 
                && o instanceof TraitTypeTarget 
                && ((TraitTypeTarget) o).trait.equals( trait );
    }
    @Override
    public int hashCode() {
        return trait.hashCode();
    }
}
