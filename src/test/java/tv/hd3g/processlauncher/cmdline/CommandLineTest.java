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

import java.util.Arrays;
import java.util.HashMap;

import junit.framework.TestCase;

public class CommandLineTest extends TestCase {
	
	public void testInjectVar() {
		final CommandLine cmd = new CommandLine("exec -a <%var1%> <%var2%> <%varNOPE%> -b");
		assertEquals("exec", cmd.getExecName());
		
		final HashMap<String, String> vars = new HashMap<>();
		vars.put("var1", "value1");
		vars.put("var2", "value2");

		assertTrue(Arrays.equals(Arrays.asList("exec", "-a", "value1", "value2", "-b").toArray(), cmd.getParametersInjectVars(vars, true).toArray()));
		
		assertTrue(Arrays.equals(Arrays.asList("exec", "-a").toArray(), new CommandLine("exec -a <%varNOPE%>").getParametersRemoveVars(false).toArray()));
		assertTrue(Arrays.equals(Arrays.asList("exec", "-b").toArray(), new CommandLine("exec -a <%varNOPE%> -b").getParametersInjectVars(new HashMap<>(), true).toArray()));
	}
	
	/* TODO not here
	public void testExecProcess() throws IOException {
		final CommandLine pcl = new CommandLine("java -a 1 -b 2");
	
		final ExecProcess ep1 = new ExecProcess(pcl, ExecProcessTest.executable_finder);
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b", "2").toArray(), ep1.getParameters().toArray()));
	
		final ExecProcessText ep2 = new ExecProcessText(pcl, ExecProcessTest.executable_finder);
		assertTrue(Arrays.equals(Arrays.asList("-a", "1", "-b", "2").toArray(), ep2.getParameters().toArray()));
	}
	
	public void testInjectParamsAroundVariable() throws IOException {
		CommandLine cl = new CommandLineProcessor().createCommandLine("exec -before <%myvar%> -after");
		cl.injectParamsAroundVariable("myvar", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		assertEquals("exec -before -addedbefore 1 <%myvar%> -addedafter 2 -after", cl.toString());
	
		cl = new CommandLineProcessor().createCommandLine("exec -before <%myvar%> <%myvar%> -after");
		cl.injectParamsAroundVariable("myvar", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		assertEquals("exec -before -addedbefore 1 <%myvar%> -addedafter 2 -addedbefore 1 <%myvar%> -addedafter 2 -after", cl.toString());
	
		cl = new CommandLineProcessor().createCommandLine("exec -before <%myvar1%> <%myvar2%> -after");
		cl.injectParamsAroundVariable("myvar1", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		cl.injectParamsAroundVariable("myvar2", Arrays.asList("-addedbefore", "3"), Arrays.asList("-addedafter", "4"));
		assertEquals("exec -before -addedbefore 1 <%myvar1%> -addedafter 2 -addedbefore 3 <%myvar2%> -addedafter 4 -after", cl.toString());
	}
	*/
}
