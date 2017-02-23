package ch.sourcepond.testing;

import java.io.*;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by rolandhauser on 23.02.17.
 */
public class BundleWriter {
    private static final File BUNDLES_FILE = new File(System.getProperty("java.io.tmpdir"), "ch.sourcepond.testing.bundles.properties");
    private static Properties PROPS = loadProperties();

    private static Properties loadProperties() {
        try {
            if (!BUNDLES_FILE.exists()) {
                BUNDLES_FILE.createNewFile();
                BUNDLES_FILE.deleteOnExit();
            }

            final Properties props = new Properties();
            try (final InputStream in = new FileInputStream(BUNDLES_FILE)) {
                props.load(in);
            }
            return props;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeBundle(final String pKey, final InputStream pIn) {
        try {
            File f = File.createTempFile(UUID.randomUUID().toString(), ".jar");
            f.deleteOnExit();
            try (final OutputStream out = new FileOutputStream(f)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = pIn.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            PROPS.setProperty(pKey, f.toURI().toURL().toString());
            try (final OutputStream out = new FileOutputStream(BUNDLES_FILE)) {
                PROPS.store(out, null);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String getLocation(final String pKey) {
        final String location = PROPS.getProperty(pKey);
        assertNotNull(String.format("No bundle writen for key %s", pKey), location);
        return location;
    }
}
