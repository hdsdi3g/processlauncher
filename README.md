# Processlauncher

Start process with Java, with more pratical tools. 

Tested on Windows 10 and Linux. Should be ok on macOS.

Please use Maven and Java 11 (OpenJDK) for build and test.

Use internally Log4j2 for logging.

![Java CI with Maven](https://github.com/hdsdi3g/processlauncher/workflows/Java%20CI%20with%20Maven/badge.svg)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hdsdi3g_processlauncher&metric=alert_status)](https://sonarcloud.io/dashboard?id=hdsdi3g_processlauncher)

![CodeQL](https://github.com/hdsdi3g/processlauncher/workflows/CodeQL/badge.svg)

## Usage example

See `tv.hd3g.processlauncher.Exec` for shortcuts examples.

## Functionalities

  - Create process, watches it, kill it (QUIT), check if run is correctly completed
  - Catch std-out and std-err with text support, during the execution, and after.
  - Can interact with process on std-in/out/err on the fly
  - Provide an API for command line parameters
    - simply add new parameters
    - parse raw command line and extract parameters, manage _"_ and space separation.
    - get parameters value
    - use simple template with command line: the command line can be configurable and code can inject variable values
  - Provide an API for search and found executable file after its names, via classpath, system path, configurable paths, and adapt execnames on Windows (add extention).
  - Can stop process after a max execution time
  - Can be callback just after the run starts and after the running ends.
  - Can just prepare and extract a Java ProcessBuilder (for an execution outside this API).
  - Manage sub-process killing
  - Automatically kill all running process (and sub-process) if the Java app is closing.  

## Test

Use maven and Junit for run internal UT and IT.

## API organisation and relation

[![Java diagram](https://raw.githubusercontent.com/hdsdi3g/processlauncher/master/code-organization.png)](https://raw.githubusercontent.com/hdsdi3g/processlauncher/master/code-organization.png)

## Licence

LGPL v3
 
