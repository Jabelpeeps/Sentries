package net.aufdemrand.sentry;

import org.bukkit.entity.LivingEntity;

/** 
 * Defines an interface for Sentry's interactions with other Server plugins.
 * <p>
 * Implementations need to interface with the plugin they are bridging to, in 
 * order to be able to find out how the plugin groups players, and then decide
 * whether they are friend or foe.
 * <p>
 * It is suggested the Bridges implement some form of caching of target and ignore
 * information in order to avoid the potential for causing lag during battles.
 * <p>
 * Some such bridges are provided with Sentry, but in future it should be possible for 
 * others to be added by third parties.
 */
public interface PluginBridge {
	
	/**
	 * @return a string to be used for logging once this bridge is activated.
	 */
	String getActivationMessage();

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
	boolean isTarget( LivingEntity entity, SentryInstance inst );
	
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
	boolean isIgnored( LivingEntity entity, SentryInstance inst );
	
	/**
	 * Refreshes any cached information held in the PluginBridge implementation.
	 * <p>
	 * It is to be expected that this method may take longer to run than 'isTarget()' 
	 * & 'isIgnore()' and therefore it should not be called at times when performance
	 * is critical. 
	 */
	void refreshLists();
}
