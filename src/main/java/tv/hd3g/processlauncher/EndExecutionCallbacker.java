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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EndExecutionCallbacker {
	private static Logger log = LogManager.getLogger();
	
	// private final ArrayList<EndExecutionCallback<?>> end_exec_callback_list;

	public EndExecutionCallbacker() {
		// end_exec_callback_list = new ArrayList<>(1);
		// TODO Auto-generated constructor stub
	}
	
	/*public <T extends ExecProcessResult> ExecProcess addEndExecutionCallback(final Consumer<T> onEnd, final Executor executor) {
		end_exec_callback_list.add(new EndExecutionCallback<>(onEnd, executor));
		return this;
	}*/

	/*
	class EndExecutionCallback<T extends ExecProcessResult> {

	private final Consumer<T> onEnd;
	protected final Executor executor;

	EndExecutionCallback(Consumer<T> onEnd, Executor executor) {
		this.onEnd = onEnd;
		if (onEnd == null) {
			throw new NullPointerException("\"onEnd\" can't to be null");
		}
		this.executor = executor;
		if (executor == null) {
			throw new NullPointerException("\"executor\" can't to be null");
		}
	}

	 * Async
	@SuppressWarnings("unchecked")
	void onEnd(ExecProcessResult source) {
		executor.execute(() -> {
			onEnd.accept((T) source);
		});
	}
	
	}
	 */
}
