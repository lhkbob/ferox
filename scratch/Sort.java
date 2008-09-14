import java.util.Arrays;


public class Sort {
	public static void main(String[] args) {
		int numbers = 200000;
		int[] array = new int[numbers];
		System.out.println("randomizing");
		for (int i = 0; i < array.length; i++) {
			array[i] = (int)(Math.random() * numbers);
		}
		System.out.println("sorting");
		sort(array);
		verify(array);
		System.out.println("finished");
		Arrays.sort(array);
		System.out.println("benchmark");
	}
	
	public static void verify(int[] array) {
		for (int i = 1; i < array.length; i++) {
			if (array[i - 1] > array[i])
				System.out.println("Sort failed at index: " + i + " | " + array[i - 1] + " " + array[i]);
		}
	}
	
	public static void sort(int[] array) {
		int index = 0;
		
		
		while(index < array.length) {
			int min = array[index];
			int minIndex = index;
			
			for (int i = index; i < array.length; i++) {
				if (array[i] < min) {
					min = array[i];
					minIndex = i;
				}
			}
			
			int temp = array[index];
			array[index] = min;
			array[minIndex] = temp;
			index += 1;
		}
	}
}
