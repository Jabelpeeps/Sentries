package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class NightVisionCommand implements SentriesNumberCommand {

    private String helpTxt;
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        if ( number == null ) {
            sender.sendMessage( String.join( "", Col.GOLD, npcName, "'s night-vision is:- ", String.valueOf( inst.nightVision ) ) );
        }
        else {
            int vision = Util.string2Int( number );
            if ( vision < 0 ) {
                sender.sendMessage( String.join( "", S.ERROR, number, S.ERROR_NOT_NUMBER ) );
                return true;
            }
            if ( vision > 16 ) vision = 16;
            
            inst.nightVision = vision;
            sender.sendMessage( String.join( "", S.Col.GREEN, npcName, "'s night_vision set to:- ", String.valueOf( vision ) ) );
        }
        return true;
    }

    @Override
    public String getShortHelp() {
        return "allow sentry to see in darkness";
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.NIGHT_VISION, " (#)", Col.RESET, System.lineSeparator(),
            "  where # is a number (0-16) which is added to the ambient light to determine how well the sentry can see in darkness.",
            System.lineSeparator(), "  0 = only use natural light levels, 16 = always see everything.",
            System.lineSeparator(), "  If no number is given the current value is shown. (Default = 16)");
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_NIGHTVISION;
    }
}
