package net.aufdemrand.sentry;

import org.bukkit.entity.LivingEntity;

/** 
 * An abstract class to act as a bridge between Sentry and other Server plugins.
 * <p>
 * Child classes need to interface with the plugin they are bridging to, in 
 * order to be able to find out how the plugin groups players, and then decide
 * whether they are friend or foe.
 * <p>
 * It is suggested the child classes implement some form of caching of target and ignore
 * information in order to avoid the potential for causing lag during battles.
 * <p>
 * Some such bridges are provided with Sentry, but in future it should be possible for 
 * others to be added by third parties.
 */
public abstract class PluginBridge {
	
	final int bitFlag;
	
	PluginBridge( int flag ) {
		bitFlag = flag;
	}
	
	/**
	 * Carries out any initialisation that the implementation requires.
	 * 
	 * The caller will check that the third party plugin is installed and 
	 * active before calling this method, so implementations can assume 
	 * this to be the case.
	 * 
	 * @return true - if the activation was successful.
	 * @return false - if not.
	 */
	abstract boolean activate();
	
	/**
	 * Implementations may specify a string to be used for logging once a call 
	 * has been made to 'activate()'; this can either be a static string, or
	 * dynamic (e.g. to explain a failure to activate.)
	 * 
	 * @return the string.
	 */
	abstract String getActivationMessage();

	/**
	 * Determines whether the supplied LivingEntity is a valid target of the supplied
	 * SentryInstance.
	 * <p>
	 * This method should return a result as quickly as possible, so that server
	 * performance is not affected if it is called often.
	 * 
	 * @param entity - the possible target entity
	 * @param inst - the SentryInstance that is asking.
	 * @return true - if the entity is a valid target.
	 */
	abstract boolean isTarget( LivingEntity entity, SentryInstance inst );
	
	/**
	 * Determines whether the supplied LivingEntity should be ignored as a possible 
	 * target of the supplied SentryInstance.
	 * <p>
	 * This method should return a result as quickly as possible, so that server
	 * performance is not affected if it is called often.
	 * 
	 * @param entity - the entity that should possibly be ignored.
	 * @param inst - the SentryInstance that is asking.
	 * @return true - if the entity should be ignored.
	 */
	abstract boolean isIgnoring( LivingEntity entity, SentryInstance inst );
	
	/**
	 * Add a target identified by the supplied string for the SentryInstance supplied.
	 * <p>
	 * The PluginBridge should achieve this task without modifying the SentryInstance
	 * (which knows nothing about the third party plugin) but should store relevant references
	 * in preparation for a call to 'isTarget()'
	 * 
	 * @param target - a String identifying the intended target (the exact contents
	 * can vary, as long as the pluginBridge know how to parse the String).
	 * @param inst - the SentryInstance instance that will have the target recorded against it.
	 * @return true - if the target is added successfully.
	 */
	abstract boolean addTarget( String target, SentryInstance inst );
	
	/**
	 * Add an entity - identified by the supplied string - to be ignored by the supplied 
	 * SentryInstance.
	 * <p>
	 * The PluginBridge should achieve this task without modifying the SentryInstance
	 * (which knows nothing about the third party plugin) but should store relevant references
	 * in preparation for a call to 'isIgnoring()'
	 * 
	 * @param target - a String identifying the entity to ignore (the exact contents
	 * can vary, as long as the pluginBridge know how to parse the String).
	 * @param inst - the SentryInstance instance that will have the target recorded against it.
	 * @return true - if the target is added successfully.
	 */
	abstract boolean addIgnore( String target, SentryInstance inst );
	
	/**
	 * @return a string to be used as a command argument to refer to this PluginBridge.
	 */
	abstract String getCommandText();
	
	/**
	 * @return the help text describing how to identify targets and ignores for this 
	 * PluginBridge - so that they will be recognised when parsed by 'addTarget()' and 
	 * 'addIgnore()'
	 */
	abstract String getCommandHelp();
	
	/**
	 * Refreshes the information held in the cache regarding the supplied
	 * SentryInstance only.
	 * 
	 * @param inst - the SentryInstance to re-cache.
	 */
	abstract void refreshLists( SentryInstance inst );
}
