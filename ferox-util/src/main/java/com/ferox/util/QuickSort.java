package com.ferox.util;

import java.util.List;

public class QuickSort {
    public static void sort(ItemView view) {
        int[] hashes = new int[view.length()];
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = view.hash(i);
        }
        quickSort(hashes, view, 0, hashes.length);
    }

    public static <T> void sort(final T[] array, final HashFunction<? super T> hash) {
        sort(new ItemView() {
            @Override
            public void swap(int srcIndex, int dstIndex) {
                T tmp = array[srcIndex];
                array[srcIndex] = array[dstIndex];
                array[dstIndex] = tmp;
            }

            @Override
            public int length() {
                return array.length;
            }

            @Override
            public int hash(int index) {
                return (array[index] == null ? 0 : hash.hashCode(array[index]));
            }
        });
    }

    public static <T> void sort(final List<T> list, final HashFunction<? super T> hash) {
        sort(new ItemView() {
            @Override
            public void swap(int srcIndex, int dstIndex) {
                T tmp = list.get(srcIndex);
                list.set(srcIndex, list.get(dstIndex));
                list.set(dstIndex, tmp);
            }

            @Override
            public int length() {
                return list.size();
            }

            @Override
            public int hash(int index) {
                return (list.get(index) == null ? 0 : hash.hashCode(list.get(index)));
            }
        });
    }

    public static <T> void sort(T[] array) {
        sort(array, HashFunction.NATURAL_HASHER);
    }

    public static <T> void sort(List<T> list) {
        sort(list, HashFunction.NATURAL_HASHER);
    }

    // use quick sort to sort the elements of the Bag, based off of paired keys stored in x
    private static void quickSort(int[] x, ItemView view, int off, int len) {
        // insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && x[j - 1] > x[j]; j--) {
                    swap(x, view, j, j - 1);
                }
            }
            return;
        }

        // choose a partition element, v
        int m = off + (len >> 1); // small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // mid-size, med of 3
        }
        int v = x[m];

        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= v) {
                if (v == x[b]) {
                    swap(x, view, a++, b);
                }
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (v == x[c]) {
                    swap(x, view, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swap(x, view, b++, c--);
        }

        // swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, view, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, view, b, n - s, s);

        // recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            quickSort(x, view, off, s);
        }
        if ((s = d - c) > 1) {
            quickSort(x, view, n - s, s);
        }
    }

    // swaps the elements at indices a and b, along with the hashes in x
    private static void swap(int[] x, ItemView view, int a, int b) {
        int k = x[a];
        x[a] = x[b];
        x[b] = k;
        view.swap(a, b);
    }

    // swaps n elements starting at a and b, such that (a,b), (a+1, b+1), etc. are swapped
    private static void vecswap(int[] x, ItemView view, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, view, a, b);
        }
    }

    // returns the index of the median of the three indexed elements
    private static int med3(int x[], int a, int b, int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }
}
