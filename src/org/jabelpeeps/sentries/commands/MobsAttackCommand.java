package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentriesToggleCommand;
import org.jabelpeeps.sentries.SentryTrait;

import net.citizensnpcs.api.npc.NPC;


public class MobsAttackCommand implements SentriesToggleCommand {

    private String helpTxt;
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, Boolean set ) {

        inst.targetable = (set == null) ? !inst.targetable : set;

        inst.getNPC().data().set( NPC.TARGETABLE_METADATA, inst.targetable );

        sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, 
                            inst.targetable ? "will be targeted by mobs"
                                            : "will not be targeted by mobs" ) );
        return true;
    }

    @Override
    public String getShortHelp() {
        return "set whether mobs attack the Sentry";
    }

    @Override
    public String getLongHelp() {
        
        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.MOBS_ATTACK, " (on|off| ) ", Col.RESET,
                    "to set whether hostile mobs (that aren't NPC's) will attack the Sentry.",
                    " (Specify 'on' or 'off', or leave blank to toggle state.)" );
        }      
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_TARGETABLE;
    }
}
