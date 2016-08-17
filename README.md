# properties-agent

Java [agent](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html) to load properties from a file. Open Source Java project under Apache License v2.0

### Current Stable Version is [1.0.0](https://search.maven.org/#search|ga|1|g%3Aorg.javastack%20a%3Aproperties-agent)

---

## DOC

#### Usage Example

```bash
# java -javaagent:properties-agent.jar=[!]url[,[!]url] ...
java -javaagent:properties-agent.jar=https://server/config/system-properties.properties,!file:///etc/system-properties.properties ...
```

###### Exclamation mark will force overwrite system property with value from file, by default only non-existent keys are loaded.

---

## MAVEN

    <dependency>
        <groupId>org.javastack</groupId>
        <artifactId>properties-agent</artifactId>
        <version>1.0.0</version>
    </dependency>

---
Inspired in [CatalinaProperties](https://github.com/apache/tomcat70/blob/TOMCAT_7_0_70/java/org/apache/catalina/startup/CatalinaProperties.java), this code is Java-minimalistic version.
