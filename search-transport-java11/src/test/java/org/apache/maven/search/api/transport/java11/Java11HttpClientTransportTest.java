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
package org.apache.maven.search.api.transport.java11;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class Java11HttpClientTransportTest {
    @Test
    void closeClosesClient() throws Exception {
        // HttpClient is closeable (and has isTerminated) only since Java 21
        assumeTrue(Runtime.version().feature() >= 21);
        HttpClient client = HttpClient.newHttpClient();
        new Java11HttpClientTransport(Duration.ofSeconds(1), client).close();
        assertTrue((Boolean) HttpClient.class.getMethod("isTerminated").invoke(client));
    }
}
