package net.aufdemrand.sentry;

import org.bukkit.ChatColor;

/**
 * A container to externalise the Strings used by Sentry.
 */
abstract class S {
	final static String PACKAGE = "net.aufdemrand.sentry.";
	
	interface Col {
		final static String RED = ChatColor.RED.toString();
		final static String GOLD = ChatColor.GOLD.toString();
		final static String GREEN = ChatColor.GREEN.toString();
		final static String BLUE = ChatColor.BLUE.toString();
		final static String WHITE = ChatColor.WHITE.toString();
		final static String YELLOW = ChatColor.YELLOW.toString();
		final static String RESET = ChatColor.RESET.toString();
		final static String BOLD = ChatColor.BOLD.toString();
	}
	
	// Strings used in messages for players and/or console errors.
	final static String ERROR_NO_CITIZENS = "Sentry cannot be loaded without Citizens 2.0. Aborting.";
	final static String ERROR_WRONG_DENIZEN = "This version of Sentry is only compatable with Denizen 0.9 versions";
	final static String ERROR_PLUGIN_NOT_FOUND = "Could not find the other plugin called ";
	
	final static String ERROR = Col.RED.concat( "Error: '" );
	final static String ERROR_NOT_NUMBER = "' was not recognised as a valid number.";
	
	final static String ERROR_NOT_SENTRY = "That command can only be used on a Sentry";
	final static String ERROR_NO_NPC = "You must provide an NPC #id or have an NPC selected to use this command";
	final static String ERROR_ID_INVALID = "Could not find an NPC with #id of ";
	final static String ERROR_NO_COMMAND_PERM = "You do not have permission for that command";
	final static String ERROR_NO_MORE_HELP = "No further help is available for this command.";
	
	final static String GET_COMMAND_HELP = "Use /sentry help for command reference";
	final static String CANT_FOLLOW = " cannot follow you to ";
	final static String ADDED_TO_LIST = "was added to this sentry's list of";
	final static String ALLREADY_ON_LIST = "is already on this sentry's list of";
	final static String REMOVED_FROM_LIST = "was removed from this sentry's list of";
	final static String NOT_FOUND_ON_LIST = "was not found on this sentry's list of";
	final static String NOT_ANY = "does not have any";
	
	final static String NPC_DELETE_ON_DEATH = " will be deleted upon death";
	final static String NPC_NO_AUTO_RESPAWN = " will not automatically respawn";
	final static String NPC_RESPAWN_AFTER = " respawns after ";
	
	final static String HELP_COMMAND_TARGET = "do '/sentry target <option>' where <option> is:-";
	final static String HELP_COMMAND_IGNORE = "do '/sentry ignore <option>' where <option is:-";
	final static String HELP_LIST = "- to display current list of";
	final static String HELP_CLEAR = "- to clear the ALL the current";
	final static String HELP_REMOVE_TYPE = " remove <type:name>";
	final static String HELP_REMOVE = "- to remove <type:name> from the list";
	final static String HELP_ADD_TYPE = " add <type:name>";
	final static String HELP_ADD = "- to add <type:name> to the list";
	
	final static String HELP_ADD_REMOVE_TYPES = 
			"  <type:name> can be any of the following: entity:<MobName> entity:monster entity:player entity:all "
			+ "player:<PlayerName> group:<GroupName> town:<TownName> nation:<NationName> faction:<FactionName>";
	
//	a random selection of single words.
	final static String SECONDS = "seconds";
	final static String HELP = "help";
	final static String TARGET = "target";
	final static String TARGETS = "Targets";
	final static String IGNORE = "ignore";
	final static String IGNORES = "Ignores";
	final static String FOLLOW = "follow";
	final static String MOUNT = "mount";
	final static String GUARD = "guard";
	final static String DROPS = "drops";
	final static String SPAWN = "spawn";
	final static String TRUE = "true";
	final static String FALSE = "false";
	final static String ON = "On";
	final static String OFF = "Off";
	final static String YET = " yet.";
	final static String ADD = "add";
	final static String REMOVE = "remove";
	final static String EQUIP = "equip";
	final static String INFO = "info";
	final static String LIST = "list";
	final static String CLEAR = "clear";
	final static String JOIN = "join";
	
