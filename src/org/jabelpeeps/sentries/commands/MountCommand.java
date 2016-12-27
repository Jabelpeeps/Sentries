package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;


public class MountCommand implements SentriesToggleCommand {

    private String helpTxt;
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, Boolean set ) {

        set = (set == null) ? !inst.hasMount() : set;

        if ( set ) {
            inst.mount();
            Utils.sendMessage( sender, Col.GREEN, npcName, " is now mounted" );
        }
        else {
            if ( inst.hasMount() ) {
                Utils.removeMount( inst.mountID );
                inst.mountID = -1;
                Utils.sendMessage( sender, Col.GREEN, npcName, " is no longer mounted" );
            }
            else Utils.sendMessage( sender, Col.YELLOW, npcName, " is not mounted" );
        }
    }

    @Override
    public String getShortHelp() { return "set whether the sentry is mounted"; }

    @Override
    public String getLongHelp() {
        
        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.MOUNT, " (on|off)", Col.RESET,
                    ", to control whether the Sentry rides a mount (currently only horses).",
                    " (Specify 'on' or 'off', or leave blank to toggle state.)" );
        }      
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_MOUNT; }
}
