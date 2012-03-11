package com.ferox.math;


public class MathTest {
    public static void main(String[] args) {
        int numMatrices = 10000;
        int numTests = 10000;
        
        int v = 0;
        v += Long.MAX_VALUE;
        
        double[] dataDA = getData(numMatrices);
        double[] dataDB = getData(numMatrices);
        
        Matrix3[] a2 = getMatrix2(dataDA);
        Matrix3[] b2 = getMatrix2(dataDB);
        
        double d2 = test2(a2, b2);
        double d4 = test4(dataDA, dataDB);
        
        System.out.println("Test results:");
        System.out.println("\ttest2 = " + d2);
        System.out.println("\ttest4 = " + d4);
        System.out.println();
        
        for (int i = 0; i < 3; i++) {
            System.out.println("Test " + (i + 1));
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
    
    public static double test2(Matrix3[] as, Matrix3[] bs) {
        Matrix3 m = new Matrix3();
        double d = 0;
        for (int i = 0; i < as.length; i++) {
            d += m.mul(as[i], bs[i]).inverse().determinant();
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
