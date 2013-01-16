package com.ferox.resource.geom;

import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.BufferData;
import com.ferox.resource.UnsignedDataView;

public final class TopologyUtil {
    private TopologyUtil() {}

    public static int inflateTriangleStrip(BufferData strips, int stripOffset,
                                           int stripCount, BufferData targetTris,
                                           int triOffset) {
        int triangleCount = PolygonType.TRIANGLE_STRIP.getPolygonCount(stripCount);
        if (targetTris.getLength() - triOffset < triangleCount * 3) {
            throw new IndexOutOfBoundsException("Target triangle index buffer is not large enough");
        }
        if (stripOffset + stripCount > strips.getLength()) {
            throw new IndexOutOfBoundsException("Offset and count access invalid indices in source buffer");
        }
        if (stripOffset < 0 || stripCount < 0 || triOffset < 0) {
            throw new IllegalArgumentException("Offsets and count must be positive");
        }

        UnsignedDataView source = strips.getUnsignedView();
        UnsignedDataView target = targetTris.getUnsignedView();

        // if we actually have triangles, fill the buffer
        if (triangleCount >= 1) {
            // first three vertices form a triangle
            target.set(triOffset, source.get(stripOffset)); // set a = 0, writeA = false
            target.set(triOffset + 1, source.get(stripOffset + 1)); // set b = 1, writeA = true
            target.set(triOffset + 2, source.get(stripOffset + 2)); // set a = 2, writeA = false

            // according to the OpenGL spec, triangle strips form triangles with
            // the A vertex, B vertex, and the current vertex. The current vertex
            // is then assigned to A or B alternating between the two to maintain
            // proper winding.
            // - in this loop we remove the storage for A or B and alternate
            //   which index is inserted first (i - 1) or (i - 2)
            boolean aFirst = false;

            int ti = triOffset + 3;
            for (int i = stripOffset + 3; i < stripOffset + stripCount; i++) {
                if (aFirst) {
                    target.set(ti++, source.get(i - 2));
                    target.set(ti++, source.get(i - 1));
                } else {
                    target.set(ti++, source.get(i - 1));
                    target.set(ti++, source.get(i - 2));
                }
                target.set(ti++, source.get(i));
                aFirst = !aFirst;
            }
        }

        return triangleCount * 3;
    }

    public static int inflateTriangleStripArray(int stripOffset, int stripCount,
                                                BufferData targetTris, int triOffset) {
        int triangleCount = PolygonType.TRIANGLE_STRIP.getPolygonCount(stripCount);
        if (targetTris.getLength() - triOffset < triangleCount * 3) {
            throw new IndexOutOfBoundsException("Target triangle index buffer is not large enough");
        }
        if (stripOffset < 0 || stripCount < 0 || triOffset < 0) {
            throw new IllegalArgumentException("Offsets and count must be positive");
        }

        UnsignedDataView target = targetTris.getUnsignedView();

        // if we actually have triangles, fill the buffer
        if (triangleCount >= 1) {
            // first three vertices form a triangle
            target.set(triOffset, stripOffset);
            target.set(triOffset + 1, stripOffset + 1);
            target.set(triOffset + 2, stripOffset + 2);

            // according to the OpenGL spec, triangle strips form triangles with
            // the A vertex, B vertex, and the current vertex. The current vertex
            // is then assigned to A or B alternating between the two to maintain
            // proper winding.
            // - in this loop we remove the storage for A or B and alternate
            //   which index is inserted first (i - 1) or (i - 2)
            boolean aFirst = false;

            int ti = triOffset + 3;
            for (int i = stripOffset + 3; i < stripOffset + stripCount; i++) {
                if (aFirst) {
                    target.set(ti++, i - 2);
                    target.set(ti++, i - 1);
                } else {
                    target.set(ti++, i - 1);
                    target.set(ti++, i - 2);
                }
                target.set(ti++, i);
                aFirst = !aFirst;
            }
        }

        return triangleCount * 3;
    }

    public static void inflateSimpleArray(int stripOffset, int stripCount,
                                          BufferData target, int targetOffset) {
        if (targetOffset + stripCount > target.getLength()) {
            throw new IndexOutOfBoundsException("Target triangle index buffer is not large enough");
        }

        UnsignedDataView out = target.getUnsignedView();
        int ti = targetOffset;
        for (int i = stripOffset; i < stripOffset + stripCount; i++) {
            out.set(ti++, i);
        }
    }
}
