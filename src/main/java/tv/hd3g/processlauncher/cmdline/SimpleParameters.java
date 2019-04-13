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
	private String parameter_keys_starts_with = "-";

	SimpleParameters() {
		parameters = new ArrayList<>();
	}

	SimpleParameters(final String bulk_parameters) {
		this();
		addBulkParameters(bulk_parameters);
	}

	SimpleParameters(final Collection<String> parameters) {
		this();
		addParameters(parameters);
	}

	/**
	 * Don't touch to current parameters, only parameter_keys_starts_with
	 */
	public SimpleParameters transfertThisConfigurationTo(final SimpleParameters new_instance) {
		new_instance.parameter_keys_starts_with = parameter_keys_starts_with;
		return this;
	}

	/**
	 * Transfer (clone) current parameters and parameter_keys_starts_with
	 */
	public SimpleParameters importParametersFrom(final SimpleParameters previous_instance) {
		log.trace("Import from {}", () -> previous_instance);

		parameter_keys_starts_with = previous_instance.parameter_keys_starts_with;
		parameters.clear();
		parameters.addAll(previous_instance.parameters);
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
			    final int last_pos = list.size() - 1;
			    final ParameterArg last_entry = list.get(last_pos);

			    if (chr == QUOTE) {
				    if (last_entry.isInQuotes()) {
					    /**
					     * Switch off quote zone
					     */
					    list.add(new ParameterArg(false));
				    } else {
					    /**
					     * Switch on quote zone
					     */
					    if (last_entry.isEmpty()) {
						    /**
						     * Remove previous empty ParameterArg
						     */
						    list.remove(last_pos);
					    }
					    list.add(new ParameterArg(true));
				    }
			    } else if (chr == SPACE) {
				    if (last_entry.isInQuotes()) {
					    /**
					     * Add space in quotes
					     */
					    last_entry.add(chr);
				    } else {
					    if (last_entry.isEmpty() == false) {
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
				    last_entry.add(chr);
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
	public SimpleParameters ifHasNotParameter(final Runnable to_do_if_missing, final String... in_parameters) {
		Objects.requireNonNull(to_do_if_missing, "\"to_do_if_missing\" can't to be null");
		if (hasParameters(in_parameters) == false) {
			to_do_if_missing.run();
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

		final List<String> new_list = Stream.concat(params.stream().filter(p -> p != null), parameters.stream()).collect(Collectors.toUnmodifiableList());
		parameters.clear();
		parameters.addAll(new_list);

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
	 * @param parameters_keys_starts_with "-" by default
	 */
	public SimpleParameters setParametersKeysStartsWith(final String parameters_keys_starts_with) {
		parameter_keys_starts_with = parameters_keys_starts_with;
		log.debug("Set parameters key start with: {}", parameters_keys_starts_with);
		return this;
	}

	/**
	 * @return "-" by default
	 */
	public String getParametersKeysStartsWith() {
		return parameter_keys_starts_with;
	}

	boolean isParameterArgIsAParametersKey(final String arg) {
		return arg.startsWith(parameter_keys_starts_with);
	}

	/**
	 * @param parameter_key add "-" in front of param_key if needed
	 */
	protected String conformParameterKey(final String parameter_key) {
		if (isParameterArgIsAParametersKey(parameter_key) == false) {
			return parameter_keys_starts_with + parameter_key;
		}
		return parameter_key;
	}

	/**
	 * @param parameter_key can have "-" or not (it will be added).
	 * @return For "-param val1 -param val2 -param val3" -> val1, val2, val3 ; null if param_key can't be found, empty if not values for param
	 */
	public List<String> getValues(final String parameter_key) {
		Objects.requireNonNull(parameter_key, "\"parameter_key\" can't to be null");

		final String param = conformParameterKey(parameter_key);

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
	 * Search a remove all parameters with param_key as name, even associated values.
	 * @param parameters_key can have "-" or not (it will be added).
	 */
	public boolean removeParameter(final String parameters_key, final int param_as_this_key_pos) {
		Objects.requireNonNull(parameters_key, "\"parameters_key\" can't to be null");

		final String param = conformParameterKey(parameters_key);

		int to_skip = param_as_this_key_pos + 1;

		for (int pos = 0; pos < parameters.size(); pos++) {
			final String current = parameters.get(pos);
			if (current.equals(param)) {
				to_skip--;
				if (to_skip == 0) {
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
	 * @param parameter_key can have "-" or not (it will be added).
	 * @return true if done
	 */
	public boolean alterParameter(final String parameter_key, final String new_value, final int param_as_this_key_pos) {
		Objects.requireNonNull(parameter_key, "\"parameter_key\" can't to be null");
		Objects.requireNonNull(new_value, "\"new_value\" can't to be null");

		final String param = conformParameterKey(parameter_key);

		int to_skip = param_as_this_key_pos + 1;

		for (int pos = 0; pos < parameters.size(); pos++) {
			final String current = parameters.get(pos);
			if (current.equals(param)) {
				to_skip--;
				if (to_skip == 0) {
					if (parameters.size() > pos + 1) {
						final String next = parameters.get(pos + 1);
						if (isParameterArgIsAParametersKey(next) == false) {
							parameters.set(pos + 1, new_value);
						} else {
							parameters.add(pos + 1, new_value);
						}
					} else {
						parameters.add(new_value);
					}
					log.trace("Add parameter: {}", new_value);
					return true;
				}
			}
		}

		return false;
	}

}
