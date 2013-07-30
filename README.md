# Ferox

Ferox is a collection of libraries and frameworks for graphics and game
programming, with features including:

* Convenient wrapper over OpenGL that manages textures and VBOs, and
  provides type safety and enum definitions.
* Support for 2D, 3D, and cube map textures in PNG, JPEG, GIF, TGA, and DDS
  file formats.
* Support for GLSL shaders, FBOs and render-to-texture functionality, and
  multiple render targets.
* Convenient scene description library built on top of the renderer and using
  the [Entreri][] entity-component framework.
* Rigid-body physics engine ported from [Bullet][].
* Convenient input event handling using triggers.

[Entreri]: http://bitbucket.org/mludwig/entreri
[Bullet]: http://bulletphysics.org/

## Maven

Ferox will be released to the Central Repository shortly. In the mean time,
the current SNAPSHOT can be installed locally and depended on using:

    <dependency>
      <groupId>com.lhkbob.ferox</groupId>
      <artifactId>ferox</artifactId>
      <version>0.0.2-SNAPSHOT</version>
    </dependency>
    <dependency>
      <!-- to get the LWJGL-based renderer implementation -->
      <groupId>com.lhkbob.ferox</groupId>
      <artifactId>ferox-renderer-lwjgl</artifactId>
      <version>0.0.2-SNAPSHOT</version>
    </dependency>
    
## Modules

Ferox contains several modules.  The top-level `ferox` module serves
as a container to build them and provide common settings. The other
modules are as follows:

* `ferox-input` - A keyboard and mouse event abstraction that can wrap AWT's
  event system, or poll-based systems (such as LWJGL's Keyboard and Mouse).
  It provides a logic and trigger image on top of these events.
* `ferox-math` - A 3D vector math library, and implementations of spatial 
  indices, such as octrees and quadtrees. Contains optional property definitions
  to use the math types within Entreri components.
* `ferox-physics` - A rigid-body physics simulator integrated with the Entreri
  component framework. This is a from-scratch port of Bullet to Java, and is not
  related to JBullet.
* `ferox-renderer` - Type-safe and thread-safe interface to OpenGL, and object-oriented
  resource support for textures, vertex buffers, and shaders. It is forward
  compatible for OpenGL 3.0 and 4.0.
* `ferox-renderer-jogl` - Implementation of renderer API using the
  JOGL 2.0 OpenGL wrapper.
* `ferox-renderer-lwjgl` - Implementation of renderer API using the
  LWJGL OpenGL wrapper.
* `ferox-scene` - A scene library built on the Entity component framework and
  uses the Ferox renderer to render scenes. It is compatible with the Ferox
  physics engine to easily add physics simulations to 3D scenes.
* `ferox-util` - A collection of utilities ranging from java.util.collections
  extensions to profiling and runtime monitoring tools.
* `ferox-demos` - Test applications for the other modules.
  