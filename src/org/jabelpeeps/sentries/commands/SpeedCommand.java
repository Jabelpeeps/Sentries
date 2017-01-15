package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class SpeedCommand implements SentriesNumberCommand {

    private String helpTxt;
    @Getter private String shortHelp = "adjust a sentry's speed";
    @Getter private String perm = S.PERM_SPEED;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {

        if ( number == null ) {
            Utils.sendMessage( sender, Col.GOLD, npcName, "'s attack speed multiplier is:- " + inst.speed );
        }
        else {
            double speed = Utils.string2Double( number );
            if ( speed == Double.MIN_VALUE || speed < 0.0 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }

            if ( speed > 5.0 ) speed = 5.0;
            
            inst.speed = speed;
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s attack speed mulitplier set to:- ", String.valueOf( speed ) );
        }
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.SPEED, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is the speed multiplier (0-5.0) for the sentry to use when attacking.",
            System.lineSeparator(), "If no number is given the current value is shown.");
        }
        return helpTxt;
    }
}

