package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;


public class RetaliateCommand implements SentriesToggleCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, Boolean set ) {
        
        inst.iRetaliate = (set == null) ? !inst.iRetaliate : set;

        Utils.sendMessage( sender, Col.GREEN, npcName, inst.iRetaliate ? " will retalitate against all attackers" 
                                                                       : " will not retaliate when attacked" );
    }

    @Override
    public String getShortHelp() { return "set whether to attack attackers"; }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.RETALIATE, " (on|off)", Col.RESET,
                    "Tells a Sentry to always attack those who attack it, or not.",
                    " (Specify 'on' or 'off', or leave blank to toggle state.)" );
        }
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_RETALIATE; }
}
