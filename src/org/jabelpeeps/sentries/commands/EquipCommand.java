package org.jabelpeeps.sentries.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jabelpeeps.sentries.AttackType;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;


public class EquipCommand implements SentriesComplexCommand {

    private String equipCommandHelp; 
    @Getter private String shortHelp = "adjust the equipment a sentry is using";
    @Getter private String perm = S.PERM_EQUIP;
    
    public static Map<String, Integer> equipmentSlots = new HashMap<>();
    
    static {
        equipmentSlots.put( "hand", 0 );
        equipmentSlots.put( "helmet", 1 );
        equipmentSlots.put( "chestplate", 2 );
        equipmentSlots.put( "leggings", 3 );
        equipmentSlots.put( "boots", 4 );
        equipmentSlots.put( "offhand", 5 );
    }
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length <= 1 + nextArg ) {
            Utils.sendMessage( sender, S.ERROR, "More arguments needed.");
            sender.sendMessage( getLongHelp() );
            return;
        }      
        NPC npc = inst.getNPC();
        
        if ( !npc.isSpawned() ) {
            Utils.sendMessage( sender, S.ERROR, "You can only modify equipment when a sentry is spawned." );
            return;
        }

        Equipment equip = npc.getTrait( Equipment.class );

        if ( equip == null ) { 
            Utils.sendMessage( sender, S.ERROR, "Could not equip: invalid mob type?" );
            return;
        }
        
        if ( S.CLEARALL.equalsIgnoreCase( args[nextArg + 1] ) ) {
           
            for ( EquipmentSlot each : EquipmentSlot.values() ) {
                equip.set( each, null );
            }
            Utils.sendMessage( sender, Col.YELLOW, npcName, "'s equipment cleared" );
        }
        
        else if ( S.CLEAR.equalsIgnoreCase( args[nextArg + 1] ) ) {
            if ( args.length <= 2 + nextArg ) {
                Utils.sendMessage( sender, S.ERROR, "You must specify which slot to clear ", Col.RESET, System.lineSeparator(),
                        "Use one of:- hand, offhand, helmet, chestplate, leggings or boots");
                return;
            }
            String slotName = args[nextArg + 2];
            
            for ( Entry<String, Integer> each : equipmentSlots.entrySet() ) {
                
                if ( each.getKey().equalsIgnoreCase( slotName ) ) {
                    
                    if ( checkSlot( npc.getEntity().getType(), each.getValue() ) ) {
                        equip.set( each.getValue(), null ); 
                        
                        if ( "hand".equalsIgnoreCase( slotName ) ) slotName = "held item";
                        
                        Utils.sendMessage( sender, Col.GREEN, "removed ", npcName, "'s ", slotName );
                        return;
                    }
                    Utils.sendMessage( sender, S.ERROR, "Unable to clear equipment, does the sentry's type support the specified slot?" );
                    return;
                }
            }
            Utils.sendMessage( sender, S.ERROR, slotName, " was not recognised." );
            return;
        }
        else {
            Material mat;
            try {
                AttackType attack = AttackType.valueOf( args[nextArg + 1].toUpperCase() );
                mat = attack.getWeapon();
            } catch ( IllegalArgumentException e ) {
                mat = Material.matchMaterial( Utils.joinArgs( nextArg + 1, args ) );
            }
            
            if ( mat == null ) {
                Utils.sendMessage( sender, S.ERROR, "Item name not recognised.  ", Col.RESET, "do ", Col.GOLD, 
                                "/sentry help listequips ", Col.RESET, "for a list of accepted item names" );
                return;
            }            
            ItemStack item = new ItemStack( mat );
            int slot = Sentries.getSlot( item.getType() );
            
            if ( checkSlot( npc.getEntity().getType(), slot ) ) {
                equip.set( slot, item );

                if ( slot == 0 ) inst.updateAttackType();
                else inst.updateArmour();
                
                Utils.sendMessage( sender, Col.GREEN, "Equipped ", mat.toString(), " on ", npcName );
            }
        }
    }

    private boolean checkSlot( EntityType ent, int slot ) {
        return  slot == 0 || ( slot >= 1 && slot <= 5 && ent != EntityType.ENDERMAN );
    }

    @Override
    public String getLongHelp() {

        if ( equipCommandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry equip <ItemName>", Col.RESET, " to give the named item to the sentry." ) );
            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry help listequips", Col.RESET, " for a list of accepted item names" ) );
            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry equip clearall", Col.RESET, " to clear all equipment slots." ) );
            joiner.add( Utils.join( "do ", Col.GOLD, "/sentry equip clear <slot>", Col.RESET, 
                    " to clear the specified slot, where <slot> can be one of: hand, offhand, helmet, chestplate, leggings or boots." ) );
            joiner.add( Utils.join( Col.RED, Col.BOLD, "NOTE: ", Col.RESET, "equiped armour is currently only cosmetic. Use ",
                                                Col.GOLD, "/sentry armour ", Col.RESET, "to add protection from attacks." ) );
            equipCommandHelp = joiner.toString();
        }
        return equipCommandHelp;
    }
}
