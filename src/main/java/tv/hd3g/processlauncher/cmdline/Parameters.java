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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parameters extends SimpleParameters {

	private static final BinaryOperator<List<String>> LIST_COMBINER = (list1, list2) -> Stream.concat(list1.stream(),
	        list2.stream()).collect(Collectors.toUnmodifiableList());

	private String startVarTag;
	private String endVarTag;

	/**
	 * Use "&lt;%" and "%&gt;" by default
	 */
	public Parameters() {
		super();
		setVarTags("<%", "%>");
	}

	/**
	 * Use "&lt;%" and "%&gt;"" by default
	 */
	public Parameters(final String bulkParameters) {
		super(bulkParameters);
		setVarTags("<%", "%>");
	}

	/**
	 * Use "&lt;%" and "%&gt;" by default
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
	 * Use "&lt;%" and "%&gt;" by default
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
	 * @return like "%&gt;"
	 */
	public String getEndVarTag() {
		return endVarTag;
	}

	/**
	 * @return like "&lt;%"
	 */
	public String getStartVarTag() {
		return startVarTag;
	}

	/**
	 * @param param like
	 * @return true if like "&lt;%myvar%&gt;"
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
	 * @param param like &lt;%myvar%&gt;
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

	/**
	 * @return true if the update is done
	 */
	public boolean injectParamsAroundVariable(final String varName,
	                                          final Collection<String> addBefore,
	                                          final Collection<String> addAfter) {
		Objects.requireNonNull(varName, "\"varName\" can't to be null");
		Objects.requireNonNull(addBefore, "\"addBefore\" can't to be null");
		Objects.requireNonNull(addAfter, "\"addAfter\" can't to be null");

		final AtomicBoolean isDone = new AtomicBoolean(false);

		final List<String> newParameters = getParameters().stream().reduce(Collections.unmodifiableList(
		        new ArrayList<String>()), (list, arg) -> {
			        if (isTaggedParameter(arg)) {
				        final String currentVarName = extractVarNameFromTaggedParameter(arg);
				        if (currentVarName.equals(varName)) {
					        isDone.set(true);
					        return Stream.concat(list.stream(), Stream.concat(Stream.concat(addBefore.stream(), Stream
					                .of(arg)), addAfter.stream())).collect(Collectors.toUnmodifiableList());
				        }
			        }

			        return Stream.concat(list.stream(), Stream.of(arg)).collect(Collectors.toUnmodifiableList());
		        }, LIST_COMBINER);

		replaceParameters(newParameters);
		return isDone.get();
	}

	/**
	 * @param removeParamsIfNoVarToInject if true, for "-a -b ? -d" -&gt; "-a -d", else "-a -b -d"
	 * @return this
	 */
	public Parameters removeVariables(final boolean removeParamsIfNoVarToInject) {
		return injectVariables(Collections.emptyMap(), removeParamsIfNoVarToInject);
	}

	/**
	 * @param removeParamsIfNoVarToInject if true, for "-a -b ? -d" -&gt; "-a -d", else "-a -b -d"
	 * @return this
	 */
	public Parameters injectVariables(final Map<String, String> varsToInject,
	                                  final boolean removeParamsIfNoVarToInject) {
		final List<String> newParameters;
		if (removeParamsIfNoVarToInject) {
			newParameters = getParameters().stream().reduce(Collections.unmodifiableList(new ArrayList<String>()), (
			                                                                                                        list,
			                                                                                                        arg) -> {
				if (isTaggedParameter(arg)) {
					final String varName = extractVarNameFromTaggedParameter(arg);
					if (varsToInject.containsKey(varName)) {
						return Stream.concat(list.stream(), Stream.of(varsToInject.get(varName))).collect(Collectors
						        .toUnmodifiableList());
					} else {
						if (list.isEmpty()) {
							return list;
						} else if (isParameterArgIsAParametersKey(list.get(list.size() - 1))) {
							return list.stream().limit(list.size() - 1).collect(Collectors.toUnmodifiableList());
						} else {
							return list;
						}
					}
				} else {
					return Stream.concat(list.stream(), Stream.of(arg)).collect(Collectors.toUnmodifiableList());
				}
			}, LIST_COMBINER);
		} else {
			newParameters = getParameters().stream().map(arg -> {
				final String varName = extractVarNameFromTaggedParameter(arg);
				if (varName != null) {
					return varsToInject.get(varName);
				} else {
					return arg;
				}
			}).filter(arg -> arg != null).collect(Collectors.toUnmodifiableList());
		}

		replaceParameters(newParameters);
		return this;
	}

}
