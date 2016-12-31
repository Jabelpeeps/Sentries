package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;


public class AttackRateCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        if ( number == null ) {
            Utils.sendMessage( sender, Col.GOLD, npcName, "'s attack rate is:- ", 
                                String.valueOf( inst.attackRate ), " seconds between blows/shots." );
        }
        else {
            double attackrate = Utils.string2Double( number );
            if ( attackrate == Double.MIN_VALUE || attackrate < 0 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( attackrate > 30.0 ) attackrate = 30.0;
            
            inst.attackRate = attackrate;
            inst.getNavigator().getDefaultParameters().attackDelayTicks( (int) (attackrate * 20) );
            Utils.sendMessage( sender, Col.GREEN, npcName, "'s attack rate set to:- ", String.valueOf( attackrate ) );
        }
    }

    @Override
    public String getShortHelp() {
        return "sets the delay between attacks";
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.ATTACK_RATE, " (#)", Col.RESET,
            ", where # is the number of seconds (0-30) to wait between attacks. (decimal values are accepted)",
            System.lineSeparator(), "  If no number is given the current value is shown.");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_ATTACK_RATE;
    }
}
