package com.ferox.resource;

import java.util.EnumMap;
import java.util.Scanner;

public class GlslShader extends Resource {
    public static enum Version {
        // FIXME are there more?
        // should I really limit it to an enum, I could just have
        // it be an integer that is parsed that is probably safer
        V1_20, V1_30, V1_40, V1_50, V3_30, V4_00
    }

    public static enum AttributeType {
        FLOAT(1, 1), FLOAT_VEC2(2, 1), FLOAT_VEC3(3, 1), FLOAT_VEC4(4, 1), FLOAT_MAT2(2,
                                                                                      2),
        FLOAT_MAT3(3, 3), FLOAT_MAT4(4, 4), UNSUPPORTED(0, 0);

        private final int row;
        private final int col;

        private AttributeType(int r, int c) {
            row = r;
            col = c;
        }

        public int getRowCount() {
            return row;
        }

        public int getColumnCount() {
            return col;
        }
    }

    public static enum ShaderType {
        GEOMETRY, VERTEX, FRAGMENT
    }

    private Version version;
    private EnumMap<ShaderType, String> shaders;

    public GlslShader() {
        version = Version.V1_20;
        shaders = new EnumMap<ShaderType, String>(ShaderType.class);
    }

    public synchronized String getShader(ShaderType type) {
        if (type == null) {
            throw new NullPointerException("ShaderType cannot be null");
        }
        return shaders.get(type);
    }

    public synchronized void setShader(ShaderType type, String code) {
        if (type == null) {
            throw new NullPointerException("ShaderType cannot be null");
        }

        if (code == null) {
            shaders.remove(type);
            return;
        }

        code = code.trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Shader code cannot be empty");
        }
        version = updateVersion(type, code);
        shaders.put(type, code);
    }

    public synchronized void removeShader(ShaderType type) {
        setShader(type, null);
    }

    public synchronized Version getVersion() {
        return version;
    }

    private Version updateVersion(ShaderType type, String codeSrc) {
        EnumMap<ShaderType, String> clone = new EnumMap<ShaderType, String>(shaders);
        clone.put(type, codeSrc);

        Version detectedVersion = null;
        for (String code : clone.values()) {
            if (code.startsWith("#version")) {
                Scanner s = new Scanner(code.substring(9));
                int version = s.nextInt();
                Version v = null;

                switch (version) {
                case 120:
                    v = Version.V1_20;
                    break;
                case 130:
                    v = Version.V1_30;
                    break;
                case 140:
                    v = Version.V1_40;
                    break;
                case 150:
                    v = Version.V1_50;
                    break;
                case 330:
                    v = Version.V3_30;
                    break;
                case 400:
                    v = Version.V4_00;
                    break;
                }

                if (detectedVersion == null) {
                    detectedVersion = v;
                } else if (detectedVersion != v && v != null) {
                    throw new IllegalArgumentException("Shader version mismatch");
                }
            }
        }

        return (detectedVersion == null ? Version.V1_20 : detectedVersion);
    }
}
