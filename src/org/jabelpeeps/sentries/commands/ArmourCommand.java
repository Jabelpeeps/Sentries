package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class ArmourCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        
        if ( number == null ) {
            sender.sendMessage( String.join( "", Col.GOLD, npcName, "'s armour value is:- ", String.valueOf( inst.armour ) ) );
        }
        else {
            int armour = Util.string2Int( number );
            if ( armour < 0 ) {
                sender.sendMessage( String.join( "", S.ERROR, number, S.ERROR_NOT_NUMBER ) );
                return;
            }
            if ( armour > 2000000 ) armour = 2000000;
            
            inst.armour = armour;
            sender.sendMessage( String.join( "", Col.GREEN, npcName, "'s armour set to:- ", String.valueOf( armour ) ) );
        }
    }

    @Override
    public String getShortHelp() {
        return "directly set a sentry's armour value";
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.ARMOUR, " (#)", Col.RESET, 
            ", where # is the number (0-2000000) of armour points you want the sentry to have. ", System.lineSeparator(),
            Col.RED, Col.BOLD, "  NOTE: ", Col.RESET, "Attacks dealing an ammount of damage less than the armour value will be cancelled. ",
            System.lineSeparator(), "  If no number is given the current value is shown. (Default = 0)");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_ARMOUR;
    }
}
