package com.ferox.renderer.impl.jogl;

import java.util.IdentityHashMap;

import com.ferox.renderer.impl.DriverFactory;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.resource.Resource;

/** The BasicResourceDriverFactory will automatically load resource
 * drivers for a Resource if there's an implementation of ResourceDriver
 * matching this pattern:
 *		com.ferox.renderer.impl.jogl.drivers.Jogl<ResourceSimpleClassName>ResourceDriver
 * If no class is found, it will search for drivers matching the resource's superclasses.
 *
 * It will load that class, make sure it implements ResourceDriver and return one
 * instance of that class per factory lifetime.  The ResourceDriver must declare
 * a constructor that takes a JoglSurfaceFactory as an argument.
 *
 * @author Michael Ludwig
 *
 */
public class BasicResourceDriverFactory implements DriverFactory<Class<? extends Resource>, ResourceDriver> {
	private IdentityHashMap<Class<? extends Resource>, ResourceDriver> loadedDrivers;
	private JoglSurfaceFactory factory;
	
	public BasicResourceDriverFactory(JoglSurfaceFactory factory) {
		this.loadedDrivers = new IdentityHashMap<Class<? extends Resource>, ResourceDriver>();
		this.factory = factory;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ResourceDriver getDriver(Class<? extends Resource> type) {
		if (type != null) {
			while(true) {
				ResourceDriver driver = this.loadedDrivers.get(type);
				if (driver == null) // try to load the type
					driver = this.loadNewDriver(type);
				
				if (driver == null) {
					// move type to super-class
					Class<?> spr = type.getSuperclass();
					if (Resource.class.isAssignableFrom(spr))
						type = (Class<? extends Resource>) spr;
					else
						return null;
				} else
					return driver;
			}	
		}
		
		return null;
	}
	
	private ResourceDriver loadNewDriver(Class<? extends Resource> type) {
		String driverName = this.getClass().getPackage().getName() + ".drivers.Jogl" + type.getSimpleName() + "ResourceDriver";
		try {
			Class<?> driverClass = Class.forName(driverName);
			if (ResourceDriver.class.isAssignableFrom(driverClass)) {
				ResourceDriver driver = (ResourceDriver) driverClass.getDeclaredConstructor(JoglSurfaceFactory.class).newInstance(this.factory);
				this.loadedDrivers.put(type, driver);
				return driver;
			} else
				return null;
		} catch (Exception e) {
			return null;
		}
	}
}
