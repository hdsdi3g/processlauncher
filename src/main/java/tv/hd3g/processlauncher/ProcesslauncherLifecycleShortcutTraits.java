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

import java.util.Optional;

public interface ProcesslauncherLifecycleShortcutTraits {

	Process getProcess();

	EndStatus getEndStatus();

	default boolean isCorrectlyDone() {
		return getEndStatus().equals(EndStatus.CORRECTLY_DONE);
	}

	default Integer getExitCode() {
		return getProcess().exitValue();
	}

	/**
	 * on Windows, return like "HOST_or_DOMAIN"\"username"
	 */
	default Optional<String> getUserExec() {
		return getProcess().info().user();
	}

	default Optional<Long> getPID() {
		try {
			return Optional.of(getProcess().pid());
		} catch (final UnsupportedOperationException e) {
			return Optional.empty();
		}
	}

	default Boolean isRunning() {
		return getProcess().isAlive();
	}

}
