package com.ferox.resource.data;

import com.ferox.resource.DataType;

/**
 * <p/>
 * OpenGLData represents a block of primitive memory that can be pushed to the GPU to
 * store texture data, vertex data, or more. Each OpenGLData instance has a fixed length
 * and backing data store.
 * <p/>
 * The backing data store depends on the specific OpenGL implementation. The classes
 * provided use primitive arrays but this interface is also compatible with NIO buffers.
 * Framework implementations may have optimized code paths for using the existing data
 * types.
 * <p/>
 * OpenGLData instances are not thread safe, it is assumed that mutations of an OpenGLData
 * object will be done within the synchronization block of whatever resource owns it.
 *
 * @param <T> The data type that the OpenGLData instance wraps
 *
 * @author Michael Ludwig
 */
public interface OpenGLData<T> {
    // FIXME this is a pretty good abstraction over the amount of data we need to send
    // to OpenGL, but I'm not sure if the resource model that ferox use's is actually the
    // best. Perhaps something that uses factory methods from the framework would be
    // better.
    //
    // Here are my thoughts on pros:
    //  1. Would no longer need the map from resource to handle, it could be part of the
    //     implementation class
    //  2. Validation exceptions would be thrown at the point of creation if a resource
    //     couldn't be created because of support, instead of the 1st time it's used
    //  3. It makes it easy to write factory methods to create a texture object form a VBO
    //     or other weird data sharing options later on while fitting the same function model
    //  4. It makes it clear that a resource is used by a single framework, which encourages
    //     the use of only one framework (i.e. the right thing)
    //  5. It prevents someone from extending Resource willy-nilly and expecting it to
    //     work out of the box
    //
    // Here are my cons:
    //  1. It's a pretty hefty change to the class model
    //  2. Mutating an existing resource still runs into the same problem of exposing
    //     the data type, or the number of creation method permutations would be
    //     through the roof.
    //  3. The model works quite well if we have the constructed resources be immutable,
    //     which is nice from a threading perspective and would simplify the drivers because
    //     they wouldn't have to do update checks. However, it would mean a much worse
    //     performance for an application that did significant dynamic scenes, such as
    //     particle systems, CPU skinning, packed texture attributes for animations,
    //     text, and transparency sorting
    //  4. Adding update functions that hotswap the data out for an existing resource
    //     dangerously approaches the API OpenGL already exposes, but I think with resource
    //     management we don't want a functional API and OO is much better for operating
    //     on the data
    //
    //
    // A middle ground would be to define the current classes as abstract or as interfaces,
    // and then have the implementations in the impl package, but they still retain their
    // entire set of mutators, etc. only now they can update an internal flag directly
    // to trigger a refresh on the GPU side.  This gets many or all of the benefits to
    // the approach without going too far into functional land, or changing the class
    // model that much.
    //
    // Data access would still use this current approach, which isn't great, and part of
    // me wanted to have the factory methods permuted enough to take the different array
    // types so that we wouldn't need a data wrapper at all.  This would really only work
    // if the resource was immutable, since there'd be no need to get back the data. This
    // is a very attractive choice except for the performance impact of a) always having
    // to copy the entire resource to mutate it b) not being able to reuse OpenGL id's
    // to effectively do an update.  This actually makes a dynamic texture atlas impossible
    // or at least way more expensive than it would be with normal OpenGL.
    //
    // Another thing to think about is if I require the framework to construct and
    // mutate resources, that basically means I have to inject the framework into anything
    // that wants to create a resource for me, or process it if they may need to change it.
    // It means I can't have a texture loader independent of the framework unless I
    // define another mirror'ed texture data holder that can be easily converted into the
    // framework resource.
    //
    //
    // I think I've convinced myself that these changes don't hold up, and certainly
    // don't fix the data access problem so I think I will continue with my current
    // approach; the current approach also has its own way of handling shared data between
    // VBOs and textures and that is to provide specialized implementations of TexelData,
    // etc. and it wouldn't be so weird to say that these come from the framework.


    /**
     * Get the underlying data wrapped by this OpenGLData instance. This will always
     * return the same array, buffer, etc. for a given OpenGLData object. Its length,
     * however represented, will equal {@link #getLength()}.
     *
     * @return The underlying data for this instance
     */
    public T get();

    /**
     * Get the length of this OpenGLData. This will be a constant value for a given
     * instance. The length is the number of primitives held by the data, and not the
     * number of bytes.
     *
     * @return The number of primitives stored in this data instance
     */
    public int getLength();

    /**
     * Get the data type of this OpenGLData. This will be a constant value for a given
     * instance. The data type determines the underlying Java primitive, but OpenGLData
     * implementations may interpret that data differently (such as signed versus unsigned
     * integers).
     *
     * @return The data type of this OpenGLData
     */
    public DataType getType();

    /**
     * <p/>
     * Get a simple object whose identity mirrors the reference identity of this
     * OpenGLData object. The relationship {@code (data1 == data2) == (data1.getKey() ==
     * data2.getKey())} will always be true. If two keys are the same instance, the
     * producing BufferData objects are the same instance.
     * <p/>
     * This is primarily intended for Framework implementations, so they can store the
     * identify of a OpenGLData used by a resource without preventing the OpenGLData from
     * being garbage collected.
     *
     * @return An identity key for this data instance
     */
    public Object getKey();
}
