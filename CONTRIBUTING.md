# Contributing to Eclipse Trace Compass

Thanks for your interest in this project. This page explains how to contribute code to the Trace Compass project.

## Terms of Use

This repository is subject to the [Terms of Use of the Eclipse Foundation][terms].

## Code of Conduct

This project is governed by the [Eclipse Community Code of Conduct](CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code.

## Eclipse Development Process

This Eclipse Foundation open project is governed by the [Eclipse Foundation
Development Process][dev-process] and operates under the terms of the [Eclipse IP Policy][ip-policy].

## Eclipse Contributor Agreement

In order to be able to contribute to Eclipse Foundation projects you must
electronically sign the [Eclipse Contributor Agreement (ECA)][eca].

The ECA provides the Eclipse Foundation with a permanent record that you agree
that each of your contributions will comply with the commitments documented in
the Developer Certificate of Origin (DCO). Having an ECA on file associated with
the email address matching the "Author" field of your contribution's Git commits
fulfills the DCO's requirement that you sign-off on your contributions.

For more information, please see the [Eclipse Committer Handbook][commiter-handbook].

## Setting up the development environment

To setup the environment to build Trace Compass from within Eclipse, follow the
instruction in the [DEVELOPMENT_ENV_SETUP](DEVELOPMENT_ENV_SETUP.md) file.

## Building the application

In order to build the application from command-line, Maven is used. For more 
information about building the application see the description in the
[BUILDING](BUILDING.md) file.

## Source code tree
This source tree contains the source code for the Trace Compass plugins for
Eclipse.

The plugins are categorized as follows:

    analysis/    | Generic extensions to the base framework
    btf/         | Best Trace Format (BTF) integration
    common/      | Generic utilities that can be used by other plugins
    ctf/         | Common Trace Format (CTF) reader library
    doc/         | Documentation and code examples
    gdbtrace/    | Support for reading and viewing GDB traces
    lttng/       | LTTng integration
    pcap/        | libpcap integration
    rcp/         | Code specific to the RCP version
    releng/      | Releng-related plugins
    statesystem/ | State System library
    tmf/         | Core framework

See the [components.svg](components.svg) file for a diagram showing the dependencies between the
different components.

## When to submit patches

Remember that contributions are always welcome!

If you have a simple and straightforward fix to an obvious bug, feel free to push
it directly to the project's GitHub (see below).

This project uses GitHub issues to track ongoing development and issues. In order
to contribute, please first [open an issue][issues] that clearly describes the
bug you intend to fix or the feature you would like to add. Make sure you provide
a way to reproduce the bug or test the proposed feature.

If you wish to work on a larger problem or feature, it would be a good idea to 
[contact us](#contact) first. It could avoid duplicate work in case somebody is
already working on the same thing. For substantial new features, it is always good
to discuss their design and integration first.

Be sure to search for existing bugs before you create another one. 

Note that before migrating to GitHub issues ongoing development and issues were
tracked using Bugzilla. The bugs were not batch migrated. Only selected issues
have been migrated to GitHub issues. You can search for [here][bugzilla]
for Bugzilla issues.

## Where to submit

The Trace Compass project uses GitHub pull requests for submitting patches and review contributions

Once you have your code ready for review, please  [open a pull request][pull-requests]. Please follow
the [pull request guidelines][pr-guide]. If you are new to the project, the
[new contributors][new-contributors] section provides useful information to get you started. A
committer of the Trace Extension will then review your contribution and help to get it merged.

### The review process

We will try to give feedback to new patches in a reasonable amount of time, usually in the following days. Smaller patches makes the review go faster.

With the exception of “trivial patches”, as in, patches that either don’t change the compiled code (e.g., comments, commit messages), we try to have two (2) committers review every patch that goes in. For a patch from a committer, this means a second committer should review it, even for the trivial ones. For a patch from a non-committer, one committer should do a thorough review, and at least a second one should give a quick look.

A committer may update the patch being submitted, then the original author must review it. This is useful if the author has minor nits to fix, e.g., spaces.

For a patch to be approved, it needs to be marked as `Approved` and all automatic checks need to be successful. Code Review means the patch "looks" good, it follows the coding style, uses relevant algorithms and data structures, etc. Verified in general means that the patch technically works, it compiles, the tests pass, and it does what it is supposed to do.

A reviewer may request changes if pull request needs updates or if verfication reasons.

The meanings of each score are listed below. 

## Code Review

`Approved:` The reviewer considers that the patch follows the coding style, and implements the expected functionality correctly. Reviewers are strongly encouraged to run and verify the contribution of the pull requests, except very trivial ones. At least one `Approved` is required for the patch to go in. Only committers can give this score.

`Commented:` The reviewer considers that this patch is good, but that someone else should also take a look at parts or the totality of the patch. Should be accompanied with a comment indicating what is missing to approve it if the review is from a committer. Anybody with write access can give this score.

`Changes Requested:` There are some things that should be fixed first. Should always be accompanied with general and/or inline comments indicating areas of the code that need improvement. All comments that are given are expected to be either A) fixed in a subsequent patchset or B) replied to with explanation/justification. If the overall direction of the patch is not the right one and it warrants further discussion, the reviewer needs to state that in the pull request. Meaning discussions on the pull request, GitHub issues, the mailing list, or in person. 

