package org.jabelpeeps.sentries;

import org.bukkit.command.CommandSender;

public interface SentriesToggleCommand {
    
    boolean call( CommandSender sender, 
                  String npcName, 
                  SentryTrait inst, 
                  Boolean set );
}
