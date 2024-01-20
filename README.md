# MASE

MASE is "Maven Search", a "quick fork" and drop in replacement of [Maven Indexer Search API](https://github.com/apache/maven-indexer).

Just change `groupId` and `version` to switch.

## Compatibility

As of Maven Indexer 7.1.2 and MASE 1.0.1 they are equivalent. This fork existed only for "quick pace prototyping", but all the patches are now present in Maven Indexer release 7.1.1 as well.

Recommended Search API to use is the one from Maven Indexer 7.1.2 release:
```
<dependency>
  <groupId>org.apache.maven.indexer</groupId>
  <artifactId>search-api</artifactId>
  <version>7.1.2</version>
</dependency>
```

