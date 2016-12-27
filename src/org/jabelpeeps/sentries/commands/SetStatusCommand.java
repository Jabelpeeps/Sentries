package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryStatus;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;


public class SetStatusCommand implements SentriesComplexCommand {

    @Override
    public String getShortHelp() { return "set a sentry's status (for debugging)"; }

    @Override
    public String getLongHelp() { return null; }

    @Override
    public String getPerm() { return "citizens.admin"; }

    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... args ) {
        
        if ( args.length < 1 + nextArg ) return;
        
        try {
            SentryStatus status = SentryStatus.valueOf( args[nextArg + 1] );
            if ( status == null ) throw new IllegalArgumentException();
            
            inst.myStatus = status;
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s stats has been set to:- ", status.name() );
        }
        catch ( IllegalArgumentException e ) {
            Utils.sendMessage( sender, S.ERROR, args[nextArg + 1], " was not recogised as a valid SentryStatus" );
        }
    }
}
