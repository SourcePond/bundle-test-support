package ch.sourcepond.testing;

import static java.lang.String.format;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class StubServiceActivator implements BundleActivator {
	static final String SERVICE_PROPERTIES = "service.properties";
	static final String SERVICE_INTERFACE = "serviceInterface";
	static final String IMPL_CLASS = "implClass";

	// These will be initialized in the enhanced class (Javassist)
	private Class<?> serviceInterface;

	// These will be initialized in the enhanced class (Javassist)
	private Class<?> implClass;

	@SuppressWarnings("rawtypes")
	private StubServiceFactory factory;
	private Object service;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void start(final BundleContext context) throws Exception {
		final Hashtable<String, String> serviceProperties = new Hashtable<>();
		final InputStream in = getClass().getResourceAsStream(format("/%s", SERVICE_PROPERTIES));
		if (in != null) {
			final Properties tmp = new Properties();
			try {
				tmp.load(in);
			} finally {
				in.close();
			}
			final Enumeration<?> e = tmp.propertyNames();
			while (e.hasMoreElements()) {
				final String name = (String) e.nextElement();
				serviceProperties.put(name, tmp.getProperty(name));
			}
		}

		if (StubServiceFactory.class.isAssignableFrom(implClass)) {
			factory = (StubServiceFactory<?>) implClass.newInstance();
			service = factory.create();
		} else {
			service = implClass.newInstance();
		}

		context.registerService((Class) serviceInterface, service, serviceProperties);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void stop(final BundleContext context) throws Exception {
		if (factory != null && service != null) {
			factory.destroy(service);
		}
	}

}
