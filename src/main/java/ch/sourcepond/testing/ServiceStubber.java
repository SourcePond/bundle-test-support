package ch.sourcepond.testing;

import static ch.sourcepond.testing.StubServiceActivator.IMPL_CLASS;
import static ch.sourcepond.testing.StubServiceActivator.SERVICE_INTERFACE;
import static ch.sourcepond.testing.StubServiceActivator.SERVICE_PROPERTIES;
import static java.io.File.createTempFile;
import static java.lang.String.format;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static javassist.ClassPool.getDefault;
import static javassist.CtField.Initializer.byExpr;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.BundleActivator;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

/**
 * <p>
 * The service stubber gives you the possibility to install a service stub into
 * your OSGi test-container. This is useful if your bundle has service
 * dependencies but you don't want to add extra dependencies to your test
 * project.
 * </p>
 * 
 * <p>
 * Internally, an artificial bundle which contains a special
 * {@link BundleActivator} will be generated and installed into the OSGi
 * container. The {@link BundleActivator} exports a stub-service as specified
 * with this stubber.
 * </p>
 */
public class ServiceStubber<T> {
	private static final String ENHANCED_ACTIVATOR_CLASS_NAME = "ch.sourcepond.$enhanced."
			+ StubServiceActivator.class.getSimpleName();
	private Class<? extends T> implementation;
	private Class<? extends StubServiceFactory<T>> factory;
	private final List<Class<?>> classes = new LinkedList<>();
	private final Class<T> serviceInterface;
	private final Properties properties = new Properties();

	ServiceStubber(final Class<T> pServiceInterface) {
		serviceInterface = pServiceInterface;
	}

	/**
	 * Specifies the implementation class of the service stub. The class must be
	 * public and must have a public, non-argument constructor. Use either this
	 * method or {@link #withFactory(Class)} but not both.
	 * 
	 * @param pImplementation
	 *            Implementation class, must not be {@code null}
	 * @return This service-stubber
	 */
	public ServiceStubber<T> withImpl(final Class<? extends T> pImplementation) {
		assertNotNull("Implementation class should not be null!", pImplementation);
		implementation = pImplementation;
		return this;
	}

	/**
	 * Specifies the factory class to create the service stub. The class must be
	 * public and must have a public, non-argument constructor. Use either this
	 * method or {@link #withImpl(Class)} but not both.
	 * 
	 * @param pFactory
	 *            Factory class, must not be {@code null}
	 * @return
	 */
	public ServiceStubber<T> withFactory(final Class<? extends StubServiceFactory<T>> pFactory) {
		assertNotNull("Factory class should not be null!", pFactory);
		factory = pFactory;
		return this;
	}

	public ServiceStubber<T> addClass(final Class<?> pClass) {
		classes.add(pClass);
		return this;
	}

	public ServiceStubber<T> addProperty(final String pPropertyName, final String pValue) {
		properties.setProperty(pPropertyName, pValue);
		return this;
	}

	/**
	 * @param pBundle
	 * @throws Exception
	 */
	static Option buildBundle(final TinyBundle pBundle) throws Exception {
		try (final InputStream in = pBundle.build(withBnd())) {
			final File bundle = createTempFile("test-bundle.", ".jar");
			bundle.deleteOnExit();
			copy(in, bundle.toPath(), REPLACE_EXISTING);
			return new UrlProvisionOption(bundle.toURI().toURL().toExternalForm());
		}
	}

	private static void initField(final CtClass activatorCtClass, final String pName, final Class<?> pServiceInterface)
			throws Exception {
		final CtField field = activatorCtClass.getField(pName);
		activatorCtClass.removeField(field);
		activatorCtClass.addField(field, byExpr(format("%s.class", pServiceInterface.getName())));
	}

	private Class<?> enhanceActivatorClass(final Class<?> pImplClass) throws Exception {
		final ClassPool pool = getDefault();
		final CtClass activatorCtClass = pool.get(StubServiceActivator.class.getName());

		if (!activatorCtClass.isFrozen()) {
			activatorCtClass.setName(ENHANCED_ACTIVATOR_CLASS_NAME);
			initField(activatorCtClass, SERVICE_INTERFACE, serviceInterface);
			initField(activatorCtClass, IMPL_CLASS, pImplClass);
			activatorCtClass.writeFile("target/test-classes");
		}
		return getClass().getClassLoader().loadClass(ENHANCED_ACTIVATOR_CLASS_NAME);
	}

	/**
	 * Builds the final stub-bundle which can be installed with the
	 * {@link Option} instance returned.
	 * 
	 * @return {@link Option} instance, never {@code null}
	 * @throws Exception
	 *             Thrown, if something went wrong during bundle build.
	 */
	public Option build() throws Exception {
		final Class<?> impl = determineImplClass();
		final TinyBundle bundle = bundle();
		bundle.add(impl);

		if (!classes.isEmpty()) {
			for (final Class<?> cl : classes) {
				bundle.add(cl);
			}
		}

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		properties.store(out, "Service properties");
		final Class<?> enhancedActivatorClass = enhanceActivatorClass(impl);

		return composite(
				buildBundle(bundle.add(impl).add(enhancedActivatorClass)
						.set("Bundle-Activator", enhancedActivatorClass.getName())
						.add(format("/%s", SERVICE_PROPERTIES), new ByteArrayInputStream(out.toByteArray()))),

		// These bundles must be available in order to run the stub-bundle
				mavenBundle("ch.sourcepond.testing", "bundle-test-support").versionAsInProject(),
				mavenBundle("org.ops4j.pax.tinybundles", "tinybundles").versionAsInProject(),
				mavenBundle("biz.aQute.bnd", "bndlib").versionAsInProject(),
				mavenBundle("org.javassist", "javassist").versionAsInProject());
	}

	private Class<?> determineImplClass() {
		assertFalse(
				format("Either the service implementation class or an implementation of %s must be specified but not both!",
						StubServiceFactory.class.getName()),
				implementation != null && factory != null);
		final Class<?> impl = implementation == null ? factory : implementation;
		assertNotNull(
				"Neither an implementation nor a factory have been specified. Use either withImpl() or withFactory()",
				impl);
		return impl;
	}
}
