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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.mockito.Mockito;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.io.CaptureStandardOutput;

public class ProcesslauncherBuilderTest extends TestCase {

	private ProcesslauncherBuilder pb;
	private final File execFile;
	
	public ProcesslauncherBuilderTest() throws FileNotFoundException {
		execFile = new ExecutableFinder().get("test-exec");
	}
	
	@Override
	protected void setUp() throws Exception {
		pb = new ProcesslauncherBuilder(execFile, Arrays.asList("p"));
	}
	
	public void testGetSetEnvironmentVar() {
		pb.setEnvironmentVar("foo", "bar");
		Assert.assertEquals("bar", pb.getEnvironmentVar("foo"));
	}

	public void testGetSetEnvironmentVarWinPath() {
		if (System.getProperty("os.name").toLowerCase().indexOf("win") > -1) {
			pb.setEnvironmentVar("path", "foo");
			Assert.assertEquals("foo", pb.getEnvironmentVar("Path"));
			Assert.assertEquals("foo", pb.getEnvironmentVar("PATH"));
		}
	}

	public void testSetEnvironmentVarIfNotFound() {
		pb.setEnvironmentVarIfNotFound("foo", "bar");
		pb.setEnvironmentVarIfNotFound("foo", "tot");
		Assert.assertEquals("bar", pb.getEnvironmentVar("foo"));
	}
	
	public void testForEachEnvironmentVar() {
		pb.setEnvironmentVar("foo1", "bar1");
		pb.setEnvironmentVar("foo2", "bar2");

		final HashMap<String, String> val = new HashMap<>();
		pb.forEachEnvironmentVar((k, v) -> {
			val.put(k, v);
		});
		Assert.assertEquals("bar1", val.get("foo1"));
		Assert.assertEquals("bar2", val.get("foo2"));
	}
	
	public void testGetSetWorkingDirectory() throws IOException {
		Assert.assertTrue(pb.getWorkingDirectory().exists() && pb.getWorkingDirectory().isDirectory());
		pb.setWorkingDirectory(new File("."));
		Assert.assertEquals(new File("."), pb.getWorkingDirectory());
		
		try {
			pb.setWorkingDirectory(new File("./DontExists"));
			Assert.fail();
		} catch (final IOException e) {
		}
		try {
			pb.setWorkingDirectory(execFile);
			Assert.fail();
		} catch (final IOException e) {
		}
	}
	
	public void testSetIsExecCodeMustBeZero() {
		Assert.assertTrue(pb.isExecCodeMustBeZero());
		pb.setExecCodeMustBeZero(false);
		Assert.assertFalse(pb.isExecCodeMustBeZero());
	}
	
	public void testGetExecutionCallbackers() {
		Assert.assertEquals(0, pb.getExecutionCallbackers().size());
	}
	
	public void testAddExecutionCallbacker() {
		final ExecutionCallbacker executionCallbacker0 = Mockito.mock(ExecutionCallbacker.class);
		final ExecutionCallbacker executionCallbacker1 = Mockito.mock(ExecutionCallbacker.class);

		pb.addExecutionCallbacker(executionCallbacker0);
		pb.addExecutionCallbacker(executionCallbacker1);

		Assert.assertEquals(executionCallbacker0, pb.getExecutionCallbackers().get(0));
		Assert.assertEquals(executionCallbacker1, pb.getExecutionCallbackers().get(1));
	}
	
	public void testRemoveExecutionCallbacker() {
		final ExecutionCallbacker executionCallbacker = Mockito.mock(ExecutionCallbacker.class);
		pb.addExecutionCallbacker(executionCallbacker);
		pb.removeExecutionCallbacker(executionCallbacker);
		Assert.assertEquals(0, pb.getExecutionCallbackers().size());
	}
	
	public void testGetSetExecutionTimeLimiter() {
		Assert.assertFalse(pb.getExecutionTimeLimiter().isPresent());

		final ExecutionTimeLimiter executionTimeLimiter = Mockito.mock(ExecutionTimeLimiter.class);
		pb.setExecutionTimeLimiter(executionTimeLimiter);
		
		Assert.assertEquals(executionTimeLimiter, pb.getExecutionTimeLimiter().get());
	}
	
	public void testGetSetExternalProcessStartup() {
		Assert.assertFalse(pb.getExternalProcessStartup().isPresent());

		final ExternalProcessStartup externalProcessStartup = Mockito.mock(ExternalProcessStartup.class);
		pb.setExternalProcessStartup(externalProcessStartup);
		
		Assert.assertEquals(externalProcessStartup, pb.getExternalProcessStartup().get());
	}
	
	public void testSetGetCaptureStandardOutput() {
		Assert.assertFalse(pb.getCaptureStandardOutput().isPresent());

		final CaptureStandardOutput captureStandardOutput = Mockito.mock(CaptureStandardOutput.class);
		pb.setCaptureStandardOutput(captureStandardOutput);
		
		Assert.assertEquals(captureStandardOutput, pb.getCaptureStandardOutput().get());
	}
	
	public void testMakeProcessBuilder() {
		final ProcessBuilder processb = pb.makeProcessBuilder();
		Assert.assertEquals(pb.getFullCommandLine(), processb.command().stream().collect(Collectors.joining(" ")));
	}
	
	public void testGetFullCommandLine() {
		Assert.assertEquals(ProcesslauncherBuilder.addQuotesIfSpaces.apply(execFile.getAbsolutePath()) + " p", pb.getFullCommandLine());
	}

	public void testToString() {
		Assert.assertEquals(pb.getFullCommandLine(), pb.toString());
	}
	
}
