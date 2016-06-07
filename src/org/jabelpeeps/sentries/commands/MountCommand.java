package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentriesToggleCommand;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class MountCommand implements SentriesToggleCommand {

    private String helpTxt;
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, Boolean set ) {

        set = (set == null) ? !inst.hasMount() : set;

        if ( set ) {
            inst.mount();
            sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is now Mounted" ) );
        }
        else {
            if ( inst.hasMount() )
                Util.removeMount( inst.mountID );

            inst.mountID = -1;
            sender.sendMessage( String.join( " ", S.Col.GREEN, npcName, "is no longer Mounted" ) );
        }
        return true;
    }

    @Override
    public String getShortHelp() {
        return "set whether the Sentry is mounted";
    }

    @Override
    public String getLongHelp() {
        
        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.MOUNT, " (on|off| ) ", Col.RESET,
                    "to control whether the Sentry rides a mount (currently only horses)",
                    " (Specify 'on' or 'off', or leave blank to toggle state.)" );
        }      
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_MOUNT;
    }
}
