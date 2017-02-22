package ch.sourcepond.testing;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static java.lang.Thread.currentThread;
import static org.junit.Assert.*;

/**
 * Rule which sets the classloader of the the test-bundle as context classloader.
 * After evaluation, the original classloader is being restored.
 */
public class BundleContextClassLoaderRule implements TestRule {
    private final Object test;

    public BundleContextClassLoaderRule(final Object pTest) {
        assertNotNull("Test is null", pTest);
        test = pTest;
    }

    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final ClassLoader origin = currentThread().getContextClassLoader();
                currentThread().setContextClassLoader(test.getClass().getClassLoader());
                try {
                    base.evaluate();
                } finally {
                    currentThread().setContextClassLoader(origin);
                }
            }
        };
    }
}
