package org.jabelpeeps.sentries.commands;

import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.Sentries;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Utils;

import lombok.Getter;


public class ArmourCommand implements SentriesNumberCommand {

    private String helpTxt;
    @Getter private String perm = S.PERM_ARMOUR;
    @Getter private String shortHelp = "directly set a sentry's armour value";
    
    @Override
    public void call( CommandSender sender, String npcName, SentryTrait inst, String number ) {
        
        if ( number == null ) {
            if ( inst.armour >= 0 )
                Utils.sendMessage( sender, Col.GOLD, npcName, "'s armour value is:- ", String.valueOf( inst.armour ) );
            else
                Utils.sendMessage( sender, Col.GOLD, npcName, " has a calculated armour value of ", 
                                                                    String.valueOf( Math.abs( inst.armour ) ) );
        }
        else {
            int armour = Utils.string2Int( number );
            if ( armour < -1 ) {
                Utils.sendMessage( sender, S.ERROR, number, S.ERROR_NOT_NUMBER );
                return;
            }
            if ( armour > 100 ) armour = 100;
            
            inst.armour = armour;
            
            if ( armour == -1 ) {
                if ( inst.updateArmour() )
                    Utils.sendMessage( sender, Col.GREEN, npcName, "'s armour is now calculated from the equipped armour. ",
                            System.lineSeparator(), "The current value is:- ", String.valueOf( Math.abs( inst.armour ) ) );
                else
                    Utils.sendMessage( sender, Col.RED, npcName, "is not wearing any armour! Add some with ", 
                                               Col.GOLD, "/sentry equip", Col.RESET, " before trying this command again." );
            }
            else 
                Utils.sendMessage( sender, Col.GREEN, npcName, "'s armour set to:- ", String.valueOf( armour ) );
        }
    }

    @Override
    public String getLongHelp() {

        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.ARMOUR, " (#)", Col.RESET, ", where # is the ",
                Sentries.useNewArmourCalc ? "percent reduction (0-100) that will be applied to the damage from each attack. " 
                                          : "amount of damage (0-100) that each attack will be reduced by.", 
                System.lineSeparator(), Col.RED, Col.BOLD, "  NOTE: ", Col.RESET, 
                Sentries.useNewArmourCalc ? "Setting this to 100% will effectively make the sentry invincible." 
                                          : "If the base damage ammount is less than this value, the attack will be blocked.",
                System.lineSeparator(), "  set to -1 to have the armour value calculated from the armour worn.",
                System.lineSeparator(), "  If no number is given the current value is shown. (Default = 0)" );
        }
        return helpTxt;
    }
}
