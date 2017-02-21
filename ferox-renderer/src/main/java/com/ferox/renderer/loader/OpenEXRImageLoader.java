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
package com.ferox.renderer.loader;

import com.ferox.math.Functions;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 *
 */
public class OpenEXRImageLoader {
    public static void main(String[] args) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream("/Users/mludwig/Desktop/scene-uncompressed.exr"));
        RadianceImageLoader.Image img = readSimple(in);
        System.out.println("Dimensions: " + img.width + " " + img.height);

        BufferedImage bi = new BufferedImage(img.width, img.height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < img.data.length; i += 3) {
            float cR = Math.max(0f, Math.min(img.data[i], 1f));
            float cG = Math.max(0f, Math.min(img.data[i + 1], 1f));
            float cB = Math.max(0f, Math.min(img.data[i + 2], 1f));

            data[i] = (byte) (cB * 255);
            data[i + 1] = (byte) (cG * 255);
            data[i + 2] = (byte) (cR * 255);
        }

        JFrame f = new JFrame();
        JPanel content = new JPanel();
        content.add(new JLabel(new ImageIcon(bi)));
        f.add(content);
        f.setVisible(true);
        f.pack();
    }

    public static RadianceImageLoader.Image readSimple(InputStream in) throws IOException {
        return read(in)[0][0];
    }

    public static RadianceImageLoader.Image[][] read(InputStream in) throws IOException {
        OpenEXRImageLoader loader = new OpenEXRImageLoader();
        ImageFormat format = loader.readPreHeader(in);
        List<Header> headers = loader.readHeaders(in, format);
        List<OffsetTable> offsets = loader.readOffsetTables(in, format, headers);
        Map<Header, RadianceImageLoader.Image[]> images = loader.readChunks(in, format, headers, offsets);

        RadianceImageLoader.Image[][] finalResult = new RadianceImageLoader.Image[headers.size()][];
        for (int i = 0; i < finalResult.length; i++) {
            finalResult[i] = images.get(headers.get(i));
        }
        return finalResult;
    }

    private static enum ImageFormat {
        SCANLINE,
        TILE,
        DEEP,
        MULTIPART,
        MULTIPART_DEEP
    }

    private static enum PartFormat {
        SCANLINE,
        TILE,
        DEEP_SCANLINE,
        DEEP_TILE
    }

    private static enum PixelFormat {
        UINT(4),
        HALF(2),
        FLOAT(4);

        private final int bytes;

        private PixelFormat(int bytes) {
            this.bytes = bytes;
        }

        public int getByteCount() {
            return bytes;
        }
    }

    private static enum LevelMode {
        ONE_LEVEL,
        MIPMAP_LEVELS,
        RIPMAP_LEVELS
    }

    private static enum RoundingMode {
        ROUND_DOWN,
        ROUND_UP
    }

    private static enum Compression {
        NO_COMPRESSION(1),
        RLE_COMPRESSION(1),
        ZIPS_COMPRESSION(1),
        ZIP_COMPRESSION(16),
        PIZ_COMPRESSION(32),
        PXR24_COMPRESSION(16),
        B44_COMPRESSION(32),
        B44A_COMPRESSION(32);

        private final int linesInBuffer;

        private Compression(int linesInBuffer) {
            this.linesInBuffer = linesInBuffer;
        }

        public int getLinesInBuffer() {
            return linesInBuffer;
        }

        public static Compression read(InputStream in) throws IOException {
            return values()[in.read()];
        }
    }

    private static enum LineOrder {
        INCREASING_Y,
        DECREASING_Y,
        RANDOM_Y;

        public static LineOrder read(InputStream in) throws IOException {
            return values()[in.read()];
        }
    }

    private static final int VERSION_MASK = 0xff;
    private static final int TILE_BIT = 1 << 9;
    private static final int LONG_NAME_BIT = 1 << 10;
    private static final int DEEP_DATA_BIT = 1 << 11;
    private static final int MULTIPART_BIT = 1 << 12;

    // 4 LE ints (16 bytes total) in field order
    private static class Box2Int {
        final int minX, minY, maxX, maxY;

        public Box2Int(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        public int width() {
            return maxX - minX + 1;
        }

        public int height() {
            return maxY - minY + 1;
        }

        public static Box2Int read(InputStream in) throws IOException {
            byte[] data = new byte[16];
            readAll(in, data);
            return new Box2Int(bytesToInt(data, 0), bytesToInt(data, 4), bytesToInt(data, 8),
                               bytesToInt(data, 12));
        }
    }

    private static class V2F {
        final float x, y;

        public V2F(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public static V2F read(InputStream in) throws IOException {
            byte[] data = new byte[8];
            readAll(in, data);
            return new V2F(bytesToFloat(data, 0), bytesToFloat(data, 4));
        }
    }

    private static class Channel {
        final String name;
        final PixelFormat type;
        final boolean linear;
        final int xSampling;
        final int ySampling;

        public Channel(String name, PixelFormat type, boolean linear, int xSampling, int ySampling) {
            this.name = name;
            this.type = type;
            this.linear = linear;
            this.xSampling = xSampling;
            this.ySampling = ySampling;
        }

        public static Channel read(InputStream in) throws IOException {
            String name = readNullTerminatedString(in);
            byte[] data = new byte[16];
            OpenEXRImageLoader.readAll(in, data);
            return new Channel(name, PixelFormat.values()[bytesToInt(data, 0)], data[4] != 0,
                               bytesToInt(data, 8), bytesToInt(data, 12));
        }

        public static List<Channel> readAll(InputStream in) throws IOException {
            List<Channel> channels = new ArrayList<>();
            in.mark(1);
            while (in.read() != 0) {
                // reset back to the byte we just read
                in.reset();
                channels.add(read(in));
                in.mark(1);
            }
            // we're done when we reach a null byte, no need to reset the stream
            return channels;
        }
    }

    private static class TileDescription {
        final int xSize, ySize; // unsigned ints
        final LevelMode levelMode;
        final RoundingMode roundingMode;

        public TileDescription(long xSize, long ySize, LevelMode levelMode, RoundingMode roundingMode) {
            this.xSize = (int) xSize;
            this.ySize = (int) ySize;

            if (this.xSize < 0 || this.ySize < 0) {
                throw new IllegalStateException("Tile size is too large to represent in Java");
            }

            this.levelMode = levelMode;
            this.roundingMode = roundingMode;
        }

        public static TileDescription read(InputStream in) throws IOException {
            long xSize = (0xffffffffL & (long) readLEInt(in));
            long ySize = (0xffffffffL & (long) readLEInt(in));
            int mode = in.read(); // levelMode + roundingMode x 16

            RoundingMode roundingMode = RoundingMode.values()[mode / 16];
            LevelMode levelMode = LevelMode.values()[mode % 16];
            return new TileDescription(xSize, ySize, levelMode, roundingMode);
        }

        public int getLevelSize(int min, int max, int l) {
            double distance = max - min + 1;
            int scale = 1 << l;
            int size;
            if (roundingMode == RoundingMode.ROUND_UP) {
                size = (int) Math.ceil(distance / scale);
            } else {
                size = (int) Math.floor(distance / scale);
            }
            return Math.max(size, 1);
        }

        public Box2Int getLevelDataWindow(Box2Int dataWindow, int lx, int ly) {
            int maxX = dataWindow.minX + getLevelSize(dataWindow.minX, dataWindow.maxX, lx);
            int maxY = dataWindow.minY + getLevelSize(dataWindow.minY, dataWindow.maxY, ly);
            return new Box2Int(dataWindow.minX, dataWindow.minY, maxX, maxY);
        }

        public Box2Int getTileDataWindow(Box2Int dataWindow, int dx, int dy, int lx, int ly) {
            int tileMinX = dataWindow.minX + dx * xSize;
            int tileMinY = dataWindow.minY + dy * ySize;
            int tileMaxX = tileMinX + xSize - 1;
            int tileMaxY = tileMinY + ySize - 1;
            Box2Int levelWindow = getLevelDataWindow(dataWindow, lx, ly);
            tileMaxX = Math.min(tileMaxX, levelWindow.maxX);
            tileMaxY = Math.min(tileMaxY, levelWindow.maxY);

            return new Box2Int(tileMinX, tileMinY, tileMaxX, tileMaxY);
        }

        public int roundLog2(int x) {
            if (roundingMode == RoundingMode.ROUND_UP) {
                return Functions.ceilLog2(x);
            } else {
                return Functions.floorLog2(x);
            }
        }

        public int getNumXLevels(Box2Int dataWindow) {
            switch (levelMode) {
            case ONE_LEVEL:
                return 1;
            case MIPMAP_LEVELS:
                return roundLog2(Math.max(dataWindow.width(), dataWindow.height())) + 1;
            case RIPMAP_LEVELS:
                return roundLog2(dataWindow.width()) + 1;
            default:
                throw new IllegalStateException("Shouldn't happen");
            }
        }

        public int getNumYLevels(Box2Int dataWindow) {
            switch (levelMode) {
            case ONE_LEVEL:
                return 1;
            case MIPMAP_LEVELS:
                return roundLog2(Math.max(dataWindow.width(), dataWindow.height())) + 1;
            case RIPMAP_LEVELS:
                return roundLog2(dataWindow.height()) + 1;
            default:
                throw new IllegalStateException("Shouldn't happen");
            }
        }

        public int[] getNumTilesPerLevel(int numLevels, int min, int max, int size) {
            int[] numTiles = new int[numLevels];
            for (int i = 0; i < numLevels; i++) {
                numTiles[i] = (getLevelSize(min, max, i) + size - 1) / size;
            }
            return numTiles;
        }

        public int[][] getNumTiles(Box2Int dataWindow) {
            int numXLevels = getNumXLevels(dataWindow);
            int numYLevels = getNumYLevels(dataWindow);

            int[] xTiles = getNumTilesPerLevel(numXLevels, dataWindow.minX, dataWindow.maxX, xSize);
            int[] yTiles = getNumTilesPerLevel(numYLevels, dataWindow.minY, dataWindow.maxY, ySize);
            return new int[][] { xTiles, yTiles };
        }

        public int getOffsetTableSize(Box2Int dataWindow) {
            int offsetSize = 0;

            int[][] tileCounts = getNumTiles(dataWindow);
            int[] xTiles = tileCounts[0];
            int[] yTiles = tileCounts[1];

            switch (levelMode) {
            case ONE_LEVEL:
            case MIPMAP_LEVELS:
                for (int i = 0; i < xTiles.length; i++) {
                    offsetSize += xTiles[i] * yTiles[i];
                }
                break;
            case RIPMAP_LEVELS:
                for (int i = 0; i < xTiles.length; i++) {
                    for (int j = 0; j < yTiles.length; j++) {
                        offsetSize += xTiles[i] * yTiles[j];
                    }
                }
                break;
            }

            return offsetSize;
        }
    }

    private static class Attribute {
        final String name;
        final String type;
        final int size;
        final Object value; // this will be of particular classes if type is known or null if type was unknown

        public Attribute(String name, String type, int size, Object value) {
            this.name = name;
            this.type = type;
            this.size = size;
            this.value = value;
        }

        public static Map<String, Attribute> readAll(InputStream in) throws IOException {
            Map<String, Attribute> attrs = new HashMap<>();
            in.mark(1);
            while (in.read() != 0) {
                // not the end of the attribute list, so
                in.reset();
                Attribute a = read(in);
                attrs.put(a.name, a);
                in.mark(1);
            }
            // if we hit the 0 byte then we're done and don't need to reset to the mark
            return attrs;
        }

        public static Attribute read(InputStream in) throws IOException {
            String name = readNullTerminatedString(in);
            String type = readNullTerminatedString(in).toLowerCase();
            int size = readLEInt(in);

            Object value;
            switch (type) {
            case "box2i":
                if (size != 16) {
                    throw new IOException("Unexpected size (" + size + ") of type (" + type +
                                          ") for attribute (" + name + ")");
                }

                value = Box2Int.read(in);
                break;
            case "compression":
                if (size != 1) {
                    throw new IOException("Unexpected size (" + size + ") of type (" + type +
                                          ") for attribute (" + name + ")");
                }

                value = Compression.read(in);
                break;
            case "chlist":
                // don't bother checking size for null terminated attribute
                value = Channel.readAll(in);
                break;
            case "lineorder":
                if (size != 1) {
                    throw new IOException("Unexpected size (" + size + ") of type (" + type +
                                          ") for attribute (" + name + ")");
                }

                value = LineOrder.read(in);
                break;
            case "tiledesc":
                if (size != 9) {
                    throw new IOException("Unexpected size (" + size + ") of type (" + type +
                                          ") for attribute (" + name + ")");
                }
                value = TileDescription.read(in);
                break;
            case "string":
                value = readString(in, size);
                break;
            case "v2f":
                if (size != 8) {
                    throw new IOException("Unexpected size (" + size + ") of type (" + type +
                                          ") for attribute (" + name + ")");
                }

                value = V2F.read(in);
                break;
            case "float":
                if (size != 4) {
                    throw new IOException("Unexpected size (" + size + ") of type (" + type +
                                          ") for attribute (" + name + ")");
                }

                byte[] f = new byte[4];
                OpenEXRImageLoader.readAll(in, f);
                value = bytesToFloat(f, 0);
                break;
            case "int":
                if (size != 4) {
                    throw new IOException("Unexpected size (" + size + ") of type (" + type +
                                          ") for attribute (" + name + ")");
                }
                value = readLEInt(in);
                break;
            default:
                int remaining = size;
                while (remaining > 0) {
                    remaining -= in.skip(remaining);
                }
                value = null;
                break;
            }

            return new Attribute(name, type, size, value);
        }
    }

    private static class Header {
        final List<Channel> channels;
        final Compression compression; // single byte
        final Box2Int dataWindow;
        final Box2Int displayWindow;
        final LineOrder lineOrder;
        final float pixelAspectRatio;
        final V2F screenWindowCenter;
        final float screenWindowWidth;

        final TileDescription tiles; // only used for tiled images

        final String view; // only used in multipart files for stereo

        // only used in multipart and deep files
        final String name;
        final PartFormat type; // must match high-level image format
        final int version; // should be 1
        final int chunkCount;

        final int maxSamplesPerPixel; // only used for deep images

        final Map<String, Attribute> allAttrs;

        public Header(Map<String, Attribute> allAttrs) {
            this.allAttrs = Collections.unmodifiableMap(new HashMap<>(allAttrs));

            // required attributes
            channels = requiredAttr("channels", "chlist", List.class, allAttrs);
            compression = requiredAttr("compression", "compression", Compression.class, allAttrs);
            dataWindow = requiredAttr("dataWindow", "box2i", Box2Int.class, allAttrs);
            displayWindow = requiredAttr("displayWindow", "box2i", Box2Int.class, allAttrs);
            lineOrder = requiredAttr("lineOrder", "lineorder", LineOrder.class, allAttrs);
            pixelAspectRatio = requiredAttr("pixelAspectRatio", "float", Float.class, allAttrs);
            screenWindowCenter = requiredAttr("screenWindowCenter", "v2f", V2F.class, allAttrs);
            screenWindowWidth = requiredAttr("screenWindowWidth", "float", Float.class, allAttrs);

            // optional attributes
            tiles = optionalAttr("tiles", "tiledesc", TileDescription.class, allAttrs);
            view = optionalAttr("view", "string", String.class, allAttrs);
            name = optionalAttr("name", "string", String.class, allAttrs);
            version = optionalAttr("version", "int", Integer.class, allAttrs);
            chunkCount = optionalAttr("chunkCount", "int", Integer.class, allAttrs);
            maxSamplesPerPixel = optionalAttr("maxSamplesPerPixel", "int", Integer.class, allAttrs);

            String typeName = optionalAttr("type", "string", String.class, allAttrs);
            if (typeName == null) {
                type = null;
            } else {
                switch (typeName) {
                case "scanlineimage":
                    type = PartFormat.SCANLINE;
                    break;
                case "tiledimage":
                    type = PartFormat.TILE;
                    break;
                case "deepscanline":
                    type = PartFormat.DEEP_SCANLINE;
                    break;
                case "deeptile":
                    type = PartFormat.DEEP_TILE;
                    break;
                default:
                    throw new IllegalStateException("Unsupported type value: " + typeName);
                }
            }
        }

        private static <T> T requiredAttr(String name, String type, Class<T> cls,
                                          Map<String, Attribute> attrs) {
            if (!attrs.containsKey(name) || !attrs.get(name).type.equals(type)) {
                throw new IllegalStateException("File missing required attribute (" + name + ") with type " +
                                                type);
            }
            return cls.cast(attrs.get(name).value);
        }

        private static <T> T optionalAttr(String name, String type, Class<T> cls,
                                          Map<String, Attribute> attrs) {
            if (!attrs.containsKey(name) || !attrs.get(name).type.equals(type)) {
                // make sure we don't get NPEs for attribute types that turn into primitives in the header
                if (cls.equals(Float.class)) {
                    return cls.cast(0f);
                } else if (cls.equals(Integer.class)) {
                    return cls.cast(0);
                } else {
                    return null;
                }
            }
            return cls.cast(attrs.get(name).value);
        }

        public static Header read(InputStream in) throws IOException {
            Map<String, Attribute> attrs = Attribute.readAll(in);
            return new Header(attrs);
        }

        public static List<Header> readAll(InputStream in) throws IOException {
            List<Header> headers = new ArrayList<>();
            in.mark(1);
            while (in.read() != 0) {
                in.reset();
                headers.add(read(in));
                in.mark(1);
            }
            return headers;
        }
    }

    private static interface OffsetTable {
        public int getTotalOffsets();
    }

    private static class ScanLineOffsetTable implements OffsetTable {
        // line buffer offsets ordered by increasing y
        final long[] offsets;

        public ScanLineOffsetTable(long[] offsets) {
            this.offsets = offsets;
        }

        @Override
        public int getTotalOffsets() {
            return offsets.length;
        }

        public static ScanLineOffsetTable read(Header header, InputStream in) throws IOException {
            int linesInBuffer = header.compression.getLinesInBuffer();
            int size = (header.dataWindow.maxY - header.dataWindow.minY) / linesInBuffer + 1;

            // confirm chunkCount is correct
            if (header.type != null) {
                // openexr 2.0 format, so type must be specified
                if (header.chunkCount != size) {
                    throw new IOException("Calculated scanline offset table doesn't match reported chunk count: " +
                                          size + " vs " + header.chunkCount);
                }
            }

            byte[] data = new byte[size * 8];
            readAll(in, data);
            long[] offsets = new long[size];
            for (int i = 0; i < size; i++) {
                offsets[i] = bytesToLong(data, i * 8);
            }

            return new ScanLineOffsetTable(offsets);
        }
    }

    private static class TileOffsetTable implements OffsetTable {
        final int totalTiles;

        // offsets for each tile/level in the image
        final LevelMode mode;
        final int[] xLevels;
        final int[] yLevels;

        final long[][][] offsets; // 3-dimensional (lx * ly, dy, dx)

        public TileOffsetTable(LevelMode mode, int[] xLevels, int[] yLevels, long[][][] offsets) {
            this.mode = mode;
            this.offsets = offsets;
            this.xLevels = xLevels;
            this.yLevels = yLevels;

            int total = 0;
            for (int i = 0; i < offsets.length; i++) {
                for (int j = 0; j < offsets.length; j++) {
                    total += offsets[i][j].length;
                }
            }
            totalTiles = total;
        }

        @Override
        public int getTotalOffsets() {
            return totalTiles;
        }

        public long getOffset(int dx, int dy, int l) {
            return getOffset(dx, dy, l, l);
        }

        public long getOffset(int dx, int dy, int lx, int ly) {
            switch (mode) {
            case ONE_LEVEL:
                return offsets[0][dy][dx];
            case MIPMAP_LEVELS:
                return offsets[lx][dy][dx];
            case RIPMAP_LEVELS:
                return offsets[lx + ly * xLevels.length][dy][dx];
            default:
                return 0;
            }
        }

        public static TileOffsetTable read(Header header, InputStream in) throws IOException {
            int size = header.tiles.getOffsetTableSize(header.dataWindow);
            // confirm chunkCount is correct
            if (header.type != null) {
                // openexr 2.0 format, so type must be specified
                if (header.chunkCount != size) {
                    throw new IOException("Calculated scanline offset table doesn't match reported chunk count: " +
                                          size + " vs " + header.chunkCount);
                }
            }
            int[][] levelCounts = header.tiles.getNumTiles(header.dataWindow);
            int numX = levelCounts[0].length;
            int numY = levelCounts[1].length;

            byte[] data = new byte[size * 8];
            readAll(in, data);

            int offset = 0;
            long[][][] offsets;
            switch (header.tiles.levelMode) {
            case ONE_LEVEL:
            case MIPMAP_LEVELS:
                offsets = new long[numX][][];
                for (int l = 0; l < offsets.length; l++) {
                    offsets[l] = new long[levelCounts[1][l]][];
                    for (int dy = 0; dy < offsets[l].length; dy++) {
                        offsets[l][dy] = new long[levelCounts[0][l]];
                        for (int dx = 0; dx < offsets[l][dy].length; dx++) {
                            offsets[l][dy][dx] = bytesToLong(data, offset);
                            offset += 8;
                        }
                    }
                }
                break;
            case RIPMAP_LEVELS:
                offsets = new long[numX * numY][][];
                for (int ly = 0; ly < numY; ly++) {
                    for (int lx = 0; lx < numX; lx++) {
                        int l = ly * numX + lx;
                        offsets[l] = new long[levelCounts[1][l]][];
                        for (int dy = 0; dy < offsets[l].length; dy++) {
                            offsets[l][dy] = new long[levelCounts[0][l]];
                            for (int dx = 0; dx < offsets[l][dy].length; dx++) {
                                offsets[l][dy][dx] = bytesToLong(data, offset);
                                offset += 8;
                            }
                        }
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unknown level mode: " + header.tiles.levelMode);
            }

            return new TileOffsetTable(header.tiles.levelMode, levelCounts[0], levelCounts[1], offsets);
        }
    }

    private ImageFormat readPreHeader(InputStream in) throws IOException {
        int magicNumber = readLEInt(in);
        if (magicNumber != 20000630) {
            throw new IOException("File is not an OpenEXR image");
        }

        int version = readLEInt(in);
        if ((version & VERSION_MASK) > 2) {
            throw new IOException("OpenEXR version " + (version & VERSION_MASK) + " not supported");
        }

        // long names can be ignored, java doesn't require pre-allocation
        //        useLongNames = (version & LONG_NAME_BIT) != 0;
        boolean deep = (version & DEEP_DATA_BIT) != 0;
        boolean tiled = (version & TILE_BIT) != 0;
        boolean multipart = (version & MULTIPART_BIT) != 0;

        if (tiled) {
            if (deep || multipart) {
                throw new IOException("Single tiled image cannot be flagged as multipart or deep");
            }
            return ImageFormat.TILE;
        } else {
            if (!deep && !multipart) {
                return ImageFormat.SCANLINE;
            } else if (!deep) {
                return ImageFormat.MULTIPART;
            } else if (!multipart) {
                return ImageFormat.DEEP;
            } else {
                return ImageFormat.MULTIPART_DEEP;
            }
        }
    }

    private List<Header> readHeaders(InputStream in, ImageFormat format) throws IOException {
        List<Header> headers;
        if (format == ImageFormat.SCANLINE || format == ImageFormat.TILE || format == ImageFormat.DEEP) {
            // single-part file, don't look for a null byte at the end just read one header
            headers = Collections.singletonList(Header.read(in));
        } else {
            // read all headers
            headers = Header.readAll(in);
        }

        // validate the headers
        Set<String> headerNames = new HashSet<>();
        for (Header h : headers) {
            if (format == ImageFormat.TILE && h.tiles == null) {
                throw new IOException("Single-part tile image requires a tile description");
            }

            if (format == ImageFormat.DEEP || format == ImageFormat.MULTIPART ||
                format == ImageFormat.MULTIPART_DEEP) {
                // verify required 2.0 attributes are present
                if (h.name == null) {
                    throw new IOException("Header name is required for multipart and deep images");
                }
                if (!headerNames.add(h.name)) {
                    throw new IOException("Header name is not unique in image: " + h.name);
                }
                if (h.type == null) {
                    throw new IOException("Part type is required for " + h.name);
                }
                if (h.type == PartFormat.TILE || h.type == PartFormat.DEEP_TILE) {
                    // make sure tile description is specified in this case as well
                    if (h.tiles == null) {
                        throw new IOException("Multi-part image requires a tile description for " + h.name);
                    }
                }
            }

            if (format == ImageFormat.DEEP || format == ImageFormat.MULTIPART_DEEP) {
                // deep-data only validation
                if (h.maxSamplesPerPixel <= 0) {
                    throw new IOException("Max samples per pixel is not specified for deep data image in " +
                                          h.name);
                }
                if (h.version != 1) {
                    throw new IOException("Unsupported deep-pixel version number: " + h.version +
                                          " in part " + h.name);
                }
                if (h.type != PartFormat.DEEP_TILE && h.type != PartFormat.DEEP_SCANLINE) {
                    throw new IOException("Unsupported part format for deep-data image: " + h.type +
                                          " in part " + h.name);
                }
            }
        }

        return headers;
    }

    private List<OffsetTable> readOffsetTables(InputStream in, ImageFormat format, List<Header> headers)
            throws IOException {
        List<OffsetTable> offsets = new ArrayList<>(headers.size());

        if (format == ImageFormat.SCANLINE) {
            if (headers.size() != 1) {
                throw new IllegalStateException("Unexpected header count for format SCANLINE: " +
                                                headers.size());
            }
            offsets.add(ScanLineOffsetTable.read(headers.get(0), in));
        } else if (format == ImageFormat.TILE) {
            if (headers.size() != 1) {
                throw new IllegalStateException("Unexpected header count for format TILE: " + headers.size());
            }
            offsets.add(TileOffsetTable.read(headers.get(0), in));
        } else {
            // each header will have specified the part format (even if it's a single deep
            for (Header h : headers) {
                if (h.type == PartFormat.SCANLINE || h.type == PartFormat.DEEP_SCANLINE) {
                    offsets.add(ScanLineOffsetTable.read(h, in));
                } else {
                    offsets.add(TileOffsetTable.read(h, in));
                }
            }
        }

        return offsets;
    }

    private void blitUncompressedData(byte[] data, PixelFormat dataFormat, Box2Int dataWindow,
                                      RadianceImageLoader.Image image, Box2Int imageWindow, int numChannels) {
        // this currently assumes the only channels are B, G, R in that order and all three have the same type
        int channelLengthInBytes = dataWindow.width() * dataFormat.getByteCount();
        int scanlineLengthInBytes = channelLengthInBytes * numChannels;

        if (numChannels == 3) {
            // assume BGR
            for (int y = dataWindow.minY; y <= dataWindow.maxY; y++) {
                // scanlines are ordered top to bottom
                // for each scanline the channels are presented in alphabetical order for that scanline

                for (int x = dataWindow.minX; x <= dataWindow.maxX; x++) {
                    int blueChannelOffset = (y - dataWindow.minY) * scanlineLengthInBytes +
                                            (x - dataWindow.minX) * dataFormat.getByteCount();
                    int greenChannelOffset = blueChannelOffset + channelLengthInBytes;
                    int redChannelOffset = greenChannelOffset + channelLengthInBytes;

                    float red, green, blue;
                    switch (dataFormat) {
                    case FLOAT:
                        red = bytesToFloat(data, redChannelOffset);
                        green = bytesToFloat(data, greenChannelOffset);
                        blue = bytesToFloat(data, blueChannelOffset);
                        break;
                    case HALF:
                        red = bytesToHalf(data, redChannelOffset);
                        green = bytesToHalf(data, greenChannelOffset);
                        blue = bytesToHalf(data, blueChannelOffset);
                        break;
                    case UINT:
                        red = bytesToInt(data, redChannelOffset);
                        green = bytesToInt(data, greenChannelOffset);
                        blue = bytesToInt(data, blueChannelOffset);
                        break;
                    default:
                        throw new IllegalStateException("Unknown pixel format: " + dataFormat);
                    }

// FIXME reverse y into the image
                    int imageOffset = ((y - imageWindow.minY) * image.width + (x - imageWindow.minX)) * 3;
                    image.data[imageOffset] = red;
                    image.data[imageOffset + 1] = green;
                    image.data[imageOffset + 2] = blue;
                }
            }
        } else if (numChannels == 4) {
            // assume ABGR
            for (int y = dataWindow.minY; y <= dataWindow.maxY; y++) {
                // scanlines are ordered top to bottom
                // for each scanline the channels are presented in alphabetical order for that scanline

                for (int x = dataWindow.minX; x <= dataWindow.maxX; x++) {
                    int alphaChannelOffset = (y - dataWindow.minY) * scanlineLengthInBytes +
                                             (x - dataWindow.minX) * dataFormat.getByteCount();
                    int blueChannelOffset = alphaChannelOffset + channelLengthInBytes;
                    int greenChannelOffset = blueChannelOffset + channelLengthInBytes;
                    int redChannelOffset = greenChannelOffset + channelLengthInBytes;

                    float red, green, blue, alpha;
                    switch (dataFormat) {
                    case FLOAT:
                        red = bytesToFloat(data, redChannelOffset);
                        green = bytesToFloat(data, greenChannelOffset);
                        blue = bytesToFloat(data, blueChannelOffset);
                        alpha = bytesToFloat(data, alphaChannelOffset);
                        break;
                    case HALF:
                        red = bytesToHalf(data, redChannelOffset);
                        green = bytesToHalf(data, greenChannelOffset);
                        blue = bytesToHalf(data, blueChannelOffset);
                        alpha = bytesToHalf(data, alphaChannelOffset);
                        break;
                    case UINT:
                        red = bytesToInt(data, redChannelOffset);
                        green = bytesToInt(data, greenChannelOffset);
                        blue = bytesToInt(data, blueChannelOffset);
                        alpha = bytesToInt(data, alphaChannelOffset);
                        break;
                    default:
                        throw new IllegalStateException("Unknown pixel format: " + dataFormat);
                    }

                    int glY = imageWindow.height() - (y - imageWindow.minY) - 1;
                    int imageOffset = (glY * image.width + (x - imageWindow.minX)) * 4;
                    image.data[imageOffset] = red;
                    image.data[imageOffset + 1] = green;
                    image.data[imageOffset + 2] = blue;
                    image.data[imageOffset + 3] = alpha;
                }
            }
        }
    }

    private byte[] unzipBlock(byte[] compressed, Box2Int blockWindow, int bytesPerPixel) {
        byte[] uncompressed = new byte[blockWindow.width() * blockWindow.height() * bytesPerPixel];
        Inflater decompressor = new Inflater();
        decompressor.setInput(compressed);
        int read = 0;
        while (read < uncompressed.length) {
            try {
                read += decompressor.inflate(uncompressed, read, uncompressed.length - read);
            } catch (DataFormatException e) {
                throw new IllegalStateException("Expected ZIP formatted scanline block", e);
            }
        }
        decompressor.end();

        // predictor (FIXME I don't know what this means, but it's from ImfZip.cpp
        for (int i = 1; i < uncompressed.length; i++) {
            int d = (0xff & uncompressed[i - 1]) + (0xff & uncompressed[i]) - 128;
            uncompressed[i] = (byte) d;
        }

        // reorder the pixel data
        byte[] corrected = new byte[uncompressed.length];
        int i1 = 0;
        int i2 = (uncompressed.length + 1) / 2;
        int i = 0;
        while (true) {
            if (i < corrected.length) {
                corrected[i++] = uncompressed[i1++];
            } else {
                break;
            }
            if (i < corrected.length) {
                corrected[i++] = uncompressed[i2++];
            } else {
                break;
            }
        }

        return corrected;
    }

    private void readScanlineChunk(InputStream in, Header h, RadianceImageLoader.Image[] image)
            throws IOException {
        int chunkY = readLEInt(in);
        int dataSize = readLEInt(in);

        byte[] rawData = new byte[dataSize];
        readAll(in, rawData);

        int linesInChunk = h.compression.getLinesInBuffer();
        if (chunkY + linesInChunk > h.dataWindow.maxY) {
            linesInChunk = h.dataWindow.maxY - chunkY + 1;
        }

        Box2Int dataWindow = new Box2Int(h.dataWindow.minX, chunkY, h.dataWindow.maxX,
                                         chunkY + linesInChunk - 1);
        PixelFormat type = h.channels.get(0).type;
        int numChannels = h.channels.size();

        switch (h.compression) {
        case NO_COMPRESSION:
            // the byte array is as expected
            blitUncompressedData(rawData, type, dataWindow, image[0], h.dataWindow, numChannels);
            break;
        case ZIP_COMPRESSION:
        case ZIPS_COMPRESSION:
            // these both use the ZIP compression algorithm, they just differ in line block height
            byte[] uncompressed = unzipBlock(rawData, dataWindow, numChannels * type.getByteCount());
            blitUncompressedData(uncompressed, type, dataWindow, image[0], h.dataWindow, numChannels);
            break;
        case PIZ_COMPRESSION:
        case RLE_COMPRESSION:
            // FIXME I have intentions to implement RLE and PIZ
        case PXR24_COMPRESSION:
        case B44_COMPRESSION:
        case B44A_COMPRESSION:
            throw new IllegalStateException("Compression mode is not supported: " + h.compression);
        }
    }

    private void readTileChunk(InputStream in, Header h, RadianceImageLoader.Image[] image)
            throws IOException {
        int dx = readLEInt(in);
        int dy = readLEInt(in);
        int lx = readLEInt(in);
        int ly = readLEInt(in);

        // FIXME I've assumed that getTileDataWindow takes into account the boundaries where it might be smaller
        int dataSize = readLEInt(in);
        byte[] rawData = new byte[dataSize];
        readAll(in, rawData);

        Box2Int dataWindow = h.tiles.getTileDataWindow(h.dataWindow, dx, dy, lx, ly);
        int level = ly * h.tiles.getNumXLevels(h.dataWindow) + lx;
        Box2Int levelWindow = h.tiles.getLevelDataWindow(h.dataWindow, lx, ly);

        PixelFormat type = h.channels.get(0).type;
        int numChannels = h.channels.size();

        switch (h.compression) {
        case NO_COMPRESSION:
            // the byte array is as expected
            blitUncompressedData(rawData, h.channels.get(0).type, dataWindow, image[level], levelWindow,
                                 numChannels);
            break;
        case ZIP_COMPRESSION:
        case ZIPS_COMPRESSION:
            // these both use the ZIP compression algorithm, they just differ in line block height
            byte[] uncompressed = unzipBlock(rawData, dataWindow, numChannels * type.getByteCount());
            blitUncompressedData(uncompressed, type, dataWindow, image[level], levelWindow, numChannels);
            break;
        case PIZ_COMPRESSION:
        case RLE_COMPRESSION:
            // FIXME I have intentions to implement RLE and PIZ
        case PXR24_COMPRESSION:
        case B44_COMPRESSION:
        case B44A_COMPRESSION:
            throw new IllegalStateException("Compression mode is not supported: " + h.compression);
        }
    }

    private RadianceImageLoader.Image[] createLevelsForScanline(Header h) {
        int width = h.dataWindow.width();
        int height = h.dataWindow.height();
        RadianceImageLoader.Image scanline = new RadianceImageLoader.Image(width, height, new float[width *
                                                                                                    height *
                                                                                                    h.channels
                                                                                                            .size()]);
        return new RadianceImageLoader.Image[] { scanline };
    }

    private RadianceImageLoader.Image[] createLevelsForTileImage(Header h) {
        switch (h.tiles.levelMode) {
        case ONE_LEVEL:
        case MIPMAP_LEVELS: {
            int numLevels = h.tiles.getNumXLevels(h.dataWindow);
            RadianceImageLoader.Image[] levels = new RadianceImageLoader.Image[numLevels];
            for (int l = 0; l < numLevels; l++) {
                Box2Int levelWindow = h.tiles.getLevelDataWindow(h.dataWindow, l, l);
                int width = levelWindow.width();
                int height = levelWindow.height();
                levels[l] = new RadianceImageLoader.Image(width, height,
                                                          new float[width * height * h.channels.size()]);
            }
            return levels;
        }
        case RIPMAP_LEVELS: {
            int numXLevels = h.tiles.getNumXLevels(h.dataWindow);
            int numYLevels = h.tiles.getNumYLevels(h.dataWindow);
            RadianceImageLoader.Image[] levels = new RadianceImageLoader.Image[numXLevels * numYLevels];
            for (int ly = 0; ly < numYLevels; ly++) {
                for (int lx = 0; lx < numXLevels; lx++) {
                    Box2Int levelWindow = h.tiles.getLevelDataWindow(h.dataWindow, lx, ly);
                    int width = levelWindow.width();
                    int height = levelWindow.height();
                    levels[ly * numXLevels + lx] = new RadianceImageLoader.Image(width, height,
                                                                                 new float[width *
                                                                                           height *
                                                                                           h.channels
                                                                                                   .size()]);
                }
            }
            return levels;
        }
        default:
            throw new IllegalStateException("Bad level mode");
        }
    }

    private Map<Header, RadianceImageLoader.Image[]> readChunks(InputStream in, ImageFormat format,
                                                                List<Header> headers,
                                                                List<OffsetTable> offsets)
            throws IOException {
        boolean multiPart = format == ImageFormat.MULTIPART_DEEP || format == ImageFormat.MULTIPART;
        Map<Header, RadianceImageLoader.Image[]> chunks = new HashMap<>();
        for (Header h : headers) {
            for (Channel c : h.channels) {
                if (c.xSampling != 1 || c.ySampling != 1) {
                    throw new IOException("Only unit x and y sampling is supported, not: " + c.xSampling +
                                          " x " + c.ySampling);
                }
            }
            if (h.channels.size() == 3) {
                if (!h.channels.get(0).name.equalsIgnoreCase("b")) {
                    throw new IOException("Expected B channel as first, other channel layouts are unsupported");
                }
                if (!h.channels.get(1).name.equalsIgnoreCase("g")) {
                    throw new IOException("Expected G channel as second, other channel layouts are unsupported");
                }
                if (!h.channels.get(2).name.equalsIgnoreCase("r")) {
                    throw new IOException("Expected R channel as third, other channel layouts are unsupported");
                }
            } else if (h.channels.size() == 4) {
                if (!h.channels.get(0).name.equalsIgnoreCase("a")) {
                    throw new IOException("Expected A channel as first, other channel layouts are unsupported");
                }
                if (!h.channels.get(1).name.equalsIgnoreCase("b")) {
                    throw new IOException("Expected B channel as first, other channel layouts are unsupported");
                }
                if (!h.channels.get(2).name.equalsIgnoreCase("g")) {
                    throw new IOException("Expected G channel as second, other channel layouts are unsupported");
                }
                if (!h.channels.get(3).name.equalsIgnoreCase("r")) {
                    throw new IOException("Expected R channel as third, other channel layouts are unsupported");
                }
            } else {
                throw new IOException("Only 3 and 4 channels are supported currently, not: " +
                                      h.channels.size());
            }

            if (multiPart) {
                // since it's multipart the header format is specified
                switch (h.type) {
                case TILE:
                    chunks.put(h, createLevelsForTileImage(h));
                    break;
                case SCANLINE: {
                    // there is only one image in the level set
                    chunks.put(h, createLevelsForScanline(h));
                    break;
                }
                case DEEP_SCANLINE:
                case DEEP_TILE:
                    throw new UnsupportedOperationException("Deep-data images are not supported");
                }
            } else {
                if (h.type == PartFormat.DEEP_SCANLINE || h.type == PartFormat.DEEP_TILE) {
                    throw new UnsupportedOperationException("Deep-data images are not supported");
                }
                if (format == ImageFormat.TILE) {
                    chunks.put(h, createLevelsForTileImage(h));
                } else {
                    chunks.put(h, createLevelsForScanline(h));
                }
            }
        }

        // read chunks, blitting them into their appropriate frame buffers
        if (multiPart) {
            // must read part number first to get appropriate header
            int blocks = 0;
            for (OffsetTable t : offsets) {
                blocks += t.getTotalOffsets();
            }
            for (int i = 0; i < blocks; i++) {
                int part = readLEInt(in);
                Header h = headers.get(part);
                if (h.type == PartFormat.TILE) {
                    readTileChunk(in, h, chunks.get(h));
                } else if (h.type == PartFormat.SCANLINE) {
                    readScanlineChunk(in, h, chunks.get(h));
                } else {
                    throw new IOException("Unsupported part type: " + h.type);
                }
            }
        } else {
            Header h = headers.get(0);
            int blocks = offsets.get(0).getTotalOffsets();
            if (format == ImageFormat.TILE) {
                for (int i = 0; i < blocks; i++) {
                    readTileChunk(in, h, chunks.get(h));
                }
            } else {
                for (int i = 0; i < blocks; i++) {
                    readScanlineChunk(in, h, chunks.get(h));
                }
            }
        }

        return chunks;
    }

    // as bytesToInt, but for shorts (converts 2 bytes instead of 4)
    // assuming little endian
    private static short bytesToShort(byte[] in, int offset) {
        return (short) ((in[offset] & 0xff) | ((in[offset + 1] & 0xff) << 8));
    }

    // as bytesToInt, but for floats (expects 4 bytes from offset)
    private static float bytesToFloat(byte[] in, int offset) {
        return Float.intBitsToFloat(bytesToInt(in, offset));
    }

    // as bytesToInt, but for halfs (expects 2 bytes from offset)
    private static float bytesToHalf(byte[] in, int offset) {
        short bits = bytesToShort(in, offset);
        return HalfFloat.halfToFloat(bits);
    }

    // convert 4 bytes starting at offset into an integer, assuming
    // the bytes are ordered little endian.
    private static int bytesToInt(byte[] in, int offset) {
        return ((in[offset] & 0xff) | ((in[offset + 1] & 0xff) << 8) |
                ((in[offset + 2] & 0xff) << 16) | ((in[offset + 3] & 0xff) << 24));
    }

    // convert 8 bytes starting at offset into a long, assuming the bytes are ordered little endian
    private static long bytesToLong(byte[] in, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((in[offset + i] & 0xffL) << (i * 8));
        }
        return value;
    }

    // read an integer represented in little endian from the given input stream
    private static int readLEInt(InputStream in) throws IOException {
        byte[] b = new byte[4];
        readAll(in, b);
        return bytesToInt(b, 0);
    }

    private static String readString(InputStream in) throws IOException {
        int length = readLEInt(in);
        return readString(in, length);
    }

    private static String readString(InputStream in, int length) throws IOException {
        byte[] b = new byte[length];
        readAll(in, b);
        return new String(b);
    }

    private static String readNullTerminatedString(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int read;
        while ((read = in.read()) != 0) {
            sb.append((char) read);
        }
        return sb.toString();
    }

    private static void readAll(InputStream in, byte[] array) throws IOException {
        readAll(in, array, 0);
    }

    private static void readAll(InputStream in, byte[] array, int offset) throws IOException {
        readAll(in, array, offset, array.length - offset);
    }

    // read bytes from the given stream until the array has filled with length
    // fails if the end-of-stream happens before length has been read
    private static void readAll(InputStream in, byte[] array, int offset, int length) throws IOException {
        int remaining = length;
        int read;
        while (remaining > 0) {
            read = in.read(array, offset, remaining);
            if (read < 0) {
                throw new IOException("Unexpected end of stream");
            }
            offset += read;
            remaining -= read;
        }
    }
}
