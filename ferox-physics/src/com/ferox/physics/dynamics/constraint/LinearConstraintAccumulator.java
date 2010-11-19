package com.ferox.physics.dynamics.constraint;

import java.util.Arrays;

public class LinearConstraintAccumulator {
    public static enum ConstraintLevel {
        GENERIC, CONTACT, FRICTION
    }
    
    private LinearConstraint[] generic;
    private LinearConstraint[] contact;
    private LinearConstraint[] friction;
    
    private int genericCount;
    private int contactCount;
    private int frictionCount;
    
    public LinearConstraintAccumulator() {
        generic = new LinearConstraint[1];
        contact = new LinearConstraint[1];
        friction = new LinearConstraint[1];
    }
    
    public void clear(LinearConstraintPool pool) {
        for (int i = 0; i < genericCount; i++) {
            pool.add(generic[i]);
            generic[i] = null;
        }
        for (int i = 0; i < contactCount; i++) {
            pool.add(contact[i]);
            contact[i] = null;
        }
        for (int i = 0; i < frictionCount; i++) {
            pool.add(friction[i]);
            friction[i] = null;
        }
        
        genericCount = 0;
        contactCount = 0;
        frictionCount = 0;
    }
    
    public void add(LinearConstraint c, ConstraintLevel level) {
        switch(level) {
        case CONTACT:
            if (contactCount == contact.length)
                contact = Arrays.copyOf(contact, contactCount * 2);
            contact[contactCount++] = c;
            break;
        case FRICTION:
            if (frictionCount == friction.length)
                friction = Arrays.copyOf(friction, frictionCount * 2);
            friction[frictionCount++] = c;
            break;
        case GENERIC:
            if (genericCount == generic.length)
                generic = Arrays.copyOf(generic, genericCount * 2);
            generic[genericCount++] = c;
            break;
        }
    }
    
    public int getConstraintCount(ConstraintLevel level) {
        switch(level) {
        case CONTACT: return contactCount;
        case FRICTION: return frictionCount;
        case GENERIC: return genericCount;
        default:
            throw new IllegalArgumentException("Invalid ConstraintLevel, " + level);
        }
    }
    
    public LinearConstraint[] getConstraints(ConstraintLevel level) {
        switch(level) {
        case CONTACT: return contact;
        case FRICTION: return friction;
        case GENERIC: return generic;
        default:
            throw new IllegalArgumentException("Invalid ConstraintLevel, " + level);
        }
    }
}
