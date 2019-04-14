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
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 *
*/
package tv.hd3g.processlauncher.cmdline;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

public class ExecutableFinderTest extends TestCase {

	/**
	 * During some maven operation, on Linux, executable state can be drop.
	 */
	public static void patchTestExec() {
		if (File.separator.equals("\\")) {
			/**
			 * Test is running on windows, cancel this patch.
			 */
			return;
		}
		Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).map(p -> {
			return new File(p);
		}).filter(ExecutableFinder.isValidDirectory).flatMap(dir -> {
			return Arrays.stream(dir.listFiles());
		}).filter(subFile -> {
			return subFile.isFile() && subFile.canExecute() == false && subFile.getName().equals("test-exec");
		}).findFirst().ifPresent(f -> {
			System.out.println(f.getAbsolutePath() + " has not the executable bit, set it now.");
			f.setExecutable(true);
		});
	}

	public ExecutableFinderTest() {
		ExecutableFinderTest.patchTestExec();
	}

	public void testPreCheck() throws IOException {
		assertEquals("\\", "/".replaceAll("/", "\\\\"));
		assertEquals("/", "\\".replaceAll("\\\\", "/"));
	}

	public void test() throws IOException {
		final ExecutableFinder ef = new ExecutableFinder();

		assertTrue(ef.getFullPath().contains(new File(System.getProperty("user.dir"))));

		final File exec = ef.get("test-exec");
		if (File.separator.equals("/")) {
			assertEquals("test-exec", exec.getName());
		} else {
			assertEquals("test-exec.bat", exec.getName());
		}
	}

	public void testRegisterExecutable() throws IOException {
		ExecutableFinder ef = new ExecutableFinder();

		final File element = ef.get("test-exec");

		ef = new ExecutableFinder();
		ef.registerExecutable("other-test", element);

		assertEquals(element.getPath(), ef.get("other-test").getPath());

		ef.get("java");
		ef.registerExecutable("java", element);
		assertEquals(element.getPath(), ef.get("java").getPath());
	}
}
