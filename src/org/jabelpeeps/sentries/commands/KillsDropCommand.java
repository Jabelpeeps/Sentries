package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentriesToggleCommand;
import org.jabelpeeps.sentries.SentryTrait;


public class KillsDropCommand implements SentriesToggleCommand {

    private String helpTxt;
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, Boolean set ) {

        inst.killsDropInventory = (set == null) ? !inst.killsDropInventory : set;

        sender.sendMessage( String.join( "", S.Col.GREEN, npcName, 
                            inst.killsDropInventory ? "'s kills will drop items or exp"
                                                    : "'s kills will not drop items or exp" ) );
        return true;
    }

    @Override
    public String getShortHelp() {
        return "set drops from those killed by the Sentry";
    }

    @Override
    public String getLongHelp() {
        
        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.KILLSDROP, " (on|off| ) ", Col.RESET,
                    "to set whether the Sentry's victims will drop items",
                    " (Specify 'on' or 'off', or leave blank to toggle state.)" );
        }      
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_KILLDROPS;
    }
}
