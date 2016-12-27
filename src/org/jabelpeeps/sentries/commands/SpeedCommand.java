package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;


public class SpeedCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {

        if ( number == null ) {
            Utils.sendMessage( sender, Col.GOLD, npcName, "'s Speed is:- " + inst.speed );
        }
        else {
            float speed = Utils.string2Float( number );
            if ( speed == Float.MIN_VALUE || speed < 0.0 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }

            if ( speed > 2.0 ) speed = 2.0f;
            
            inst.speed = speed;
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s speed set to:- ", String.valueOf( speed ) );
        }
    }

    @Override
    public String getShortHelp() { return "adjust a sentry's speed"; }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.SPEED, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is the speed multiplier (0-2.0) for the sentry to use when attacking.",
            System.lineSeparator(), "If no number is given the current value is shown.");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_SPEED; }
}

