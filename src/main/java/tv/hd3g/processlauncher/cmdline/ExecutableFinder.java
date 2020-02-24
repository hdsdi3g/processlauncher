/*
 * This file is part of processlauncher.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 *
 */
package tv.hd3g.processlauncher.cmdline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * It will resolve/find valid executable files in *NIX and valid executable extensions in Windows.
 * On system PATH, classpath, current dir, and local user dir (/bin).
 */
public class ExecutableFinder {
	private static final Logger log = LogManager.getLogger();

	/**
	 * Return exists and isDirectory and canRead
	 */
	public static final Predicate<File> isValidDirectory = f -> {
		return f.exists() && f.isDirectory() && f.canRead();
	};

	/**
	 * unmodifiableList
	 */
	public static final List<String> WINDOWS_EXEC_EXTENSIONS;

	/**
	 * unmodifiableList
	 * Specified by -Dexecfinder.searchdir=path1;path2... or path1:path2... on *Nix systems.
	 */
	public static final List<File> GLOBAL_DECLARED_DIRS;

	// public static final Map<String, File> GLOBAL_DECLARED_EXECUTABLES;

	static {
		if (System.getenv().containsKey("PATHEXT")) {
			/**
			 * Like .COM;.EXE;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;.MSC
			 */
			final String pathExt = System.getenv("PATHEXT");
			if (pathExt.indexOf(";") > 0) {
				WINDOWS_EXEC_EXTENSIONS = Collections.unmodifiableList(Arrays.stream(pathExt.split(";")).map(ext -> ext
				        .toLowerCase().substring(1)).collect(Collectors.toUnmodifiableList()));
			} else {
				log.warn("Invalid PATHEXT env.: " + pathExt);
				WINDOWS_EXEC_EXTENSIONS = Collections.unmodifiableList(Arrays.asList("exe", "com", "cmd", "bat"));
			}
		} else {
			WINDOWS_EXEC_EXTENSIONS = Collections.unmodifiableList(Arrays.asList("exe", "com", "cmd", "bat"));
		}

		if (System.getProperty("execfinder.searchdir", "").equals("") == false) {
			GLOBAL_DECLARED_DIRS = Collections.unmodifiableList(Arrays.stream(System.getProperty("execfinder.searchdir")
			        .split(File.pathSeparator)).map(File::new).filter(isValidDirectory).map(File::getAbsoluteFile)
			        .collect(Collectors.toList()));

			log.debug("Specific executable path declared via system property: " + GLOBAL_DECLARED_DIRS.stream().map(
			        File::getPath).collect(Collectors.joining(", ")));
		} else {
			GLOBAL_DECLARED_DIRS = Collections.emptyList();
		}

		/*if (System.getProperty("execfinder.exec", "").equals("") == false) {
			// GLOBAL_DECLARED_EXECUTABLES = Collections.unmodifiableList(Arrays.stream(System.getProperty("execfinder.searchdir").split(File.pathSeparator)).map(File::new).filter(isValidDirectory).map(File::getAbsoluteFile).collect(Collectors.toList()));
		
			// log.debug("Specific executable path declared via system property: " + GLOBAL_DECLARED_DIRS.stream().map(File::getPath).collect(Collectors.joining(", ")));
		} else {
			GLOBAL_DECLARED_EXECUTABLES = Collections.emptyMap();
		}*/

	}

	/**
	 * synchronizedList
	 */
	private final LinkedList<File> paths;
	private final LinkedHashMap<String, File> declaredInConfiguration;
	private final boolean isWindowsStylePath;

	public ExecutableFinder() {
		declaredInConfiguration = new LinkedHashMap<>();
		isWindowsStylePath = File.separator.equals("\\");// System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;

		/**
		 * Adds only valid dirs
		 */
		paths = new LinkedList<>(GLOBAL_DECLARED_DIRS);

		addLocalPath("/bin");
		addLocalPath("/App/bin");

		paths.add(new File(System.getProperty("user.dir")));

		paths.addAll(Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).map(p -> {
			return new File(p);
		}).filter(isValidDirectory).collect(Collectors.toUnmodifiableList()));

		paths.addAll(Arrays.stream(System.getenv("PATH").split(File.pathSeparator)).map(p -> new File(p)).filter(
		        isValidDirectory).collect(Collectors.toUnmodifiableList()));

