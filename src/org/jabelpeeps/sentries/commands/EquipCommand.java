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


public class EquipCommand implements SentriesComplexCommand {

    private String equipCommandHelp; 
    private String materialList;
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length <= 1 + nextArg ) {
            sender.sendMessage( String.join( "", S.ERROR, "More arguments needed.") );
            sender.sendMessage( getLongHelp());
            return true;
        }
        
        if ( S.LIST.equalsIgnoreCase( args[nextArg + 1] ) ) {
            
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
                        case STANDING_BANNER: case WALL_BANNER: case POWERED_RAIL: case DETECTOR_RAIL: case TRAPPED_CHEST: 
                            continue;
                        default:                   
                    }                    
                    joiner.add( each.name() );
                }
                materialList = String.join( "", Col.GOLD, "Valid Item Names:- ", Col.RESET, joiner.toString() );
            }            
            sender.sendMessage( materialList );
            return true;
        }
        
        NPC npc = inst.getNPC();
        
        if ( !npc.isSpawned() ) {
            sender.sendMessage( String.join( "", Col.RED, "You can only modify equipment when a sentry is spawned." ) );
            return true;
        }
        
        EntityType type = npc.getEntity().getType();
        
        // TODO figure out why zombies and skele's are not included here.
        if ( type == EntityType.ENDERMAN || type == EntityType.PLAYER ) {

            if ( S.CLEARALL.equalsIgnoreCase( args[nextArg + 1] ) ) {

                inst.equip( null );
                sender.sendMessage( String.join( "", Col.YELLOW, npcName, "'s equipment cleared" ) );
            }
            else if ( S.CLEAR.equalsIgnoreCase( args[nextArg + 1] ) ) {

                for ( Entry<String, Integer> each : Sentries.equipmentSlots.entrySet() ) {

                    String slotName = args[nextArg + 2];
                
                    if ( each.getKey().equalsIgnoreCase( slotName ) ) {
                        
                        npc.getTrait( Equipment.class ).set( each.getValue(), null ); 
                        
                        if ( "hand".equalsIgnoreCase( slotName ) ) slotName = "held item";
                        
                        sender.sendMessage( String.join( "", Col.GREEN, "removed ", npcName, "'s ", slotName ) );
                    }
                    else sender.sendMessage( String.join( "", Col.RED, slotName, " was not recognised." ) );
                }
            }
            else {
                Material mat = Material.matchMaterial( Util.joinArgs( nextArg + 1, args ) );

                if ( mat == null ) {
                    sender.sendMessage( Col.RED.concat( "Could not equip: item name not recognised" ) );
                    return true;
                }

                ItemStack item = new ItemStack( mat );

                if ( inst.equip( item ) )
                    sender.sendMessage( String.join( " ", Col.GREEN, "equipped", mat.toString(), "on", npcName ) );
                else
                    sender.sendMessage( Col.RED.concat( "Could not equip: invalid mob type?" ) );
            }
        }
        else sender.sendMessage( Col.RED.concat( "Could not equip: must be Player or Enderman type" ) );
        return true;
    }
    @Override
    public String getShortHelp() { return "adjust the equipment a sentry is using"; }

    @Override
    public String getLongHelp() {

        if ( equipCommandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry equip <ItemName>", Col.RESET ) );
            joiner.add( "  to give the named item to the sentry" );
            joiner.add( "  item names are the offical MC item names" );
            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry equip clearall", Col.RESET ) );
            joiner.add( "  to clear all equipment slots." );
            joiner.add( String.join( "", "do ", Col.GOLD, "/sentry equip clear <slot>", Col.RESET ) );
            joiner.add( "  to clear the specified slot, where <slot> can be one of: hand, helmet, chestplate, leggings or boots." );
            joiner.add( String.join( "", Col.RED, Col.BOLD, "NOTE: ", Col.RESET, "equiped armour is currently only cosmetic. Use ",
                                                Col.GOLD, "/sentry armour ", Col.RESET, "to add protection from attacks." ) );

            equipCommandHelp = joiner.toString();
        }
        return equipCommandHelp;
    }
    @Override
    public String getPerm() { return S.PERM_EQUIP; }
}
