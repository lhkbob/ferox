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
package com.ferox.physics;

import com.ferox.math.Vector3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PLYReader {
    private static enum State {
        PREAMBLE,
        HEADER,
        VERTICES,
        FACES,
        POST_FACE
    }

    public static List<Vector3> readVertices(String file) {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            int vertexCount = -1;
            int xPropertyIndex = -1;
            int yPropertyIndex = -1;
            int zPropertyIndex = -1;
            int[] faces = null;
            List<Vector3> vertices = new ArrayList<>();

            int propertyCount = 0;
            int faceCount = 0;
            State state = State.PREAMBLE;

            String line;
            while ((line = in.readLine()) != null) {
                switch (state) {
                case PREAMBLE:
                    if (line.trim().equalsIgnoreCase("ply")) {
                        state = State.HEADER;
                    }
                    break;
                case HEADER:
                    if (line.trim().equalsIgnoreCase("end_header")) {
                        state = State.VERTICES;
                    } else {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length == 3 && parts[0].equalsIgnoreCase("element") &&
                            parts[1].equalsIgnoreCase("vertex")) {
                            vertexCount = Integer.parseInt(parts[2]);
                        } else if (parts.length == 3 && parts[0].equalsIgnoreCase("element") &&
                                   parts[1].equalsIgnoreCase("face")) {
                            int count = Integer.parseInt(parts[2]);
                            faces = new int[count * 3];
                        } else if (parts.length >= 1 && parts[0].equalsIgnoreCase("property")) {
                            if (parts.length == 3 && parts[1].equalsIgnoreCase("float")) {
                                if (parts[2].equalsIgnoreCase("x")) {
                                    xPropertyIndex = propertyCount;
                                } else if (parts[2].equalsIgnoreCase("y")) {
                                    yPropertyIndex = propertyCount;
                                } else if (parts[2].equalsIgnoreCase("z")) {
                                    zPropertyIndex = propertyCount;
                                }
                            }
                            propertyCount++;
                        }
                    }
                    break;
                case VERTICES:
                    String[] parts = line.trim().split("\\s+");
                    vertices.add(new Vector3(.2 * Float.parseFloat(parts[xPropertyIndex]),
                                             .2 * Float.parseFloat(parts[yPropertyIndex]),
                                             .2 * Float.parseFloat(parts[zPropertyIndex])));
                    if (vertices.size() >= vertexCount) {
                        state = State.FACES;
                    }
                    break;
                case FACES:
                    String[] ind = line.trim().split("\\s+");
                    if (ind.length > 0 && ind[0].equals("3")) {
                        faces[faceCount * 3] = Integer.parseInt(ind[1]);
                        faces[faceCount * 3 + 1] = Integer.parseInt(ind[2]);
                        faces[faceCount * 3 + 2] = Integer.parseInt(ind[3]);
                        faceCount++;
                    }
                    break;
                }
            }

            return vertices;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