	// Strings used as keys for config values, and npc attributes for saving.
	final static String RESPAWN_DELAY = "RespawnDelay";
	final static String WARNING_RANGE = "WarningRange";
	final static String ATTACK_RATE = "AttackRate";
	final static String NIGHT_VISION = "NightVision";
	final static String HEALRATE = "HealRate";
	final static String FOLLOW_DISTANCE = "FollowDistance";
	final static String GUARD_TARGET = "GuardTarget";
	final static String DROP_INVENTORY = "DropInventory";
	final static String KILLS_DROP = "KillDrops";
	final static String TARGETABLE = "Targetable";
	final static String RETALIATE = "Retaliate";
	final static String INVINCIBLE = "Invincible";
	final static String CRITICAL_HITS = "CriticalHits";
	final static String DEFAULT_TARGETS = "DefaultTargets";
	final static String DEFAULT_IGNORES = "DefaultIgnores";
	final static String GREETING = "Greeting";
	final static String WARNING = "Warning";
	final static String HEALTH = "Health";
	final static String RANGE = "Range";
	final static String RESPAWN = "Respawn";
	final static String SPEED = "Speed";
	final static String WEIGHT = "Weight";
	final static String ARMOR = "Armor";
	final static String STRENGTH = "Strength";
	final static String MOUNTID = "MountID";
	
	// Some strings to hold the names of external plugins in one location (in case of future changes to the names.)
	final static String TOWNY = "Towny";
	final static String FACTIONS = "Factions";
	final static String VAULT = "Vault";
	final static String CITIZENS = "Citizens";
	final static String DENIZEN = "Denizen";
	final static String SCORE = "ScoreboardTeams";
	// the last one is not an external plugin, but refers to the minecraft scoreboard system.
	
	
	// Strings used for permission strings - these *must* be the same as those in plugin.yml
	final static String PERM_TARGET = "sentry.target";
	final static String PERM_IGNORE = "sentry.ignore";
	final static String PERM_INFO = "sentry.info";
	final static String PERM_EQUIP = "sentry.equip";
	final static String PERM_RELOAD = "sentry.reload";
	final static String PERM_DEBUG = "sentry.debug";
	final static String PERM_SPAWN = "sentry.spawn";
	final static String PERM_GUARD = "sentry.guard";
	final static String PERM_WARNING = "sentry.warning";
	final static String PERM_GREETING = "sentry.greeting";
	final static String PERM_WARNING_RANGE = "sentry.stats.warningrange";
	final static String PERM_SPEED = "sentry.stats.speed";
	final static String PERM_RANGE = "sentry.stats.range";
	final static String PERM_HEALTH = "sentry.stats.health";
	final static String PERM_HEAL_RATE = "sentry.stats.healrate";
	final static String PERM_ARMOR = "sentry.stats.armor";
	final static String PERM_STRENGTH = "sentry.stats.strength";
	final static String PERM_NIGHTVISION = "sentry.stats.nightvision";
	final static String PERM_ATTACK_RATE = "sentry.stats.attackrate";
	final static String PERM_RESPAWN_DELAY = "sentry.stats.respawn";
	final static String PERM_FOLLOW_DIST = "sentry.stats.follow";
	final static String PERM_MOUNT = "sentry.options.mount";
	final static String PERM_TARGETABLE = "sentry.options.targetable";
	final static String PERM_KILLDROPS = "sentry.options.killdrops";
	final static String PERM_DROPS = "sentry.options.drops";
	final static String PERM_CRITICAL_HITS = "sentry.options.criticals";
	final static String PERM_RETALIATE = "sentry.options.retaliate";
	final static String PERM_INVINCIBLE = "sentry.options.invincible";
	final static String PERM_CITS_ADMIN = "citizens.admin";
	
}
