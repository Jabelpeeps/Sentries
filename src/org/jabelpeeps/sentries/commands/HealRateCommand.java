package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;


public class HealRateCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        
        if ( number == null ) {
            Utils.sendMessage( sender, Col.GOLD, npcName," will heal every ", String.valueOf( inst.healRate ), " seconds" );
        }
        else {
            double healrate = Utils.string2Double( number );
            if ( healrate == Double.MIN_VALUE || healrate < 0.0 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( healrate > 300.0 ) healrate = 300.0;
            
            inst.healRate = healrate;
            Utils.sendMessage( sender, Col.GREEN, npcName, " will now heal every ", String.valueOf( healrate ), " seconds" );
        }
    }

    @Override
    public String getShortHelp() { return "set how fast a sentry will heal"; }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.HEALRATE, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is the number (0-300) of seconds the sentry will take to heal 1 HP.",
            System.lineSeparator(), "  0 = no healing (this is the default setting)",
            System.lineSeparator(), "If no number is given the current value is shown.");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() { return S.PERM_HEAL_RATE; }
}
