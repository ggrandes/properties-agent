package org.javastack.agents;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * If you do manual packaging:
 * 
 * Add the following to your MANIFEST.MF:
 * 
 * Premain-Class: org.javastack.agents.PropertiesAgent
 * 
 * Create Jar:
 * 
 * jar -cvfm bootstrap.jar MANIFEST.MF <classfiles>
 * 
 * Execute:
 * 
 * java -javaagent:properties-agent.jar=[!]url[,[!]url] ...
 * 
 * Examples:
 * 
 * java -javaagent:properties-agent.jar=https://server/config/system-properties.properties ...
 * java -javaagent:properties-agent.jar=!file:///etc/system-properties.properties ...
 * 
 */
public class PropertiesAgent {
	private static final Logger log = Logger.getLogger(PropertiesAgent.class.getName());
	private static final String javaTmpDir = System.getProperty("java.io.tmpdir");
	private static final String FILE_SEPARATOR = ",";
	private static final String FORCE_OVERWRITE_PROPERTY = "!";

	/**
	 * Load Java Agent on premain
	 * 
	 * @param agentArgs
	 * @param inst
	 * @throws Throwable
	 */
	public static void premain(final String agentArgs, final Instrumentation inst) throws Throwable {
		internal("premain", agentArgs, inst);
	}

	/**
	 * Load Java Agent after JVM Statup
	 * 
	 * @param agentArgs
	 * @param inst
	 * @throws Throwable
	 */
	public static void agentmain(final String agentArgs, final Instrumentation inst) throws Throwable {
		internal("agentmain", agentArgs, inst);
	}

	private static void internal(final String entry, final String agentArgs, final Instrumentation inst)
			throws Throwable {
		if ((agentArgs != null) && !agentArgs.isEmpty()) {
			final String toks[] = agentArgs.split(FILE_SEPARATOR);
			for (final String tok : toks) {
				final boolean force = tok.startsWith(FORCE_OVERWRITE_PROPERTY);
				final String file = (force ? tok.substring(FORCE_OVERWRITE_PROPERTY.length()) : tok);
				setSystemPropertiesFromURL(file, force);
			}
		}
	}

	/**
	 * Load system properties from specified agent url to proterties file
	 * 
	 * @throws IOException
	 */
	public static void setSystemPropertiesFromURL(final String sourceFile, final boolean forceLoad) {
		if ((sourceFile == null) || sourceFile.isEmpty()) {
			return;
		}
		final String fileName = sourceFile.replaceAll("[^a-zA-Z0-9-]", "_") + ".cache";
		final String cacheBaseName = new File(javaTmpDir, fileName).getAbsolutePath();
		final File cache = new File(cacheBaseName);
		final File cacheNew = new File(cacheBaseName + ".new");
		InputStream is = null;
		OutputStream os = null;
		try {
			if (sourceFile.startsWith("http:") || sourceFile.startsWith("https:")
					|| sourceFile.startsWith("file:")) {
				try {
					final URL url = new URL(sourceFile);
					if (log.isLoggable(Level.FINE)) {
						log.fine("Loading data from url=" + url);
					}
					is = url.openStream();
				} catch (Throwable t) {
					log.log(Level.SEVERE, "Unable to open url=" + sourceFile, t);
					handleThrowable(t);
				}
			}
			if (is == null) {
				try {
					if (log.isLoggable(Level.FINE)) {
						log.fine("Loading data from url=" + sourceFile);
					}
					is = new FileInputStream(sourceFile);
				} catch (Throwable t) {
					log.log(Level.SEVERE, "Unable to open file=" + sourceFile, t);
					handleThrowable(t);
				}
			}
			if (is != null) {
				try {
					os = new BufferedOutputStream(new FileOutputStream(cacheNew, false), 32);
					int len = 0;
					byte[] buf = new byte[512];
					os.write("# BEGIN # ".getBytes());
					os.write(String.valueOf(new Date()).getBytes());
					os.write('\n');
					while ((len = is.read(buf)) != -1) {
						os.write(buf, 0, len);
					}
					os.write('\n');
					os.write("# END #".getBytes());
					os.write('\n');
					os.flush();
					closeQuiet(os);
					os = null;
					cache.delete();
					cacheNew.renameTo(cache);
				} catch (Throwable t) {
					log.log(Level.SEVERE, "Unable to create cache file=" + cache, t);
					handleThrowable(t);
				}
			}
		} finally {
			closeQuiet(os);
			closeQuiet(is);
		}
		final Properties p = new Properties();
		try {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Reading properties from Cache file=" + cache);
			}
			is = new FileInputStream(cache);
			p.load(is);
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Unable to load file=" + cache, t);
			handleThrowable(t);
		} finally {
			closeQuiet(is);
		}
		for (final String propName : p.stringPropertyNames()) {
			final String value = p.getProperty(propName);
			if (forceLoad || (System.getProperty(propName) == null)) {
				if (log.isLoggable(Level.FINE)) {
					log.fine("Setting" + (forceLoad ? " (forced)" : "") + " property name=" + propName);
				}
				System.setProperty(propName, value);
			}
		}
	}

	private static final void closeQuiet(final Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Exception ign) {
			}
		}
	}

	// Copied from ExceptionUtils since that class is not visible during start
	private static void handleThrowable(final Throwable t) {
		if (t instanceof ThreadDeath) {
			throw (ThreadDeath) t;
		}
		if (t instanceof VirtualMachineError) {
			throw (VirtualMachineError) t;
		}
		// All other instances of Throwable will be silently swallowed
	}
}
