package org.jabelpeeps.sentries.commands;

import java.util.Map.Entry;
import java.util.StringJoiner;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
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
//    private String materialList;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length <= 1 + nextArg ) {
            Utils.sendMessage( sender, "", S.ERROR, "More arguments needed.");
            sender.sendMessage( getLongHelp() );
            return;
        }      
        NPC npc = inst.getNPC();
        
        if ( !npc.isSpawned() ) {
            Utils.sendMessage( sender, S.ERROR, "You can only modify equipment when a sentry is spawned." );
            return;
        }

        Equipment equip = npc.getTrait( Equipment.class );
        
        if ( S.CLEARALL.equalsIgnoreCase( args[nextArg + 1] ) ) {
           
            for ( EquipmentSlot each : EquipmentSlot.values() ) {
                equip.set( each, null );
            }
            Utils.sendMessage( sender, Col.YELLOW, npcName, "'s equipment cleared" );
        }
        
        else if ( S.CLEAR.equalsIgnoreCase( args[nextArg + 1] ) ) {

            for ( Entry<String, Integer> each : Sentries.equipmentSlots.entrySet() ) {

                String slotName = args[nextArg + 2];
            
                if ( each.getKey().equalsIgnoreCase( slotName ) ) {
                    
                    if ( checkSlot( npc.getEntity().getType(), each.getValue() ) ) {
                        equip.set( each.getValue(), null ); 
                        
                        if ( "hand".equalsIgnoreCase( slotName ) ) slotName = "held item";
                        
                        Utils.sendMessage( sender, Col.GREEN, "removed ", npcName, "'s ", slotName );
                        break;
                    }
                    Utils.sendMessage( sender, S.ERROR, "Unable to set equipment, does the sentry's type support the specified slot?" );
                }
                Utils.sendMessage( sender, S.ERROR, slotName, " was not recognised." );
            }
        }
        else {
            Material mat = Material.matchMaterial( Utils.joinArgs( nextArg + 1, args ) );

            if ( mat == null ) {
                Utils.sendMessage( sender, S.ERROR, "Item name not recognised.  "
                                        + "Do '/sentry help listequips' for a list of accepted item names" );
                return;
            }            
            if ( equip != null ) {

                ItemStack item = new ItemStack( mat );
                int slot = Sentries.getSlot( item.getType() );
                
                if ( checkSlot( npc.getEntity().getType(), slot ) ) {
                    equip.set( slot, item );
    
                    if ( slot == 0 ) inst.updateAttackType();
                    else inst.updateArmour();
                    
                    Utils.sendMessage( sender, " ", Col.GREEN, "equipped", mat.toString(), "on", npcName );
                }
            }
            else Utils.sendMessage( sender, S.ERROR, "Could not equip: invalid mob type?" );
        }
    }

    private boolean checkSlot( EntityType ent, int slot ) {
        return  slot == 0 || ( slot >= 1 && slot <= 5 && ent != EntityType.ENDERMAN );
    }

    @Override
    public String getLongHelp() {

        if ( equipCommandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry equip <ItemName>", Col.RESET, " to give the named item to the sentry." ) );
            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry help listequips", Col.RESET, " for a list of accepted item names" ) );
            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry equip clearall", Col.RESET, " to clear all equipment slots." ) );
            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry equip clear <slot>", Col.RESET, 
                    " to clear the specified slot, where <slot> can be one of: hand, offhand, helmet, chestplate, leggings or boots." ) );
            joiner.add( String.join( "", Col.RED, Col.BOLD, "NOTE: ", Col.RESET, "equiped armour is currently only cosmetic. Use ",
                                                Col.GOLD, "/sentry armour ", Col.RESET, "to add protection from attacks." ) );
            equipCommandHelp = joiner.toString();
        }
        return equipCommandHelp;
    }
}
