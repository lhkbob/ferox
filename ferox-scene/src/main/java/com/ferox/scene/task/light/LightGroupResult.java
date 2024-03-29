/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.scene.task.light;

import com.ferox.scene.Light;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.task.Result;

import java.util.List;
import java.util.Set;

public class LightGroupResult extends Result {
    private final IntProperty groupAssignment;
    private final List<Set<Light>> groups;

    public LightGroupResult(List<Set<Light>> groups, IntProperty assignment) {
        if (assignment == null) {
            throw new NullPointerException("Property cannot be null");
        }
        if (groups == null) {
            throw new NullPointerException("Group array cannot be null");
        }
        groupAssignment = assignment;
        this.groups = groups;
    }

    public IntProperty getAssignmentProperty() {
        return groupAssignment;
    }

    public Set<Light> getGroup(int group) {
        return groups.get(group);
    }

    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
