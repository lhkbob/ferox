package com.ferox.physics.dynamics.constraint;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.Bag;

public class SolverBodyPool {
    private final Bag<SolverBody> pool;
    private final Map<RigidBody, SolverBody> bodyMap;
    
    private final Vector3f velCache;
    
    public SolverBodyPool() {
        pool = new Bag<SolverBody>();
        bodyMap = new IdentityHashMap<RigidBody, SolverBody>();
        velCache = new Vector3f();
    }
    
    public SolverBody get(RigidBody body) {
        SolverBody b = bodyMap.get(body);
        if (b == null) {
            b = getSolverBody();
            b.body = body;
            b.inverseMass = body.getInverseMass();
            
            b.dlX = 0f; b.dlY = 0f; b.dlZ = 0f;
            b.daX = 0f; b.daY = 0f; b.daZ = 0f;
            
            bodyMap.put(body, b);
        }
        
        return b;
    }
    
    public void updateRigidBodies() {
        ReadOnlyVector3f v, av;
        Iterator<Entry<RigidBody, SolverBody>> it = bodyMap.entrySet().iterator();
        while(it.hasNext()) {
            Entry<RigidBody, SolverBody> e = it.next();
            RigidBody rb = e.getKey();
            SolverBody sb = e.getValue();
            
            v = rb.getVelocity();
            av = rb.getAngularVelocity();
            
            velCache.set(v.getX() + sb.dlX, v.getY() + sb.dlY, v.getZ() + sb.dlZ);
            rb.setVelocity(velCache);
            velCache.set(av.getX() + sb.daX, av.getY() + sb.daY, av.getZ() + sb.daZ);
            rb.setAngularVelocity(velCache);
            
            it.remove();
            pool.add(sb);
        }
    }
    
    private SolverBody getSolverBody() {
        if (!pool.isEmpty()) {
            SolverBody body = pool.remove(pool.size() - 1);
            return body;
        } else
            return new SolverBody();
    }
}
