package com.ferox.physics.dynamics.constraint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.algorithm.ClosestPair;

public class ContactManifoldCache {
    private final Map<CollidablePair, ContactManifold> manifolds;
    private final CollidablePair query; // mutable, do not put in manifolds
    
    private final float contactBreakingThreshold;
    private final float contactProcessingThreshold;
    
    public ContactManifoldCache() {
        this(0f, .02f);
    }
    
    public ContactManifoldCache(float contactProcessingThreshold, float contactBreakingThreshold) {
        if (contactProcessingThreshold > contactBreakingThreshold)
            throw new IllegalArgumentException("Processing threshold (" + contactProcessingThreshold + 
                                               ") must be greater than breaking threshold (" + contactBreakingThreshold + ")");
        
        this.contactBreakingThreshold = contactBreakingThreshold;
        this.contactProcessingThreshold = contactProcessingThreshold;
        
        manifolds = new HashMap<CollidablePair, ContactManifold>();
        query = new CollidablePair();
    }

    public Collection<ContactManifold> getContacts() {
        return manifolds.values();
    }
    
    public void addContact(Collidable objA, Collidable objB, ClosestPair pair) {
        query.a = objA;
        query.b = objB;
        
        ContactManifold oldManifold = manifolds.get(query);
        if (oldManifold != null) {
            // objects already in contact, just update existing manifold
            oldManifold.addContact(pair, objA == oldManifold.getCollidableB());
            return;
        }
        
        ContactManifold newManifold = new ContactManifold(objA, objB, contactProcessingThreshold, contactBreakingThreshold);
        newManifold.addContact(pair, false);
        
        CollidablePair contact = new CollidablePair();
        contact.a = objA;
        contact.b = objB;
        
        manifolds.put(contact, newManifold);
    }
    
    public void update() {
        Iterator<Entry<CollidablePair, ContactManifold>> it = manifolds.entrySet().iterator();
        while(it.hasNext()) {
            ContactManifold cm = it.next().getValue();
            cm.update();
            if (cm.getManifoldSize() == 0)
                it.remove();
        }
    }
    
    private static class CollidablePair {
        Collidable a;
        Collidable b;
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CollidablePair))
                return false;
            CollidablePair t = (CollidablePair) o;
            return (t.a == a && t.b == b) || (t.b == a && t.a == b);
        }
        
        @Override
        public int hashCode() {
            // sum of hashes -> follow Set hashcode since a pair is just a 2 element set
            return a.hashCode() + b.hashCode();
        }
    }
}
