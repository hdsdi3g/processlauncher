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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Deprecated
public class ExecutionCallbackerBulk implements ExecutionCallbacker { // TODO remove

	/**
	 * UnmodifiableList
	 */
	private final List<ExecutionCallbacker> callbackers;

	public ExecutionCallbackerBulk(final ExecutionCallbacker... callbackers) {
		this.callbackers = Arrays.stream(Objects.requireNonNull(callbackers, "callbackers can't to be null")).filter(c -> c != null).collect(Collectors.toUnmodifiableList());
	}

	public ExecutionCallbackerBulk(final Collection<ExecutionCallbacker> callbackers) {
		this.callbackers = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(callbackers, "callbackers can't to be null")));
	}
	
	@Override
	public void onEndExecution(final ProcesslauncherLifecycle processlauncherLifecycle) {
		callbackers.forEach(c -> c.onEndExecution(processlauncherLifecycle));
	}

	@Override
	public void postStartupExecution(final ProcesslauncherLifecycle processlauncherLifecycle) {
		callbackers.forEach(c -> c.postStartupExecution(processlauncherLifecycle));
	}

}
