package io.github.clescot.kafka.connect.http.sink.client.ahc;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.clescot.kafka.connect.http.core.HttpRequest;
import io.github.clescot.kafka.connect.http.core.HttpResponse;
import io.github.clescot.kafka.connect.http.sink.HttpSinkTaskTest;
import io.github.clescot.kafka.connect.http.sink.client.DummyX509Certificate;
import io.github.clescot.kafka.connect.http.sink.client.HttpClient;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.clescot.kafka.connect.http.sink.HttpSinkConfigDefinition.*;
import static io.github.clescot.kafka.connect.http.sink.HttpSinkConfigDefinition.CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_ALGORITHM;
import static io.github.clescot.kafka.connect.http.sink.client.ahc.AHCHttpClient.SUCCESS;
import static io.github.clescot.kafka.connect.http.sink.config.AddMissingCorrelationIdHeaderToHttpRequestFunction.HEADER_X_CORRELATION_ID;
import static io.github.clescot.kafka.connect.http.sink.config.AddMissingRequestIdHeaderToHttpRequestFunction.HEADER_X_REQUEST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AHCHttpClientTest {

    private AsyncHttpClient asyncHttpClient;

    @BeforeEach
    public void setUp() {
        asyncHttpClient = Mockito.mock(AsyncHttpClient.class);
    }

    @Test
    public void build_HttpExchange_test_all_null() {
        AHCHttpClient httpClient = new AHCHttpClient(asyncHttpClient);
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () ->
                httpClient.buildHttpExchange(null,
                        null,
                        Stopwatch.createUnstarted(),
                        OffsetDateTime.now(ZoneId.of(AHCHttpClient.UTC_ZONE_ID)),
                        new AtomicInteger(2),
                        SUCCESS
                ));
    }


    @Test
    public void build_HttpExchange_test_message_is_null() {
        AHCHttpClient httpClient = new AHCHttpClient(asyncHttpClient);
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () ->
                httpClient.buildHttpExchange(null,
                        getDummyHttpResponse(200),
                        Stopwatch.createUnstarted(),
                        OffsetDateTime.now(ZoneId.of(AHCHttpClient.UTC_ZONE_ID)),
                        new AtomicInteger(2),
                        SUCCESS));
    }

    @Test
    public void build_HttpExchange_test_response_code_is_lower_than_0() {
        AHCHttpClient httpClient = new AHCHttpClient(asyncHttpClient);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> httpClient.buildHttpExchange(getDummyHttpRequest(),
                getDummyHttpResponse(-12),
                Stopwatch.createUnstarted(),
                OffsetDateTime.now(ZoneId.of(AHCHttpClient.UTC_ZONE_ID)),
                new AtomicInteger(2),
                SUCCESS));
    }


    @Test
    public void build_HttpExchange_test_nominal_case() {
        HashMap<String,
                String> vars = Maps.newHashMap();
        AHCHttpClient httpClient = new AHCHttpClient(asyncHttpClient);
        httpClient.buildHttpExchange(getDummyHttpRequest(),
                getDummyHttpResponse(200),
                Stopwatch.createUnstarted(),
                OffsetDateTime.now(ZoneId.of(AHCHttpClient.UTC_ZONE_ID)),
                new AtomicInteger(2),
                SUCCESS);
    }

    private HttpResponse getDummyHttpResponse(int statusCode) {
        HttpResponse httpResponse = new HttpResponse(statusCode, "OK");
        httpResponse.setResponseBody("my response");
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put("Content-Type", Lists.newArrayList("application/json"));
        headers.put("X-stuff", Lists.newArrayList("foo"));
        httpResponse.setResponseHeaders(headers);
        return httpResponse;
    }


    @Test
    public void call_test_nominal_case() throws ExecutionException, InterruptedException {

        //given
        AsyncHttpClient asyncHttpClient = Mockito.mock(AsyncHttpClient.class);
        ListenableFuture<Response> listener = Mockito.mock(ListenableFuture.class);
        ListenableFuture<Object> listenerObject = Mockito.mock(ListenableFuture.class);
        Response response = Mockito.mock(Response.class);

        Mockito.when(response.getResponseBody()).thenReturn("body");
        int statusCode = 200;
        Mockito.when(response.getStatusCode()).thenReturn(statusCode);
        String statusMessage = "OK";
        Mockito.when(response.getStatusText()).thenReturn(statusMessage);

        Mockito.when(listener.get()).thenReturn(response);
        Mockito.when(listenerObject.toCompletableFuture()).thenReturn(CompletableFuture.supplyAsync(() -> response));
        Mockito.when(listenerObject.get()).thenReturn(response);
        Mockito.when(asyncHttpClient.executeRequest(ArgumentMatchers.any(Request.class))).thenReturn(listener);
        Mockito.when(asyncHttpClient.executeRequest(ArgumentMatchers.any(Request.class), ArgumentMatchers.any())).thenReturn(listenerObject);
        AHCHttpClient httpClient = new AHCHttpClient(asyncHttpClient);

        //when
        httpClient.call(getDummyHttpRequest(), new AtomicInteger(2))
                .thenAccept(
                        exchange -> {
                            //then
                            assertThat(exchange).isNotNull();
                            assertThat(exchange.getHttpRequest().getUrl()).isEqualTo("http://localhost:8089");
                            assertThat(exchange.getHttpResponse().getStatusCode()).isEqualTo(statusCode);
                            assertThat(exchange.getHttpResponse().getStatusMessage()).isEqualTo(statusMessage);
                        }
                ).get();
    }

    @Test
    public void call_test_any_positive_int_success_code_lower_than_500() throws ExecutionException, InterruptedException {
        //given
        AsyncHttpClient asyncHttpClient = Mockito.mock(AsyncHttpClient.class);
        ListenableFuture<Response> listener = Mockito.mock(ListenableFuture.class);
        ListenableFuture<Object> listenerObject = Mockito.mock(ListenableFuture.class);
        Response response = Mockito.mock(Response.class);
        Uri uri = new Uri("http", null, "fakeHost", 8080, "/toto", "param1=3", "#4");
        Mockito.when(response.getUri()).thenReturn(uri);

        when(response.getResponseBody()).thenReturn("body");
        when(response.getStatusCode()).thenReturn(404);
        when(response.getStatusText()).thenReturn("OK");
        when(listener.get()).thenReturn(response);
        when(listenerObject.get()).thenReturn(response);
        when(asyncHttpClient.executeRequest(ArgumentMatchers.any(Request.class))).thenReturn(listener);
        when(asyncHttpClient.executeRequest(ArgumentMatchers.any(Request.class), ArgumentMatchers.any())).thenReturn(listenerObject);
        when(listenerObject.toCompletableFuture()).thenReturn(CompletableFuture.supplyAsync(() -> response));
        AHCHttpClient httpClient = new AHCHttpClient(asyncHttpClient);
        //when
        httpClient.call(getDummyHttpRequest(), new AtomicInteger(2))
                .thenAccept(exchange -> {
                    //then
                    assertThat(exchange).isNotNull();
                }).get();

    }


    @Test
    public void call_test_failure_server_side() throws ExecutionException, InterruptedException {
        //given
        AsyncHttpClient asyncHttpClient = Mockito.mock(AsyncHttpClient.class);
        ListenableFuture<Response> listener = Mockito.mock(ListenableFuture.class);
        ListenableFuture<Object> listenerObject = Mockito.mock(ListenableFuture.class);
        Response response = Mockito.mock(Response.class);
        when(response.getResponseBody()).thenReturn("body");
        when(response.getStatusCode()).thenReturn(500);
        when(response.getStatusText()).thenReturn("OK");
        when(listener.get()).thenReturn(response);
        when(listenerObject.get()).thenReturn(response);
        when(asyncHttpClient.executeRequest(ArgumentMatchers.any(Request.class))).thenReturn(listener);
        when(asyncHttpClient.executeRequest(ArgumentMatchers.any(Request.class), ArgumentMatchers.any())).thenReturn(listenerObject);
        when(listener.toCompletableFuture()).thenReturn(CompletableFuture.supplyAsync(() -> response));
        when(listenerObject.toCompletableFuture()).thenReturn(CompletableFuture.supplyAsync(() -> response));
        AHCHttpClient httpClient = new AHCHttpClient(asyncHttpClient);

        //when
        httpClient.call(getDummyHttpRequest(), new AtomicInteger(2)).thenAccept(
                httpExchange -> //then
                        assertThat(httpExchange).isNotNull()
        ).get();

    }

    @Test
    public void call_test_failure_client_side() throws ExecutionException, InterruptedException {
        //given
        AsyncHttpClient asyncHttpClient = Mockito.mock(AsyncHttpClient.class);
        ListenableFuture<Response> listener = Mockito.mock(ListenableFuture.class);
        ListenableFuture<Object> listenerObject = Mockito.mock(ListenableFuture.class);
        Response response = Mockito.mock(Response.class);
        Uri uri = new Uri("http", null, "fakeHost", 8080, "/toto", "param1=3", "#4");
        Mockito.when(response.getUri()).thenReturn(uri);

        Mockito.when(response.getResponseBody()).thenReturn("body");
        Mockito.when(response.getStatusCode()).thenReturn(400);
        Mockito.when(response.getStatusText()).thenReturn("OK");

        when(listener.get()).thenReturn(response);
        when(listenerObject.get()).thenReturn(response);
        when(asyncHttpClient.executeRequest(ArgumentMatchers.any(Request.class))).thenReturn(listener);
        when(asyncHttpClient.executeRequest(ArgumentMatchers.any(Request.class), ArgumentMatchers.any())).thenReturn(listenerObject);
        when(listenerObject.toCompletableFuture()).thenReturn(CompletableFuture.supplyAsync(() -> response));
        AHCHttpClient httpClient = new AHCHttpClient(asyncHttpClient);
        //when
        httpClient.call(getDummyHttpRequest(), new AtomicInteger(2))
                .thenAccept(httpExchange -> assertThat(httpExchange).isNotNull())
                .get();
    }

    @Test
    public void test_build_http_request_nominal_case() {
        //given
        AsyncHttpClient asyncHttpClient = Mockito.mock(AsyncHttpClient.class);
        AHCHttpClient httpClient = new AHCHttpClient(asyncHttpClient);

        //when
        HttpRequest httpRequest = getDummyHttpRequest();
        Request request = httpClient.buildRequest(httpRequest);

        //then
        assertThat(request.getUrl()).isEqualTo(httpRequest.getUrl());
        assertThat(request.getMethod()).isEqualTo(httpRequest.getMethod());
    }


    private static HttpRequest getDummyHttpRequest() {
        return getDummyHttpRequest("{\"param\":\"name\"}");
    }

    private static HttpRequest getDummyHttpRequest(String body) {
        HashMap<String, List<String>> headers = Maps.newHashMap();
        headers.put("Content-Type", Lists.newArrayList("application/json"));
        headers.put("X-Stuff", Lists.newArrayList("dummy stuff"));
        HttpRequest httpRequest = new HttpRequest(
                "http://localhost:8089",
                "GET",
                "STRING");
        httpRequest.setHeaders(headers);
        httpRequest.setBodyAsString(body);
        httpRequest.getHeaders().put(HEADER_X_CORRELATION_ID, Lists.newArrayList("45-66-33"));
        httpRequest.getHeaders().put(HEADER_X_REQUEST_ID, Lists.newArrayList("77-3333-11"));
        return httpRequest;
    }

    @Test
    public void test_getTrustManagerFactory_jks_nominal_case() {

        //given
        String truststorePath = Thread.currentThread().getContextClassLoader().getResource(HttpSinkTaskTest.CLIENT_TRUSTSTORE_JKS_FILENAME).getPath();
        String password = HttpSinkTaskTest.CLIENT_TRUSTSTORE_JKS_PASSWORD;
        Map<String, Object> config = Maps.newHashMap();
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_PATH, truststorePath);
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_PASSWORD, password);
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_TYPE, HttpSinkTaskTest.JKS_STORE_TYPE);
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_ALGORITHM, HttpSinkTaskTest.TRUSTSTORE_PKIX_ALGORITHM);
        //when
        TrustManagerFactory trustManagerFactory = HttpClient.getTrustManagerFactory(config);
        //then
        assertThat(trustManagerFactory).isNotNull();
        assertThat(trustManagerFactory.getTrustManagers()).hasSize(1);
    }

    @Test
    public void test_getTrustManagerFactory_always_trust() throws CertificateException {

        //given
        Map<String, Object> config = Maps.newHashMap();
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_ALWAYS_TRUST, true);
        //when
        TrustManagerFactory trustManagerFactory = HttpClient.getTrustManagerFactory(config);
        //then
        assertThat(trustManagerFactory).isNotNull();
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        assertThat(trustManagers).hasSize(1);
        assertThat(trustManagers[0]).isInstanceOf(X509TrustManager.class);
        X509TrustManager x509TrustManager = (X509TrustManager) trustManagers[0];
        X509Certificate dummyCertificate = new DummyX509Certificate();
        X509Certificate[] certs = new X509Certificate[]{dummyCertificate};
        Assertions.assertThatCode(()->x509TrustManager.checkServerTrusted(certs,"RSA")).doesNotThrowAnyException();
    }

    @Test
    public void test_getTrustManagerFactory_always_trust_set_to_false() throws CertificateException {

        //given
        Map<String, Object> config = Maps.newHashMap();
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_ALWAYS_TRUST, false);
        String truststorePath = Thread.currentThread().getContextClassLoader().getResource(HttpSinkTaskTest.CLIENT_TRUSTSTORE_JKS_FILENAME).getPath();
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_PATH, truststorePath);
        String password = HttpSinkTaskTest.CLIENT_TRUSTSTORE_JKS_PASSWORD;
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_PASSWORD, password);
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_TYPE, HttpSinkTaskTest.JKS_STORE_TYPE);
        config.put(CONFIG_HTTP_CLIENT_SSL_TRUSTSTORE_ALGORITHM, HttpSinkTaskTest.TRUSTSTORE_PKIX_ALGORITHM);
        //when
        TrustManagerFactory trustManagerFactory = HttpClient.getTrustManagerFactory(config);
        //then
        assertThat(trustManagerFactory).isNotNull();
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        assertThat(trustManagers).hasSize(1);
        assertThat(trustManagers[0]).isInstanceOf(X509TrustManager.class);
        X509TrustManager x509TrustManager = (X509TrustManager) trustManagers[0];
        X509Certificate dummyCertificate = new DummyX509Certificate();
        X509Certificate[] certs = new X509Certificate[]{dummyCertificate};
        Assertions.assertThatCode(()->x509TrustManager.checkServerTrusted(certs,"RSA")).hasCauseInstanceOf(IOException.class);
    }


}