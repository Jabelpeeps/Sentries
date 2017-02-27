package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;
import net.citizensnpcs.api.npc.NPC;


public class DropsCommand implements SentriesToggleCommand {

    @Getter private String shortHelp = "control the drops from a sentry";
    @Getter private String perm = S.PERM_DROPS;
    @Getter private String longHelp = 
            Utils.join( "do ", Col.GOLD, "/sentry ", S.DROPS, " (on|off)", Col.RESET,
                            ", to set whether a sentry should drop equiped items when they die.", System.lineSeparator(),
                            " (Specify 'on' or 'off', or leave blank to toggle state.)" );
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, Boolean set ) {
        
        inst.dropInventory = (set == null) ? !inst.dropInventory : set;

        inst.getNPC().data().set( NPC.DROPS_ITEMS_METADATA, inst.dropInventory );

        Utils.sendMessage( sender, S.Col.GREEN, npcName, inst.dropInventory ? " will drop items and xp"
                                                                            : " will not drop items or xp" );
    }
}
