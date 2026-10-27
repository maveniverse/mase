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
package org.apache.maven.search.api.transport.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import org.apache.maven.search.api.transport.Transport;
import org.junit.jupiter.api.Test;

public class ApacheHttpClientTransportTest {
    @Test
    void repeatedResponseHeader() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Content-Security-Policy", "first");
            exchange.getResponseHeaders().add("Content-Security-Policy", "second");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try (ApacheHttpClientTransport transport = new ApacheHttpClientTransport();
                Transport.Response response =
                        transport.get("http://localhost:" + server.getAddress().getPort() + "/", Map.of())) {
            assertEquals("first", response.getHeaders().get("content-security-policy"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void headHasEmptyBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try (ApacheHttpClientTransport transport = new ApacheHttpClientTransport();
                Transport.Response response =
                        transport.head("http://localhost:" + server.getAddress().getPort() + "/", Map.of())) {
            assertEquals(-1, response.getBody().read());
        } finally {
            server.stop(0);
        }
    }
}
