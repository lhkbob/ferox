package com.ferox.math2;

import com.ferox.math.MutableMatrix3f;
import com.ferox.math.ReadOnlyMatrix3f;

public class MathTest {
    public static void main(String[] args) {
        int numMatrices = 10000;
        int numTests = 10000;
        
        int v = 0;
        v += Long.MAX_VALUE;
        
        double[] dataDA = getData(numMatrices);
        double[] dataDB = getData(numMatrices);
        float[] dataFA = cast(dataDA);
        float[] dataFB = cast(dataDB);
        
        ReadOnlyMatrix3f[] a1 = getMatrix1(dataFA);
        ReadOnlyMatrix3f[] b1 = getMatrix1(dataFB);
        Matrix3[] a2 = getMatrix2(dataDA);
        Matrix3[] b2 = getMatrix2(dataDB);
        
        float d1 = test1(a1, b1);
        double d2 = test2(a2, b2);
        float d3 = test3(dataFA, dataFB);
        double d4 = test4(dataDA, dataDB);
        
        System.out.println("Test results:");
        System.out.println("\ttest1 = " + d1);
        System.out.println("\ttest2 = " + d2);
        System.out.println("\ttest3 = " + d3);
        System.out.println("\ttest4 = " + d4);
        System.out.println();
        
        for (int i = 0; i < 3; i++) {
            System.out.println("Test " + (i + 1));
            {
                long now = System.currentTimeMillis();
                for (int j = 0; j < numTests; j++) {
                    test1(a1, b1);
                }
                System.out.println("ReadOnlyMatrix3f: " + (System.currentTimeMillis() - now));
            }
            {
                long now = System.currentTimeMillis();
                for (int j = 0; j < numTests; j++) {
                    test2(a2, b2);
                }
                System.out.println("Matrix3: " + (System.currentTimeMillis() - now));
            }
            {
                long now = System.currentTimeMillis();
                for (int j = 0; j < numTests; j++) {
                    test3(dataFA, dataFB);
                }
                System.out.println("float[] ReadOnlyMatrix3f: " + (System.currentTimeMillis() - now));
            }
            {
                long now = System.currentTimeMillis();
                for (int j = 0; j < numTests; j++) {
                    test4(dataDA, dataDB);
                }
                System.out.println("float[] Matrix3: " + (System.currentTimeMillis() - now));
            }
            System.out.println();
        }
    }
    
    public static double r() {
        return Math.random() * 10;
    }
    
    public static double[] getData(int num) {
        double[] m = new double[9 * num];
        for (int i = 0; i < m.length; i++)
            m[i] = r();
        return m;
    }
    
    public static float[] cast(double[] d) {
        float[] m = new float[d.length];
        for (int i = 0; i < d.length; i++)
            m[i] = (float) d[i];
        return m;
    }
    
    public static Matrix3[] getMatrix2(double[] data) {
        Matrix3[] m = new Matrix3[data.length / 9];
        for (int i = 0; i < m.length; i ++) {
            m[i] = new Matrix3().set(data, i * 9, false);
        }
        
        return m;
    }
    
    public static ReadOnlyMatrix3f[] getMatrix1(float[] data) {
        ReadOnlyMatrix3f[] m = new ReadOnlyMatrix3f[data.length / 9];
        for (int i = 0; i < m.length; i ++) {
            MutableMatrix3f t = new com.ferox.math.Matrix3f();
            t.set(data, i * 9, false);
            m[i] = t;
        }
        return m;
    }
    
    public static float test1(ReadOnlyMatrix3f[] as, ReadOnlyMatrix3f[] bs) {
        MutableMatrix3f m = new com.ferox.math.Matrix3f();
        float d = 0;
        for (int i = 0; i < as.length; i++) {
            d += as[i].mul(bs[i], m).inverse().determinant();
        }
        return d;
    }
    
    public static double test2(Matrix3[] as, Matrix3[] bs) {
        Matrix3 m = new Matrix3();
        double d = 0;
        for (int i = 0; i < as.length; i++) {
            d += m.mul(as[i], bs[i]).inverse().determinant();
        }
        return d;
    }
    
    public static float test3(float[] dataA, float[] dataB) {
        MutableMatrix3f m1 = new com.ferox.math.Matrix3f();
        MutableMatrix3f m2 = new com.ferox.math.Matrix3f();
        float d = 0;
        for (int i = 0; i < dataA.length; i += 9) {
            m1.set(dataA, i, false);
            m2.set(dataB, i, false);
            
            d += m1.mul(m2).inverse().determinant();
        }
        return d;
    }
    
    public static double test4(double[] dataA, double[] dataB) {
        Matrix3 m1 = new Matrix3();
        Matrix3 m2 = new Matrix3();
        double d = 0;
        for (int i = 0; i < dataA.length; i += 9) {
            m1.set(dataA, i, false);
            m2.set(dataB, i, false);
            
            d += m1.mul(m2).inverse().determinant();
        }
        return d;
    }
}
