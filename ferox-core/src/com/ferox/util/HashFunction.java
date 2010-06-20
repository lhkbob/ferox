package com.ferox.util;

/**
 * HashFunction provides the ability to provide custom hash functions to class
 * types. In essence, for collections which support it, a HashFunction can be
 * used to override the default behavior of {@link Object#hashCode() hashCode()}
 * .
 * 
 * @author Michael Ludwig
 * @param <E> The Object type that will be hashed
 */
public interface HashFunction<E> {

    /**
     * Compute and return the hash code for the given <tt>value</tt>. If
     * <tt>value</tt> is null, return 0. The rules for this function are that if
     * two instances are equal, with respect to the fields used to generate the
     * hash, the returned hashes must be equivalent. Non-equal inputs can
     * generate the same hash.
     * 
     * @param value The value to hash
     * @return Value's hash code based on this function's internal rules
     */
    public int hashCode(E value);
}