		/**
		 * Remove duplicate entries
		 */
		final List<File> newList = paths.stream().distinct().collect(Collectors.toUnmodifiableList());
		paths.clear();
		paths.addAll(newList);

		if (log.isTraceEnabled()) {
			log.trace("Full path: " + paths.stream().map(f -> f.getPath()).collect(Collectors.joining(
			        File.pathSeparator)));
		}
	}

	public String getFullPathToString() {
		return paths.stream().map(f -> f.getPath()).reduce((BinaryOperator<String>) (left, right) -> {
			return left + File.pathSeparator + right;
		}).get();
	}

	/**
	 * @return unmodifiableList
	 */
	public List<File> getFullPath() {
		return Collections.unmodifiableList(paths);
	}

	/**
	 * Put in top priority.
	 * Path / or \ will be corrected
	 */
	public ExecutableFinder addLocalPath(final String relativeUserHomePath) {
		if (isWindowsStylePath) {
			relativeUserHomePath.replaceAll("/", "\\\\");
		} else {
			relativeUserHomePath.replaceAll("\\\\", "/");
		}

		final String userHome = System.getProperty("user.home");
		final File f = new File(userHome + File.separator + relativeUserHomePath).getAbsoluteFile();

		return addPath(f);
	}

	/**
	 * Put in top priority.
	 */
	public ExecutableFinder addPath(final File filePath) {
		final File f = filePath.getAbsoluteFile();

		if (isValidDirectory.test(f)) {
			synchronized (this) {
				log.debug("Register path: " + f.getPath());
				paths.addFirst(f);
			}
		}
		return this;
	}

	private boolean validExec(final File exec) {
		if (exec.exists() == false) {
			return false;
		} else if (exec.isFile() == false) {
			return false;
		} else if (exec.canRead() == false) {
			return false;
		} else {
			return exec.canExecute();
		}
	}

	public ExecutableFinder registerExecutable(final String name, final File fullPath) throws IOException {
		if (validExec(fullPath) == false) {
			throw new IOException("Invalid declaredInConfiguration executable: " + name
			                      + " can't be correctly found in " + fullPath);
		}
		declaredInConfiguration.put(name, fullPath);
		return this;
	}

	/**
	 * Can add .exe to name if OS == Windows and if it's missing.
	 * @param name can be a simple exec name, or a full path.
	 * @return never null
	 * @throws FileNotFoundException if exec don't exists or is not correctly registed.
	 */
	public File get(final String name) throws FileNotFoundException {
		if (declaredInConfiguration.containsKey(name)) {
			return declaredInConfiguration.get(name);
		}

		final File exec = new File(name);
		if (validExec(exec)) {
			return exec;
		}

		final List<File> allFileCandidates = Stream.concat(declaredInConfiguration.values().stream().map(file -> {
			return file.getParentFile();
		}), paths.stream()).map(dir -> {
			return new File(dir + File.separator + name).getAbsoluteFile();
		}).distinct().collect(Collectors.toUnmodifiableList());

		if (isWindowsStylePath == false) {
			/**
			 * *nix flavors
			 */
			return allFileCandidates.stream().filter(file -> {
				return validExec(file);
			}).findFirst().orElseThrow(() -> new FileNotFoundException("Can't found executable \"" + name + "\""));
		} else {
			/**
			 * Windows flavor
			 * Try with add windows ext
			 */
			return allFileCandidates.stream().flatMap(file -> {
				final boolean hasAlreadyValidExt = WINDOWS_EXEC_EXTENSIONS.stream().anyMatch(ext -> {
					return file.getName().toLowerCase().endsWith("." + ext.toLowerCase());
				});

				if (hasAlreadyValidExt) {
					if (validExec(file)) {
						return Stream.of(file);
					} else {
						return Stream.empty();
					}
				} else {
					/**
					 * We must to add ext, we try with all avaliable ext.
					 */
					return WINDOWS_EXEC_EXTENSIONS.stream().flatMap(ext -> {
					    /**
					     * Try with lower/upper case extensions.
					     */
					    return Stream.of(new File(file + "." + ext.toLowerCase()), new File(file + "." + ext
					            .toUpperCase()));
					}).filter(fileExt -> {
						return validExec(fileExt);
					});
				}
			}).findFirst().orElseThrow(() -> new FileNotFoundException("Can't found executable \"" + name + "\""));
		}
	}

}
