package com.ferox.scene.controller.light;

import java.util.Set;

import com.ferox.scene.Light;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Result;
import com.lhkbob.entreri.property.IntProperty;

public class LightGroupResult implements Result {
    private final IntProperty groupAssignment;
    private final Set<Component<? extends Light<?>>>[] groups;
    
    public LightGroupResult(Set<Component<? extends Light<?>>>[] groups, IntProperty assignment) {
        if (assignment == null)
            throw new NullPointerException("Property cannot be null");
        if (groups == null)
            throw new NullPointerException("Group array cannot be null");
        groupAssignment = assignment;
        this.groups = groups;
    }
    
    public IntProperty getAssignmentProperty() {
        return groupAssignment;
    }
    
    public Set<Component<? extends Light<?>>> getGroup(int group) {
        return groups[group];
    }
    
    public int getGroupCount() {
        return groups.length;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}