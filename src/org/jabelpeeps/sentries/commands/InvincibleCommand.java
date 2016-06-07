package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentriesToggleCommand;
import org.jabelpeeps.sentries.SentryTrait;


public class InvincibleCommand implements SentriesToggleCommand {

    private String helpTxt;
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, Boolean set ) {
        
        inst.invincible = (set == null) ? !inst.invincible : set;

        sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, inst.invincible ? "is now INVINCIBLE" : "now takes damage" ) );
    
        return true;
    }

    @Override
    public String getShortHelp() {
        return "set damamge and knockback";
    }

    @Override
    public String getLongHelp() {
        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.INVINCIBLE, " (on|off| )", Col.RESET, 
                                " to set whether a Sentry will take damage & knockback.",
                                " (Specify 'on' or 'off', or leave blank to toggle state.)" );
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_INVINCIBLE;
    }
}
