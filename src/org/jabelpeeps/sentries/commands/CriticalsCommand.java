package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;


public class CriticalsCommand implements SentriesToggleCommand {

    private String helpTxt;
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, Boolean set ) {

        inst.acceptsCriticals = (set == null) ? !inst.acceptsCriticals : set;

        sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, inst.acceptsCriticals ? "will take critical hits"
                                                                                          : "will take normal damage" ) );
        return true;
    }

    @Override
    public String getShortHelp() {
        return "control critcal hits to the sentry";
    }

    @Override
    public String getLongHelp() {
        
        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.CRITICALS, " (on|off)", Col.RESET,
                    ", to set whether damamge to a sentry is managed by the plugin's own Critical Hit system.",
                    " (Specify 'on' or 'off', or leave blank to toggle state.)" );
        }      
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_CRITICAL_HITS;
    }
}
