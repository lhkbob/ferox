package com.ferox;

import com.ferox.math.AffineTransform;
import com.ferox.math.Matrix3f;
import com.ferox.math.ReadOnlyMatrix3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

// Conclusion:
// 1. Garbage collection will move objects around the heap making array scans
//    more expensive than expected (this performance impact is more than a x2 hit)
// 2. With +DoEscapeAnalysis, new'ing each vector does better than object arrays
//    assuming that there is garbage collection
// 3. Using a wrapper object to fill array data each iteration is fastest
//    Often by a factor of 3-4
// 4. When the dataset gets larger, tmp object creation scales better than
//    object arrays or new'ing.  new'ing scales worst but still performs better
//    object arrays when using 100000 objects
// 5. On small numbers of objects, object arrays are as fast or faster than
//    tmp objects (because GC doesn't kick in).
// 6. On truly large arrays, it seems like new'ing starts to suffer more
//    as well as excessive abstraction (although that is less consistent).
//    Using a tmp object through an array is still the fastest by far.

// This suggests that a backed primitive array approach will be best and most stable,
// although it makes the nature of the entity system more difficult.
//  Namely adds/removes of components on an entity is tricky because we want to keep
//   an entities components all in their relative place within the backed array
// - Could have added components use a small temp buffer until things can be packed
//   at the end/start of the frame.

// Would be cool if we had a property class that handled the mapping, as well as,
// a property that supported temp objects to do the reading. Must worry slightly
// about abstraction cost but I think JIT should handle that pretty well.
// although maybe the casts would get expensive - and what about accessors for
// these properties (lots of interface impls.)

// Maybe we allow each component direct access to the array, so it can be faster
// and they can expose additionaly access methods as supported for tmp objects?

