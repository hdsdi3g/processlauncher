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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

class ParametersTest {

	@Test
	void test() {
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

	@Test
	void testInjectVarKeepEmptyParam() {
		final Parameters p = new Parameters("-a <%var1%> <%var2%> <%varNOPE%> -b <%varNOPE%> -c");
		final HashMap<String, String> vars = new HashMap<>();
		vars.put("var1", "value1");
		vars.put("var2", "value2");
		p.injectVariables(vars, false);

		assertEquals("-a value1 value2 -b -c", p.toString());
	}

	@Test
	void testRemoveVarsKeepEmptyParam() {
		final Parameters p = new Parameters("-a <%var1%> <%var2%> <%varNOPE%> -b <%varNOPE%> -c");
		p.removeVariables(false);

		assertEquals("-a -b -c", p.toString());
	}

	@Test
	void testInjectVarRemoveEmptyParam() {
		final Parameters p = new Parameters("-a <%var1%> <%var2%> <%varNOPE%> -b <%varNOPE%> -c");
		final HashMap<String, String> vars = new HashMap<>();
		vars.put("var1", "value1");
		vars.put("var2", "value2");
		p.injectVariables(vars, true);

		assertEquals("-a value1 value2 -c", p.toString());
	}

	@Test
	void testRemoveVarsRemoveEmptyParam() {
		final Parameters p = new Parameters("-a <%var1%> <%var2%> <%varNOPE%> -b <%varNOPE%> -c");
		p.removeVariables(true);
		assertEquals("-c", p.toString());
	}

	@Test
	void testInjectParamsAroundVariable() throws IOException {
		Parameters p = new Parameters("-before <%myvar%> -after");

		p.injectParamsAroundVariable("myvar", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		assertEquals("-before -addedbefore 1 <%myvar%> -addedafter 2 -after", p.toString());

		p = new Parameters("-before <%myvar%> <%myvar%> -after");
		p.injectParamsAroundVariable("myvar", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		assertEquals(
		        "-before -addedbefore 1 <%myvar%> -addedafter 2 -addedbefore 1 <%myvar%> -addedafter 2 -after", p
		                .toString());

		p = new Parameters("-before <%myvar1%> <%myvar2%> -after");
		p.injectParamsAroundVariable("myvar1", Arrays.asList("-addedbefore", "1"), Arrays.asList("-addedafter", "2"));
		p.injectParamsAroundVariable("myvar2", Arrays.asList("-addedbefore", "3"), Arrays.asList("-addedafter", "4"));
		assertEquals(
		        "-before -addedbefore 1 <%myvar1%> -addedafter 2 -addedbefore 3 <%myvar2%> -addedafter 4 -after", p
		                .toString());
	}

}
