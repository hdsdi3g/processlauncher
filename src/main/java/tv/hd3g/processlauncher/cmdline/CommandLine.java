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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandLine {

	private static final BinaryOperator<List<String>> LIST_COMBINER = (list1, list2) -> Stream.concat(list1.stream(), list2.stream()).collect(Collectors.toUnmodifiableList());

	private final File executable;
	private final ExecutableFinder executableFinder;
	private final Parameters parameters;

	public CommandLine(final File executable, final Parameters parameters) throws IOException {
		this.executable = executable;
		if (executable.isFile() == false | executable.exists() == false) {
			throw new FileNotFoundException("Can't found " + executable);
		} else if (executable.canExecute() == false) {
			throw new IOException("Can't execute " + executable);
		}
		executableFinder = null;

		this.parameters = Objects.requireNonNull(parameters, "\"parameters\" can't to be null").clone();
	}

	public CommandLine(final String execName, final Parameters parameters, final ExecutableFinder executableFinder) throws IOException {
		Objects.requireNonNull(execName, "\"execName\" can't to be null");
		this.executableFinder = executableFinder;
		if (executableFinder != null) {
			executable = executableFinder.get(execName);
		} else {
			executable = new File(execName);
			if (executable.isFile() == false | executable.exists() == false) {
				throw new FileNotFoundException("Can't found " + executable);
			} else if (executable.canExecute() == false) {
				throw new IOException("Can't execute " + executable);
			}
		}
		this.parameters = Objects.requireNonNull(parameters, "\"parameters\" can't to be null").clone();
	}

	public CommandLine(final File executable, final String parameters) throws IOException {
		this(executable, new Parameters(Objects.requireNonNull(parameters, "\"parameters\" can't to be null")));
	}

	public CommandLine(final String execName, final String parameters, final ExecutableFinder executableFinder) throws IOException {
		this(execName, new Parameters(Objects.requireNonNull(parameters, "\"parameters\" can't to be null")), executableFinder);
	}

	/**
	 * @return true if the update is done
	 */
	public boolean injectParamsAroundVariable(final String varName, final Collection<String> addBefore, final Collection<String> addAfter) {
		Objects.requireNonNull(varName, "\"varName\" can't to be null");
		Objects.requireNonNull(addBefore, "\"addBefore\" can't to be null");
		Objects.requireNonNull(addAfter, "\"addAfter\" can't to be null");

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
		return executable.getPath() + " " + parameters.toString();
	}

	String getParametersToString() {
		return parameters.toString();
	}

	public Optional<ExecutableFinder> getExecutableFinder() {
		return Optional.ofNullable(executableFinder);
	}

	public File getExecutable() {
		return executable;
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
