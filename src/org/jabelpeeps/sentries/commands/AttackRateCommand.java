package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class AttackRateCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        if ( number == null ) {
            sender.sendMessage( String.join( "", S.Col.GOLD, npcName, "'s ranged attack rate is:- ", 
                                String.valueOf( inst.arrowRate), " seconds between shots." ) );
        }
        else {
            double attackrate = Util.string2Double( number );
            if ( attackrate < 0.0 ) {
                sender.sendMessage( String.join( "", S.ERROR, number, S.ERROR_NOT_NUMBER ) );
                return true;
            }
            if ( attackrate > 30.0 ) attackrate = 30.0;
            
            inst.arrowRate = attackrate;
            sender.sendMessage( String.join( "", S.Col.GREEN, npcName, "'s ranged attack rate set to:- ", String.valueOf( attackrate ) ) );
        }
        return true;
    }

    @Override
    public String getShortHelp() {
        return "sets the delay between ranged attacks";
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.ARROW_RATE, " (#)", Col.RESET,
            ", where # is the number of seconds (0-30) to wait between ranged (all projectile) attacks.",
            System.lineSeparator(), "  If no number is given the current value is shown.");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return null;
    }
}
