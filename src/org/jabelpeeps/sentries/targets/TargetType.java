package org.jabelpeeps.sentries.targets;

import org.bukkit.entity.LivingEntity;

/** 
 * Represents a possible type of Target (or ignore) for Sentries. 
 * <p>
 * Implementations should encapsulate the minimum state needed to identify the entities they 
 * should match, and the minimum amount of logic needed to determine whether the entity 
 * supplied to {@link #includes(LivingEntity)} is included in set of defined targets.
 * <p> 
 * The handling of TargetTypes in Sentries is such that it will always be better to create
 * fewer of these objects that define multiple targets each (rather than having many
 * instances, each defining a single target entity. 
 */
public interface TargetType extends Comparable<TargetType> {
    
    /** 
     * Method to quickly determine whether the supplied <b>entity</b> is a member
     * of the set defined by instance of TargetType. 
     * 
     * @param entity - the LivingEntity to be checked.
     * @return true - if the set includes <b>entity</b> 
     */
    public boolean includes( LivingEntity entity );

    /** 
     * Method to supply a string to be used when saving the sentry; it should define the targets that 
     * a TargetType will encompass, and be parse-able to enable the TargetType to be re-created.
     * <p>
     * The String may be parsed to define targets, or a constructor used to pass typed objects directly.
     * <p>
     * In either case, the supplied string argument should be retained unmodified so that it can be retrieved 
     * by {@link #getTargetString()}
     * 
     * @param type - a string that can be parsed to provide a definition of the desired targets.
     */
    public TargetType setTargetString( String type );
    
    /**
     *  Method to get a representation of the parameters of this TargetType.
     *  
     *  @return the String supplied to {@link #setTargetString(String)}
     */
    public String getTargetString();
    
    @Override
    public boolean equals( Object o );
    
    @Override
    public int hashCode();
    
    public interface Internal {}
}
