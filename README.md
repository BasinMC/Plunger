[![License](https://img.shields.io/github/license/BasinMC/Plunger.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.txt)
[![GitHub Release](https://img.shields.io/github/release/BasinMC/Plunger.svg?style=flat-square)](https://github.com/BasinMC/Plunger/releases)
[![CircleCI](https://img.shields.io/circleci/project/github/BasinMC/Plunger.svg?style=flat-square)](https://circleci.com/gh/BasinMC/Plunger)

Plunger
=======

A modern source code and bytecode transformation library with built-in support for name and access
mappings.

# Table of Contents

* [Features](#features)
* [Usage](#usage)
* [Building](#building)
* [Contact](#contact)
* [Issues](#issues)
* [Contributing](#contributing)
* [License](#license)

Features
--------

* Bytecode and Sourcecode level transformation
* Comprehensive API
* Built-in support for Mapping
* Various standard transformers

Usage
-----

**Artifact Coordinates:** `org.basinmc:plunger:1.0-SNAPSHOT`

```xml
<repository>
  <id>basin-bintray</id>
  <name>Basin Releases</name>
  <url>https://dl.bintray.com/basin/maven/</url>
</repository>

<!-- ... -->

<dependency>
  <groupId>org.basinmc</groupId>
  <artifactId>plunger</artifactId>
  <version>1.0</version>
</dependency>
```

The library differentiates between two different main modes: Bytecode and Sourcecode where each
provides its unique set of transformers. For instance:

```java
Path source = ...;
Path target = ...;
AccessMapping mapping = ...;

Plunger plunger = BytecodePlunger.builder()
  .withTransformer(new AccessMappingBytecodeTransformer(mapping))
  .build(source, target);

// or

Plunger plunger = Plunger.sourceBuilder()
  .withTransformer(new AccessMappingSourcecodeTransformer(mapping))
  .build(source, target);

// both provide:
plunger.apply();
```

Note that the source and target paths may be located in any type of NIO filesystem (including the
jar file system). For your convenience, Plunger includes the factory methods
Plunger#openZipArchive(Path) and Plunger#createZipArchive(Path).

Building
--------

1. Clone this repository via ```git clone https://github.com/BasinMC/Plunger.git``` or download a [zip](https://github.com/BasinMC/Plunger/archive/master.zip)
2. Build the modification by running ```mvn clean install```
3. The resulting jars can be found in their respective ```target``` directories as well as your local maven repository

Contact
-------

* [IRC #Basin on EsperNet](http://webchat.esper.net/?channels=Basin)
* [Twitter](https://twitter.com/BasinMC)
* [GitHub](https://github.com/BasinMC/Plunger)

Issues
------

You encountered problems with the library or have a suggestion? Create an issue!

1. Make sure your issue has not been fixed in a newer version (check the list of [closed issues](https://github.com/BasinMC/Plunger/issues?q=is%3Aissue+is%3Aclosed)
1. Create [a new issue](https://github.com/BasinMC/Plunger/issues/new) from the [issues page](https://github.com/BasinMC/Plunger/issues)
1. Enter your issue's title (something that summarizes your issue) and create a detailed description containing:
   - What is the expected result?
   - What problem occurs?
   - How to reproduce the problem?
   - Crash Log (Please use a [Pastebin](https://gist.github.com) service)
1. Click "Submit" and wait for further instructions

Contributing
------------

Before you add any major changes to the library you may want to discuss them with us (see
[Contact](#contact)) as we may choose to reject your changes for various reasons. All contributions
are applied via [Pull-Requests](https://help.github.com/articles/creating-a-pull-request). Patches
will not be accepted. Also be aware that all of your contributions are made available under the
terms of the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt). Please read
the [Contribution Guidelines](CONTRIBUTING.md) for more information.

License
-------

This project is released under the terms of the
[Apache License](https://www.apache.org/licenses/LICENSE-2.0.txt), Version 2.0.

The following note shall be replicated by all contributors within their respective newly created
files (variables are to be replaced; E-Mail address or URL are optional):

```
Copyright <year> <first name> <surname <[email address/url]>
and other copyright owners as documented in the project's IP log.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

