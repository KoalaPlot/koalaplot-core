# Contributing to Koala Plot

:+1::tada: First off, thanks for taking the time to contribute! :tada::+1:

The following is a set of guidelines for contributing to Koala Plot and its repositories,
which are hosted in the [KoalaPlot Organization](https://github.com/KoalaPlot) on GitHub.
These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to
this document in a pull request.

#### Table Of Contents

[I don't want to read this whole thing, I just have a question!!!](#i-dont-want-to-read-this-whole-thing-i-just-have-a-question)

[What should I know before I get started?](#what-should-i-know-before-i-get-started)

[How Can I Contribute?](#how-can-i-contribute)

* [Reporting Bugs](#reporting-bugs)
* [Suggesting Enhancements](#suggesting-enhancements)
* [Your First Code Contribution](#your-first-code-contribution)
* [Pull Requests](#pull-requests)

[Styleguides](#styleguides)

* [Git Commit Messages](#git-commit-messages)
* [Kotlin Styleguide & Coding Standards](#kotlin-styleguide--coding-standards)
* [Documentation Styleguide](#documentation-styleguide)

## I don't want to read this whole thing I just have a question!!!

> **
> Note:** Please don't file an issue to ask a question.
> You'll get faster results by using the resources below.

We have an official message board with a detailed FAQ and where the community chimes in with helpful
advice if you have questions.

* [Github Discussions](https://github.com/KoalaPlot/koalaplot-core/discussions)

## What should I know before I get started?

### Multiplatform & Compose Centric

Koala Plot is a multiplatform plotting and charting library with an objective to maintain the same interface
and feature set across all supported platforms. Therefore, new contributions should address any
platform differences in order to eliminate platform variations in the publicly exposed API.

The library is also Compose centric, and seeks to adhere to conventions adopted by
the [Compose API](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md)
.

### API Stability

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) to provide
API stability between releases and a clear deprecation and backwards compatibility change process.

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report. Following these guidelines helps
maintainers and the community understand your report :pencil:, reproduce the behavior :computer:,
and find related reports :mag_right:.

Before creating bug reports, please check [this list](#before-submitting-a-bug-report) as you might
find out that you don't need to create one. When you are creating a bug report,
please [include as many details as possible](#how-do-i-submit-a-good-bug-report).

> **Note:** If you find a **Closed** issue that seems like it is the same thing that you're
> experiencing, open a new issue and include a link to the original issue in the body of your new
> one.

#### Before Submitting A Bug Report

* **Check the [discussions](https://github.com/KoalaPlot/koalaplot-core/discussions)**
  for a list of common questions and problems.
* **Perform a [cursory search](https://github.com/KoalaPlot/koalaplot-core/issues)** to see
  if the
  problem has already been reported. If it has **and the issue is still open**, add a comment to the
  existing issue instead of opening a new one.

#### How Do I Submit A (Good) Bug Report?

Bugs are tracked as [GitHub issues](https://guides.github.com/features/issues/). After you've
determined which repository your bug is related to, create an issue on that
repository and provide the following information.

Explain the problem and include additional details to help maintainers reproduce the problem:

* **Use a clear and descriptive title** for the issue to identify the problem.
* **Describe the exact steps which reproduce the problem** in as many details as possible.
* **Provide specific examples to demonstrate the steps**. Include links to files or GitHub projects,
  or copy/pasteable snippets, which you use in those examples. If you're providing snippets in the
  issue,
  use [Markdown code blocks](https://help.github.com/articles/markdown-basics/#multiple-lines).
* **Describe the behavior you observed after following the steps** and point out what exactly is the
  problem with that behavior.
* **Explain which behavior you expected to see instead and why.**
* **Include screenshots and animated GIFs** which show you following the described steps and clearly
  demonstrate the problem. You can
  use [this tool](https://www.cockos.com/licecap/) to record GIFs on macOS and Windows,
  and [this tool](https://github.com/colinkeenan/silentcast)
  or [this tool](https://github.com/GNOME/byzanz) on Linux.
* **If you're reporting that Koala Plot threw an exception**, include a stack trace from the
  operating system. Include the stack trace in the issue in
  a [code block](https://help.github.com/articles/markdown-basics/#multiple-lines),
  a [file attachment](https://help.github.com/articles/file-attachments-on-issues-and-pull-requests/)
  , or put it in a [gist](https://gist.github.com/) and provide the link to that gist.

Provide more context by answering these questions:

* **Did the problem start happening recently** (e.g. after updating to a new version) or was
  this always a problem?
* If the problem started happening recently, **can you reproduce the problem in an older version?**
* What's the most recent version in which the problem doesn't happen?
* **Can you reliably reproduce the issue?** If not, provide details about how often the problem
  happens and under which conditions it normally happens.

Include details about your configuration and environment:

* **Which version of Koala Plot, Kotlin, and other libraries are you using?**
* **What's the name and version of the OS you're using**?
* **Does it occur on all platforms or only some of them**? Which ones and what versions?

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for Koala Plot,
including completely
new features and minor improvements to existing functionality. Following these guidelines helps
maintainers and the community understand your suggestion :pencil: and find related suggestions
:mag_right:.

Before creating enhancement suggestions, please
check [this list](#before-submitting-an-enhancement-suggestion) as you might find out that you don't
need to create one. When you are creating an enhancement suggestion,
please [include as many details as possible](#how-do-i-submit-a-good-enhancement-suggestion).

#### Before Submitting An Enhancement Suggestion

* **Perform a [cursory search](https://github.com/KoalaPlot/koalaplot-core/issues)** to
  see if the
  enhancement has already been suggested. If it has, add a comment to the existing issue instead of
  opening a new one.

#### How Do I Submit A (Good) Enhancement Suggestion?

Enhancement suggestions are tracked as [GitHub issues](https://guides.github.com/features/issues/).
After you've determined which repository your enhancement suggestion is
related to, create an issue on that repository and provide the following information:

* **Use a clear and descriptive title** for the issue to identify the suggestion.
* **Provide a step-by-step description of the suggested enhancement** in as many details as
  possible.
* **Provide specific examples to demonstrate the steps**. Include copy/pasteable snippets which you
  use in those examples,
  as [Markdown code blocks](https://help.github.com/articles/markdown-basics/#multiple-lines).
* **Describe the current behavior** and **explain which behavior you expected to see instead** and
  why.
* **Include screenshots and animated GIFs** which help you demonstrate the steps or point out the
  part of Koala Plot which the suggestion is related to. You can
  use [this tool](https://www.cockos.com/licecap/) to record GIFs on macOS and Windows,
  and [this tool](https://github.com/colinkeenan/silentcast)
  or [this tool](https://github.com/GNOME/byzanz) on Linux.
* **Explain why this enhancement would be useful** to most Koala Plot.
* **List some other charting libraries or applications where this enhancement exists.**
* **Specify which version of Koala Plot you're using.**
* **Specify the name and version of the OS you're using.**

### Your First Code Contribution

Unsure where to begin contributing to Koala Plot? You can start by looking through these `beginner`
and `help-wanted` issues:

* [Beginner issues][beginner] - issues which should only require a few lines of code, and a test or
  two.
* [Help wanted issues][help-wanted] - issues which should be a bit more involved than `beginner`
  issues.

Both issue lists are sorted by total number of comments. While not perfect, number of comments is a
reasonable proxy for impact a given change will have.

### Pull Requests

The process described here has several goals:

- Maintain Koala Plot quality
- Fix problems that are important to users
- Engage the community in working toward the best possible KiwkCharts
- Enable a sustainable system for Koala Plot maintainers to review contributions

Please follow these steps to have your contribution considered by the maintainers:

1. Follow the [styleguides](#styleguides)
2. Use concise and descriptive pull request titles
3. Provide a short summary of your changes in the pull request description, noting any related
   issues that your pull request solves
4. Avoid multiple unrelated changes in a single pull request
5. Include an update to the change log, if necessary, in accordance with
   the [keep a changelog](https://keepachangelog.com/en/1.0.0/) guidelines
7. After you submit your pull request, verify that
   all [status checks](https://help.github.com/articles/about-status-checks/) are
   passing <details><summary>What if the status checks are failing?</summary>If a status check is
   failing, and you believe that the failure is unrelated to your change, please leave a comment on
   the pull request explaining why you believe the failure is unrelated. A maintainer will re-run
   the status check for you. If we conclude that the failure was a false positive, then we will open
   an issue to track that problem with our status check suite.</details>

While the prerequisites above must be satisfied prior to having your pull request reviewed, the
reviewer(s) may ask you to complete additional design work, tests, or other changes before your pull
request can be ultimately accepted.

## Styleguides

### Git Commit Messages

* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
* Limit the first line to 72 characters or less
* Reference issues and pull requests liberally after the first line
* When only changing documentation, include `[ci skip]` in the commit title
* Consider starting the commit message with an applicable emoji:
    * :art: `:art:` when improving the format/structure of the code
    * :racehorse: `:racehorse:` when improving performance
    * :non-potable_water: `:non-potable_water:` when plugging memory leaks
    * :memo: `:memo:` when writing docs
    * :penguin: `:penguin:` when fixing something on Linux
    * :apple: `:apple:` when fixing something on macOS
    * :checkered_flag: `:checkered_flag:` when fixing something on Windows
    * :bug: `:bug:` when fixing a bug
    * :fire: `:fire:` when removing code or files
    * :green_heart: `:green_heart:` when fixing the CI build
    * :white_check_mark: `:white_check_mark:` when adding tests
    * :lock: `:lock:` when dealing with security
    * :arrow_up: `:arrow_up:` when upgrading dependencies
    * :arrow_down: `:arrow_down:` when downgrading dependencies
    * :shirt: `:shirt:` when removing linter warnings

### Kotlin Styleguide & Coding Standards

In general, Koala Plot uses
the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with some
minor changes, notably to align
with [Compose conventions](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md)
.

All code is scanned using [detekt](https://detekt.dev/),
including [ktlint](https://ktlint.github.io/), to enforce code standards, best practices,
and formatting. Use of .editorconfig and
the [Detekt IDEA Plugin](https://plugins.jetbrains.com/plugin/10761-detekt) helps to provide
real-time violation feedback in the IDE, but please also run the detekt job directly to ensure
compliance before submitting a pull request.

```
./gradlew detekt
```

When introducing new public API or changes to existing public API, please be thoughtful about
which API should be public and consider using internal or private where possible. Keep in
mind [Semantic Versioning](https://semver.org/spec/v2.0.0.html) and how your public API changes
will impact versioning and maintainability.

### Documentation Styleguide

Please document all public APIs using [KDoc](https://kotlinlang.org/docs/kotlin-doc.html) comments,
including at the minimum:

* A brief description of the class, method, function, or object
* Include `@param` and `@return` tags for all parameters
* If you explicitly throw any exceptions, include an `@throws` tag

Documentation is also highly encouraged for all internal and private members, and within code,
especially when complex algorithms, layouts, or calculations are being performed.

[Dokka](https://kotlin.github.io/dokka) is used to generate API documentation. You can generate
a documentation set to review your comments before submitting a pull request with the
command:

`./gradlew dokkaCustomFormat`

The generated documentation is placed in `./build/docs/api`.
