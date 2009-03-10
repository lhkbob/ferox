package com.ferox.renderer.impl.jogl;

import java.util.IdentityHashMap;

import com.ferox.renderer.impl.DriverFactory;
import com.ferox.renderer.impl.StateDriver;
import com.ferox.state.State.Role;

/** The BasicStateDriverFactory will automatically load resource
 * drivers for a Role if there's an implementation of StateDriver
 * matching this pattern:
 *		com.ferox.renderer.impl.jogl.drivers.Jogl<RoleName>StateDriver
 * where RoleName is the enum name such that XYZ_ABC becomes XyzAbc.
 * 
 * It will load that class, make sure it implements StateDriver and return one
 * instance of that class per factory lifetime.  The StateDriver must declare a 
 * constructor that takes a JoglSurfaceFactory as an argument.
 *
 * @author Michael Ludwig
 *
 */
public class BasicStateDriverFactory implements DriverFactory<Role, StateDriver>{
	private IdentityHashMap<Role, StateDriver> loadedDrivers;
	private JoglSurfaceFactory factory;
	
	public BasicStateDriverFactory(JoglSurfaceFactory factory) {
		this.loadedDrivers = new IdentityHashMap<Role, StateDriver>();
		this.factory = factory;
	}
	
	@Override
	public StateDriver getDriver(Role type) {
		if (type != null) {
			StateDriver driver = this.loadedDrivers.get(type);
			if (driver == null)
				driver = this.loadNewDriver(type);
			return driver;
		}
		
		return null;
	}
	
	private StateDriver loadNewDriver(Role type) {
		String enumName = convertRoleName(type);
		String driverName = this.getClass().getPackage().getName() + ".drivers.Jogl" + enumName + "StateDriver";
		try {
			Class<?> driverClass = Class.forName(driverName);
			if (StateDriver.class.isAssignableFrom(driverClass)) {
				StateDriver driver = (StateDriver) driverClass.getDeclaredConstructor(JoglSurfaceFactory.class).newInstance(this.factory);
				this.loadedDrivers.put(type, driver);
				return driver;
			} else
				return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	private static String convertRoleName(Role role) {
		String name = role.name();
		StringBuilder b = new StringBuilder();
		
		int size = name.length();
		boolean capitilizeNext = true;
		char curr;
		for (int i = 0; i < size; i++) {
			curr = name.charAt(i);
			if (curr == '_')
				capitilizeNext = true;
			else {
				if (capitilizeNext)
					b.append(Character.toUpperCase(curr));
				else
					b.append(Character.toLowerCase(curr));
				capitilizeNext = false;
			}
		}
		
		return b.toString();
	}
}
