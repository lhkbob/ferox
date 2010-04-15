package com.ferox.util.entity;

import java.util.HashMap;
import java.util.Map;

import com.ferox.util.entity.ComponentTable.Index;

/**
 * <p>
 * EntityComponentLink is a link between an Entity and a Component. It also
 * stores {@link Index} data associated with the Entity's current
 * {@link EntitySystem} owner. The validity of an EntityComponentLink's state is
 * contingent upon proper use by the Entity and ComponentTable.
 * </p>
 * <p>
 * An EntityComponentLink is still valid even when its primary Component is
 * null. This is just an empty link.
 * </p>
 * <p>
 * The following rules must be followed:
 * <ul>
 * <li>{@link #setTable(ComponentTable)} should be called whenever the Entity's
 * owner is modified, or the index data is invalidated because of external
 * forces.</li>
 * <li>{@link #setIndex(Index)} should only be used by ComponentTable to store
 * its created Index data.</li>
 * <li>The Entity is responsible for maintaining Component type safety, e.g. it
 * should not assign a ComponentTable that has a different type than the link's
 * primary Component.</li>
 * </ul>
 * </p>
 * 
 * @author Michael Ludwig
 */
final class EntityComponentLink {
	private final Entity entity;
	
	private Index indexData;
	
	private Component primaryComponent;
	private Map<ComponentId<?>, Component> attachedComponents;

	/**
	 * Create an empty link that is associated with the given Entity.
	 * 
	 * @param e The Entity responsible for controlling this link, it should not
	 *            be null
	 */
	public EntityComponentLink(Entity e) {
		entity = e;
	}

	/**
	 * @return The Index data associated with this link, which can be null if
	 *         its Entity has no owner, or the owner has no ComponentTable for
	 *         this link's type.
	 */
	public Index getIndex() {
		return indexData;
	}

	/**
	 * Assign the Index data to use for this EntityComponentLink. This is
	 * intended to only be invoked by ComponentTable. To clear a non-null Index
	 * on the link, use {@link Index#clearIndex()}, which will invoke this
	 * method as needed.
	 * 
	 * @param index The Index data to store with this link, or null if there is
	 *            to be no more indexing
	 */
	public void setIndex(Index index) {
		indexData = index;
	}
	
	/**
	 * @return The Entity that owns this link
	 */
	public Entity getEntity() {
		return entity;
	}

	/**
	 * @return The primary Component of this EntityComponentLink, which may be
	 *         null if a Component hasn't been assigned, or if it was later
	 *         removed
	 */
	public Component getComponent() {
		return primaryComponent;
	}

	/**
	 * <p>
	 * Assign the primary Component to this link. If there is a change in
	 * Component, then any assigned meta-Components will be reset (because a
	 * meta-Component is only persisted for a given primary Component). If the
	 * new Component is null, any associated Index will be cleared and the
	 * link's Entity's owner will have that Component type's count decremented.
	 * </p>
	 * <p>
	 * Similarly, if it's a new Component (e.g. old Component was null), the
	 * EntitySystem's type count is incremented and Index data is added if
	 * necessary. When the old Component and new Component are both non-null,
	 * the counts and index data are left unchanged and only the primary and
	 * meta Components are updated.
	 * </p>
	 * <p>
	 * In essence, {@link #setComponent(Component)} handles all of the necessary
	 * details for removing a Component from an Entity, and correctly handles
	 * when the Entity is owned or not, and when the component type is indexed
	 * or not.
	 * </p>
	 * 
	 * @param component The new Component that this links to, or null if it is
	 *            to become an empty link
	 */
	public void setComponent(Component component) {
		if (primaryComponent != component) {
			// any change, we clear the meta-components
			attachedComponents = null;
			
			if (component == null) {
				// possibly remove the old index since we're clearing the link
				if (indexData != null)
					indexData.clearIndex();
				
				if (entity.owner != null)
					entity.owner.decrementCount(primaryComponent.getComponentId());
			} else if (primaryComponent == null || indexData == null) {
				// add a link if this a new link, or if we didn't have any index data before
				if (entity.owner != null) {
					ComponentTable table = entity.owner.lookupTable(component.getComponentId().getId());
					if (table != null)
						table.assignIndex(this);
					
					if (primaryComponent == null) // only increase count when its a new link
						entity.owner.incrementCount(component.getComponentId());
				}
			}
			primaryComponent = component;
		}
	}

	/**
	 * Return a meta-Component of the given id type. This should only be used
	 * when the link's primary Component is not null.
	 * 
	 * @param <T> The Component type of the meta-Component
	 * @param id The ComponentId to query with
	 * @return The meta-Component stored in this link with type matching
	 *         <tt>id</tt>, or null if no meta-Component is associated with the
	 *         given type
	 */
	@SuppressWarnings("unchecked")
	public <T extends Component> T getMetaComponent(ComponentId<T> id) {
		return (attachedComponents == null ? null : (T) attachedComponents.get(id));
	}

	/**
	 * Assign a meta-Component <tt>component</tt> with the given <tt>id</tt> to
	 * this link. If <tt>component</tt> is null then any previous meta-Component
	 * of the id type is removed. This should only be used when the link's
	 * primary Component is not null.
	 * 
	 * @param <T> The Component type of the meta-Component
	 * @param id The ComponentId of the meta-Component being assigned
	 * @param component The Component that will be stored as meta-data with this
	 *            link
	 */
	public <T extends Component> void setMetaComponent(ComponentId<T> id, T component) {
		if (attachedComponents == null)
			attachedComponents = new HashMap<ComponentId<?>, Component>();
		
		if (component != null)
			attachedComponents.put(id, component);
		else
			attachedComponents.remove(id);
	}

	/**
	 * <p>
	 * Notify the link of an external change to the Entity that can invalidate
	 * its index data. This occurs when the Entity is added or removed from an
	 * EntitySystem, or when the Entity's owner has a type index added or
	 * removed.
	 * </p>
	 * <p>
	 * This will do nothing if the link has no primary Component. However, when
	 * it has a Component, any previous Index data will be cleared, and this
	 * link will be added to the new table using
	 * {@link ComponentTable#assignIndex(EntityComponentLink)} (assuming the table isn't
	 * null).
	 * </p>
	 * 
	 * @param newTable The new ComponentTable to index against, or null if this
	 *            link should no longer store index data
	 */
	public void setTable(ComponentTable newTable) {
		if (primaryComponent != null) {
			if (indexData != null)
				indexData.clearIndex(); // invokes #setIndex(null)
			if (newTable != null)
				newTable.assignIndex(this); // add new index data
		}
	}
}
