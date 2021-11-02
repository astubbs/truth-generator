# truth-generator
Subject generator library for Google Truth

# Usage

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