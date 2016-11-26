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
import org.jabelpeeps.sentries.Util;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;


public class EquipCommand implements SentriesComplexCommand {

    private String equipCommandHelp; 
    private String materialList;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length <= 1 + nextArg ) {
            Util.sendMessage( sender, "", S.ERROR, "More arguments needed.");
            sender.sendMessage( getLongHelp() );
            return;
        }

        if ( "listAll".equalsIgnoreCase( args[nextArg + 1] ) ) {
            
            if ( materialList == null ) {
                StringJoiner joiner = new StringJoiner( ", " );
                
                for ( Material each : Material.values() ) {
                    
                    if ( each.isEdible() || each.isRecord() ) continue;
                    
                    switch ( each ) {
                    case AIR: case LONG_GRASS: case REDSTONE_WIRE: case CROPS: case SNOW: case PORTAL: case PUMPKIN_STEM: case MELON_STEM:
                    case ENDER_PORTAL: case DOUBLE_PLANT: case BARRIER: case END_GATEWAY: case STRUCTURE_BLOCK: case COMMAND_REPEATING:
                    case COMMAND_CHAIN: case COMMAND: case WATER: case LAVA: case STATIONARY_WATER: case STATIONARY_LAVA: case BEDROCK:
                    case BED_BLOCK: case DROPPER: case DISPENSER: case PISTON_STICKY_BASE: case PISTON_BASE: case PISTON_EXTENSION:
                    case PISTON_MOVING_PIECE: case MOB_SPAWNER: case WOODEN_DOOR: case IRON_DOOR: case IRON_DOOR_BLOCK: case DIODE:
                    case DIODE_BLOCK_OFF: case DIODE_BLOCK_ON: case DOUBLE_STEP: case WOOD_DOUBLE_STEP: case DOUBLE_STONE_SLAB2:
                    case PURPUR_DOUBLE_SLAB: case REDSTONE_COMPARATOR: case REDSTONE_COMPARATOR_OFF: case REDSTONE_COMPARATOR_ON:
                    case COMMAND_MINECART: case EMPTY_MAP: case ARMOR_STAND: case IRON_BARDING: case GOLD_BARDING: case DIAMOND_BARDING:
                    case STANDING_BANNER: case WALL_BANNER: case POWERED_RAIL: case DETECTOR_RAIL: case TRAPPED_CHEST: case STONE_BUTTON:
                    case THIN_GLASS: case STAINED_GLASS: case WOOD_BUTTON: case STAINED_CLAY: case STAINED_GLASS_PANE: case CARPET:
                    case STRUCTURE_VOID: case REDSTONE: case EXPLOSIVE_MINECART: case HOPPER_MINECART: case SPRUCE_DOOR_ITEM: 
                    case BIRCH_DOOR_ITEM: case JUNGLE_DOOR_ITEM: case ACACIA_DOOR_ITEM: case DARK_OAK_DOOR_ITEM: case SPRUCE_DOOR: 
                    case BIRCH_DOOR: case JUNGLE_DOOR: case ACACIA_DOOR: case DARK_OAK_DOOR: case SPRUCE_FENCE_GATE: case BIRCH_FENCE_GATE: 
                    case JUNGLE_FENCE_GATE: case DARK_OAK_FENCE_GATE: case ACACIA_FENCE_GATE: case SPRUCE_FENCE: case BIRCH_FENCE: 
                    case JUNGLE_FENCE: case DARK_OAK_FENCE: case ACACIA_FENCE: case FENCE:case FENCE_GATE: case STORAGE_MINECART: 
                    case POWERED_MINECART: case IRON_FENCE: case ACTIVATOR_RAIL:
                        continue;
                    default:                   
                    }                    
                    joiner.add( each.name() );
                }
                materialList = String.join( "", Col.GOLD, "Valid Item Names:- ", Col.RESET, joiner.toString() );
            }            
            sender.sendMessage( materialList );
            return;
        }       
        NPC npc = inst.getNPC();
        
        if ( !npc.isSpawned() ) {
            Util.sendMessage( sender, S.ERROR, "You can only modify equipment when a sentry is spawned." );
            return;
        }

        Equipment equip = npc.getTrait( Equipment.class );
        
        if ( S.CLEARALL.equalsIgnoreCase( args[nextArg + 1] ) ) {
           
            for ( EquipmentSlot each : EquipmentSlot.values() ) {
                equip.set( each, null );
            }
            Util.sendMessage( sender, Col.YELLOW, npcName, "'s equipment cleared" );
        }
        
        else if ( S.CLEAR.equalsIgnoreCase( args[nextArg + 1] ) ) {

            for ( Entry<String, Integer> each : Sentries.equipmentSlots.entrySet() ) {

                String slotName = args[nextArg + 2];
            
                if ( each.getKey().equalsIgnoreCase( slotName ) ) {
                    
                    if ( checkSlot( npc.getEntity().getType(), each.getValue() ) ) {
                        equip.set( each.getValue(), null ); 
                        
                        if ( "hand".equalsIgnoreCase( slotName ) ) slotName = "held item";
                        
                        Util.sendMessage( sender, Col.GREEN, "removed ", npcName, "'s ", slotName );
                        break;
                    }
                    Util.sendMessage( sender, S.ERROR, "Unable to set equipment, does the sentry's type support the specified slot?" );
                }
                Util.sendMessage( sender, S.ERROR, slotName, " was not recognised." );
            }
        }
        else {
            Material mat = Material.matchMaterial( Util.joinArgs( nextArg + 1, args ) );

            if ( mat == null ) {
                Util.sendMessage( sender, S.ERROR, "Item name not recognised.  "
                                        + "Do '/sentry help listequips' for a list of accepted item names" );
                return;
            }            
            if ( equip != null ) {

                ItemStack item = new ItemStack( mat );
                int slot = Sentries.getSlot( item.getType() );
                
                if ( checkSlot( npc.getEntity().getType(), slot ) ) {
                    equip.set( slot, item );
    
                    if ( slot == 0 ) inst.updateAttackType();
                    
                    Util.sendMessage( sender, " ", Col.GREEN, "equipped", mat.toString(), "on", npcName );
                }
            }
            else Util.sendMessage( sender, S.ERROR, "Could not equip: invalid mob type?" );
        }
    }

    private boolean checkSlot( EntityType ent, int slot ) {
        return  slot == 0 || ( slot >= 1 && slot <= 5 && ent != EntityType.ENDERMAN );
    }
    
    @Override
    public String getShortHelp() { return "adjust the equipment a sentry is using"; }

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
    @Override
    public String getPerm() { return S.PERM_EQUIP; }
}
