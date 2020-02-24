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
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.processlauncher.io;

import java.io.InputStream;

import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

public interface CaptureStandardOutput {

	/**
	 * Called one time juste after process starts.
	 * @param processInputStream consuming should be in another and dedicated thread.
	 */
	void stdOutStreamConsumer(InputStream processInputStream, ProcesslauncherLifecycle source);

	/**
	 * Called one time juste after process starts.
	 * @param processInputStream consuming should be in another and dedicated thread.
	 */
	void stdErrStreamConsumer(InputStream processInputStream, ProcesslauncherLifecycle source);

}
