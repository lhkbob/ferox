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
package com.ferox.physics.collision.shape;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

import java.util.*;

public class ConvexHull extends ConvexShape {
    private final Set<Vector3> points;

    public ConvexHull(Collection<Vector3> points) {
        this.points = Collections.unmodifiableSet(new HashSet<>(points));
    }

    private ConvexHull(Set<Vector3> points) {
        this.points = points;
    }

    public Set<Vector3> getPoints() {
        return points;
    }

    @Override
    public Vector3 computeSupport(@Const Vector3 v, Vector3 result) {
        double maxDot = Double.NEGATIVE_INFINITY;
        Vector3 max = null;

        // FIXME this should be optimized, it will be a big performance benefit if we can make this faster
        // than bullets. I see two possible ways of doing this:
        //   1. Maintain an edge mapping between each point and do a search.  This would require transient state that remembered
        //      the last search, so its performance benefits are limited
        //   2. Come up with some query structure that lets me get all vectors close to a certain input direction
        //      This could be done with a quadtree holding points, and queries over increasingly large areas until a point has
        //      been found

        //  The last option might require a switch to say if there are few enough points to only do brute force
        //   (e.g. for like 10 points or so).
        //  Does this deserve a special 2D detected point grid? maybes
        for (Vector3 p : points) {
            double d = v.dot(p);
            if (max == null || d > maxDot) {
                maxDot = d;
                max = p;
            }
        }

        if (result == null) {
            return new Vector3(max);
        } else {
            return result.set(max);
        }
    }

    @Override
    public Vector3 getInertiaTensor(double mass, Vector3 result) {
        // FIXME two approaches:
        // 1. approximate it with the intertia tensor of the bounding box
        // 2. determine triangles of the convex hull and use the algorithm based on canonical tetrahedrons
        //
        // if we keep the edge information from gift wrap then we can use that in both support optimization
        // and in determining the triangles of the hull. the math involved in #2 doesn't seem so hard
        // since they already produced the equation for the canonical tetrahedron. we just have to find the
        // transformations and accumulate
        return null;
    }

    public static ConvexHull simplify(ConvexHull hull) {
        return simplify(hull, 64);
    }

    public static ConvexHull simplify(ConvexHull hull, int samples) {
        Vector3 dir = new Vector3();
        Set<Vector3> simple = new HashSet<>();
        for (int i = 0; i < samples; i++) {
            dir.set(Math.random() - .5, Math.random() - .5, Math.random() - .5).normalize();
            simple.add(hull.computeSupport(dir, null));
        }
        return new ConvexHull(simple);
    }

    public static ConvexHull construct(Collection<Vector3> points) {
        return new ConvexHull(divideAndWrap(new ArrayList<>(points)));
        //        return new ConvexHull(giftWrap(new ArrayList<>(points)));
    }

    private static Set<Vector3> divideAndWrap(List<Vector3> points) {
        Set<Vector3> result;

        if (points.size() < 10000) {
            result = giftWrap(points);
        } else {

            int numDivides = 8;
            @SuppressWarnings("unchecked") List<Vector3>[] divides = new List[numDivides];
            int i = 0;
            for (Vector3 v : points) {
                int d = (int) (i / (double) points.size() * numDivides);
                if (divides[d] == null) {
                    divides[d] = new ArrayList<>();
                }
                divides[d].add(v);
                i++;
            }

            List<Vector3> merged = new ArrayList<>();
            for (List d : divides) {
                Set<Vector3> s = divideAndWrap(d);
                merged.addAll(s);
            }
            result = giftWrap(merged);
        }
        return result;
    }

    private static Set<Vector3> giftWrap(List<Vector3> points) {
        Set<Vector3> vs = new HashSet<>();
        Queue<Edge> openEdges = new ArrayDeque<>();
        Set<Edge> visitedEdges = new HashSet<>();

        // clone in case we have a duplicate maximum (there's probably a more efficient way about this)
        Vector3 p1 = getOuterPoint(points);
        Vector3 p2 = getFarthestAngledPoint(points, p1, null);

        // add first edge
        openEdges.add(new Edge(p1, p2));
        while (!openEdges.isEmpty()) {
            Edge e = openEdges.poll();
            if (visitedEdges.add(e)) {
                // record vertices on hull
                vs.add(e.v1);
                vs.add(e.v2);

                // expand graph
                Vector3 p3 = getFarthestAngledPoint(points, e.v1, e.v2);

                // some edges have already been maxified implicitly, so 'visit' them and add reverse edges
                openEdges.add(new Edge(e.v2, e.v1));
                if (visitedEdges.add(new Edge(e.v2, p3))) {
                    openEdges.add(new Edge(p3, e.v2)); // reverse for the above
                }
                if (visitedEdges.add(new Edge(p3, e.v1))) {
                    openEdges.add(new Edge(e.v1, p3)); // reverse for the above
                }
            }
        }

        return vs;
    }

    private static Vector3 getFarthestAngledPoint(Collection<Vector3> points, Vector3 p1, Vector3 p2) {
        // if we're just starting out, offset p1 by a vector that will keep p2 in the hyperplane
        // but be guaranteed out of the hull, because p1 will have the smallest x (unique) and if that's
        // not true, it will have the smallest y or z
        if (p2 == null) {
            p2 = new Vector3(0, 1, 1).sub(p1).scale(-1);
        }

        Vector3 edge = new Vector3().sub(p2, p1);

        Vector3 currentNormal = new Vector3();
        Vector3 candidateNormal = new Vector3();
        Vector3 cross = new Vector3();

        Vector3 candidate = null;
        for (Vector3 p : points) {
            if (p != p1 && p != p2) {
                currentNormal.sub(p, p1).ortho(edge);
                if (candidate == null || cross.cross(currentNormal, candidateNormal).dot(edge) > 0) {
                    candidateNormal.set(currentNormal);
                    candidate = p;
                }
            }
        }
        return candidate;
    }

    private static Vector3 getOuterPoint(Collection<Vector3> points) {
        Vector3 outer = null;

        Iterator<Vector3> it = points.iterator();
        while (it.hasNext()) {
            Vector3 v = it.next();

            if (outer == null || v.x < outer.x) {
                outer = v;
            } else if (v.x == outer.x) {
                if (v.y < outer.y) {
                    outer = v;
                } else if (v.y == outer.y) {
                    if (v.z < outer.z) {
                        outer = v;
                    } else if (v.z == outer.z) {
                        // duplicate vertex remove it
                        it.remove();
                    }
                }
            }
        }
        return outer;
    }

    private static class Edge {
        private final Vector3 v1;
        private final Vector3 v2;

        public Edge(Vector3 v1, Vector3 v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        @Override
        public int hashCode() {
            return v1.hashCode() ^ v2.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge)) {
                return false;
            }
            Edge e = (Edge) o;
            return e.v1.equals(v1) && e.v2.equals(v2);
        }
    }
}
