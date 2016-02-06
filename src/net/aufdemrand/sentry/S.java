package net.aufdemrand.sentry;

import org.bukkit.ChatColor;

/**
 * A container to externalise the Strings used by Sentry.
 * @author jabelpeeps
 */
class S {
	final static String PACKAGE = "net.aufdemrand.sentry.";
	
	final static String ERROR_NO_CITIZENS = "Sentry cannot be loaded without Citizens 2.0. Aborting.";
	final static String ERROR_WRONG_DENIZEN = "This version of Sentry is only compatable with Denizen 0.9 versions";
	final static String ERROR_PLUGIN_NOT_FOUND = "Could not find or register with ";
	final static String ERROR_NOT_SENTRY = ChatColor.RED + "That command can only be used on a Sentry";
	final static String ERROR_NO_NPC = ChatColor.RED + "You must provide an NPC #id or have an NPC selected to use this command";
	final static String ERROR_ID_INVALID = ChatColor.RED + "Could not find an NPC with #id of ";
	final static String NO_COMMAND_PERM = ChatColor.RED + "You do not have permission for that command";
	final static String GET_COMMAND_HELP = ChatColor.RED + "Use /sentry help for command reference";
	final static String CANT_FOLLOW = " cannot follow you to ";
	
	final static String NPC_DELETE_ON_DEATH = " will be deleted upon death";
	final static String NPC_NO_AUTO_RESPAWN = " will not automatically respawn";
	final static String NPC_RESPAWN_AFTER = " respawns after ";
	
	final static String HELP_COMMAND_TARGET_LIST = ChatColor.GOLD + "Usage: /sentry target list";
	final static String HELP_COMMAND_TARGET_CLEAR = ChatColor.GOLD + "Usage: /sentry target clear";
	final static String HELP_COMMAND_TARGET_REMOVE = ChatColor.GOLD + "Usage: /sentry target remove <type:name>";
	final static String HELP_COMMAND_TARGET_ADD = ChatColor.GOLD + "Usage: /sentry target add <type:name>";
	
	final static String HELP_COMMAND_IGNORE_LIST = ChatColor.GOLD + "Usage: /sentry ignore list";
	final static String HELP_COMMAND_IGNORE_CLEAR = ChatColor.GOLD + "Usage: /sentry ignore clear";
	final static String HELP_COMMAND_IGNORE_ADD = ChatColor.GOLD + "Usage: /sentry ignore add <type:name>";
	final static String HELP_COMMAND_IGNORE_REMOVE = ChatColor.GOLD + "Usage: /sentry ignore remove <type:name>";
	
	final static String HELP_COMMAND_ADD_REMOVE_TYPES = ChatColor.GOLD + " <type:name> can be any of the following: "
										+ "entity:MobName entity:monster entity:player entity:all player:PlayerName "
											+ "group:GroupName town:TownName nation:NationName faction:FactionName";
	
//	final static String BLANK = ;
	final static String SECONDS = "seconds";
	final static String HELP = "help";
	final static String TARGETS = "Targets";
	final static String IGNORES = "Ignores";
	final static String HEALTH = "Health";
	final static String RANGE = "Range";
	final static String RESPAWN_DELAY = "RespawnDelay";
	final static String SPEED = "Speed";
	final static String WEIGHT = "Weight";
	final static String HEALRATE = "HealRate";
	final static String ARMOR = "Armor";
	final static String STRENGTH = "Strength";
	final static String WARNING_RANGE = "WarningRange";
	final static String ATTACK_RATE = "AttackRate";
	final static String NIGHT_VISION = "NightVision";
	final static String FOLLOW_DISTANCE = "FollowDistance";
	final static String MOUNTID = "MountID";
	final static String GUARD_TARGET = "GuardTarget";
	final static String RETALIATE = "Retaliate";
	final static String INVINCIBLE = "Invincible";
	final static String DROP_INVENTORY = "DropInventory";
	final static String TARGETABLE = "Targetable";
	final static String KILLS_DROP = "KillDrops";
	final static String CRITICAL_HITS = "CriticalHits";
	final static String GREETING = "Greeting";
	final static String WARNING = "Warning";
	final static String DEFAULT_TARGETS = "DefaultTargets";
	final static String DEFAULT_IGNORES = "DefaultIgnores";
	
	
	// Some strings to hold the names of external plugins in one location (in case of future changes to the names.)
	final static String TOWNY = "Towny";
	final static String FACTIONS = "Factions";
	final static String WAR = "War";
	final static String CLANS = "SimpleClans";
	final static String VAULT = "Vault";
	final static String CITIZENS = "Citizens";
	final static String DENIZEN = "Denizen";
	final static String SCORE = "ScoreboardTeams";
	// the last one is not an external plugin, but refers to the minecraft scoreboard system.
	
	private S() {}
}
