package org.jabelpeeps.sentries;

import org.bukkit.command.CommandSender;

public interface SentriesNumberCommand extends SentriesCommand {
    
    boolean call( CommandSender sender, 
                  String npcName, 
                  SentryTrait inst,
                  String number );
}
