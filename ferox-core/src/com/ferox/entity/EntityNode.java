package com.ferox.entity;

/**
 * <p>
 * EntityNode provides a node data type for a linked-list structure containing
 * Entities. It is intended to be managed by an EntitySystem and so exposes only
 * fields and performs no validation.
 * </p>
 * <p>
 * When an Entity is removed from an EntitySystem, it should have its entity set
 * to null as a signal that it is invalid. It's previous and next pointers
 * should remain unchanged so that any Iterators relying on the node can
 * continue to process the list.
 * </p>
 * 
 * @author Michael Ludwig
 */
class EntityNode {
    Entity entity;
    
    EntityNode next;
    EntityNode previous;
}
