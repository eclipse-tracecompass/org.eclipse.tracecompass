# Building the application

## Compiling manually

The Maven project build requires version 3.9 or later. It can be downloaded from
<http://maven.apache.org> or from the package management system of your distro.

It also requires Java version 11 or later.

To build the project manually using Maven, simply run the following command from
the top-level directory:

```
mvn clean install
```

The default command will compile and run the unit tests. Running the tests can
take some time, to skip them you can append `-Dmaven.test.skip=true` to the
`mvn` command:

```
mvn clean install -Dmaven.test.skip=true
```

Stand-alone application (RCP) packages will be placed in
`rcp/org.eclipse.tracecompass.rcp.product/target/products`.

The p2 update site, used for installation as plugins inside Eclipse, will be
placed in `releng/org.eclipse.tracecompass.releng-site/target/repository`.

To generate the javadoc from the Trace Compass source code, run the following
command from the top-level directory:

```
mvn compile javadoc:aggregate
```
The javadoc html files will be under `target/site/apidocs`.

## Tracing Trace Compass

Trace Compass can be traced by doing the following in the launch configuration:

* -vmargs
* -Djava.util.logging.config.file=%gitroot%/logging.properties (where %gitroot% is the directory tracecompass is checked out to)
* -Dorg.eclipse.tracecompass.logging=true

Additionally the folder the trace is being written to (default is `home/.tracecompass/logs`) needs to be created in advance. After running Trace Compass, a `trace_n.json` will be created in the tracing folder. It needs to be converted to true json, so use the `jsonify.sh` script in the root directory to convert it. Then it can be loaded into Trace Compass, if the **Trace Event format** is installed from the incubator, or from a web browser such as Chromium.

The performance impact is low enough as long as the log level is greater than "*FINEST*".

NOTE: thread 1 is always the UI thread for Trace Compass. Also, the thread numbers are the JVM threads and do not correspond necessarily to Process IDs.

## Maven profiles and properties

The following Maven profiles and properties are defined in
the build system. You can set them by using `-P[profile name]` and
`-D[property name]=[value]` in `mvn` commands.

* `-Dtarget-platform=[target]`

  Defines which target to use. This is used to build against various versions of
  the Eclipse platform. Available ones are in
  `releng/org.eclipse.tracecompass.target`. The default is usually the latest
  stable platform. To use the staging target for example, use
  `-Dtarget-platform=tracecompass-eStaging`.

NOTE: To support building for older platforms from the same source code tree, it is often required to
  modify the tracecompass.product file or the RCP feature.xml. You can find them under a sub-directory
  of the respective folders. For example, copy the legacy `tracing.product`:
`cp rcp/org.eclipse.tracecompass.rcp.product/legacy/tracing.product rcp/org.eclipse.tracecompass.rcp.product/`

If you encounter a problem for a certain target, please contact the Trace Compass
  development team on the Trace Compass mailing list <https://accounts.eclipse.org/mailing-list/tracecompass-dev>.

* `-Dskip-tc-core-tests`

  Skips the automated core tests. Not required when using
  `-Dmaven.test.skip=true` or `-DskipTests=true` , which already skips
  all the tests.

* `-Dskip-short-tc-ui-tests`

  Skips the short UI integration tests. Not required when using
  `-Dmaven.test.skip=true` or `-DskipTests=true`, which already skips
  all the tests.

* `-Dskip-long-tc-ui-tests`

  Skips the long UI integration tests. Not required when using
  `-Dmaven.test.skip=true` or `-DskipTests=true`, which already skips
  all the tests.

* `-Dskip-rcp`

  Skips building the RCP archives and related deployment targets. Only works in
  conjunction with `-Dskip-tc-long-ui-tests`, due to a limitation in Maven.

* `-Pctf-grammar`

  Re-compiles the CTF grammar files. This should be enabled if you modify the
  `.g` files in the `ctf.parser` plugin.

* `-Prun-custom-test-suite`

  Runs a test suite present in `releng/org.eclipse.tracecompass.alltests`. The
  test suite to run has to be defined by `-DcustomTestSuite=[name]`, for example
  `-DcustomTestSuite=RunAllPerfTests`.

* `-Pdeploy-rcp`

  Mainly for use on build servers. Copies the generated RCP archives, as well as
  the RCP-specific update site, to the paths specified by
  `-DrcpDestination=/absolute/path/to/destination` and
  `-DrcpSiteDestination=/absolute/path/to/destination`, respectively.

* `-Pdeploy-update-site`

  Mainly for use on build servers. Copies the standard update site (for the
  Eclipse plugin installation) to the destination specified by
  `-DsiteDestination=/absolute/path/to/destination`.

* `-Psign-update-site`

  Mainly for use on build servers. Signs all the generated update sites using
  the Eclipse signing server.

* `-Pdeploy-doc`

  Mainly for use on build servers. Copies the generated HTML documentation to
  the destination specified by `-DdocDestination=/absolute/path/to/destination`.
  Some directories may need to already exist at the destination (or Maven will
  throw related errors).

## Build Trace Compass image with Docker

To compile the image of Trace Compass with Docker, run the following command from the top level directory:

    docker build -f releng/dockerfiles/Dockerfile -t tracecompass .

The image will be tagged `tracecompass`.
