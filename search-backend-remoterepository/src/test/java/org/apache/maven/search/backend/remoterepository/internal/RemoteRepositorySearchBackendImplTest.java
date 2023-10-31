/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.search.backend.remoterepository.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.request.BooleanQuery;
import org.apache.maven.search.api.request.FieldQuery;
import org.apache.maven.search.api.request.Query;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchBackend;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchResponse;
import org.junit.jupiter.api.Test;

/**
 * UT for 2 backends: Maven Central and RAO releases. This tests make use of the fact that RAO is used as "staging"
 * area for Maven Central, hence RAO releases contains everything that was staged and synced to Maven Central.
 */
public abstract class RemoteRepositorySearchBackendImplTest {

    private final RemoteRepositorySearchBackend backend;

    public RemoteRepositorySearchBackendImplTest(RemoteRepositorySearchBackend backend) {
        this.backend = backend;
    }

    private void dumpSingle(AtomicInteger counter, List<Record> page) {
        for (Record record : page) {
            StringBuilder sb = new StringBuilder();
            sb.append(record.getValue(MAVEN.GROUP_ID)).append(":").append(record.getValue(MAVEN.ARTIFACT_ID));
            if (record.hasField(MAVEN.VERSION)) {
                sb.append(":").append(record.getValue(MAVEN.VERSION));
            }
            if (record.hasField(MAVEN.CLASSIFIER)) {
                sb.append(":").append(record.getValue(MAVEN.CLASSIFIER));
            }
            if (record.hasField(MAVEN.FILE_EXTENSION)) {
                sb.append(":").append(record.getValue(MAVEN.FILE_EXTENSION));
            }

            List<String> remarks = new ArrayList<>();
            if (record.getLastUpdated() != null) {
                remarks.add("lastUpdate=" + Instant.ofEpochMilli(record.getLastUpdated()));
            }
            if (record.hasField(MAVEN.VERSION_COUNT)) {
                remarks.add("versionCount=" + record.getValue(MAVEN.VERSION_COUNT));
            }
            if (record.hasField(MAVEN.HAS_SOURCE)) {
                remarks.add("hasSource=" + record.getValue(MAVEN.HAS_SOURCE));
            }
            if (record.hasField(MAVEN.HAS_JAVADOC)) {
                remarks.add("hasJavadoc=" + record.getValue(MAVEN.HAS_JAVADOC));
            }

            System.out.print(counter.incrementAndGet() + ". " + sb);
            if (!remarks.isEmpty()) {
                System.out.print(" " + remarks);
            }
            System.out.println();
        }
    }

    private void dumpPage(RemoteRepositorySearchResponse searchResponse) throws IOException {
        AtomicInteger counter = new AtomicInteger(0);
        System.out.println(
                "QUERY: " + searchResponse.getSearchRequest().getQuery().toString());
        System.out.println("URL: " + searchResponse.getUri());
        dumpSingle(counter, searchResponse.getPage());
        while (searchResponse.getTotalHits() > searchResponse.getCurrentHits()) {
            System.out.println("NEXT PAGE (size "
                    + searchResponse.getSearchRequest().getPaging().getPageSize() + ")");
            searchResponse = backend.search(searchResponse.getSearchRequest().nextPage());
            dumpSingle(counter, searchResponse.getPage());
            if (counter.get() > 50) {
                System.out.println("ABORTED TO NOT SPAM");
                break; // do not spam the SMO service
            }
        }
        System.out.println();
    }

    @Test
    public void smoke() {
        SearchRequest searchRequest = new SearchRequest(Query.query("smoke"));
        assertThrows(IllegalArgumentException.class, () -> backend.search(searchRequest));
    }

