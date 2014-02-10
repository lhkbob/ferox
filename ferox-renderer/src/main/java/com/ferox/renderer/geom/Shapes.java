package com.ferox.renderer.geom;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.renderer.Framework;
import com.ferox.renderer.loader.GeometryLoader;

import java.io.IOException;

/**
 *
 */
public final class Shapes {
    private Shapes() {
    }

    /**
     * Construct a box centered on its origin, with the given side length. So, Box(1f) creates a unit cube.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param side      The side length of the created cube
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if side is negative
     */
    public static Geometry createBox(Framework framework, double side) {
        return createBox(framework, side, side, side);
    }

    /**
     * Construct a box centered on its origin, with the given side lengths along each local axis.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param xExtent   The side length along the x axis
     * @param yExtent   The side length along the y axis
     * @param zExtent   The side length along the z axis
     *
     * @return The new geometry
     *
     * @throws NullPointerException         if mode is null
     * @throws IllegalMonitorStateException if any dimension is negative
     */
    public static Geometry createBox(Framework framework, double xExtent, double yExtent, double zExtent) {
        return createBox(framework, new Vector3(-xExtent / 2, -yExtent / 2, -zExtent / 2),
                         new Vector3(xExtent / 2, yExtent / 2, zExtent / 2));
    }

    /**
     * Construct a new Box with the given minimum and maximum points. These points are opposite corners of the
     * box.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param min       Minimum corner of the box
     * @param max       Maximum corner of the box
     *
     * @return The new geometry
     *
     * @throws NullPointerException     if min, max or mode are null
     * @throws IllegalArgumentException if min has any coordinate less than the corresponding coordinate of
     *                                  max
     */
    public static Geometry createBox(Framework framework, @Const Vector3 min, @Const Vector3 max) {
        return new BoxImpl(framework, min, max);
    }

    /**
     * Create a new Cylinder with the given radius and height, and a resolution of 8. Its axis will be the
     * positive y-axis.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param radius    The radius of the cylinder, in local space
     * @param height    The height of the cylinder
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0
     */
    public static Geometry createCylinder(Framework framework, double radius, double height) {
        return createCylinder(framework, radius, height, 8);
    }

    /**
     * Create a new Cylinder with the given radius, height, and resolution. Its axis will be the positive
     * y-axis.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param radius    The radius of the cylinder, in local space
     * @param height    The height of the cylinder
     * @param res       The resolution of the cylinder, the higher the value the smoother the tesselation
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0 or if res < 4
     */
    public static Geometry createCylinder(Framework framework, double radius, double height, int res) {
        return createCylinder(framework, new Vector3(0, 1, 0), new Vector3(0, 0, 0), radius, height, res);
    }

    /**
     * Create a new cylinder with the given vertical axis, radius, height, resolution and StorageMode.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param axis      The vertical axis of the cylinder
     * @param origin    The point this cylinder is centered about
     * @param radius    The radius of the cylinder, in local space
     * @param height    The height of the cylinder
     * @param res       The resolution of the sphere
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0 or if res < 4
     * @throws NullPointerException     if mode is null
     */
    public static Geometry createCylinder(Framework framework, @Const Vector3 axis, @Const Vector3 origin,
                                          double radius, double height, int res) {
        return new CylinderImpl(framework, axis, origin, radius, height, res);
    }

    /**
     * Create a Rectangle with an x basis vector of (1, 0, 0) and a y basis vector of (0, 1, 0), and the given
     * edge dimensions..
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param left      The left edge of the rectangle
     * @param right     The right edge of the rectangle
     * @param bottom    The bottom edge of the rectangle
     * @param top       The top edge of the rectangle
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if left > right or bottom > top
     */
    public static Geometry createRectangle(Framework framework, double left, double right, double bottom,
                                           double top) {
        return createRectangle(framework, left, right, bottom, top, new Vector3(1f, 0f, 0f),
                               new Vector3(0f, 1f, 0f));
    }

    /**
     * Create a Rectangle with the given basis vectors and edge dimensions.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param left      The left edge of the rectangle
     * @param right     The right edge of the rectangle
     * @param bottom    The bottom edge of the rectangle
     * @param top       The top edge of the rectangle
     * @param xAxis     Local x-axis of the rectangle
     * @param yAxis     Local y-axis of the rectangle
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if left > right or bottom > top
     * @throws NullPointerException     if xAxis or yAxis are null
     */
    public static Geometry createRectangle(Framework framework, double left, double right, double bottom,
                                           double top, @Const Vector3 xAxis, @Const Vector3 yAxis) {
        return new RectangleImpl(framework, left, right, bottom, top, xAxis, yAxis);
    }

    /**
     * Create a new Sphere with the given radius, a resolution of 8.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param radius    The radius of the sphere, in local space
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0
     */
    public static Geometry createSphere(Framework framework, double radius) {
        return createSphere(framework, radius, 8);
    }

    /**
     * Create a new Sphere with the given radius and resolution.
     *
     * @param framework The Framework that creates the vertex and element buffers
     * @param radius    The radius of the sphere, in local space
     * @param res       The resolution of the sphere, the higher the value the smoother the tesselation
     *
     * @return The new geometry
     *
     * @throws IllegalArgumentException if radius <= 0 or if res < 4
     */
    public static Geometry createSphere(Framework framework, double radius, int res) {
        return new SphereImpl(framework, radius, res);
    }

    /**
     * Instantiates a new Utah teapot object.
     *
     * @param framework The Framework that creates the vertex and element buffers
     *
     * @return The new geometry
     */
    public static Geometry createTeapot(Framework framework) {
        try {
            return GeometryLoader.readGeometry(framework, Shapes.class.getResource("teapot.ply"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load teapot geometry from PLY file", e);
        }
    }

    /**
     * Instantiates a new mesh from the classic armadillo model.
     *
     * @param framework The Framework that creates the vertex and element buffers
     *
     * @return The new geometry
     */
    public static Geometry createArmadillo(Framework framework) {
        try {
            return GeometryLoader.readGeometry(framework, Shapes.class.getResource("armadillo2.ply"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load teapot geometry from PLY file", e);
        }
    }

    /**
     * Instantiates a new mesh from the classic Stanford bunny model.
     *
     * @param framework The Framework that creates the vertex and element buffers
     *
     * @return The new geometry
     */
    public static Geometry createBunny(Framework framework) {
        try {
            return GeometryLoader.readGeometry(framework, Shapes.class.getResource("bunny.ply"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load teapot geometry from PLY file", e);
        }
    }
}
