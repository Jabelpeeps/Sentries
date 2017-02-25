package org.jabelpeeps.sentries.targets;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;


public class HoldingTarget extends AbstractTargetType implements TargetType.Internal {

    private final Material holding;
    
    public HoldingTarget( Material mat ) {
        super( 16 );
        holding = mat; 
        targetString = "holding:" + holding.name();
        prettyString = "Anyone holding a " + holding.name().toLowerCase();
    }

    @Override
    public boolean includes( LivingEntity entity ) {
        EntityEquipment equip = entity.getEquipment();
        if ( equip == null ) return false;
        
        ItemStack item = equip.getItemInMainHand();
        if ( item == null ) return false;
        
        return holding.equals( item.getType() );
    }
    @Override
    public boolean equals( Object o ) {
        return  o != null
                && o instanceof HoldingTarget
                && holding.equals( ((HoldingTarget) o).holding );
    }
    @Override
    public int hashCode() {
        return holding.hashCode();
    }
}
