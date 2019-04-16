package tv.hd3g.processlauncher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.io.CaptureStandardOutput;

public class ProcesslauncherBuilder implements ProcesslauncherBuilderShortcutTraits {

	private final File executable;
	private final List<String> parameters;
	private final LinkedHashMap<String, String> environment;
	private File workingDirectory;

	private boolean execCodeMustBeZero;
	private final List<ExecutionCallbacker> executionCallbackers;
	private Optional<ExecutionTimeLimiter> executionTimeLimiter;
	private Optional<CaptureStandardOutput> captureStandardOutput;
	private Optional<ExternalProcessStartup> externalProcessStartup;

	public ProcesslauncherBuilder(final File executable, final Collection<String> parameters, final ExecutableFinder execFinder) {
		this.executable = Objects.requireNonNull(executable, "\"executable\" can't to be null");
		this.parameters = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(parameters, "\"parameters\" can't to be null")));

		environment = new LinkedHashMap<>();

		environment.putAll(System.getenv());
		if (environment.containsKey("LANG") == false) {
			environment.put("LANG", Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry() + "." + Charset.forName("UTF-8"));
		}
		if (execFinder != null) {
			environment.put("PATH", execFinder.getFullPathToString());
		} else {
			environment.put("PATH", System.getenv("PATH"));
		}
		execCodeMustBeZero = true;
		executionCallbackers = new ArrayList<>();
		executionTimeLimiter = Optional.empty();
		captureStandardOutput = Optional.empty();
		externalProcessStartup = Optional.empty();

		try {
			setWorkingDirectory(new File(System.getProperty("java.io.tmpdir", "")));
		} catch (final IOException e) {
			throw new RuntimeException("Invalid java.io.tmpdir", e);
		}
	}

	public ProcesslauncherBuilder(final File executable, final Collection<String> parameters) {
		this(executable, parameters, null);
	}

	/**
	 * @param commandLine with getParametersRemoveVars(false) ; don't manage param vars
	 */
	public ProcesslauncherBuilder(final CommandLine commandLine) {
		this(commandLine.getExecutable(), commandLine.getParametersRemoveVars(false), commandLine.getExecutableFinder().orElseGet(() -> new ExecutableFinder()));
	}

	/**
	 * @return null if not found
	 */
	public String getEnvironmentVar(final String key) {
		return environment.get(key);
	}

	public ProcesslauncherBuilder setEnvironmentVar(final String key, final String value) {
		if (key.equalsIgnoreCase("path") && System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
			environment.put("PATH", value);
			environment.put("Path", value);
		} else {
			environment.put(key, value);
		}
		return this;
	}

	public ProcesslauncherBuilder setEnvironmentVarIfNotFound(final String key, final String value) {
		if (environment.containsKey(key)) {
			return this;
		}
		return setEnvironmentVar(key, value);
	}

	public void forEachEnvironmentVar(final BiConsumer<String, String> action) {
		environment.forEach(action);
	}

	public File getWorkingDirectory() {
		return workingDirectory;
	}

	public ProcesslauncherBuilder setWorkingDirectory(final File workingDirectory) throws IOException {
		Objects.requireNonNull(workingDirectory, "\"workingDirectory\" can't to be null");

		if (workingDirectory.exists() == false) {
			throw new FileNotFoundException("\"" + workingDirectory.getPath() + "\" in filesytem");
		} else if (workingDirectory.canRead() == false) {
			throw new IOException("Can't read workingDirectory \"" + workingDirectory.getPath() + "\"");
		} else if (workingDirectory.isDirectory() == false) {
			throw new FileNotFoundException("\"" + workingDirectory.getPath() + "\" is not a directory");
		}
		this.workingDirectory = workingDirectory;
		return this;
	}

	/**
	 * Default, yes.
	 */
	public ProcesslauncherBuilder setExecCodeMustBeZero(final boolean execCodeMustBeZero) {
		this.execCodeMustBeZero = execCodeMustBeZero;
		return this;
	}

	/**
	 * Default, yes.
	 */
	public boolean isExecCodeMustBeZero() {
		return execCodeMustBeZero;
	}

	/**
	 * @return unmodifiableList
	 */
	public List<ExecutionCallbacker> getExecutionCallbackers() {
		synchronized (executionCallbackers) {
			return Collections.unmodifiableList(executionCallbackers);
		}
	}

	public ProcesslauncherBuilder addExecutionCallbacker(final ExecutionCallbacker executionCallbacker) {
		Objects.requireNonNull(executionCallbacker, "\"endExecutionCallbacker\" can't to be null");
		synchronized (executionCallbackers) {
			executionCallbackers.add(executionCallbacker);
		}
		return this;
	}

	public ProcesslauncherBuilder removeExecutionCallbacker(final ExecutionCallbacker executionCallbacker) {
		Objects.requireNonNull(executionCallbacker, "\"endExecutionCallbacker\" can't to be null");
		synchronized (executionCallbackers) {
			executionCallbackers.remove(executionCallbacker);
		}
		return this;
	}

	public Optional<ExecutionTimeLimiter> getExecutionTimeLimiter() {
		return executionTimeLimiter;
	}

	@Override
	public ProcesslauncherBuilder setExecutionTimeLimiter(final ExecutionTimeLimiter executionTimeLimiter) {
		this.executionTimeLimiter = Optional.ofNullable(executionTimeLimiter);
		return this;
	}

	public Optional<ExternalProcessStartup> getExternalProcessStartup() {
		return externalProcessStartup;
	}

	public ProcesslauncherBuilder setExternalProcessStartup(final ExternalProcessStartup externalProcessStartup) {
		this.externalProcessStartup = Optional.ofNullable(externalProcessStartup);
		return this;
	}

	@Override
	public ProcesslauncherBuilder setCaptureStandardOutput(final CaptureStandardOutput captureStandardOutput) {
		this.captureStandardOutput = Optional.ofNullable(captureStandardOutput);
		return this;
	}

	public Optional<CaptureStandardOutput> getCaptureStandardOutput() {
		return captureStandardOutput;
	}

	public ProcessBuilder makeProcessBuilder() {
		final List<String> fullCommandLine = new ArrayList<>();
		fullCommandLine.add(executable.getPath());
		fullCommandLine.addAll(parameters);

		final ProcessBuilder processBuilder = new ProcessBuilder(fullCommandLine);
		processBuilder.environment().putAll(environment);

		if (workingDirectory != null) {
			processBuilder.directory(workingDirectory);
		}
		return processBuilder;
	}

	static final Function<String, String> addQuotesIfSpaces = s -> {
		if (s.contains(" ")) {
			return "\"" + s + "\"";
		} else {
			return s;
		}
	};

	public String getFullCommandLine() {
		final StringBuilder sb = new StringBuilder();
		sb.append(addQuotesIfSpaces.apply(executable.getPath()));
		sb.append(" ");
		sb.append(parameters.stream().map(addQuotesIfSpaces).collect(Collectors.joining(" ")));
		return sb.toString().trim();
	}

	/**
	 * @return getFullCommandLine()
	 */
	@Override
	public String toString() {
		return getFullCommandLine();
	}

	/**
	 * @return new Processlauncher(this)
	 */
	@Override
	public Processlauncher toProcesslauncher() {
		return new Processlauncher(this);
	}

}
