Ferox
=====

Ferox is a collection of libraries and frameworks for graphics and game
programming. It contains the following modules, which are all reasonably
independent Maven projects. Any dependencies on other Ferox modules are 
expressed in their own Maven configurations.

ferox-input
~~~~~~~~~~~

A keyboard and mouse input event abstraction that is capable of wrapping AWT's 
event system, and poll-based systems (such as LWJGL's Keyboard and Mouse).

It also contains a logic and trigger framework for more convenient processing of
input events. This allows high-level code to declare a trigger to run when
'A' is pressed and 'Mouse1' is down, as an example.

ferox-math
~~~~~~~~~~

A 3D vector math library that contains implementations of 3- and 4-tuple 
vectors, 3x3 and 4x4 matrices, and quaternions. It also has spatial index 
implementations for octrees and quadtrees.

It has an optional dependency on http://bitbucket.org/entreri to provide
Property implementations for its vector math types.

ferox-physics
~~~~~~~~~~~~~

A rigid-body physics simulator integrated with the Entreri component framework.

ferox-renderer-api
~~~~~~~~~~~~~~~~~~

A multi-threaded API that wraps OpenGL to provide a simplified interface,
with better self-documentation and less prone to errors. It also provides an
API for multi-threaded resource management for textures and geometry.

It is a forward-looking API that can target OpenGL 3.0 and 4.0 features
(although they are not all implemented yet).

ferox-renderer-impl
~~~~~~~~~~~~~~~~~~~

Abstract implementation and utilities that are OpenGL wrapper agnostic that
greatly reduces the amount of work needed to complete an implementation of
the renderer API.

ferox-renderer-jogl
~~~~~~~~~~~~~~~~~~~

An implementation of the renderer based on the JOGL 2.0 OpenGL wrapper.

ferox-renderer-lwjgl
~~~~~~~~~~~~~~~~~~~~

An implementation of the renderer based on the LWJGL OpenGL wrapper.

ferox-scene
~~~~~~~~~~~

A scene library built on top of the Entity component framework that uses the
Ferox renderer to perform the actual rendering.

It is designed to be fully compatible with the Ferox physics engine to allow
for easy visualizations of rigid-body physics.

ferox-util
~~~~~~~~~~

A collection of utilities ranging from java.util.collections extensions to
profiling and runtime monitoring tools.

Maven
=====

Ferox will be released to Maven in the near future as:
 * groupId: com.lhkbob.ferox
 * artifactId: ferox
