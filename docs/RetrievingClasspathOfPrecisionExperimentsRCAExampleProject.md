# Retrieval of Runnable Classpath of `precision-experiments-rca` Example Project

## Generate Example Project
1. Install required dependencies in local maven repository.
```bash
git clone -b develop https://github.com/DaGeRe/precision-experiments.git && cd precision-experiments/precision-analysis/ && ../gradlew publishToMavenLocal
```
And from another path:
```bash
git clone https://github.com/DaGeRe/pmd-check.git && cd pmd-check/analysis && ./mvnw clean install
```
2. Clone [precision-experiments-rca](https://github.com/DaGeRe/precision-experiments-rca) Github repository.
3. Run `mvn test` to ensure that all dependencies have been installed properly and `precision-experiments-rca` project works locally.
4. Then, from `precision-experiments-rca` run the following command to generate Example Project:
```bash
mvn clean package && java -jar target/precision-experiments-rca-0.1-SNAPSHOT.jar -out example-project -createBytecodeweavingEnvironment
```
5. In `precision-experiments-rca` project you will find `example-project` directory.

## Retrieving Classpath
1. Disable Kieker in ` src/test/java/de/dagere/peass/MainTest.java`
```java
@PerformanceTest(warmup=5, iterations=5, repetitions=100000, dataCollectors = "ONLYTIME", timeout=3600000, redirectToNull = true, useKieker=false)
```
2. In `example-project` run: ` mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=cp.txt`
3. You will need to figure out absolute paths for your target classes. Maven does not export classpaths for project classes, only for third-party dependencies. Add the following lines at the beginning of newly created `cp.txt`:
```text
/{absolute path}/example-project/target/test-classes:/{absolute path}/example-project/target/classes:........
```