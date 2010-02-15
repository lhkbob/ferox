package com.ferox;

import java.util.Iterator;

import com.ferox.util.entity.Component;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;
import com.ferox.util.entity.NonIndexable;

public class EntityTest {
	public static void main(String[] args) {
		EntitySystem system = new EntitySystem();
		A a1 = new A(1);
		A a2 = new A(2);
		B b1 = new B(1);
		B b2 = new B(2);
		
		Entity e1 = system.newEntity();
		e1.add(a1);
		e1.add(b1);
		
		Entity e2 = system.newEntity();
		e2.add(a2);
		e2.add(b2);
		
		System.out.println(a1 + " " + a1.getComponentHash());
		System.out.println(a2 + " " + a2.getComponentHash());
		System.out.println(b1 + " " + b1.getComponentHash());
		System.out.println(b2 + " " + b2.getComponentHash());
		
		System.out.println(e1 + " " + e1.getComponentHash());
		System.out.println(e2 + " " + e2.getComponentHash());
		
		printAll("ALL", system.iterator());
		
		e1.remove(A.class);
		printAll("A", system.iterator(A.class));
		
		e2.remove(B.class);
		printAll("B", system.iterator(B.class));
	}
	
	private static void printAll(String name, Iterator<Entity> it) {
		System.out.println("Iterate " + name);
		while(it.hasNext()) {
			System.out.println(it.next());
			it.remove();
		}
		System.out.println("Complete");
	}
	
	@NonIndexable
	private static class A extends Component {
		private int a;
		public A(int a) {
			super("A");
			this.a = a;
		}
		
		public int getA() {
			return a;
		}
		
		public void setA(int a) {
			this.a = a;
			notifyReferenceChange();
		}
	}
	
	private static class B extends Component {
		int b;
		public B(int b) {
			super("B");
			this.b = b;
		}
		
		public int getB() {
			return b;
		}
		
		public void setB(int b) {
			this.b = b;
			notifyReferenceChange();
		}
	}
}