    @Test
    public void notFound404Response() throws IOException {
        // LIST GAs
        SearchRequest searchRequest =
                new SearchRequest(FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.cstamas.no-such-thing"));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertEquals(0, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
    }

    @Test
    public void g() throws IOException {
        // LIST GAs
        SearchRequest searchRequest =
                new SearchRequest(FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven.plugins"));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertNotEquals(0, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void ga() throws IOException {
        // LIST GAVs
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven.plugins"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "maven-clean-plugin")));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertNotEquals(0, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void gav() throws IOException {
        // LIST GAVCEs
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven.plugins"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "maven-clean-plugin"),
                FieldQuery.fieldQuery(MAVEN.VERSION, "3.1.0")));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertNotEquals(0, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void gavWithTarGz() throws IOException {
        // LIST GAVCEs
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "apache-maven"),
                FieldQuery.fieldQuery(MAVEN.VERSION, "3.9.3")));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertEquals(8, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void gavce() throws IOException {
        // EXISTENCE check: total hits != 0 => exists, total hits == 0 => not exists
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven.plugins"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "maven-clean-plugin"),
                FieldQuery.fieldQuery(MAVEN.VERSION, "3.1.0"),
                FieldQuery.fieldQuery(MAVEN.FILE_EXTENSION, "jar")));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertEquals(1, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void gavcesha1RightChecksum() throws IOException {
        // validity check: total hits != 0 => valid, total hits == 0 => invalid
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven.plugins"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "maven-clean-plugin"),
                FieldQuery.fieldQuery(MAVEN.VERSION, "3.1.0"),
                FieldQuery.fieldQuery(MAVEN.FILE_EXTENSION, "jar"),
                FieldQuery.fieldQuery(MAVEN.SHA1, "2e030994e207ee572491927b198b139424133b2e")));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertEquals(1, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void gavcesha1WrongChecksum() throws IOException {
        // validity check: total hits != 0 => valid, total hits == 0 => invalid
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven.plugins"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "maven-clean-plugin"),
                FieldQuery.fieldQuery(MAVEN.VERSION, "3.1.0"),
                FieldQuery.fieldQuery(MAVEN.FILE_EXTENSION, "jar"),
                FieldQuery.fieldQuery(MAVEN.SHA1, "wrong")));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertEquals(0, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void gavcesha1WClassifierRightChecksum() throws IOException {
        // validity check: total hits != 0 => valid, total hits == 0 => invalid
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "apache-maven"),
                FieldQuery.fieldQuery(MAVEN.VERSION, "3.9.3"),
                FieldQuery.fieldQuery(MAVEN.CLASSIFIER, "bin"),
                FieldQuery.fieldQuery(MAVEN.FILE_EXTENSION, "tar.gz"),
                FieldQuery.fieldQuery(MAVEN.SHA1, "f700d2bf6a11803c29a3240a26e91b2d1c530f79")));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertEquals(1, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void gavcesha1WClassifierWrongChecksum() throws IOException {
        // validity check: total hits != 0 => valid, total hits == 0 => invalid
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "apache-maven"),
                FieldQuery.fieldQuery(MAVEN.VERSION, "3.9.3"),
                FieldQuery.fieldQuery(MAVEN.CLASSIFIER, "bin"),
                FieldQuery.fieldQuery(MAVEN.FILE_EXTENSION, "tar.gz"),
                FieldQuery.fieldQuery(MAVEN.SHA1, "wrong")));
        RemoteRepositorySearchResponse searchResponse = backend.search(searchRequest);
        assertEquals(0, searchResponse.getTotalHits());
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void sha1() {
        SearchRequest searchRequest =
                new SearchRequest(FieldQuery.fieldQuery(MAVEN.SHA1, "8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12"));
        assertThrows(IllegalArgumentException.class, () -> backend.search(searchRequest));
    }

    @Test
    public void cn() {
        SearchRequest searchRequest =
                new SearchRequest(FieldQuery.fieldQuery(MAVEN.CLASS_NAME, "MavenRepositorySystem"));
        assertThrows(IllegalArgumentException.class, () -> backend.search(searchRequest));
    }

    @Test
    public void fqcn() {
        SearchRequest searchRequest = new SearchRequest(
                FieldQuery.fieldQuery(MAVEN.FQ_CLASS_NAME, "org.apache.maven.bridge.MavenRepositorySystem"));
        assertThrows(IllegalArgumentException.class, () -> backend.search(searchRequest));
    }
}
