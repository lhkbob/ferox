package com.ferox.renderer.impl.jogl;

import java.util.IdentityHashMap;

import com.ferox.renderer.impl.DriverFactory;
import com.ferox.renderer.impl.GeometryDriver;
import com.ferox.resource.Geometry;

/** The BasicGeometryDriverFactory will automatically load resource
 * drivers for a Geometry if there's an implementation of GeometryDriver
 * matching this pattern:
 *		com.ferox.renderer.impl.jogl.drivers.Jogl<GeometrySimpleClassName>Driver
 * If no class is found, it will search for drivers matching the geometry's superclasses.
 *  
 * It will load that class, make sure it implements GeometryDriver and return one
 * instance of that class per factory lifetime.  The GeometryDriver must declare a 
 * constructor that takes a JoglSurfaceFactory as an argument.
 *
 * @author Michael Ludwig
 *
 */
public class BasicGeometryDriverFactory implements DriverFactory<Class<? extends Geometry>, GeometryDriver> {
	private IdentityHashMap<Class<? extends Geometry>, GeometryDriver> loadedDrivers;
	private JoglSurfaceFactory factory;
	
	public BasicGeometryDriverFactory(JoglSurfaceFactory factory) {
		this.loadedDrivers = new IdentityHashMap<Class<? extends Geometry>, GeometryDriver>();
		this.factory = factory;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public GeometryDriver getDriver(Class<? extends Geometry> type) {
		if (type != null) {
			while(true) {
				GeometryDriver driver = this.loadedDrivers.get(type);
				if (driver == null) // try to load the type
					driver = this.loadNewDriver(type);
				
				if (driver == null) {
					// move type to super-class
					Class<?> spr = type.getSuperclass();
					if (Geometry.class.isAssignableFrom(spr))
						type = (Class<? extends Geometry>) spr;
					else
						return null;
				} else
					return driver;
			}		
		}
		
		return null;
	}
	
	private GeometryDriver loadNewDriver(Class<? extends Geometry> type) {
		String driverName = this.getClass().getPackage().getName() + ".drivers.Jogl" + type.getSimpleName() + "Driver";
		try {
			Class<?> driverClass = Class.forName(driverName);
			if (GeometryDriver.class.isAssignableFrom(driverClass)) {
				GeometryDriver driver = (GeometryDriver) driverClass.getDeclaredConstructor(JoglSurfaceFactory.class).newInstance(this.factory);
				this.loadedDrivers.put(type, driver);
				return driver;
			} else
				return null;
		} catch (Exception e) {
			return null;
		}
	}
}
