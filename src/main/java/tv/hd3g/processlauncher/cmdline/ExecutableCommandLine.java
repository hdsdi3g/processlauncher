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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
*/
package tv.hd3g.processlauncher.cmdline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutableCommandLine { // TODO test

	private final File executable;
	private final List<String> parameters;
	private final ExecutableFinder executableFinder;

	public ExecutableCommandLine(final File executable, final List<String> parameters) throws IOException {
		this.executable = executable;
		if (executable.isFile() == false | executable.exists() == false) {
			throw new FileNotFoundException("Can't found " + executable);
		} else if (executable.canExecute() == false) {
			throw new IOException("Can't execute " + executable);
		}
		this.parameters = parameters;
		if (parameters == null) {
			throw new NullPointerException("\"parameters\" can't to be null");
		}
		executableFinder = null;
	}

	public ExecutableCommandLine(final String execName, final List<String> parameters, final ExecutableFinder executableFinder) throws IOException {
		if (execName == null) {
			throw new NullPointerException("\"execName\" can't to be null");
		}
		this.parameters = parameters;
		if (parameters == null) {
			throw new NullPointerException("\"parameters\" can't to be null");
		}
		this.executableFinder = executableFinder;
		if (executableFinder != null) {
			executable = executableFinder.get(execName);
		} else {
			executable = new File(execName);
			if (executable.isFile() == false | executable.exists() == false) {
				throw new FileNotFoundException("Can't found " + executable);
			} else if (executable.canExecute() == false) {
				throw new IOException("Can't execute " + executable);
			}
		}
	}

	@Override
	public String toString() {
		return executable.getPath() + " " + parameters.stream().collect(Collectors.joining(" "));
	}
	
	/**
	 * @return can be null
	 */
	public ExecutableFinder getExecutableFinder() {
		return executableFinder;
	}

	public File getExecutable() {
		return executable;
	}

	public List<String> getParameters() {
		return parameters;
	}
}
