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
package tv.hd3g.processlauncher.cmdline;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ParametersTest extends TestCase {

	public void test() {
		Parameters clp = new Parameters();

		assertEquals("<%", clp.getStartVarTag());
		assertEquals("%>", clp.getEndVarTag());
		assertTrue(clp.isTaggedParameter("<%ok%>"));
		assertFalse(clp.isTaggedParameter("<%nope"));
		assertFalse(clp.isTaggedParameter("nope%>"));
		assertFalse(clp.isTaggedParameter("<nope>"));
		assertFalse(clp.isTaggedParameter("nope"));
		assertFalse(clp.isTaggedParameter("%>nope<%"));
		assertFalse(clp.isTaggedParameter("<%nope %>"));
		assertEquals("my_var", clp.extractVarNameFromTaggedParameter("<%my_var%>"));

		clp = new Parameters().setVarTags("{", "}");
		assertEquals("{", clp.getStartVarTag());
		assertEquals("}", clp.getEndVarTag());
		assertTrue(clp.isTaggedParameter("{ok}"));
		assertFalse(clp.isTaggedParameter("{ok }"));
		assertFalse(clp.isTaggedParameter("{nope"));
		assertFalse(clp.isTaggedParameter("nope}"));
		assertFalse(clp.isTaggedParameter("nope"));
		assertFalse(clp.isTaggedParameter("}nope{"));
		assertEquals("my_var", clp.extractVarNameFromTaggedParameter("{my_var}"));

		clp = new Parameters();
		assertNull(clp.extractVarNameFromTaggedParameter("<%%>"));
		assertNull(clp.extractVarNameFromTaggedParameter("<%"));
		assertNull(clp.extractVarNameFromTaggedParameter("%>"));
		assertNull(clp.extractVarNameFromTaggedParameter("nope"));
	}

	public void testInjectVarKeepEmptyParam() {
		final Parameters p = new Parameters("-a <%var1%> <%var2%> <%varNOPE%> -b <%varNOPE%> -c");
		final HashMap<String, String> vars = new HashMap<>();
		vars.put("var1", "value1");
		vars.put("var2", "value2");
		p.injectVariables(vars, false);

		Assert.assertEquals("-a value1 value2 -b -c", p.toString());
	}

	public void testRemoveVarsKeepEmptyParam() {
		final Parameters p = new Parameters("-a <%var1%> <%var2%> <%varNOPE%> -b <%varNOPE%> -c");
		p.removeVariables(false);

		Assert.assertEquals("-a -b -c", p.toString());
	}

	public void testInjectVarRemoveEmptyParam() {
		final Parameters p = new Parameters("-a <%var1%> <%var2%> <%varNOPE%> -b <%varNOPE%> -c");
		final HashMap<String, String> vars = new HashMap<>();
		vars.put("var1", "value1");
		vars.put("var2", "value2");
		p.injectVariables(vars, true);

		Assert.assertEquals("-a value1 value2 -c", p.toString());
	}

	public void testRemoveVarsRemoveEmptyParam() {
		final Parameters p = new Parameters("-a <%var1%> <%var2%> <%varNOPE%> -b <%varNOPE%> -c");
		p.removeVariables(true);
		Assert.assertEquals("-c", p.toString());
	}

	public void testInjectParamsAroundVariable() throws IOException {
		Parameters p = new Parameters("-before <%myvar%> -after");

		p.injectParamsAroundVariable("myvar", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		Assert.assertEquals("-before -addedbefore 1 <%myvar%> -addedafter 2 -after", p.toString());

		p = new Parameters("-before <%myvar%> <%myvar%> -after");
		p.injectParamsAroundVariable("myvar", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		Assert.assertEquals(
		        "-before -addedbefore 1 <%myvar%> -addedafter 2 -addedbefore 1 <%myvar%> -addedafter 2 -after", p
		                .toString());

		p = new Parameters("-before <%myvar1%> <%myvar2%> -after");
		p.injectParamsAroundVariable("myvar1", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		p.injectParamsAroundVariable("myvar2", Arrays.asList("-addedbefore", "3"), Arrays.asList("-addedafter", "4"));
		Assert.assertEquals(
		        "-before -addedbefore 1 <%myvar1%> -addedafter 2 -addedbefore 3 <%myvar2%> -addedafter 4 -after", p
		                .toString());
	}

}
