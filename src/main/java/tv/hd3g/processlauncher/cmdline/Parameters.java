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
import java.util.Collection;
import java.util.Objects;

public class Parameters extends SimpleParameters {

	private String startVarTag;
	private String endVarTag;

	/**
	 * Use "<%" and "%>" by default
	 */
	public Parameters() {
		super();
		setVarTags("<%", "%>");
	}

	/**
	 * Use "<%" and "%>" by default
	 */
	public Parameters(final String bulkParameters) {
		super(bulkParameters);
		setVarTags("<%", "%>");
	}

	/**
	 * Use "<%" and "%>" by default
	 */
	public Parameters(final String... bulkParameters) {
		super();
		setVarTags("<%", "%>");

		Objects.requireNonNull(bulkParameters, "\"bulkParameters\" can't to be null");
		Arrays.stream(bulkParameters).filter(p -> {
			return p != null;
		}).forEach(bulkParameter -> super.addBulkParameters(bulkParameter));
	}

	/**
	 * Use "<%" and "%>" by default
	 */
	public Parameters(final Collection<String> parameters) {
		super(parameters);
		setVarTags("<%", "%>");
	}

	public Parameters setVarTags(final String startVarTag, final String endVarTag) {
		this.startVarTag = Objects.requireNonNull(startVarTag, "\"startVarTag\" can't to be null");
		if (startVarTag.isEmpty()) {
			throw new NullPointerException("\"startVarTag\" can't to be empty");
		}
		this.endVarTag = Objects.requireNonNull(endVarTag, "\"endVarTag\" can't to be null");
		if (endVarTag.isEmpty()) {
			throw new NullPointerException("\"endVarTag\" can't to be empty");
		}
		return this;
	}

	/**
	 * @return like "%>"
	 */
	public String getEndVarTag() {
		return endVarTag;
	}

	/**
	 * @return like "<%"
	 */
	public String getStartVarTag() {
		return startVarTag;
	}

	/**
	 * @param param like
	 * @return true if like "<%myvar%>"
	 */
	public boolean isTaggedParameter(final String param) {
		Objects.requireNonNull(param, "\"param\" can't to be null");
		if (param.isEmpty()) {
			return false;
		} else if (param.contains(" ")) {
			return false;
		}
		return param.startsWith(startVarTag) & param.endsWith(endVarTag);
	}

	/**
	 * @param param like <%myvar%>
	 * @return like "myvar" or null if param is not a valid variable of if it's empty.
	 */
	public String extractVarNameFromTaggedParameter(final String param) {
		if (isTaggedParameter(param) == false) {
			return null;
		}
		if (param.length() == startVarTag.length() + endVarTag.length()) {
			return null;
		}
		return param.substring(startVarTag.length(), param.length() - endVarTag.length());
	}

	/**
	 * @return varName
	 */
	public String addVariable(final String varName) {
		addParameters(startVarTag + varName + endVarTag);
		return varName;
	}

	@Override
	public Parameters clone() {
		final Parameters newInstance = new Parameters(startVarTag, endVarTag);
		newInstance.importParametersFrom(this);
		return newInstance;
	}

}
