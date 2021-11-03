[![Build Status][ci-shield]][ci-link]
[![Maven Release][maven-shield]][maven-link]
[![Stackoverflow][stackoverflow-shield]][stackoverflow-link]

# truth-generator
Subject generator library for [Google Truth].

# Usage
This is incomplete - see [the tests app](https://github.com/astubbs/truth-generator/blob/master/plugin-maven/src/test/resources/project-to-test/pom.xml) for more usage.

    <build>
        <plugins>
            <plugin>
                <!-- mvn  io.stubbs.truth:truth-generator-maven-plugin:generate -->
                <groupId>io.stubbs.truth</groupId>
                <artifactId>truth-generator-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <classes>
                        <param>io.stubbs.truth.generator.testModel.MyEmployee</param>
                    </classes>
                    <entryPointClassPackage>io.stubbs.truth.extensions.tests.projectUnderTest</entryPointClassPackage>
                </configuration>
            </plugin>
        </plugins>
    </build>

<!-- references -->

[Google Truth]: https://truth.dev/
[ci-shield]: https://github.com/astubbs/truth-generator/workflows/CI/badge.svg?branch=master
[ci-link]: https://github.com/astubbs/truth-generator/actions
[maven-shield]: https://img.shields.io/maven-central/v/io.stubbs/truth-generator.png
[maven-link]: https://search.maven.org/artifact/io.stubbs/truth-generator
[stackoverflow-shield]: https://img.shields.io/badge/stackoverflow-google‐truth-5555ff.png?style=flat
[stackoverflow-link]: https://stackoverflow.com/questions/tagged/google-truth