## Jenkins review

For each submitted pull request a Jenkins verification job is triggered which executes a full build including JUnit tests. The verification results will be shown in the `Checks` section of the pull request.

## Pull request guidelines

**Changes to the project** are made by submitting code with a pull request (PR).

* [How to write and submit changes][creating-changes]

**Good commit messages** make it easier to review code and understand why the changes were made.
Please include a:

* `Title:` Concise and complete title written in imperative (e.g. "Update Gitpod demo screenshots"
or "Single-click to select or open trace")
* `Problem:` What is the situation that needs to be resolved? Why does the problem need fixing?
Link to related issues (e.g. "Fixes #317").
* `Solution:` What changes were made to resolve the situation? Why are these changes the right fix?
* `Impact:` What impact do these changes have? (e.g. Numbers to show a performance improvement,
screenshots or a video for a UI change)
* [*Sign-off:*][sign-off] Use your full name and a long-term email address. This certifies that you
have written the code and that, from a licensing perspective, the code is appropriate for use in open-source.

Other commit information:

* [How to format the message][commit-message-message]
* Your patch should not introduce any error or warning.
* Follow the project's [coding style][code-style]. It is defined in Eclipse project settings, which are committed in the tree, so simply hitting Ctrl+Shift+F in Eclipse should auto-format the current file.
  * Basically, 4 spaces for indentation, opening brackets on the same line.
  * We've turned off the auto-wrapping and unwrapping of lines, because it was doing more harm than good. Please wrap lines to reasonable lengths (100-120 is a good soft limit). Do NOT wrap after '.' (method invocations), it makes the code less readable. After '(' or ',' is usually good.
* Split your larger contributions in smaller, independent pull requests 'that could go in independently of each other'. It is not unusual to have patch #1 go in quickly, but then have patch #2 go through many rounds of review. Smaller commits make the process faster for everyone (remember, the time taken to review a patch is ''n^2'', where ''n'' is the number of lines in the patch!)
* Not a hard requirement, but we normally prefix the commit titles with the name of the component to which the patch applies, in lowercase. This normally corresponds to the plugin name, so "lttng: xxxx" or "tmf: xxxx".
* If the patch is significant enough to have a mention in the New & Noteworthy, annotate the patch with `[Fixed]`, `[Added]`, `[Changed]`, `[Deprecated]`, or `[Security]` with a short description of what the change does.

# API policy

For more information about the API policies, read the [API_POLICY](API_POLICY.md) file.

## Contact

Contact the project developers via the [project's "dev" mailing list][mailing-list] or open an [issue tracker][issues].

[bugzilla]: https://bugs.eclipse.org/bugs/buglist.cgi?product=Tracecompass
[code-style]: https://wiki.eclipse.org/Trace_Compass/Code_Style
[commit-message-message]: https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
[commiter-handbook]: https://www.eclipse.org/projects/handbook/#resources-commit
[creating-changes]: https://www.dataschool.io/how-to-contribute-on-github/
[dev-process]: https://eclipse.org/projects/dev_process
[eca]: http://www.eclipse.org/legal/ECA.php
[ip-policy]: https://www.eclipse.org/org/documents/Eclipse_IP_Policy.pdf
[issues]: https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/issues
[mailing-list]: https://dev.eclipse.org/mailman/listinfo/tracecompass-dev
[pr-guide]: #pull-request-guidelines
[pull-requests]: https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/pulls
[sign-off]: https://git-scm.com/docs/git-commit#Documentation/git-commit.txt---signoff
[terms]: https://www.eclipse.org/legal/termsofuse.php
