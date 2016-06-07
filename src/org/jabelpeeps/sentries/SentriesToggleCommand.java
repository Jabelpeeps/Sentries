package org.jabelpeeps.sentries;

import org.bukkit.command.CommandSender;

public interface SentriesToggleCommand extends SentriesCommand {
    
    boolean call( CommandSender sender, 
                  String npcName, 
                  SentryTrait inst, 
                  Boolean set );
}
