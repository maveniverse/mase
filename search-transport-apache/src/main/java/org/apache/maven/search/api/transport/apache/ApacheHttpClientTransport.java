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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.search.api.transport.Transport;

/**
 * {@link CloseableHttpClient} backed transport.
 */
public class ApacheHttpClientTransport implements Transport {
    protected static class ResponseImpl implements Response {
        protected final String uri;

        protected final CloseableHttpResponse response;

        protected ResponseImpl(String uri, CloseableHttpResponse response) {
            this.uri = requireNonNull(uri);
            this.response = requireNonNull(response);
        }

        @Override
        public int getCode() {
            return response.getStatusLine().getStatusCode();
        }

        @Override
        public Map<String, String> getHeaders() {
            return Arrays.stream(response.getAllHeaders())
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getName().toLowerCase(Locale.ENGLISH), e.getValue()))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        }

        @Override
        public InputStream getBody() throws IOException {
            return response.getEntity().getContent();
        }

        @Override
        public void close() throws IOException {
            response.close();
        }

        @Override
        public String toString() {
            return this.uri + " -> " + response.getStatusLine();
        }
    }

    protected final Duration timeout;

    protected final CloseableHttpClient client;

    public ApacheHttpClientTransport() {
        this(Duration.ofSeconds(10L));
    }

    public ApacheHttpClientTransport(Duration timeout) {
        this(timeout, HttpClientBuilder.create().build());
    }

    public ApacheHttpClientTransport(Duration timeout, CloseableHttpClient client) {
        this.timeout = requireNonNull(timeout);
        this.client = requireNonNull(client);
    }

    @Override
    public Response get(String serviceUri, Map<String, String> headers) throws IOException {
        return execute(serviceUri, headers, new HttpGet(serviceUri));
    }

    @Override
    public Response head(String serviceUri, Map<String, String> headers) throws IOException {
        return execute(serviceUri, headers, new HttpHead(serviceUri));
    }

    protected Response execute(String serviceUri, Map<String, String> headers, HttpRequestBase req) throws IOException {
        req.setConfig(RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setConnectionRequestTimeout((int) timeout.toMillis())
                .setConnectTimeout((int) timeout.toMillis())
                .setSocketTimeout((int) timeout.toMillis())
                .build());
        for (Map.Entry<String, String> header : headers.entrySet()) {
            req.addHeader(header.getKey(), header.getValue());
        }
        return new ResponseImpl(serviceUri, client.execute(req));
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
