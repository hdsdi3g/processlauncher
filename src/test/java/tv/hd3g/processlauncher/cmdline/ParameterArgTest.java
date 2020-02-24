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

import junit.framework.Assert;
import junit.framework.TestCase;

public class ParameterArgTest extends TestCase {

	private ParameterArg pArg;

	@Override
	protected void setUp() throws Exception {
		pArg = new ParameterArg(true);
		pArg.add('a');
		pArg.add('b');
		pArg.add('c');
	}

	public void testToString() {
		Assert.assertEquals("abc", pArg.toString());
	}

	public void testIsInQuotes() {
		Assert.assertTrue(pArg.isInQuotes());
	}

	public void testIsEmpty() {
		Assert.assertFalse(pArg.isEmpty());
	}
}
