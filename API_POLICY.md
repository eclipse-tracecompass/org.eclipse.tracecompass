# Trace Compass API policy
This page contains `Trace Compass` guidelines on API deprecation and API deletion. The Trace Compass Incubator project, as it's name indicates, doesn't follow this policy.

## Deprecation of APIs

The Trace Compass project provides APIs for building custom trace analysis and visualization applications. Despite the project's best efforts, the APIs evolve over the course of the project lifecycle, so that APIs need to be added, enhanced, and even removed. The Trace Compass project uses a light-weight process for deprecation and removal of APIs. API deprecation is used to inform API clients that a particular API is no longer recommended for use. Note that deprecated APIs still have to work as originally specified. 

## Process to deprecate an API
* Optionally open a bug report to discuss the API deprecation
* In the java-doc, add a @deprecated tag with a description if the reason for deprecation (optional), and provide steps for how to replace their usage with an alternative implementation. Note that deprecated APIs still have to work as originally specified.
* Mark the API element with @Deprecated annotation
* In the commit message, have one or more lines with starting with [Deprecated] to describe the deprecation. This lines will be shown in the [Note&Noteworthy][nan] of the following release.
* Push the change to Gerrit. Trace Compass committers will review and provide feedback.

## Identifying Deprecated API

This section describes how clients can identify what API is deprecated. To identify API from non-API, public APIs are part of java packages without the sub-string "internal" in it. The Trace Compass team will mark the java packages in the MANIFEST.MF file of the plug-ins as internal.

## Java API

Java API is deprecated through use of the @deprecated java-doc tag on types, methods, and fields. The java-doc paragraph following the @deprecated tag defines the rationale for the deprecation and instructions on moving to equivalent new API.

## Extension Points

Elements and attributes in extension points are deprecated by setting the "Deprecated" property to true in the Eclipse's `Plug-in Development Environment` extension point schema editor. The entire extension point can be deprecated by deprecating the "extension" element, which is the top level element at the root of any contribution to the extension point.

## Process to remove Deprecated APIs

The Trace Compass project is doing a major release version increase once a year, co-existing with the June release, and with the simultaneous Eclipse releases. Deprecated APIs can be removed for this release that follows the [Retention Policy](#retention-policy) below. 

To remove deprecated APIs, a Trace Compass committer will do the following steps:
* Create a [GitHub issue][issues] for discussion the removal of APIs, similar to example in bugzilla the [Bug572888][Bug572888]
* Make an announcement on the Trace Compass mailing list to inform the API clients about that major change, for example: [API Clean-up for Trace Compass 7.0][mail].
* Provide pull requests that remove API
  * Add reference of the issue tracker
  * In the commit message, add the commit sha of the commit where it was deprecated
  * In the commit message, have one or more lines with starting with [Removed] line to describe the removal(s) which will be picked up in the News@Noteworthy
  * Example: [Bug 579484: Remove deprecated segmentstore.SubSecondTimeWithUnitFormat][example-patch]

## Retention Policy

The Eclipse Trace Compass policy is to maintain deprecated API for at least one major release cycle. For example, if an API was deprecated for the minor release 6.3 in March 2021, and another API was deprecated for major release 7.0 in June 2021, both APIs can be removed for release 8.0 in June 2022. The retention time will give adopters at least 1 year, and usually 3 minor releases, time to adjust their code base before APIs are removed. 

API clients should follow the Trace Compass [Note&Noteworthy](nan) for each release to know about API deprecation of a release and plan to prepare their code base for the removal of APIs after this retention time. At any time after the deprecation, adopters can provide feedback and discuss to keep the deprecated API via bugzilla bug report. The announcement on the mailing list will provide a final warning where adopters can raise their concerns or adapt to the API changes.

[Bug572888]: https://bugs.eclipse.org/bugs/show_bug.cgi?id=572888
[example-patch]: https://git.eclipse.org/r/c/tracecompass/org.eclipse.tracecompass/+/193428
[issues]: https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/issues
[mail]: https://www.eclipse.org/lists/tracecompass-dev/msg01661.html
[nan]: https://wiki.eclipse.org/Trace_Compass/NewInTraceCompass