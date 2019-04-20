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
package tv.hd3g.processlauncher;

public class InvalidExecution extends RuntimeException {

	// private final String fullCommandLine;
	// private final EndStatus endStatus;
	// private final int exitCode;
	private String stdErr;

	InvalidExecution(final ProcesslauncherLifecycle processlauncherLifecycle) {
		super("Can't execute correcly " + processlauncherLifecycle.getFullCommandLine() + "; " + processlauncherLifecycle.getEndStatus() + " [" + processlauncherLifecycle.getExitCode() + "]");
		// fullCommandLine = processlauncherLifecycle.getFullCommandLine();
		// endStatus = processlauncherLifecycle.getEndStatus();
		// exitCode = processlauncherLifecycle.getExitCode();
	}

	public InvalidExecution setStdErr(final String stdErr) {
		this.stdErr = stdErr;
		return this;
	}

	public String getStdErr() {
		return stdErr;
	}
}
