package org.jabelpeeps.sentries;

import org.bukkit.command.CommandSender;

public interface SentriesComplexCommand extends SentriesCommand {
    
    boolean call( CommandSender sender, // the sender of the command
                  String npcName,       // the NPC's name
                  SentryTrait inst,     // the NPC's SentryTrait
                  int nextArg,          // the next argument to be parsed
                  String... args );     // the arguments array.
  
}
