# Development environment setup

This page describes how to setup the `Trace Compass` development environment.

Note that this should only be required if you want to make modifications to the code or contribute patches. If you only want to run the plugin as a user, you would probably be better served by the stable or nightly builds, available on [https://projects.eclipse.org/projects/tools.tracecompass/downloads this page].

## Using the Eclipse Installer (Oomph)

You can use the Eclipse Installer to setup the Eclipse installation and everything required to get started with `Trace Compass` development. It downloads Eclipse, sets the API baseline, clones the `Trace Compass` git repository and imports the projects for you.

* Download the Eclipse Oomph installer, available at https://wiki.eclipse.org/Eclipse_Oomph_Installer
* Extract and start the installer.
* After the initial preference questions, at the package selection, click the menu button in the top-right corner and select `Advanced Mode`.
* If you are behind a proxy, at this point you might want to double check your network settings by clicking in the "Network Proxy Settings" at the bottom.
* Under Eclipse.org, Select Eclipse IDE for Eclipse Committers. Click `Next`.
* Under Eclipse Projects, double-click on `Trace Compass`. Click `Next`.
* Enter installation folder name, workspace name, etc. Click `Next`, `Finish`.

After the Eclipse workbench is installed and launched, wait for the `Executing startup tasks` job to be completed, then you should have a complete Eclipse environment to work on Trace Compass.

## Manual setup of development environment

### Getting the Eclipse SDK

Under [eclipse.org download packages](https://www.eclipse.org/downloads/packages/), select the `Eclipse IDE for Eclipse Committers` archive. You can start with any Eclipse pre-package, but make sure you have the `Eclipse Plug-in Development Environment` feature installed.

Uncompress and start Eclipse. Example for Linux:

```bash
tar xzvf eclipse-committers-2023-12-R-linux-gtk-x86_64.tar.gz 
cd eclipse
./eclipse
```

The first time you run it, it will ask for a workspace directory. You can use the default location.

You will then be greeted by the welcome screen, which you can dismiss by clicking the `Workbench` arrow in the upper-right corner.

### Setup Java 17

`Trace Compass` uses source compatibility to Java 11. However, to run the eclipse.org download packages RCP and to develop it, Java 17 is required. Here is how to install Java 17 on recent Ubuntu:

``` bash
sudo apt-get install openjdk-17-jdk
```

For old versions of Ubuntu, install from the [OpenJDK PPA](https://launchpad.net/~openjdk-r/+archive/ubuntu/ppa).

Next, the JRE must be added in Eclipse. 

* Select `Window -> Preferences`.
* Navigate to `Java -> Installed JREs`.
* Click `Add...` and enter the path to Java 17 home directory (ex: /usr/lib/jvm/java-17-openjdk-amd64).
* Check the Java 17 entry to use it by default for new projects and run configurations.

Note: if you migrate your environment to Java 17, existing debug and run configurations will continue to use the previous JRE. To use the new JRE, open the run configuration pannel `Run -> Run configurations...` and from the tab "Main", select the appropriate JRE under "Java Runtime Environment -> Execution environment".

### Setup Maven

To be able run a Maven build from command-line, Maven version v3.9.x or later needs to be installed. Follow the [installation guide](https://maven.apache.org/install.html) for that.

### Setup Maven Plug-ins

When developing for the `Trace Compass incubator` project, make sure to install the `M2E` plug-ins to your Eclipse IDE. You can omit these steps if not developing incubator features. This is a recent addition to `M2E` and requires an `Eclipse IDE 2021-06` or later.

To install `M2E`, select menu `Help -> Install New Software...`. An `Install` dialog will open. Select the release update site from the `Work with` drop-down menu. For example, if your IDE is based on `Eclipse 2023-03` then select `2023-03 - http://download.eclipse.org/releases/2023-03`. Then select `M2E- Integration for Eclipse` and `M2E - PDE Integration` and select `Next` button and the `Finish`. Restart Eclipse when asked for it.

### Getting the source code for org.eclipse.tracecompass

First, make sure you have a Git client installed (either the git command-line tool, or the [Eclipse Git Team Provider](http://www.eclipse.org/egit/) plugin, also available in Eclipse's `Install New Software`.

Then, simply clone the following repository somewhere on your hard drive:
* <https://github.com/eclipse-tracecompass/org.eclipse.tracecompass>

```bash
git clone https://github.com/eclipse-tracecompass/org.eclipse.tracecompass.git
```

If adding tests that require an actual trace, then it should be added to the following trace repository first:

* https://github.com/eclipse-tracecompass/tracecompass-test-traces

```bash
git clone https://github.com/eclipse-tracecompass/tracecompass-test-traces.git
```

Extra functionalities are developed in the `Trace Compass Incubator` sub-project. If interested in any of those feature, the following repository can be cloned

* https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator.git

```bash
git clone https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator.git
```

### Import the `Trace Compass` projects into the workspace

* Select `File -> Import...`
* Select `General -> Existing Projects into Workspace`
* Next to `Select root directory` click `Browse...`
* Navigate to the directory where you git clone'd the project earlier.
* It should now list the available plugins. Make sure they are all checked and click `Finish`.
* The plugins should now be added to your workspace, and visible in the Package Explorer view on the left.

You will probably get a bunch of build errors at this point. DON'T PANIC! This is because `Trace Compass` needs additional dependencies that might not be present in your current Eclipse installation. We will install those in the following section.

### Setting the Target Platform

Eclipse offers the ability to set target platforms, which will download a specific environment to run your plugins, without having to "pollute" your Eclipse install. `Trace Compass` ships target definition files, which is the recommended way of installing its dependencies.
**If you have downloaded the `Trace Compass Incubator` sub-project you should use the org.eclipse.tracecompass.incubator.target project instead.**

To set the target platform for the `Trace Compass` plugins:

* Expand the `org.eclipse.tracecompass.target` project.
* Double-click the .target file (e.g. tracecompass-e4.30.target, at the time of this writing).
* In the view that just opened, click `Set as Target Platform` on the top right.

Eclipse will now download the required dependencies, which may take some time the first time.

If you wish to switch target platforms in the future, you can come back to this plugin and redo the steps above for another target file, or you can go to `Window -> Preferences -> Plug-in Development -> Target Platform`. From that page, you can switch between any of the known targets, or your base Eclipse runtime.

### Defining an API baseline

`Starting with Eclipse 4.5.0, it is no longer necessary to download and maintain a separate Eclipse installation for the API baseline. You can simply use a target definition file, as explained below.`

Since `Trace Compass` is out of incubation with its 1.0 release, all API changes have to be tracked and marked with the proper @since annotations. The Eclipse API tool can compare the contents of the current workspace with the last stable version, defined in a target definition file.

To set up the API baseline, follow these steps:

* Select ``Window -> Preferences`. In the window that opens, select `Plug-in Development -> API Baselines` on the left pane.
* Click on `Add Baseline...`
* Choose `A target platform` and click `Next`.
* In the next page check the box next to the target which contains "baseline" in its name, like `tracecompass-baseline-9.2`. Use the version of `Trace Compass` that you're are developing against, which is usually the latest release.
* Click `Refresh` to download the contents of the target.
* Specify a name for this baseline in the top area, like `Trace Compass 9.2` for example.
* Click `Finish`, then `OK` in the previous dialog.

It should offer you do to a full rebuild. You can click `Yes` at this point.

Once that is done, your workspace will be rebuilt and the API changes will now be tracked. Any new method or class will have to be annotated with `@since n`, where n is the major.minor version number found in the plugin's MANIFEST.MF file.

### Build the documentation (optional)

If you imported the *.help plugins (which contain the user and developer guides), you might notice warnings in these plugins because of missing files. It is because the documentation plugins need to be built using maven. If you do not care for the documentation, you can ignore those warnings, or even remove those plugins from your workspace.

You can now build the documentation plugins using maven build from the command-line from the `Trace Compass` root directory:

```bash
mvn clean install -DskipTests=true
```

After it is built, the warning should disappear, and the HTML files should be present in its `doc/` subdirectory.

Note that this builder does not run automatically ; Ant is not very smart at figuring out which files were changed, so it would end up constantly rebuilding the doc plugins for nothing. For this reason, if you modify the source (.mediawiki) files, you will have to rebuild the HTML manually, using the same method.

### Running (or Debugging) the plugins

To run (or debug) the code, start a nested Eclipse with the plugins loaded:

* Right-click the "org.eclipse.tracecompass.tmf.core" plugin
* Select "Run As -> Eclipse Application" (or "Debug As -> Eclipse Application" to run in debug mode).

Alternatively, run (or debug) the Trace Compass RCP:

* Open file `tracing.product` from plug-in `org.eclipse.tracecompass.rcp.product`
* The `product configuration editor` will open
* Select the `Overview` tab at the bottom of the `product configuration editor`
* Click on `Run` (or Debug) button at the top left corner of the `product configuration editor`

The next time you can just select `Eclipse Application` or `tracing.product` from the Run (or Debug) icon in the toolbar.

### Running test suite with Maven

Before submitting a PR to GitHub, it may be useful to validate changes for possible regression. Running the GUI tests opens windows that will interfere with the main desktop session. On Linux, the windows can be redirected to a virtual display.

### Static code verification

In the testing phase, it is suggested to use [Findbugs](http://www.vogella.com/tutorials/Findbugs/article.html FindBugs) to check the code for common errors. Coding conventions are checked with Checkstyle. Bad practices are verified by (PMD).

Conversely, one can use [sonar for eclipse](https://marketplace.eclipse.org/content/sonarlint) to check all the issues in one place.

You can also consult the static analysis results at:

* [Trace Compass at sonarcloud.io](https://sonarcloud.io/project/overview?id=org.eclipse.tracecompass)
* [Trace Compass Incubator at sonarcloud.io](https://sonarcloud.io/project/overview?id=org.eclipse.tracecompass.incubator)

### Setup virtual display on Ubuntu

* Install Xephyr and Metacity: sudo apt-get install xserver-xephyr metacity
* Start the virtual display: Xephyr :2 -screen 1024x768 &
* Setup environment: export DISPLAY=:2
* Start the Metacity window manager: metacity --replace &
* Execute the tests: mvn clean install
