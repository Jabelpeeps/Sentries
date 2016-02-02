package net.aufdemrand.sentry;

import org.bukkit.entity.LivingEntity;

/** 
 * An abstract class defining Sentry's interactions with other Server plugins.
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
	 * The caller is responsible for checking that the third party plugin
	 * is installed and active before calling this method, so implementations
	 * can assume this to be the case.
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
	
	abstract boolean addTarget( String target, SentryInstance inst );
	
	abstract boolean addIgnore( String target, SentryInstance inst );

//  actually, I don't think I'll need this one...
//	/**
//	 * Refreshes all cached information held in the PluginBridge implementation.
//	 * <p>
//	 * It is to be expected that this method may take longer to run than 'isTarget()' 
//	 * & 'isIgnore()' and therefore it should not be called at times when performance
//	 * is critical. 
//	 */
//	void refreshAllLists();
	
	/**
	 * Refreshes the information held in the cache regarding the supplied
	 * SentryInstance only.
	 * 
	 * @param inst - the SentryInstance to re-cache.
	 */
	abstract void refreshLists( SentryInstance inst );
}
