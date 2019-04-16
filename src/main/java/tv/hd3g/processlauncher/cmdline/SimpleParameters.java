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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class SimpleParameters {
	private static Logger log = LogManager.getLogger();

	private static final Character QUOTE = '"';
	private static final Character SPACE = ' ';

	private final List<String> parameters;
	private String parameterKeysStartsWith = "-";

	SimpleParameters() {
		parameters = new ArrayList<>();
	}

	SimpleParameters(final String bulkParameters) {
		this();
		addBulkParameters(bulkParameters);
	}

	SimpleParameters(final Collection<String> parameters) {
		this();
		addParameters(parameters);
	}

	/**
	 * Don't touch to current parameters, only parameterKeysStartsWith
	 */
	public SimpleParameters transfertThisConfigurationTo(final SimpleParameters newInstance) {
		newInstance.parameterKeysStartsWith = parameterKeysStartsWith;
		return this;
	}

	/**
	 * Transfer (clone) current parameters and parameterKeysStartsWith
	 */
	public SimpleParameters importParametersFrom(final SimpleParameters previousInstance) {
		log.trace("Import from {}", () -> previousInstance);

		parameterKeysStartsWith = previousInstance.parameterKeysStartsWith;
		parameters.clear();
		parameters.addAll(previousInstance.parameters);
		return this;
	}

	/**
	 * Don't touch to actual parameters, and clone from source.
	 */
	public void addAllFrom(final SimpleParameters source) {
		parameters.addAll(source.parameters);
	}

	private final Function<String, Stream<ParameterArg>> filterAnTransformParameter = p -> {
	    /**
	     * Split >-a -b "c d" e< to [-a, -b, c d, e]
	     */
	    return p.trim().chars().mapToObj(i -> (char) i).reduce(new ArrayList<ParameterArg>(), (list, chr) -> {
		    if (list.isEmpty()) {
			    /**
			     * First entry
			     */
			    if (chr == QUOTE) {
				    /**
				     * Start quote zone
				     */
				    list.add(new ParameterArg(true));
			    } else if (chr == SPACE) {
				    /**
				     * Trailing space > ignore it
				     */
			    } else {
				    /**
				     * Start first "classic" ParameterArg
				     */
				    list.add(new ParameterArg(false).add(chr));
			    }
		    } else {
			    /**
			     * Get current entry
			     */
			    final int lastPos = list.size() - 1;
			    final ParameterArg lastEntry = list.get(lastPos);

			    if (chr == QUOTE) {
				    if (lastEntry.isInQuotes()) {
					    /**
					     * Switch off quote zone
					     */
					    list.add(new ParameterArg(false));
				    } else {
					    /**
					     * Switch on quote zone
					     */
					    if (lastEntry.isEmpty()) {
						    /**
						     * Remove previous empty ParameterArg
						     */
						    list.remove(lastPos);
					    }
					    list.add(new ParameterArg(true));
				    }
			    } else if (chr == SPACE) {
				    if (lastEntry.isInQuotes()) {
					    /**
					     * Add space in quotes
					     */
					    lastEntry.add(chr);
				    } else {
					    if (lastEntry.isEmpty() == false) {
						    /**
						     * New space -> new ParameterArg (and ignore space)
						     */
						    list.add(new ParameterArg(false));
					    } else {
						    /**
						     * Space between ParameterArgs > ignore it
						     */
					    }
				    }
			    } else {
				    lastEntry.add(chr);
			    }
		    }
		    return list;
	    }, (list1, list2) -> {
		    final ArrayList<ParameterArg> ParameterArgs = new ArrayList<>(list1);
		    ParameterArgs.addAll(list2);
		    return ParameterArgs;
	    }).stream();
	};

	public SimpleParameters clear() {
		log.trace("Clear all");
		parameters.clear();
		return this;
	}

	/**
	 * @param params (anyMatch) ; params can have "-" or not (it will be added).
	 */
	public boolean hasParameters(final String... params) {
		Objects.requireNonNull(params, "\"params\" can't to be null");

		return Arrays.stream(params).filter(p -> {
			return p != null;
		}).anyMatch(parameter -> {
			final String param = conformParameterKey(parameter);
			return parameters.contains(param);
		});
	}

	/**
	 * @see SimpleParameters#hasParameters()
	 */
	public SimpleParameters ifHasNotParameter(final Runnable toDoIfMissing, final String... inParameters) {
		Objects.requireNonNull(toDoIfMissing, "\"toDoIfMissing\" can't to be null");
		if (hasParameters(inParameters) == false) {
			toDoIfMissing.run();
		}

		return this;
	}

	/**
	 * @return never null
	 */
	public List<String> getParameters() {
		return parameters;
	}

	/**
	 * @param params don't alter params
	 */
	public SimpleParameters addParameters(final String... params) {
		Objects.requireNonNull(params, "\"params\" can't to be null");

		parameters.addAll(Arrays.stream(params).filter(p -> {
			return p != null;
		}).collect(Collectors.toUnmodifiableList()));

		log.trace("Add parameters: {}", () -> Arrays.stream(params).collect(Collectors.toUnmodifiableList()));

		return this;
	}

	/**
	 * @param params don't alter params
	 */
	public SimpleParameters addParameters(final Collection<String> params) {
		Objects.requireNonNull(params, "\"params\" can't to be null");

		parameters.addAll(params.stream().filter(p -> {
			return p != null;
		}).collect(Collectors.toUnmodifiableList()));

		log.trace("Add parameters: {}", () -> params);

		return this;
	}

	/**
	 * @param params transform spaces in each param to new params: "a b c d" -> ["a", "b", "c", "d"], and it manage " but not tabs.
	 */
	public SimpleParameters addBulkParameters(final String params) {
		Objects.requireNonNull(params, "\"params\" can't to be null");

		parameters.addAll(filterAnTransformParameter.apply(params).map(ParameterArg -> {
			return ParameterArg.toString();
		}).collect(Collectors.toUnmodifiableList()));

		log.trace("Add parameters: {}", params);

		return this;
	}

	/**
	 * @param params don't alter params
	 */
	public SimpleParameters prependParameters(final Collection<String> params) {
		Objects.requireNonNull(params, "\"params\" can't to be null");

		final List<String> newList = Stream.concat(params.stream().filter(p -> p != null), parameters.stream()).collect(Collectors.toUnmodifiableList());
		parameters.clear();
		parameters.addAll(newList);

		log.trace("Prepend parameters: {}", () -> params);

		return this;
	}

	/**
	 * @param params add all in front of command line, don't alter params
	 */
	public SimpleParameters prependParameters(final String... params) {
		Objects.requireNonNull(params, "\"params\" can't to be null");

		prependParameters(Arrays.stream(params).filter(p -> {
			return p != null;
		}).collect(Collectors.toUnmodifiableList()));

		return this;
	}

	/**
	 * @param params params add all in front of command line, transform spaces in each param to new params: "a b c d" -> ["a", "b", "c", "d"], and it manage " but not tabs.
	 */
	public SimpleParameters prependBulkParameters(final String params) {
		Objects.requireNonNull(params, "\"params\" can't to be null");

		prependParameters(filterAnTransformParameter.apply(params).map(ParameterArg -> {
			return ParameterArg.toString();
		}).collect(Collectors.toUnmodifiableList()));

		return this;
	}

	@Override
	public String toString() {
		return parameters.stream().collect(Collectors.joining(" "));
	}

	/**
	 * @param parameterKeysStartsWith "-" by default
	 */
	public SimpleParameters setParametersKeysStartsWith(final String parameterKeysStartsWith) {
		this.parameterKeysStartsWith = parameterKeysStartsWith;
		log.debug("Set parameters key start with: {}", parameterKeysStartsWith);
		return this;
	}

	/**
	 * @return "-" by default
	 */
	public String getParametersKeysStartsWith() {
		return parameterKeysStartsWith;
	}

	boolean isParameterArgIsAParametersKey(final String arg) {
		return arg.startsWith(parameterKeysStartsWith);
	}

	/**
	 * @param parameterKey add "-" in front of paramKey if needed
	 */
	protected String conformParameterKey(final String parameterKey) {
		if (isParameterArgIsAParametersKey(parameterKey) == false) {
			return parameterKeysStartsWith + parameterKey;
		}
		return parameterKey;
	}

	/**
	 * @param parameterKey can have "-" or not (it will be added).
	 * @return For "-param val1 -param val2 -param val3" -> val1, val2, val3 ; null if parameterKey can't be found, empty if not values for param
	 */
	public List<String> getValues(final String parameterKey) {
		Objects.requireNonNull(parameterKey, "\"parameterKey\" can't to be null");

		final String param = conformParameterKey(parameterKey);

		final ArrayList<String> result = new ArrayList<>();

		boolean has = false;
		for (int pos = 0; pos < parameters.size(); pos++) {
			final String current = parameters.get(pos);
			if (current.equals(param)) {
				has = true;
				if (parameters.size() > pos + 1) {
					final String next = parameters.get(pos + 1);
					if (isParameterArgIsAParametersKey(next) == false) {
						result.add(next);
					}
				}
			}
		}

		if (has) {
			return Collections.unmodifiableList(result);
		} else {
			return null;
		}
	}

	/**
	 * Search a remove all parameters with paramKey as name, even associated values.
	 * @param parametersKey can have "-" or not (it will be added).
	 */
	public boolean removeParameter(final String parametersKey, final int paramAsThisKeyPos) {
		Objects.requireNonNull(parametersKey, "\"parametersKey\" can't to be null");

		final String param = conformParameterKey(parametersKey);

		int toSkip = paramAsThisKeyPos + 1;

		for (int pos = 0; pos < parameters.size(); pos++) {
			final String current = parameters.get(pos);
			if (current.equals(param)) {
				toSkip--;
				if (toSkip == 0) {
					if (parameters.size() > pos + 1) {
						final String next = parameters.get(pos + 1);
						if (isParameterArgIsAParametersKey(next) == false) {
							parameters.remove(pos + 1);
						}
					}
					log.trace("Remove parameter: {}", parameters.remove(pos));
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * @param parameterKey can have "-" or not (it will be added).
	 * @return true if done
	 */
	public boolean alterParameter(final String parameterKey, final String newValue, final int paramAsThisKeyPos) {
		Objects.requireNonNull(parameterKey, "\"parameterKey\" can't to be null");
		Objects.requireNonNull(newValue, "\"newValue\" can't to be null");

		final String param = conformParameterKey(parameterKey);

		int toSkip = paramAsThisKeyPos + 1;

		for (int pos = 0; pos < parameters.size(); pos++) {
			final String current = parameters.get(pos);
			if (current.equals(param)) {
				toSkip--;
				if (toSkip == 0) {
					if (parameters.size() > pos + 1) {
						final String next = parameters.get(pos + 1);
						if (isParameterArgIsAParametersKey(next) == false) {
							parameters.set(pos + 1, newValue);
						} else {
							parameters.add(pos + 1, newValue);
						}
					} else {
						parameters.add(newValue);
					}
					log.trace("Add parameter: {}", newValue);
					return true;
				}
			}
		}

		return false;
	}

}
