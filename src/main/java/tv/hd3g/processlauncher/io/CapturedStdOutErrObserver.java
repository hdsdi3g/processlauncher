/*
 * This file is part of fflauncher.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 *
*/
package tv.hd3g.processlauncher.io;

import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

@FunctionalInterface
public interface CapturedStdOutErrObserver {
	
	void onText(ProcesslauncherLifecycle source, String line, boolean isStdErr);
	
	default void onStdout(final ProcesslauncherLifecycle source, final String line) {
		onText(source, line, false);
	}
	
	default void onStderr(final ProcesslauncherLifecycle source, final String line) {
		onText(source, line, true);
	}
	
	// TODO impl with retention + interactive
}