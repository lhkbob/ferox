package com.ferox.physics;

import com.ferox.input.KeyEvent;
import com.ferox.input.logic.Action;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.Predicates;
import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.shape.ConvexHull;
import com.ferox.renderer.*;
import com.ferox.renderer.geom.Geometry;
import com.ferox.scene.Renderable;
import com.lhkbob.entreri.Entity;

import java.util.*;

/**
 *
 */
public class ConvexHullTest extends PhysicsApplicationStub {
    private Geometry fastHull;

    private List<Geometry> rotation;
    private int selectedGeometry;

    private Entity entity;

    public static void main(String[] args) {
        new ConvexHullTest().run();
    }

    @Override
    protected void installInputHandlers(InputManager io) {
        super.installInputHandlers(io);
        io.on(Predicates.keyPress(KeyEvent.KeyCode.SPACE)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                selectedGeometry = (selectedGeometry + 1) % rotation.size();
                entity.get(Renderable.class).setGeometry(rotation.get(selectedGeometry));
            }
        });
    }

    @Override
    protected void init(OnscreenSurface surface) {
        super.init(surface);

        rotation = new ArrayList<>();

        long now = System.nanoTime();
        List<Vector3> pointCloud = createPointCloud(10000);
        System.out.printf("Creation time: %.4f\n", (System.nanoTime() - now) / 1e6);
        //        Set<Vector3> divideHull = divideAndWrap(pointCloud, 0);
        now = System.nanoTime();
        Set<Vector3> divideHull = ConvexHull.construct(pointCloud).getPoints();
        System.out.printf("Convexify time: %.4f\n", (System.nanoTime() - now) / 1e6);

        //        now = System.nanoTime();
        //        Set<Vector3> giftWrapHull = giftWrap(pointCloud);
        //        System.out.printf("gift-wrap hull size: %d compute time: %.3f ms\n", giftWrapHull.size(),
        //                          (System.nanoTime() - now) / 1e6);
        //
        //        now = System.nanoTime();
        //        Set<Vector3> myGiftWrapHull = myGiftWrap(pointCloud);
        //        System.out.printf("my-gift-wrap hull size: %d compute time: %.3f ms\n", myGiftWrapHull.size(),
        //                          (System.nanoTime() - now) / 1e6);

        rotation.add(makeFromPoints(pointCloud));
        rotation.add(makeFromPoints(divideHull));
        //        rotation.add(makeFromPoints(giftWrapHull));
        //        rotation.add(makeFromPoints(myGiftWrapHull));

        entity = system.addEntity();
        selectedGeometry = 0;
        entity.add(Renderable.class).setGeometry(rotation.get(0));
    }

    private List<Vector3> createPointCloud(int size) {
        //        Set<Vector3> p = new HashSet<>(size);
        //        for (int i = 0; i < 10; i++) {
        //            double cx = Math.random() * 16 - 8;
        //            double cy = Math.random() * 16 - 8;
        //            double cz = Math.random() * 16 - 8;
        //
        //            double s = Math.random() * 8;
        //            for (int j = 0; j < size; j++) {
        //                p.add(new Vector3(cx + Math.random() * 2 * s - s, cy + Math.random() * 2 * s - s,
        //                                  cz + Math.random() * 2 * s - s));
        //            }
        //        }
        //                        for (int i = 0; i < size; i++) {
        //                                        p.add(new Vector3(Math.random() * 16 - 8, Math.random() * 16 - 8, Math.random() * 16 - 8));
        //                            p.add(new Vector3(Math.random() * 16 - 8, 0, Math.random() * 16 - 8f));
        //                        }


        //                Teapot.GeometryData d = new Teapot.GeometryData();
        //                for (int i = 0; i < d.vertices.length; i += 3) {
        //                    p.add(new Vector3(d.vertices[i], d.vertices[i + 1], d.vertices[i + 2]));
        //                }
        //        return new ArrayList<>(p);
        return PLYReader.readVertices("/Users/mludwig/Desktop/teapot-tris.ply");
    }

    private Set<Vector3> divideAndWrap(List<Vector3> points, int depth) {
        Set<Vector3> result;
        long now = System.nanoTime();
        int intermediate;

        if (points.size() < 10000) {
            intermediate = points.size();
            result = myGiftWrap(points);
        } else {

            int numDivides = 8;
            List[] divides = new List[numDivides];
            int i = 0;
            for (Vector3 v : points) {
                //                int d = (int) (Math.random() * numDivides);
                int d = (int) (i / (double) points.size() * numDivides);
                if (divides[d] == null) {
                    divides[d] = new ArrayList();
                }
                divides[d].add(v);
                i++;
            }

            List<Vector3> merged = new ArrayList<>();
            for (List d : divides) {
                Set<Vector3> s = divideAndWrap(d, depth + 1);
                merged.addAll(s);
            }
            intermediate = merged.size();
            result = myGiftWrap(merged);
        }
        System.out.printf("depth %d from %d -> %d -> %d in %.3f\n", depth, points.size(), intermediate,
                          result.size(), (System.nanoTime() - now) / 1e6);
        return result;
    }

    private Set<Vector3> myGiftWrap(List<Vector3> points) {
        Set<Vector3> vs = new HashSet<>();
        Queue<Edge> openEdges = new ArrayDeque<>();
        Set<Edge> visitedEdges = new HashSet<>();

        // clone in case we have a duplicate maximum (there's probably a more efficient way about this)
        List<Vector3> workingPoints = new ArrayList<>(points);
        int startingIndex = getOuterPoint(workingPoints);
        int secondIndex = getFarthestAngledPoint(workingPoints, startingIndex, -1);

        // add first edge
        openEdges.add(new Edge(startingIndex, secondIndex));
        while (!openEdges.isEmpty()) {
            Edge e = openEdges.poll();
            if (visitedEdges.add(e)) {
                // record vertices on hull
                // TODO this can be optimized if we use 16bit indices and or-shift them together to create a pair index
                vs.add(workingPoints.get(e.index1));
                vs.add(workingPoints.get(e.index2));

                // expand graph
                int nextVertex = getFarthestAngledPoint(workingPoints, e.index1, e.index2);

                // some edges have already been maxified implicitly, so 'visit' them and add reverse edges
                openEdges.add(new Edge(e.index2, e.index1));
                if (visitedEdges.add(new Edge(e.index2, nextVertex))) {
                    openEdges.add(new Edge(nextVertex, e.index2)); // reverse for the above
                }
                if (visitedEdges.add(new Edge(nextVertex, e.index1))) {
                    openEdges.add(new Edge(e.index1, nextVertex)); // reverse for the above
                }
            }
        }

        return vs;
    }

    private int getFarthestAngledPoint(List<Vector3> points, int index1, int index2) {
        Vector3 p1 = points.get(index1);

        // if we're just starting out, offset p1 by a vector that will keep p2 in the hyperplane
        // but be guaranteed out of the hull, because p1 will have the smallest x (unique) and if that's
        // not true, it will have the smallest y or z
        Vector3 p2 = (index2 < 0 ? new Vector3(0, 1, 1).sub(p1).scale(-1) : points.get(index2));

        Vector3 edge = new Vector3().sub(p2, p1);

        Vector3 currentNormal = new Vector3();
        Vector3 candidateNormal = new Vector3();
        Vector3 cross = new Vector3();
        int candidateIndex = -1;

        for (int i = 0; i < points.size(); i++) {
            if (i != index1 && i != index2) {
                currentNormal.sub(points.get(i), p1).ortho(edge);
                if (candidateIndex < 0 || cross.cross(currentNormal, candidateNormal).dot(edge) > 0) {
                    candidateIndex = i;
                    candidateNormal.set(currentNormal);
                }
            }
        }
        return candidateIndex;
    }

    private Vector3 getCenter(List<Vector3> points) {
        Vector3 min = new Vector3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                                  Double.POSITIVE_INFINITY);
        Vector3 max = new Vector3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                                  Double.NEGATIVE_INFINITY);

        for (Vector3 v : points) {
            // accumulate outer bounds
            min.x = Math.min(min.x, v.x);
            min.y = Math.min(min.y, v.y);
            min.z = Math.min(min.z, v.z);
            max.x = Math.max(max.x, v.x);
            max.y = Math.max(max.y, v.y);
            max.z = Math.max(max.z, v.z);
        }

        return new Vector3().add(min, max).scale(0.5);
    }

    private int getOuterPoint(List<Vector3> points) {
        Vector3 outer = null;
        int outerIndex = -1;

        int currentIndex = 0;
        Iterator<Vector3> it = points.iterator();
        while (it.hasNext()) {
            Vector3 v = it.next();

            if (outer == null || v.x < outer.x) {
                outer = v;
                outerIndex = currentIndex;
            } else if (v.x == outer.x) {
                if (v.y < outer.y) {
                    outer = v;
                    outerIndex = currentIndex;
                } else if (v.y == outer.y) {
                    if (v.z < outer.z) {
                        outer = v;
                        outerIndex = currentIndex;
                    } else if (v.z == outer.z) {
                        // duplicate vertex remove it
                        it.remove();
                    }
                }
            }

            currentIndex++;
        }
        return outerIndex;
    }

    // FIXME this implementation only works for non-planar points
    private Set<Vector3> giftWrap(List<Vector3> points) {
        Set<Vector3> vs = new HashSet<>();
        Queue<Edge> openEdges = new ArrayDeque<>();
        Set<Edge> createdEdges = new HashSet<>();

        int index1 = lower(points);
        int index2 = getNextPoint(points, index1, -1);
        addEdge(index1, index2, createdEdges, openEdges);

        while (!openEdges.isEmpty()) {
            Edge p = openEdges.poll();
            if (!createdEdges.contains(p)) {
                int index3 = getNextPoint(points, p.index1, p.index2);

                vs.add(points.get(p.index1));
                vs.add(points.get(p.index2));
                vs.add(points.get(index3));

                addEdge(p.index1, p.index2, createdEdges, openEdges);
                addEdge(p.index2, index3, createdEdges, openEdges);
                addEdge(index3, p.index1, createdEdges, openEdges);
            }
        }

        return vs;
    }

    private void addEdge(int index1, int index2, Set<Edge> created, Queue<Edge> open) {
        Edge e = new Edge(index1, index2);
        Edge sym = new Edge(index2, index1);

        created.add(e);
        if (!created.contains(sym)) {
            open.add(sym);
        }
    }

    private int getNextPoint(List<Vector3> points, int index1, int index2) {
        Vector3 p1 = points.get(index1);

        // FIXME why does this vector work for index = -1?
        // it's because (1, 1, 0) creates a new point that is still in the hyperplane formed from (0, 0, 1)
        // and p1
        Vector3 p2 = (index2 < 0 ? new Vector3(0, 1, 1).sub(p1).scale(-1) : points.get(index2));

        Vector3 edge = new Vector3().sub(p2, p1).normalize();

        Vector3 work1 = new Vector3();
        Vector3 work2 = new Vector3();
        Vector3 work3 = new Vector3();
        int candidateIndex = -1;
        // FIXME how does this loop select the valid next point?
        for (int i = 0; i < points.size(); i++) {
            if (i != index1 && i != index2) {
                if (candidateIndex == -1) {
                    candidateIndex = i;
                } else {
                    // v = work1
                    work1.sub(points.get(i), p1);
                    work1.ortho(edge);
                    //                    work1.sub(work2.project(work1, edge));

                    // candidate = work2
                    work2.sub(points.get(candidateIndex), p1);
                    work2.ortho(edge);
                    //                    work2.sub(work3.project(work2, edge));

                    // cross = work3
                    work3.cross(work2, work1);
                    if (work3.dot(edge) > 0) {
                        candidateIndex = i;
                    }
                }
            }
        }
        return candidateIndex;
    }

    private int lower(List<Vector3> p) {
        int index = 0;
        for (int i = 1; i < p.size(); i++) {
            if (p.get(i).x < p.get(index).x) {
                index = i;
            } else if (p.get(i).z == p.get(index).z) {
                if (p.get(i).y < p.get(index).y) {
                    index = i;
                } else if (p.get(i).x < p.get(index).x) {
                    index = i;
                }
                // else duplicate point, there's a problem
            }
        }
        return index;
    }

    private static class Edge {
        private final int index1;
        private final int index2;

        public Edge(int i1, int i2) {
            index1 = i1;
            index2 = i2;
        }

        @Override
        public int hashCode() {
            return index1 ^ index2;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge)) {
                return false;
            }
            Edge e = (Edge) o;
            return e.index1 == index1 && e.index2 == index2;
        }
    }

    private Geometry makeFromPoints(Collection<Vector3> points) {
        float[] verts = new float[points.size() * 3];
        int i = 0;
        for (Vector3 v : points) {
            verts[i * 3] = (float) v.x;
            verts[i * 3 + 1] = (float) v.y;
            verts[i * 3 + 2] = (float) v.z;
            i++;
        }

        VertexBuffer vbo = getFramework().newVertexBuffer().from(verts).build();
        final AxisAlignedBox aabb = new AxisAlignedBox(verts, 0, 0, points.size());
        final VertexAttribute v = new VertexAttribute(vbo, 3);
        return new Geometry() {
            @Override
            public AxisAlignedBox getBounds() {
                return aabb;
            }

            @Override
            public Renderer.PolygonType getPolygonType() {
                return Renderer.PolygonType.POINTS;
            }

            @Override
            public ElementBuffer getIndices() {
                return null;
            }

            @Override
            public int getIndexOffset() {
                return 0;
            }

            @Override
            public int getIndexCount() {
                return v.getMaximumNumVertices();
            }

            @Override
            public VertexAttribute getVertices() {
                return v;
            }

            @Override
            public VertexAttribute getNormals() {
                return null;
            }

            @Override
            public VertexAttribute getTextureCoordinates() {
                return null;
            }

            @Override
            public VertexAttribute getTangents() {
                return null;
            }
        };
    }

    @Override
    protected void renderFrame(OnscreenSurface surface) {
        system.getScheduler().runOnCurrentThread(renderJob);
    }
}
