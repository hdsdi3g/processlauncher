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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandLine {

	private static final BinaryOperator<List<String>> LIST_COMBINER = (list1, list2) -> Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toUnmodifiableList());

	private final String execName;
	private final Parameters parameters;
	
	public CommandLine(final String execName, final Parameters sourceParameters) {
		this.execName = execName;
		if (execName == null) {
			throw new NullPointerException("\"execName\" can't to be null");
		}
		if (sourceParameters == null) {
			throw new NullPointerException("\"sourceParameters\" can't to be null");
		}
		parameters = sourceParameters.clone();
	}

	/**
	 * @param fullCommandLine MUST containt at least an executable reference (exec name or path). It can contain vars
	 */
	public CommandLine(final String fullCommandLine) {
		if (fullCommandLine == null) {
			throw new NullPointerException("\"fullCommandLine\" can't to be null");
		}
		parameters = new Parameters(fullCommandLine);
		execName = parameters.getParameters().get(0);
		parameters.getParameters().remove(0);
	}

	public String getExecName() {
		return execName;
	}

	/**
	 * @return true if the update is done
	 */
	public boolean injectParamsAroundVariable(final String varName, final Collection<String> addBefore, final Collection<String> addAfter) {
		if (varName == null) {
			throw new NullPointerException("\"varName\" can't to be null");
		} else if (addBefore == null) {
			throw new NullPointerException("\"addBefore\" can't to be null");
		} else if (addAfter == null) {
			throw new NullPointerException("\"addAfter\" can't to be null");
		}
		
		final AtomicBoolean isDone = new AtomicBoolean(false);
		
		final List<String> newParameters = parameters.getParameters().stream().reduce(Collections.unmodifiableList(new ArrayList<String>()), (list, arg) -> {
			if (parameters.isTaggedParameter(arg)) {
				final String currentVarName = parameters.extractVarNameFromTaggedParameter(arg);
				if (currentVarName.equals(varName)) {
					isDone.set(true);
					return Stream.concat(list.stream(), Stream.concat(Stream.concat(addBefore.stream(), Stream.of(arg)), addAfter.stream())).collect(Collectors.toUnmodifiableList());
				}
			}
			
			return Stream.concat(list.stream(), Stream.of(arg)).collect(Collectors.toUnmodifiableList());
		}, LIST_COMBINER);
		
		parameters.getParameters().clear();
		parameters.getParameters().addAll(newParameters);
		
		return isDone.get();
	}

	@Override
	public String toString() {
		return execName + " " + parameters.toString();
	}
	
	/**
	 * @param removeParamsIfNoVarToInject if true, for "-a -b c -d" -> "-a -d", else "-a -b -d"
	 * @return unmodifiableList
	 */
	public List<String> getParametersRemoveVars(final boolean removeParamsIfNoVarToInject) {
		return getParametersInjectVars(Collections.emptyMap(), removeParamsIfNoVarToInject);
	}
	
	/**
	 * @param removeParamsIfNoVarToInject if true, for "-a -b c -d" -> "-a -d", else "-a -b -d"
	 * @return unmodifiableList
	 */
	public List<String> getParametersInjectVars(final Map<String, String> varsToInject, final boolean removeParamsIfNoVarToInject) {
		final List<String> newParameters;
		if (removeParamsIfNoVarToInject) {
			newParameters = parameters.getParameters().stream().reduce(Collections.unmodifiableList(new ArrayList<String>()), (list, arg) -> {
				if (parameters.isTaggedParameter(arg)) {
					final String varName = parameters.extractVarNameFromTaggedParameter(arg);
					if (varsToInject.containsKey(varName)) {
						return Stream.concat(list.stream(), Stream.of(varsToInject.get(varName))).collect(Collectors.toUnmodifiableList());
					} else {
						if (list.isEmpty()) {
							return list;
						} else if (parameters.isParameterArgIsAParametersKey(list.get(list.size() - 1))) {
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
			newParameters = parameters.getParameters().stream().map(arg -> {
				final String varName = parameters.extractVarNameFromTaggedParameter(arg);
				if (varName != null) {
					return varsToInject.get(varName);
				} else {
					return arg;
				}
			}).filter(arg -> arg != null).collect(Collectors.toUnmodifiableList());
		}
		return Collections.unmodifiableList(newParameters);
	}

}
