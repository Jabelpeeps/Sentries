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
import org.jabelpeeps.sentries.SentriesComplexCommand;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;


public class EquipCommand implements SentriesComplexCommand {

    private String equipCommandHelp;  
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length <= 1 + nextArg ) {
            sender.sendMessage( String.join( "", S.ERROR, "More arguments needed.") );
            sender.sendMessage( getLongHelp());
            return true;
        }
        NPC npc = inst.getNPC();
        EntityType type = npc.getEntity().getType();
        
        // TODO figure out why zombies and skele's are not included here.
        if ( type == EntityType.ENDERMAN || type == EntityType.PLAYER ) {

            if ( S.CLEARALL.equalsIgnoreCase( args[nextArg + 1] ) ) {

                inst.equip( null );
                sender.sendMessage( String.join( "", S.Col.YELLOW, npcName, "'s equipment cleared" ) );
            }
            else if ( S.CLEAR.equalsIgnoreCase( args[nextArg + 1] ) ) {

                for ( Entry<String, Integer> each : Sentries.equipmentSlots.entrySet() )

                    if ( each.getKey().equalsIgnoreCase( args[nextArg + 2] ) ) {
                        
                        npc.getTrait( Equipment.class ).set( each.getValue(), null );                                
                        sender.sendMessage( String.join( "", S.Col.GREEN, "removed ", npcName, "'s ", args[nextArg +2] ) );
                    }
            }
            else {
                Material mat = Material.matchMaterial( Util.joinArgs( nextArg + 1, args ) );

                if ( mat == null ) {
                    sender.sendMessage( S.Col.RED.concat( "Could not equip: item name not recognised" ) );
                    return true;
                }

                ItemStack item = new ItemStack( mat );

                if ( inst.equip( item ) )
                    sender.sendMessage( String.join( " ", S.Col.GREEN, "equipped", mat.toString(), "on", npcName ) );
                else
                    sender.sendMessage( S.Col.RED.concat( "Could not equip: invalid mob type?" ) );
            }
        }
        else sender.sendMessage( S.Col.RED.concat( "Could not equip: must be Player or Enderman type" ) );
        return false;
    }

    @Override
    public String getShortHelp() {
        return "adjust the equipment a sentry is using";
    }

    @Override
    public String getLongHelp() {

        if ( equipCommandHelp == null ) {

            StringJoiner joiner = new StringJoiner( System.lineSeparator() ).add( "" );

            joiner.add( String.join( "", Col.GOLD, "do '/sentry equip <ItemName>'", Col.RESET ) );
            joiner.add( "  to give the named item to the sentry" );
            joiner.add( "  item names are the offical MC item names" );
            joiner.add( String.join( "", Col.GOLD, "do '/sentry equip clearall'", Col.RESET ) );
            joiner.add( "  to clear all equipment slots." );
            joiner.add( String.join( "", Col.GOLD, "do '/sentry equip clear <slot>'", Col.RESET ) );
            joiner.add( "  to clear the specified slot, where slot can be one of: hand, helmet, chestplate, leggings or boots." );
            joiner.add( "NOTE: equiped armour is currently only cosmetic. Use '/sentry armour' to add protection from attacks." );

            equipCommandHelp = joiner.toString();
        }
        return equipCommandHelp;
    }

    @Override
    public String getPerm() {
        return S.PERM_EQUIP;
    }

}
