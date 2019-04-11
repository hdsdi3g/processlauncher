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
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.mockito.Mockito;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.io.CaptureStandardOutput;

public class ProcesslauncherTest extends TestCase {

	private final List<ExecutionCallbacker> executionCallbackers;
	private final ExecutionTimeLimiter executionTimeLimiter;
	private final CaptureStandardOutput captureStandardOutput;
	private final ExternalProcessStartup externalProcessStartup;
	private final ProcessBuilder processBuilder;
	private final String fullCommandLine;
	private final ProcesslauncherBuilder processlauncherBuilder;

	private final Processlauncher pl;

	public ProcesslauncherTest() {
		executionCallbackers = new ArrayList<>();
		executionTimeLimiter = Mockito.mock(ExecutionTimeLimiter.class);
		captureStandardOutput = Mockito.mock(CaptureStandardOutput.class);
		externalProcessStartup = Mockito.mock(ExternalProcessStartup.class);
		processBuilder = new ProcessBuilder("");
		fullCommandLine = "aaa bbb ccc";
		processlauncherBuilder = Mockito.mock(ProcesslauncherBuilder.class);

		Mockito.when(processlauncherBuilder.getExecutionCallbackers()).thenReturn(executionCallbackers);
		Mockito.when(processlauncherBuilder.getExecutionTimeLimiter()).thenReturn(Optional.of(executionTimeLimiter));
		Mockito.when(processlauncherBuilder.getCaptureStandardOutput()).thenReturn(Optional.of(captureStandardOutput));
		Mockito.when(processlauncherBuilder.getExternalProcessStartup()).thenReturn(Optional.of(externalProcessStartup));
		Mockito.when(processlauncherBuilder.makeProcessBuilder()).thenReturn(processBuilder);
		Mockito.when(processlauncherBuilder.getFullCommandLine()).thenReturn(fullCommandLine);
		
		pl = new Processlauncher(processlauncherBuilder);
	}
	
	public void testGetExecutionCallbackers() {
		Assert.assertTrue(CollectionUtils.isEqualCollection(executionCallbackers, pl.getExecutionCallbackers()));
	}

	public void testGetExecutionTimeLimiter() {
		Assert.assertEquals(executionTimeLimiter, pl.getExecutionTimeLimiter().get());
	}

	public void testGetCaptureStandardOutput() {
		Assert.assertEquals(captureStandardOutput, pl.getCaptureStandardOutput().get());
	}

	public void testGetExternalProcessStartup() {
		Assert.assertEquals(externalProcessStartup, pl.getExternalProcessStartup().get());
	}

	public void testIsExecCodeMustBeZero() {
		Assert.assertFalse(pl.isExecCodeMustBeZero());
	}

	public void testGetProcessBuilder() {
		Assert.assertEquals(processBuilder, pl.getProcessBuilder());
	}

	public void testGetFullCommandLine() {
		Assert.assertEquals(fullCommandLine, pl.getFullCommandLine());
	}

	public void testGetProcesslauncherBuilder() {
		Assert.assertEquals(processlauncherBuilder, pl.getProcesslauncherBuilder());
	}
	
	public void testToString() {
		Assert.assertEquals(fullCommandLine, pl.toString());
	}
}
