package tv.hd3g.processlauncher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.function.BiConsumer;

import tv.hd3g.processlauncher.cmdline.ExecutableCommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

public class ProcesslauncherBuilder {
	// TODO ProcessBuilder.startPipeline(builders) :: external laucher

	private final ExecutableCommandLine executableCommandLine;
	private final LinkedHashMap<String, String> environment;
	private boolean exec_code_must_be_zero;
	private File working_directory;
	private EndExecutionCallbacker endExecutionCallbacker;
	private ExecutionTimeLimiter executionTimeLimiter;

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
		exec_code_must_be_zero = true;

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
	public ProcesslauncherBuilder setExecCodeMustBeZero(final boolean exec_code_must_be_zero) {
		this.exec_code_must_be_zero = exec_code_must_be_zero;
		return this;
	}
	
	public boolean isExecCodeMustBeZero() {
		return exec_code_must_be_zero;
	}

	public ProcesslauncherBuilder setEndExecutionCallbacker(final EndExecutionCallbacker endExecutionCallbacker) {
		this.endExecutionCallbacker = endExecutionCallbacker;
		return this;
	}

	public ProcesslauncherBuilder setExecutionTimeLimiter(final ExecutionTimeLimiter executionTimeLimiter) {
		this.executionTimeLimiter = executionTimeLimiter;
		return this;
	}
	
	/*
	enum CaptureOutStreamsBehavior { BOTH_STDOUT_STDERR ONLY_STDOUT ONLY_STDERR
	StdOutErrObserver onText(ExecProcessTextResult source, String line, boolean is_std_err)
	StdOutErrCallback onStdout(ExecProcessTextResult source, String line) ...
	StdInInjection -> OutputStream
	InteractiveExecProcessHandler ->  onText(ExecProcessTextResult source, String line, boolean is_std_err)
	EndExecutionCallback<T extends ExecProcessResult>  onEnd(ExecProcessResult source) executor.execute(
	
	enum EndStatus NOT_YET_DONE, CORRECTLY_DONE, DONE_WITH_ERROR, KILLED, TOO_LONG_EXECUTION_TIME;
	
	ExecProcessResult
	ExecProcessText extends ExecProcess
	ExecProcessTextResult extends ExecProcessResult
	
	TODO refactor: eclipse coll, Objects.requireNonNull, exec_name/execName
	 */
}