// Then I need to think how I want to do my math library. I might simplify it
// to public field access with only a single level of each.  Must consider this carefully,
// because on the other hand, the abstraction right now isn't costing me much performance
public class VectorTest {
    public static void main(String[] args) {
        ReadOnlyVector3f m = new Matrix3f().getCol(2);
        ReadOnlyMatrix4f m1 = new AffineTransform();
        ReadOnlyVector3f m2 = m1.getCol(3).getAsVector3f();
        System.out.println(m + " " + m2);
        
        int numVectors = 10000;
        int numTests = 10000;
        float[] vectorData = makeVectorData(numVectors);
        float[] matrixData = makeMatrixData(numVectors);
        
        ReadOnlyVector3f[] roVectors = makeVectors(vectorData);
        ReadOnlyMatrix3f[] roMatrices = makeMatrices(matrixData);
        FieldVector3[] fVectors = makeFieldVectors(vectorData);
        FieldMatrix3[] fMatrices = makeFieldMatrices(matrixData);
        
        Runtime r = Runtime.getRuntime();
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));
        
        for (int i = 0; i < numTests; i++) {
            testStdAdd(roVectors);
            testNewFieldAdd(numVectors, vectorData);
            testFieldAdd(fVectors);
            testInPlaceFieldAdd(numVectors, vectorData);
            testInPlaceAdd(numVectors, vectorData);
            
            testNewFieldMul(numVectors, vectorData, matrixData);
            testStdMul(roVectors, roMatrices);
            testFieldMul(fVectors, fMatrices);
            testInPlaceFieldMul(numVectors, vectorData, matrixData);
            testInPlaceMul(numVectors, vectorData, matrixData);
        }
        System.gc();
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));

        
        // adds
        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testStdAdd(roVectors);
            }
            System.out.println("Add ReadOnlyVector3f[] = " + (System.currentTimeMillis() - now));
        }
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));

        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testNewFieldAdd(numVectors, vectorData);
            }
            System.out.println("Add new field vectors = " + (System.currentTimeMillis() - now));
        }
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));

        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testInPlaceFieldAdd(numVectors, vectorData);
            }
            System.out.println("Add in place field vectors = " + (System.currentTimeMillis() - now));
        }
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));
        
        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testFieldAdd(fVectors);
            }
            System.out.println("Add field vectors = " + (System.currentTimeMillis() - now));
        }
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));
        
        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testInPlaceAdd(numVectors, vectorData);
            }
            System.out.println("Add in place normal vectors = " + (System.currentTimeMillis() - now));
        }
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));


        // muls
        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testStdMul(roVectors, roMatrices);
            }
            System.out.println("Mul ReadOnlyVector3f[]/ReadOnlyMatrix3f[] = " + (System.currentTimeMillis() - now));
        }
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));

        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testNewFieldMul(numVectors, vectorData, matrixData);
            }
            System.out.println("Mul new field vectors/matrices = " + (System.currentTimeMillis() - now));
        }
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));

        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testFieldMul(fVectors, fMatrices);
            }
            System.out.println("Mul field vectors/matrices = " + (System.currentTimeMillis() - now));
        }
        
        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testInPlaceFieldMul(numVectors, vectorData, matrixData);
            }
            System.out.println("Mul in place field vectors/matrices = " + (System.currentTimeMillis() - now));
        }
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));
        
        {
            long now = System.currentTimeMillis();
            for (int i = 0; i < numTests; i++) {
                testInPlaceMul(numVectors, vectorData, matrixData);
            }
            System.out.println("Mul in place normal vectors/matrices = " + (System.currentTimeMillis() - now));
        }
        System.out.printf("Mem: %.2f/%.2f\n", r.freeMemory() / (1024f * 1024f), r.totalMemory() / (1024f * 1024f));
    }
    
    public static float[] makeVectorData(int numVectors) {
        float[] v = new float[numVectors * 3];
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) Math.random() * 1000;
        }
        return v;
    }
    
    public static ReadOnlyVector3f[] makeVectors(float[] vectorBacking) {
        ReadOnlyVector3f[] v = new ReadOnlyVector3f[vectorBacking.length / 3];
        for (int i = 0; i < v.length; i++) {
            v[i] = new Vector3f(vectorBacking[i * 3], vectorBacking[i * 3 + 1], vectorBacking[i * 3 + 2]);
        }
        return v;
    }
    
    public static float[] makeMatrixData(int numVectors) {
        float[] v = new float[numVectors * 9];
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) Math.random() * 1000;
        }
        return v;
    }
    
    public static ReadOnlyMatrix3f[] makeMatrices(float[] matrixBacking) {
        ReadOnlyMatrix3f[] m = new ReadOnlyMatrix3f[matrixBacking.length / 9];
        for (int i = 0; i < m.length; i++) {
            m[i] = new Matrix3f();
            m[i].get(matrixBacking, i * 9, false);
        }
        return m;
    }
    
    public static FieldVector3[] makeFieldVectors(float[] vec) {
        FieldVector3[] v = new FieldVector3[vec.length / 3];
        for (int i = 0; i < v.length; i++) {
            v[i] = new FieldVector3(vec[i * 3], vec[i * 3 + 1], vec[i * 3 + 2]);
        }
        return v;
    }
    
    public static FieldMatrix3[] makeFieldMatrices(float[] matrix) {
        FieldMatrix3[] m = new FieldMatrix3[matrix.length / 9];
        for (int i = 0; i < m.length; i++) {
            m[i] = new FieldMatrix3(new FieldVector3(matrix[i * 9 + 0], matrix[i * 9 + 1], matrix[i * 9 + 2]),
                                    new FieldVector3(matrix[i * 9 + 3], matrix[i * 9 + 4], matrix[i * 9 + 5]),
                                    new FieldVector3(matrix[i * 9 + 6], matrix[i * 9 + 7], matrix[i * 9 + 8]));
        }
        return m;
    }
    
    public static ReadOnlyVector3f testStdAdd(ReadOnlyVector3f[] readOnlyBacking) {
        Vector3f dest = new Vector3f();
        for (int i = 0; i < readOnlyBacking.length; i++) {
            dest.add(readOnlyBacking[i]);
        }
        return dest;
    }
    
    public static FieldVector3 testNewFieldAdd(int numVectors, float[] readOnlyBacking) {
        FieldVector3 dest = new FieldVector3(0f, 0f, 0f);
        FieldVector3 roReturn;
        for (int i = 0; i < numVectors; i++) {
            roReturn = new FieldVector3(readOnlyBacking[i * 3], readOnlyBacking[i * 3 + 1], readOnlyBacking[i * 3 + 2]);
            dest.add(roReturn);
        }
        return dest;
    }
    
    public static FieldVector3 testInPlaceFieldAdd(int numVectors, float[] readOnlyBacking) {
        FieldVector3 dest = new FieldVector3(0f, 0f, 0f);
        FieldVector3 tmp = new FieldVector3(0f, 0f, 0f);
        for (int i = 0; i < numVectors; i++) {
            tmp.x = readOnlyBacking[i * 3];
            tmp.y = readOnlyBacking[i * 3 + 1];
            tmp.z = readOnlyBacking[i * 3 + 2];
            dest.add(tmp);
        }
        return dest;
    }
    
    public static FieldVector3 testFieldAdd(FieldVector3[] readOnlyBacking) {
        FieldVector3 dest = new FieldVector3(0f, 0f, 0f);
        for (int i = 0; i < readOnlyBacking.length; i++) {
            dest.add(readOnlyBacking[i]);
        }
        return dest;
    }
    
    public static ReadOnlyVector3f testStdMul(ReadOnlyVector3f[] v, ReadOnlyMatrix3f[] m) {
        Vector3f dest = new Vector3f();
        for (int i = 0; i < v.length; i++) {
            m[i].mul(v[i], dest);
        }
        return dest;
    }
    
    public static FieldVector3 testNewFieldMul(int numVectors, float[] v, float[] m) {
        FieldVector3 dest = new FieldVector3(0f, 0f, 0f);
        FieldVector3 rov;
        FieldMatrix3 rom;
        for (int i = 0; i < numVectors; i++) {
            rom = new FieldMatrix3(new FieldVector3(m[i * 9 + 0], m[i * 9 + 1], m[i * 9 + 2]),
                                   new FieldVector3(m[i * 9 + 3], m[i * 9 + 4], m[i * 9 + 5]),
                                   new FieldVector3(m[i * 9 + 6], m[i * 9 + 7], m[i * 9 + 8]));
            rov = new FieldVector3(v[i * 3], v[i * 3 + 1], v[i * 3 + 2]);
            
            rom.mul(rov, dest);
        }
        
        return dest;
    }
    
    public static FieldVector3 testInPlaceFieldMul(int numVectors, float[] v, float[] m) {
        FieldVector3 dest = new FieldVector3(0f, 0f, 0f);
        FieldVector3 tv = new FieldVector3(0f, 0f, 0f);
        FieldMatrix3 tm = new FieldMatrix3(new FieldVector3(0f, 0f, 0), new FieldVector3(0f, 0f, 0f), new FieldVector3(0f, 0f, 0f));
        for (int i = 0; i < numVectors; i++) {
            tv.x = v[i * 3];
            tv.y = v[i * 3 + 1];
            tv.z = v[i * 3 + 2];
            
            tm.col1.x = m[i * 9 + 0];
            tm.col1.y = m[i * 9 + 1];
            tm.col1.z = m[i * 9 + 2];
            tm.col2.x = m[i * 9 + 3];
            tm.col2.y = m[i * 9 + 4];
            tm.col2.z = m[i * 9 + 5];
            tm.col3.x = m[i * 9 + 6];
            tm.col3.y = m[i * 9 + 7];
            tm.col3.z = m[i * 9 + 8];
            
            tm.mul(tv, dest);
        }
        
        return dest;
    }
    
    public static ReadOnlyVector3f testInPlaceAdd(int numVectors, float[] v) {
        Vector3f dest = new Vector3f();
        Vector3f tv = new Vector3f();
        for (int i = 0; i < numVectors; i++) {
            tv.set(v, i * 3);
            dest.add(tv);
        }
        
        return dest;
    }
    
    public static ReadOnlyVector3f testInPlaceMul(int numVectors, float[] v, float[] m) {
        Vector3f dest = new Vector3f();
        Vector3f tv = new Vector3f();
        Matrix3f tm = new Matrix3f();
        for (int i = 0; i < numVectors; i++) {
            tv.set(v, i * 3);
            tm.set(m, i * 9, false);
            
            tm.mul(tv, dest);
        }
        return dest;
    }
    
    public static FieldVector3 testFieldMul(FieldVector3[] v, FieldMatrix3[] m) {
        FieldVector3 dest = new FieldVector3(0f, 0f, 0f);
        for (int i = 0; i < v.length; i++) {
            m[i].mul(v[i], dest);
        }
        return dest;
    }
    
    public static class FieldVector3 {
        public float x, y, z;
        
        public FieldVector3(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public void add(FieldVector3 v) {
            this.x += v.x;
            this.y += v.y;
            this.z += v.z;
        }
    }
    
    public static class FieldMatrix3 {
        public final FieldVector3 col1, col2, col3;
        
        public FieldMatrix3(FieldVector3 c1, FieldVector3 c2, FieldVector3 c3) {
            col1 = c1;
            col2 = c2;
            col3 = c3;
        }
        
        public void mul(FieldVector3 v, FieldVector3 result) {
            result.x = col1.x * v.x + col2.x * v.y + col3.x * v.z;
            result.y = col1.y * v.x + col2.y * v.y + col3.y * v.z;
            result.z = col1.z * v.x + col2.z * v.y + col3.z * v.z;
        }
    }
}
