package org.jabelpeeps.sentries.commands;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jabelpeeps.sentries.S;
import org.jabelpeeps.sentries.S.Col;
import org.jabelpeeps.sentries.SentriesComplexCommand;
import org.jabelpeeps.sentries.SentryTrait;
import org.jabelpeeps.sentries.Util;


public class GreetingCommand implements SentriesComplexCommand {

    private String helpTxt;
    
    @Override
    public boolean call( CommandSender sender, String npcName, SentryTrait inst, int nextArg, String... inargs ) {
        
        if ( inargs.length >= 2 + nextArg ) {

            String str = Util.removeQuotes( Util.joinArgs( 1 + nextArg, inargs ) );
            str = ChatColor.translateAlternateColorCodes( '&', str );
            inst.greetingMsg = str;
            
            sender.sendMessage( String.join( "", Col.GREEN, npcName, ": Greeting message set to:- ", S.Col.RESET, str ) );
        }
        else {
            sender.sendMessage( String.join( "", Col.GOLD, npcName, "'s Greeting Message is:- ", S.Col.RESET, inst.greetingMsg ) );
        }
        return true;
    }

    @Override
    public String getShortHelp() {
        return "set how the sentry greets friends";
    }

    @Override
    public String getLongHelp() {
        if ( helpTxt == null ) {
            helpTxt = String.join( "", "do ", Col.GOLD, "/sentry ", S.GREETING, " <text to use>", Col.RESET,
                    " to set the sentry's greeting text. <NPC> and <PLAYER> can be used ",
                    "as placeholders, and will be replaced by the appropriate names." );
        }
        return helpTxt;
    }

    @Override
    public String getPerm() {
        return S.PERM_GREETING;
    }
}
