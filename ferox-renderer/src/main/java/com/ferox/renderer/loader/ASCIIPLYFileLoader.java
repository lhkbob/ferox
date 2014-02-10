package com.ferox.renderer.loader;

import com.ferox.math.AxisAlignedBox;
import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.Framework;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.VertexAttribute;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Tangents;
import com.ferox.renderer.geom.TriangleIterator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 *
 */
public class ASCIIPLYFileLoader implements GeometryFileLoader {
    @Override
    public Geometry read(Framework framework, BufferedInputStream input) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(input))) {
            VertexElementDefinition vertices = null;
            FaceElementDefinition faces = null;

            List<ElementDefinition> elements = new ArrayList<>();
            boolean headerDone = false;
            int currentElement = 0;
            int elementCount = 0;

            // FIXME if this file is total hocum, then a new line could read way more than 32 bytes
            // and then mark is hosed, should handle the whole parsing more like how it's done in Radiance or DDS loaders
            input.mark(32);
            String line = in.readLine();
            if (!"ply".equalsIgnoreCase(line)) {
                input.reset();
                return null;
            }
            line = in.readLine();
            if (!"format ascii 1.0".equalsIgnoreCase(line)) {
                input.reset();
                return null;
            }

            while ((line = in.readLine()) != null) {
                if (!headerDone) {
                    // header
                    if (line.trim().equalsIgnoreCase("end_header")) {
                        // start elements
                        headerDone = true;
                    } else {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length == 3 && parts[0].equalsIgnoreCase("element")) {
                            if (parts[1].equalsIgnoreCase("vertex")) {
                                vertices = new VertexElementDefinition(Integer.parseInt(parts[2]));
                                elements.add(vertices);
                            } else if (parts[1].equalsIgnoreCase("face")) {
                                faces = new FaceElementDefinition(Integer.parseInt(parts[2]));
                                elements.add(faces);
                            } else {
                                elements.add(new ElementDefinition(parts[1], Integer.parseInt(parts[2])));
                            }
                        } else if (parts.length > 0 && parts[0].equalsIgnoreCase("property")) {
                            elements.get(elements.size() - 1).addProperty(parts);
                        }
                    }
                } else {
                    String[] parts = line.trim().split("\\s+");
                    ElementDefinition e = elements.get(currentElement);
                    e.processElement(parts, elementCount++);
                    if (elementCount >= e.count) {
                        // next element
                        currentElement++;
                        elementCount = 0;
                    }
                }
            }

            return new PLYGeometryImpl(framework, vertices, faces);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Bad element or property definition", e);
        } catch (NumberFormatException e) {
            throw new IOException("Bad element entry, unable to parse float value", e);
        }
    }

    private static class PLYGeometryImpl implements Geometry {
        private final VertexAttribute vertices;
        private final VertexAttribute normals;
        private final VertexAttribute texCoords;
        private final VertexAttribute tangents;

        private final ElementBuffer indices;

        private final AxisAlignedBox bounds;

        public PLYGeometryImpl(Framework framework, VertexElementDefinition v, FaceElementDefinition f) {
            // finalize triangle index list
            int[] i = new int[f.indices.size()];
            for (int j = 0; j < i.length; j++) {
                i[j] = f.indices.get(j);
            }

            // compute additional vector attributes
            // FIXME compute normals here as well, if they're not provided
            if (v.hasTexCoords()) {
                TriangleIterator ti = TriangleIterator.Builder.newBuilder().vertices(v.pos).normals(v.norm)
                                                              .textureCoordinates(v.tc).tangents(v.tan)
                                                              .fromElements(i, 0, i.length).build();
                Tangents.compute(ti);
            }

            vertices = new VertexAttribute(framework.newVertexBuffer().from(v.pos).build(), 3);
            normals = new VertexAttribute(framework.newVertexBuffer().from(v.norm).build(), 3);
            if (v.hasTexCoords()) {
                texCoords = new VertexAttribute(framework.newVertexBuffer().from(v.tc).build(), 2);
                tangents = new VertexAttribute(framework.newVertexBuffer().from(v.tan).build(), 4);
            } else {
                texCoords = null;
                tangents = null;
            }


            indices = framework.newElementBuffer().fromUnsigned(i).build();
            bounds = new AxisAlignedBox(v.pos, 0, 0, v.count);
        }

        @Override
        public AxisAlignedBox getBounds() {
            return bounds;
        }

        @Override
        public Renderer.PolygonType getPolygonType() {
            return Renderer.PolygonType.TRIANGLES;
        }

        @Override
        public ElementBuffer getIndices() {
            return indices;
        }

        @Override
        public int getIndexOffset() {
            return 0;
        }

        @Override
        public int getIndexCount() {
            return indices.getLength();
        }

        @Override
        public VertexAttribute getVertices() {
            return vertices;
        }

        @Override
        public VertexAttribute getNormals() {
            return normals;
        }

        @Override
        public VertexAttribute getTextureCoordinates() {
            return texCoords;
        }

        @Override
        public VertexAttribute getTangents() {
            return tangents;
        }
    }

    private static class ElementDefinition {
        final String name;
        final int count;

        final Map<String, Integer> properties;

        public ElementDefinition(String name, int count) {
            this.name = name;
            this.count = count;
            properties = new HashMap<>();
        }

        // assumes line[0] == "property"
        void addProperty(String[] line) throws IOException {
            properties.put(line[line.length - 1], properties.size());
        }

        void processElement(String[] line, int i) {
            // do nothing in base class, i.e. ignore unknown element types
        }
    }

    private static class VertexElementDefinition extends ElementDefinition {
        private static final List<String> SPECIAL = Arrays.asList("x", "y", "z", "nx", "ny", "nz", "u", "v",
                                                                  "s", "t");

        final float[] pos;
        final float[] norm;
        final float[] tc;
        final float[] tan;

        public VertexElementDefinition(int count) {
            super("vertex", count);
            pos = new float[count * 3];
            norm = new float[count * 3];
            tc = new float[count * 2];
            tan = new float[count * 4];
        }

        boolean hasPosition() {
            return properties.containsKey("x") && properties.containsKey("y") && properties.containsKey("z");
        }

        boolean hasNormal() {
            return properties.containsKey("nx") && properties.containsKey("ny") &&
                   properties.containsKey("nz");
        }

        boolean hasTexCoords() {
            return (properties.containsKey("u") && properties.containsKey("v")) ||
                   (properties.containsKey("s") && properties.containsKey("t"));
        }

        // TODO add support for vertex colors and the red, green, blue properties

        @Override
        void addProperty(String[] line) throws IOException {
            if (SPECIAL.contains(line[line.length - 1])) {
                // require type to be float
                if (!line[1].equalsIgnoreCase("float")) {
                    throw new IOException("Vertex property type other than float is unsupported");
                }
            } else if (line[1].equalsIgnoreCase("list")) {
                // FIXME we could get rid of this restriction if we walked past unknown properties and handled lists correctly
                throw new IOException("List vertex properties are not supported");
            }
            super.addProperty(line);
        }

        @Override
        void processElement(String[] line, int i) {
            if (hasPosition()) {
                pos[i * 3] = Float.parseFloat(line[properties.get("x")]);
                pos[i * 3 + 1] = Float.parseFloat(line[properties.get("y")]);
                pos[i * 3 + 2] = Float.parseFloat(line[properties.get("z")]);
            }
            if (hasNormal()) {
                norm[i * 3] = Float.parseFloat(line[properties.get("nx")]);
                norm[i * 3 + 1] = Float.parseFloat(line[properties.get("ny")]);
                norm[i * 3 + 2] = Float.parseFloat(line[properties.get("nz")]);
            }
            if (hasTexCoords()) {
                if (properties.containsKey("u") && properties.containsKey("v")) {
                    tc[i * 2] = Float.parseFloat(line[properties.get("u")]);
                    tc[i * 2 + 1] = Float.parseFloat(line[properties.get("v")]);
                } else {
                    tc[i * 2] = Float.parseFloat(line[properties.get("s")]);
                    tc[i * 2 + 1] = Float.parseFloat(line[properties.get("t")]);
                }
            }
        }
    }

    private static class FaceElementDefinition extends ElementDefinition {
        final List<Integer> indices;

        public FaceElementDefinition(int count) {
            super("face", count);
            indices = new ArrayList<>();
        }

        @Override
        void addProperty(String[] line) throws IOException {
            if (line[line.length - 1].equalsIgnoreCase("vertex_index") ||
                line[line.length - 1].equalsIgnoreCase("vertex_indices")) {
                if (line.length != 5 || !line[1].equalsIgnoreCase("list") ||
                    line[2].equalsIgnoreCase("float") || line[3].equalsIgnoreCase("float")) {
                    throw new IOException("Face vertex_index property must be list with integer params, any other type is unsupported");
                }
            }
            super.addProperty(line);
        }

        @Override
        void processElement(String[] line, int i) {
            // skip if we don't have the conventional vertex_index list defined
            int base;
            if (properties.containsKey("vertex_index")) {
                base = properties.get("vertex_index");
            } else if (properties.containsKey("vertex_indices")) {
                base = properties.get("vertex_indices");
            } else {
                return;
            }

            int ct = Integer.parseInt(line[base]);
            if (ct == 3) {
                // plain triangle
                indices.add(Integer.parseInt(line[base + 1]));
                indices.add(Integer.parseInt(line[base + 2]));
                indices.add(Integer.parseInt(line[base + 3]));
            } else if (ct == 4) {
                // split quad into 2 triangles
                indices.add(Integer.parseInt(line[base + 1]));
                indices.add(Integer.parseInt(line[base + 2]));
                indices.add(Integer.parseInt(line[base + 3]));

                indices.add(Integer.parseInt(line[base + 1]));
                indices.add(Integer.parseInt(line[base + 3]));
                indices.add(Integer.parseInt(line[base + 4]));
            }
            // else ignore the face format
        }
    }
}
