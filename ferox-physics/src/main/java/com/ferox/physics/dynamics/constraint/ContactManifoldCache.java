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
        this(.1f, .0343f);
    }
    
    public ContactManifoldCache(float contactProcessingThreshold, float contactBreakingThreshold) {
        if (contactProcessingThreshold < 0f || contactBreakingThreshold < 0f)
            throw new IllegalArgumentException("Contact processing and breaking thresholds must be positive, not: " 
                                               + contactProcessingThreshold + ", " + contactBreakingThreshold);
        
        this.contactBreakingThreshold = contactBreakingThreshold;
        this.contactProcessingThreshold = contactProcessingThreshold;
        
        manifolds = new HashMap<CollidablePair, ContactManifold>();
        query = new CollidablePair();
    }
    
    public Collection<ContactManifold> getContacts() {
        return manifolds.values();
    }
    
    public ContactManifold getContactManifold(Collidable objA, Collidable objB) {
        query.a = objA;
        query.b = objB;
        return manifolds.get(query);
    }
    
    public void addContact(Collidable objA, Collidable objB, ClosestPair pair) {
        ContactManifold oldManifold = getContactManifold(objA, objB);
        if (oldManifold != null) {
            // object are already in contact, just update existing manifold
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
    
    public void remove(Collidable c) {
        Iterator<Entry<CollidablePair, ContactManifold>> it = manifolds.entrySet().iterator();
        while(it.hasNext()) {
            CollidablePair cp = it.next().getKey();
            if (cp.a == c || cp.b == c)
                it.remove();
        }
    }
    
    public void update() {
        Iterator<Entry<CollidablePair, ContactManifold>> it = manifolds.entrySet().iterator();
        while(it.hasNext()) {
            ContactManifold cm = it.next().getValue();
            cm.update();
            if (cm.getManifoldSize() == 0) {
                it.remove();
            }
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
