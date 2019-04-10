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

import java.util.Collection;
import java.util.Objects;

public class Parameters extends SimpleParameters { // TODO test
	
	private String startVarTag;
	private String endVarTag;
	
	/**
	 * Use "<%" and "%>" by default
	 */
	public Parameters() {
		super();
		setVarTags("<%", "%>");
	}
	
	public Parameters(final String start_var_tag, final String end_var_tag) {
		super();
		setVarTags(start_var_tag, end_var_tag);
	}

	/**
	 * Use "<%" and "%>" by default
	 */
	public Parameters(final String bulk_parameters) {
		super(bulk_parameters);
		setVarTags("<%", "%>");
	}

	/**
	 * Use "<%" and "%>" by default
	 */
	public Parameters(final Collection<String> parameters) {
		super(parameters);
		setVarTags("<%", "%>");
	}

	public Parameters(final String bulk_parameters, final String start_var_tag, final String end_var_tag) {
		super(bulk_parameters);
		setVarTags(start_var_tag, end_var_tag);
	}

	public Parameters(final Collection<String> parameters, final String start_var_tag, final String end_var_tag) {
		super(parameters);
		setVarTags(start_var_tag, end_var_tag);
	}

	private void setVarTags(final String start_var_tag, final String end_var_tag) {
		startVarTag = Objects.requireNonNull(start_var_tag, "\"start_var_tag\" can't to be null");
		if (start_var_tag.isEmpty()) {
			throw new NullPointerException("\"start_var_tag\" can't to be empty");
		}
		endVarTag = Objects.requireNonNull(end_var_tag, "\"end_var_tag\" can't to be null");
		if (end_var_tag.isEmpty()) {
			throw new NullPointerException("\"end_var_tag\" can't to be empty");
		}
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
	 * @return var_name
	 */
	public String addVariable(final String var_name) {
		addParameters(startVarTag + var_name + endVarTag);
		return var_name;
	}

	@Override
	public Parameters clone() {
		final Parameters new_instance = new Parameters(startVarTag, endVarTag);
		new_instance.importParametersFrom(this);
		return new_instance;
	}

}
