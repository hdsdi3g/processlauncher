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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.collections4.CollectionUtils;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CommandLineTest extends TestCase {

	private final ExecutableFinder ef;
	private final CommandLine cmd;

	public CommandLineTest() throws IOException {
		ef = new ExecutableFinder();
		cmd = new CommandLine("test-exec", "-a <%var1%> <%var2%> <%varNOPE%> -b <%varNOPE%> -c", ef);
	}

	public void testInjectVarKeepEmptyParam() {
		final HashMap<String, String> vars = new HashMap<>();
		vars.put("var1", "value1");
		vars.put("var2", "value2");

		Assert.assertTrue(CollectionUtils.isEqualCollection(Arrays.asList("-a", "value1", "value2", "-b", "-c"), cmd.getParametersInjectVars(vars, false)));
	}

	public void testRemoveVarsKeepEmptyParam() {
		Assert.assertTrue(CollectionUtils.isEqualCollection(Arrays.asList("-a", "-b", "-c"), cmd.getParametersRemoveVars(false)));
	}

	public void testInjectVarRemoveEmptyParam() {
		final HashMap<String, String> vars = new HashMap<>();
		vars.put("var1", "value1");
		vars.put("var2", "value2");
		Assert.assertTrue(CollectionUtils.isEqualCollection(Arrays.asList("-a", "value1", "value2", "-c"), cmd.getParametersInjectVars(vars, true)));
	}

	public void testRemoveVarsRemoveEmptyParam() {
		Assert.assertTrue(CollectionUtils.isEqualCollection(Arrays.asList("-c"), cmd.getParametersRemoveVars(true)));
	}

	public void testGetExecutableFinder() {
		Assert.assertEquals(ef, cmd.getExecutableFinder().get());
	}

	public void testGetExecutable() throws FileNotFoundException {
		Assert.assertEquals(ef.get("test-exec"), cmd.getExecutable());
	}

	public void testInjectParamsAroundVariable() throws IOException {
		CommandLine cl = new CommandLine("test-exec", "-before <%myvar%> -after", ef);

		cl.injectParamsAroundVariable("myvar", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		Assert.assertEquals("-before -addedbefore 1 <%myvar%> -addedafter 2 -after", cl.getParametersToString());

		cl = new CommandLine("test-exec", "-before <%myvar%> <%myvar%> -after", ef);
		cl.injectParamsAroundVariable("myvar", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		Assert.assertEquals("-before -addedbefore 1 <%myvar%> -addedafter 2 -addedbefore 1 <%myvar%> -addedafter 2 -after", cl.getParametersToString());

		cl = new CommandLine("test-exec", "-before <%myvar1%> <%myvar2%> -after", ef);
		cl.injectParamsAroundVariable("myvar1", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		cl.injectParamsAroundVariable("myvar2", Arrays.asList("-addedbefore", "3"), Arrays.asList("-addedafter", "4"));
		Assert.assertEquals("-before -addedbefore 1 <%myvar1%> -addedafter 2 -addedbefore 3 <%myvar2%> -addedafter 4 -after", cl.getParametersToString());
	}
}
