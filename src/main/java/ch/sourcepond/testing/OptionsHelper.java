/*Copyright (C) 2015 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.testing;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import org.ops4j.pax.exam.Option;

/**
 * Small helper class for Pax-Exam test configuration.
 *
 */
public class OptionsHelper {

	/**
	 * Creates the default options necessary to test a SourcePond component in
	 * an OSGi environment. All SourcePond components are split into an API
	 * (example artifactId ch.sourcepond.componentname.api) and implementations
	 * (example artifactId ch.sourcepond.componentname.impl). The arguments
	 * passed to this method are always component names without *.api or *.impl
	 * at their end, for instance for the component
	 * ch.sourcepond.mdcwrapper.impl you would pass ch.sourcepond.mdcwrapper.
	 * 
	 * @param pExaminee
	 *            The name of the component to be tested
	 * @param pDependencyComponent
	 *            The names of the components which shall additionally be
	 *            installed into the test-container; can be empty.
	 * @return Option to be passed to the Pax-Exam test-container.
	 */
	public static Option defaultOptions(final String pExaminee, final String... pDependencyComponent) {
		return composite(examineeOption(pExaminee), dependenciesOption(pDependencyComponent),
				mavenBundle("com.google.guava", "guava").versionAsInProject(),
				mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
				mavenBundle("org.objenesis", "objenesis").versionAsInProject(),
				mavenBundle("org.mockito", "mockito-core").versionAsInProject(),
				frameworkProperty("felix.bootdelegation.implicit").value("false"), junitBundles(),

		// Override regression default; we use the logback.xml located
		// in this jar
				systemProperty("logback.configurationFile").value(""));
	}

	/**
	 * @param pDependencyComponent
	 * @return
	 */
	private static Option dependenciesOption(final String[] pDependencyComponent) {
		final Option[] option = new Option[pDependencyComponent.length];
		for (int i = 0; i < option.length; i++) {
			option[i] = componentOption(pDependencyComponent[i]);
		}
		return composite(option);
	}

	/**
	 * @param pComponentName
	 * @return
	 */
	private static Option examineeOption(final String pComponentName) {
		final String[] coordinates = coordinates(pComponentName);
		return composite(componentOption(pComponentName),
				wrappedBundle(maven(coordinates[0], coordinates[1] + "-impl").classifier("tests"))
						.imports("org.hamcrest.*;resolution:=optional,org.ops4j.pax.exam.*;resolution:=optional,*"));
	}

	/**
	 * @param pComponentName
	 * @return
	 */
	private static Option componentOption(final String pComponentName) {
		final String[] coordinates = coordinates(pComponentName);
		return composite(mavenBundle(coordinates[0], coordinates[1] + "-api").versionAsInProject(),
				mavenBundle(coordinates[0], coordinates[1] + "-impl").versionAsInProject());
	}

	/**
	 * @param pComponentName
	 * @return
	 */
	private static String[] coordinates(final String pComponentName) {
		final String[] coordinates = new String[2];
		coordinates[0] = pComponentName.substring(0, pComponentName.lastIndexOf('.'));
		coordinates[1] = pComponentName.substring(coordinates[0].length() + 1);
		return coordinates;
	}
}
