package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class RangeCommand implements SentriesNumberCommand {

    private String helpTxt;
    @Getter private String shortHelp = "set the sentry's target detection range";
    @Getter private String perm = S.PERM_RANGE;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {

        if ( number == null ) {
            Utils.sendMessage( sender, Col.GOLD, npcName, "'s attack range is:- ", String.valueOf( inst.range ) );
        }
        else {
            int range = Utils.string2Int( number );
            if ( range < 2 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( range > 64 ) range = 64;
            
            inst.range = range;
            inst.setRange();
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s attack range set to:- ", String.valueOf( range ) );
        }
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = Utils.join( "do ", Col.GOLD, "/sentry ", S.RANGE, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is the number (2-64) of blocks distance at which the sentry will assess entities as possible targets",
            System.lineSeparator(), "If no number is given the current value is shown. (Default = 10)" );
        }
        return helpTxt;
    }
}
