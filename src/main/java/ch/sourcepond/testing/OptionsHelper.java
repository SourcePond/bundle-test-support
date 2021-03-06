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

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafFeaturesOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.newDirectoryStream;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

/**
 * Small helper class for Pax-Exam test configuration.
 */
public class OptionsHelper {

    public static Option karafContainer(final KarafFeaturesOption... pFeaturesToBeInstalled) {
        return karafContainer(getDefault().getPath(getProperty("user.dir"), "target", "exam"), pFeaturesToBeInstalled);
    }

    public static Option karafContainer(final Path pUnpackDir, final KarafFeaturesOption... pFeaturesToBeInstalled) {
        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.apache.karaf")
                .artifactId("apache-karaf").versionAsInProject()
                .type("tar.gz");

        return composite(
                // KarafDistributionOption.debugConfiguration("5005", true),
                karafDistributionConfiguration()
                        .frameworkUrl(karafUrl)
                        .unpackDirectory(pUnpackDir.toFile())
                        .useDeployFolder(false).runEmbedded(true),
                logLevel(LogLevelOption.LogLevel.INFO),
                features(maven()
                        .groupId("org.apache.karaf.features")
                        .artifactId("standard")
                        .classifier("features")
                        .type("xml")
                        .versionAsInProject(), "scr", "config"),
                composite(pFeaturesToBeInstalled),
                keepRuntimeFolder()
        );
    }

    public static Option provisionBundlesFromUserDir(final String... pPath) {
        final List<String> urls = new LinkedList<>();
        Path dir = getDefault().getPath(getProperty("user.dir"));

        for (final String pathElement : pPath) {
            dir = dir.resolve(pathElement);
        }

        try (final DirectoryStream<Path> stream = newDirectoryStream(dir, entry -> entry.getFileName().toString().endsWith(".jar"))) {
            for (final Path jar : stream) {
                urls.add(jar.toUri().toURL().toString());
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return provision(urls.toArray(new String[0]));
    }

    /**
     * Creates the default options necessary to test a bundle component in an
     * OSGi environment. All SourcePond components are split into an API
     * (example artifactId ch.sourcepond.componentname.api) and implementations
     * (example artifactId ch.sourcepond.componentname.impl). The arguments
     * passed to this method are always component names without *.api or *.impl
     * at their end, for instance for the component
     * ch.sourcepond.mdcwrapper.impl you would pass ch.sourcepond.mdcwrapper.
     *
     * @param pExaminee            The name of the component to be tested
     * @param pDependencyComponent The names of the components which shall additionally be
     *                             installed into the test-container; can be empty.
     * @return Option to be passed to the Pax-Exam test-container.
     */
    public static Option defaultOptions(final String pExaminee, final String... pDependencyComponent) {
        return composite(examineeOption(pExaminee), dependenciesOption(pDependencyComponent),
                mavenBundle("com.google.guava", "guava").versionAsInProject(),
                mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject(),
                mavenBundle("org.objenesis", "objenesis").versionAsInProject(),
                mavenBundle("org.mockito", "mockito-core").versionAsInProject(),
                mavenBundle("net.bytebuddy", "byte-buddy").versionAsInProject(), // Necessary
                // for
                // Mockito
                frameworkProperty("felix.bootdelegation.implicit").value("false"), junitBundles(),

                // Override regression default; we use the logback.xml located
                // in this jar
                systemProperty("logback.configurationFile").value(""));
    }

    public static Option mockitoBundles() {
        return composite(mavenBundle().groupId("org.mockito").artifactId("mockito-core").versionAsInProject(),
                mavenBundle().groupId("net.bytebuddy").artifactId("byte-buddy").versionAsInProject(),
                mavenBundle().groupId("net.bytebuddy").artifactId("byte-buddy-agent").versionAsInProject(),
                mavenBundle().groupId("org.objenesis").artifactId("objenesis").versionAsInProject());
    }

    public static Option tinyBundles() {
        return composite(mavenBundle().groupId("org.ops4j.pax.tinybundles").
                        artifactId("tinybundles").versionAsInProject(),
                mavenBundle().groupId("biz.aQute.bnd").artifactId("biz.aQute.bndlib").versionAsInProject());
    }

    /**
     * Create a composite option which references all necessary bundles to run a
     * Blueprint extender.
     *
     * @return Option, never {@code null}
     */
    public static Option blueprintBundles() {
        return composite(mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api").versionAsInProject(),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.impl").versionAsInProject(),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core").versionAsInProject());
    }

    /**
     * Creates a service stubber.
     *
     * @param pServiceInterface The service interface to be stubbed.
     * @return Service-Stubber, never {@code null}
     */
    public static <T> ServiceStubber<T> stubService(final Class<T> pServiceInterface) {
        return new ServiceStubber<>(pServiceInterface);
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
                wrappedBundle(maven(coordinates[0], coordinates[1] + "-impl").classifier("tests")));
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
