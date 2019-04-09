package tv.hd3g.processlauncher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import tv.hd3g.processlauncher.cmdline.ExecutableCommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.io.CaptureStandardOutput;

public class ProcesslauncherBuilder {
	// TODO ProcessBuilder.startPipeline(builders) :: external laucher
	
	private final ExecutableCommandLine executableCommandLine;
	private final LinkedHashMap<String, String> environment;
	private File working_directory;

	private boolean execCodeMustBeZero;
	private Optional<EndExecutionCallbacker> endExecutionCallbacker;
	private Optional<ExecutionTimeLimiter> executionTimeLimiter;
	private Optional<CaptureStandardOutput> captureStandardOutput;
	
	public ProcesslauncherBuilder(final ExecutableCommandLine executableCommandLine) {
		this.executableCommandLine = executableCommandLine;
		if (executableCommandLine == null) {
			throw new NullPointerException("\"executableCommandLine\" can't to be null");
		}
		environment = new LinkedHashMap<>();

		environment.putAll(System.getenv());
		if (environment.containsKey("LANG") == false) {
			environment.put("LANG", Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry() + "." + Charset.forName("UTF-8"));
		}
		final ExecutableFinder eF = executableCommandLine.getExecutableFinder();
		if (eF != null) {
			environment.put("PATH", eF.getFullPathToString());
		} else {
			environment.put("PATH", System.getenv("PATH"));
		}
		execCodeMustBeZero = true;
		endExecutionCallbacker = Optional.empty();
		executionTimeLimiter = Optional.empty();
		captureStandardOutput = Optional.empty();
		
		try {
			setWorkingDirectory(new File(System.getProperty("java.io.tmpdir", "")));
		} catch (final IOException e) {
			throw new RuntimeException("Invalid java.io.tmpdir", e);
		}
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
	
	/**
	 * @return never null
	 */
	public File getWorkingDirectory() {
		return working_directory;
	}
	
	public ProcesslauncherBuilder setWorkingDirectory(final File working_directory) throws IOException {
		if (working_directory == null) {
			throw new NullPointerException("\"working_directory\" can't to be null");
		} else if (working_directory.exists() == false) {
			throw new FileNotFoundException("\"" + working_directory.getPath() + "\" in filesytem");
		} else if (working_directory.canRead() == false) {
			throw new IOException("Can't read working_directory \"" + working_directory.getPath() + "\"");
		} else if (working_directory.isDirectory() == false) {
			throw new FileNotFoundException("\"" + working_directory.getPath() + "\" is not a directory");
		}
		this.working_directory = working_directory;
		return this;
	}

	/**
	 * Default, yes.
	 */
	public ProcesslauncherBuilder setExecCodeMustBeZero(final boolean execCodeMustBeZero) {
		this.execCodeMustBeZero = execCodeMustBeZero;
		return this;
	}

	public boolean isExecCodeMustBeZero() {
		return execCodeMustBeZero;
	}
	
	public Optional<EndExecutionCallbacker> getEndExecutionCallbacker() {
		return endExecutionCallbacker;
	}
	
	public Optional<ExecutionTimeLimiter> getExecutionTimeLimiter() {
		return executionTimeLimiter;
	}

	public ProcesslauncherBuilder setEndExecutionCallbacker(final EndExecutionCallbacker endExecutionCallbacker) {
		this.endExecutionCallbacker = Optional.ofNullable(endExecutionCallbacker);
		return this;
	}
	
	public ProcesslauncherBuilder setExecutionTimeLimiter(final ExecutionTimeLimiter executionTimeLimiter) {
		this.executionTimeLimiter = Optional.ofNullable(executionTimeLimiter);
		return this;
	}

	public ProcesslauncherBuilder setCaptureStandardOutput(final CaptureStandardOutput captureStandardOutput) {
		this.captureStandardOutput = Optional.ofNullable(captureStandardOutput);
		return this;
	}

	public Optional<CaptureStandardOutput> captureStandardOutput() {
		return captureStandardOutput;
	}
	
	public ProcessBuilder makeProcessBuilder() {
		final List<String> fullCommandLine = new ArrayList<>();
		fullCommandLine.add(executableCommandLine.getExecutable().getAbsolutePath());
		fullCommandLine.addAll(executableCommandLine.getParameters());
		
		final ProcessBuilder process_builder = new ProcessBuilder(fullCommandLine);
		process_builder.environment().putAll(environment);
		
		if (working_directory != null) {
			process_builder.directory(working_directory);
		}
		return process_builder;
	}
	
	private static final Function<String, String> addQuotesIfSpaces = s -> {
		if (s.contains(" ")) {
			return "\"" + s + "\"";
		} else {
			return s;
		}
	};

	public String getFullCommandLine() {
		final StringBuilder sb = new StringBuilder();
		sb.append(addQuotesIfSpaces.apply(executableCommandLine.getExecutable().getAbsolutePath()));
		sb.append(" ");
		sb.append(executableCommandLine.getParameters().stream().map(addQuotesIfSpaces).collect(Collectors.joining(" ")));
		return sb.toString().trim();
	}
	
}
