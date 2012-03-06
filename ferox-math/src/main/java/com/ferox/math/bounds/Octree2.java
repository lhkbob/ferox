package com.ferox.math.bounds;

public class Octree2 {
 // FIXME: I think the Octree class can be heavily optimized and improved/simplified
    // I need to decide to which style of octree is best, or if I need both: data stored in multiple leaves,
    // or data stored in smallest containing node.
    
    // I will need to analyze the situations where each performs the best, and
    // then determine how that fits in with Grid's responsibilities.
    
    // It would be nice if I could simplify the use of interfaces within Octree
    // so that I could avoid that type abstraction.  Additionally, if I could somehow
    // figure out how to pack the data so that the queries can somehow take advantage
    // of the memory cache, that would be great.
    //  - The memory cache is probably a bust, a linear octree requires a layout
    //    pattern that represents a space-filling curve, and I feel like that might
    //    not have the best cache performance.
    
    // So what are the pros/cons of a leaf-only octree, dynamic octree, and grid:
    // Grid:
    //   - constant memory size based on grid dimensions -> pro
    //   - 3D memory cost limits size of grid to something small like 16, even 32 is getting up to 32000 grid cells
    //   - constant iteration cost over all cells, which can be costly for visibility of a general frustum
    //   - visibility query could be implemented like an octree query
    //   - but then you might as well allocate a complete leaf-only octree and use that
    //   - if z was limited, grid might pay off more because you could put more resolution in xy planes
    
    // leaf-only octree:
    //   - visibility queries with frustums have overwork since you might encounter the same object in more than one cell
    //   - as a hierarchy, its queries will theoretically be faster than a grid, especially if many things are outside of view
    //   - a complete tree is better, otherwise memory use is unpredictable and potentially breaking if oom occurs
    //   - hot spots will not occur, because they will just go in multiple cells
    //   - updates/moving objects are expensive, although with a complete tree it might be less so, so long as you could still keep track of emptiness of zones, etc.
    
    // dynamic octrees
    //   - visibility queries are theoretically efficient because you will only see something once in the tree
    //   - however, hot spots occur which can push objects up to higher than expected nodes, and congest the root making performance hit n^2
    //   - updates are pretty fast since you can walk from the current node up and/or down until the new bounds are completely contained
    //   - intersection queries between elements suffer from hot spots as well
    //   - general aabb queries are still pretty fast although hot spots still hurt
    
    // loose octree:
    //   - this might be a better solution to the dynamic octree since it removes hot spots
    //   - each node has a buffer around it that elements can fit into, allowing smaller objects to still fit within low level or leaf nodes
    //     large objects on edges still move up to the root, but those should be few so it's not an issue
    //   - intersection between entities will be more expensive but it might be entirely reasonable for 
    //     visibility queries.
    //
    
    
    // I think I should write a fast static octree that is complete, with the understanding that most octrees,
    // don't require that high of a resolution.  I will not need to implement growing or node removal,
    // if I can keep track of node counts.  With this, I might be able to get updates faster than a total
    // removal and then re-add.
    //
    // The static octree will be good enough and do better than the grid I think, although being able to
    // limit resolution along a certain dimension seems valuable, although if I don't need to allocate
    // the complete tree, I get the same benefits.  Hmmm, there is a trade off here, I should investigate how
    // much memory is actually used up.
    
    //  EX: 32x32x32 is the leaf-level of the octree.  That is 33000 leafs, no array 0+ elements in each
    //   next is 16x16x16 -> 4000 nodes, each with an array of 8
    //   next is 8x8x8 -> 512 nodes, each with an array of 8
    //   next is 4x4x4 -> 64 nodes, each ""
    //   next is 2x2x2 -> 8 nodes, each ""
    //   next is 1x1x1 -> 1 node, with an array of 8
    
    // That is < 5000 8 element arrays -> 5000 lightweight objects + 40000 length array
    // and 33000 more lightweight leafs (assuming separate type)
    // each of these might take an int, plus 2 or 3 points, so lets say 32 bytes per object
    // 38000 * 32 + 40000 * 4 = 1376000 = 1.3 MB
    
    // That doesn't seem terrible, although 64x64x64 is probably getting more expensive, closer to 10MB
    
    // Of course, if I allow for the static octree to not be fully allocated at the start, then I really
    // don't need to worry about the grid as an index option since the portions of the octree not filled
    // will not be allocated.  This is probably what people expect, but then I have to figure out how
    // to make the updates faster than remove/add.
    //
    // Looking at the graphs from gamedev.net, sort/sweep will be the best for finding pairs of intersections.
    // a grid does reasonably, but would require maintenance.  sorting is pretty quick
    // Does this mean I should remove the intersections pair query from spatial-index? It would mean that
    // the physics engine cannot use plugin spatial indices, which is kind of lame but than again if I'm
    // just doing axis sweeping why spend the trouble when the rest of the spatial index is best used for
    // single-input queries.
    
    // I think it would be best to remove the intersection-pairs query from the interface, but I should
    // add one that supports ray queries.
    // - This brings up the point that we should support some potentially more detailed bounds or shape
    //   description to support building trees about primitives, or more specific bounds than the aabb.
    // - This would also let me reuse some code if I ever wanted to get into raytracing (wait a while why don't you)
    // - Also per-triangle picking support would be pretty cool since that is what the ray query will
    //   be most used for in the game engine (and not a ray tracer)
    
    // Is it required for the spatial indices to store aabb's for every element? If we go ahead and put
    // triangles into the index, there would be an aabb per triangle which for large models would be too
    // expensive.  In some cases, it seems much better to take some interface that can be used to
    // query its edges or intersections or something.
    
    
    // Other decisions that I have made are:
    // 1. The visibility system will generate a list of objects so that it can be iterated over.
    //     This will be a significant performance boost when most objects are not visible, which I 
    //     think is a reasonable assumption.
    // 2. I need to figure out how this information is passed around since the key store system
    //     doesn't work when part of the key depends on the given camera.
    //     The genericness of the setVisible() on renderable is still good and that will stay around I think,
    //     but I want to provide an alternate access path
    // 3. I'm not sure what to do about light influences, since only 2 out of 3 renderers care about them,
    //     because deferred shaders just handle the lights directly.  Is it worth embedding that
    //     information in with Material?  Is it weird to have deferred shaders ignore that information
    //     or disable its computation?
    // 4. I do not like that the vector/matrices of a component are shared by all components, I'm positive
    //    that that will become bug prone.  It would be simpler if there were only canonical instances,
    //    since we can't do caching on the component instance when multiple instances could point to the
    //    same component.
    
    // I think the solution I came up with for information sharing is to have some way of describing
    // interests, so anyone can ask for "controllers interested in visible entities" and then 
    // send the entities to them, and certain controllers would then expose this interest. Now I'm
    // not sure exactly what this interface would be because you'd have to specify the entity as well as
    // the camera.  Mayhaps the "interest" is an interface that exposes the injection methods.
}
