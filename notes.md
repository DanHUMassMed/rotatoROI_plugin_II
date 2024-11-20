### Create a groovy application
* Run `gradle init`
```
Found existing files in the project directory: '/Users/dan/Code/groovy/rotatoROI_plugin_II'.
Directory will be modified and existing files may be overwritten.  Continue? (default: no) [yes, no] yes

Select type of build to generate:
  1: Application
  2: Library
  3: Gradle plugin
  4: Basic (build structure only)
Enter selection (default: Application) [1..4] 1

Select implementation language:
  1: Java
  2: Kotlin
  3: Groovy
  4: Scala
  5: C++
  6: Swift
Enter selection (default: Java) [1..6] 3

Enter target Java version (min: 7, default: 21): 

Project name (default: rotatoROI_plugin_II): 

Select application structure:
  1: Single application project
  2: Application and library project
Enter selection (default: Single application project) [1..2] 

Select build script DSL:
  1: Kotlin
  2: Groovy
Enter selection (default: Groovy) [1..2] 2

Generate build using new APIs and behavior (some features may change in the next minor release)? (default: no) [yes, no] 
```

* Update `build.gradle`
```
plugins {
    id  'base'
    // Apply the groovy Plugin to add support for Groovy.
    id 'groovy'

    // Apply the application plugin to add support for building a CLI application in Java.
    id 'application'
}

base {
    version = '0.1.1'
    archivesName = rootProject.name
    group = 'com.amywalkerlab'
}


repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {

    implementation 'org.codehaus.groovy:groovy:3.0.4'

    implementation group: 'org.scijava', name: 'pom-scijava', version: '33.0.0', ext: 'pom'

    implementation group: 'net.imagej', name: 'ij', version: '1.54f'

    //implementation group: 'org.codehaus.groovy.modules.http-builder', name: 'http-builder', version: '0.7.1'

    //implementation group: 'org.apache.poi', name: 'poi-ooxml', version: '4.1.2'

    //implementation group: 'org.jfree', name: 'jfreechart', version: '1.5.3'

    //implementation group: 'org.jfree', name: 'jcommon', version: '1.0.24'

    // Use the awesome Spock testing and specification framework even with Java
    testImplementation libs.spock.core
    testImplementation libs.junit

    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

application {
    // Define the main class for the application.
    mainClass = 'com.amywalkerlab.rotato_roi_ii.RotatoROI_II'
}

tasks.named('test') {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

task copyJar(type: Copy) {
    dependsOn ':app:jar' 
    from './build/libs/'
    into '/Applications/Fiji.app/plugins/' // Specify the local directory you want to copy the JAR to
    include 'puncta_process_plugin-0.1.1.jar' // Include only JAR files
}


```

### To build the project 
./gradlew clean jar


### To deploy 
./gradlew copyJar
kill -9 $(ps auxww|grep Fiji|grep -v grep|cut -f15 -d' ')
open -a Fiji


### Utilities
__Rename files in a directory__
* find file in the current directory starting with lgg_ rename them to the same name but without the lgg_ prefix
* `find . -type f -name 'lgg_*' -exec bash -c 'mv "$0" "${0##*/lgg_}"' {} \;`


