package io.github.clescot.kafka.connect.http;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.trafficlistener.ConsoleNotifyingWiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.clescot.kafka.connect.MapUtils;
import io.github.clescot.kafka.connect.http.client.HttpClientConfiguration;
import io.github.clescot.kafka.connect.http.client.HttpConfiguration;
import io.github.clescot.kafka.connect.http.client.okhttp.OkHttpClient;
import io.github.clescot.kafka.connect.http.client.okhttp.OkHttpClientFactory;
import io.github.clescot.kafka.connect.http.core.HttpExchange;
import io.github.clescot.kafka.connect.http.core.HttpRequest;
import io.github.clescot.kafka.connect.http.sink.HttpConnectorConfig;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.kafka.common.config.AbstractConfig;
import org.assertj.core.util.Sets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static io.github.clescot.kafka.connect.Configuration.DEFAULT_CONFIGURATION_ID;
import static io.github.clescot.kafka.connect.http.sink.HttpConfigDefinition.DEFAULT_DEFAULT_RETRY_RESPONSE_CODE_REGEX;
import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.SAME_THREAD)
class HttpTaskTest {
    private static final HttpRequest.Method DUMMY_METHOD = HttpRequest.Method.POST;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);
    public static final String AUTHORIZED_STATE = "Authorized";
    public static final String INTERNAL_SERVER_ERROR_STATE = "InternalServerError";
    @RegisterExtension
    static WireMockExtension wmHttp;

    static {

        wmHttp = WireMockExtension.newInstance()
                .options(
                        WireMockConfiguration.wireMockConfig()
                                .dynamicPort()
                                .networkTrafficListener(new ConsoleNotifyingWiremockNetworkTrafficListener())
                                .useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER)
                )
                .build();
    }



    @Nested
    class CallWithRetryPolicy {
        private HttpTask<OkHttpClient,Request,Response> httpTask;

        @BeforeEach
        public void setUp(){
            Map<String,String> configs = Maps.newHashMap();
            AbstractConfig config = new HttpConnectorConfig(configs);

            CompositeMeterRegistry compositeMeterRegistry = new CompositeMeterRegistry();
            JmxMeterRegistry jmxMeterRegistry = new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM);
            jmxMeterRegistry.start();
            compositeMeterRegistry.add(jmxMeterRegistry);
            HttpClientConfiguration<OkHttpClient,Request, Response> test = new HttpClientConfiguration<>("test", new OkHttpClientFactory(), config.originalsStrings(), null, compositeMeterRegistry);
            HttpConfiguration<OkHttpClient, Request, Response> httpConfiguration = new HttpConfiguration<>(test);
            Map<String, HttpConfiguration<OkHttpClient, Request, Response>> map = Maps.newHashMap();
            map.put(DEFAULT_CONFIGURATION_ID, httpConfiguration);
            httpTask = new HttpTask<>(config.originalsStrings(),
                    map,
                    compositeMeterRegistry);
        }

        @Test
        void test_successful_request_at_first_time() throws ExecutionException, InterruptedException {

            //given
            String scenario = "test_successful_request_at_first_time";
            WireMockRuntimeInfo wmRuntimeInfo = wmHttp.getRuntimeInfo();
            WireMock wireMock = wmRuntimeInfo.getWireMock();
            wireMock
                    .register(WireMock.post("/ping").inScenario(scenario)
                            .whenScenarioStateIs(STARTED)
                            .willReturn(WireMock.aResponse()
                                    .withStatus(200)
                                    .withStatusMessage("OK")
                                    .withBody("")
                            ).willSetStateTo(AUTHORIZED_STATE)
                    );
            //when
            HttpRequest httpRequest = getDummyHttpRequest(wmHttp.url("/ping"));
            Map<String, String> settings = Maps.newHashMap();
            HttpConnectorConfig httpConnectorConfig = new HttpConnectorConfig(settings);
            HttpClientConfiguration<OkHttpClient,Request, Response> httpClientConfiguration = new HttpClientConfiguration<>(
                    "dummy",
                    new OkHttpClientFactory(),
                    httpConnectorConfig.originalsStrings(),
                    executorService,
                    getCompositeMeterRegistry()
            );
            HttpConfiguration<OkHttpClient,Request, Response> httpConfiguration = new HttpConfiguration<>(httpClientConfiguration);
            HttpExchange httpExchange = httpConfiguration.call(httpRequest).get();

            //then
            assertThat(httpExchange.isSuccess()).isTrue();
        }
        @Test
        void test_successful_request_with_status_message_limit() throws ExecutionException, InterruptedException {

            //given
            String scenario = "test_successful_request_at_first_time";
            WireMockRuntimeInfo wmRuntimeInfo = wmHttp.getRuntimeInfo();
            WireMock wireMock = wmRuntimeInfo.getWireMock();
            wireMock
                    .register(WireMock.post("/ping").inScenario(scenario)
                            .whenScenarioStateIs(STARTED)
                            .willReturn(WireMock.aResponse()
                                    .withStatus(200)
                                    .withStatusMessage("OK!!!!!!!!!")
                                    .withBody("")
                            ).willSetStateTo(AUTHORIZED_STATE)
                    );
            //when
            HttpRequest httpRequest = getDummyHttpRequest(wmHttp.url("/ping"));
            Map<String, String> settings = Maps.newHashMap();
            settings.put("config.dummy.http.response.message.status.limit","4");
            HttpConnectorConfig httpConnectorConfig = new HttpConnectorConfig(settings);
            HttpClientConfiguration<OkHttpClient,Request, Response> httpClientConfiguration = new HttpClientConfiguration<>(
                    "dummy",
                    new OkHttpClientFactory(),
                    MapUtils.getMapWithPrefix(httpConnectorConfig.originalsStrings(),"config.dummy."),
                    executorService,
                    getCompositeMeterRegistry()
            );
            HttpConfiguration<OkHttpClient,okhttp3.Request,okhttp3.Response> httpConfiguration = new HttpConfiguration<>(httpClientConfiguration);
            HttpExchange httpExchange = httpConfiguration.call(httpRequest).get();

            //then
            assertThat(httpExchange.isSuccess()).isTrue();
            assertThat(httpExchange.getHttpResponse().getStatusMessage()).isEqualTo("OK!!");
        }

        @Test
        void test_successful_request_with_body_limit() throws ExecutionException, InterruptedException {

            //given
            String scenario = "test_successful_request_at_first_time";
            WireMockRuntimeInfo wmRuntimeInfo = wmHttp.getRuntimeInfo();
            WireMock wireMock = wmRuntimeInfo.getWireMock();
            wireMock
                    .register(WireMock.post("/ping").inScenario(scenario)
                            .whenScenarioStateIs(STARTED)
                            .willReturn(WireMock.aResponse()
                                    .withStatus(200)
                                    .withStatusMessage("OK!!!!!!!!!")
                                    .withBody("01234567890123")
                            ).willSetStateTo(AUTHORIZED_STATE)
                    );
            //when
            HttpRequest httpRequest = getDummyHttpRequest(wmHttp.url("/ping"));
            Map<String, String> settings = Maps.newHashMap();
            settings.put("config.dummy.http.response.body.limit","10");
            HttpConnectorConfig httpConnectorConfig = new HttpConnectorConfig(settings);
            HttpClientConfiguration<OkHttpClient,Request, Response> httpClientConfiguration = new HttpClientConfiguration<>(
                    "dummy",
                    new OkHttpClientFactory(),
                    MapUtils.getMapWithPrefix(httpConnectorConfig.originalsStrings(),"config.dummy."),
                    executorService,
                    getCompositeMeterRegistry()
            );
            HttpConfiguration<OkHttpClient,okhttp3.Request,okhttp3.Response> httpConfiguration = new HttpConfiguration<>(httpClientConfiguration);
            HttpExchange httpExchange = httpConfiguration.call(httpRequest).get();

            //then
            assertThat(httpExchange.isSuccess()).isTrue();
            assertThat(httpExchange.getHttpResponse().getStatusMessage()).isEqualTo("OK!!!!!!!!!");
            assertThat(httpExchange.getHttpResponse().getBodyAsString()).isEqualTo("0123456789");
        }

        @Test
        void test_successful_request_with_status_message_limit_and_body_limit() throws ExecutionException, InterruptedException {

            //given
            String scenario = "test_successful_request_at_first_time";
            WireMockRuntimeInfo wmRuntimeInfo = wmHttp.getRuntimeInfo();
            WireMock wireMock = wmRuntimeInfo.getWireMock();
            wireMock
                    .register(WireMock.post("/ping").inScenario(scenario)
                            .whenScenarioStateIs(STARTED)
                            .willReturn(WireMock.aResponse()
                                    .withStatus(200)
                                    .withStatusMessage("OK!!")
                                    .withBody("01234567890123")
                            ).willSetStateTo(AUTHORIZED_STATE)
                    );
            //when
            HttpRequest httpRequest = getDummyHttpRequest(wmHttp.url("/ping"));
            Map<String, String> settings = Maps.newHashMap();
            settings.put("config.dummy.http.response.status.message.limit","4");
            settings.put("config.dummy.http.response.body.limit","10");
            HttpConnectorConfig httpConnectorConfig = new HttpConnectorConfig(settings);
            HttpClientConfiguration<OkHttpClient,Request, Response> httpClientConfiguration = new HttpClientConfiguration<>(
                    "dummy",
                    new OkHttpClientFactory(),
                    MapUtils.getMapWithPrefix(httpConnectorConfig.originalsStrings(),"config.dummy."),
                    executorService,
                    getCompositeMeterRegistry()
            );
            HttpConfiguration<OkHttpClient,okhttp3.Request,okhttp3.Response> httpConfiguration = new HttpConfiguration<>(httpClientConfiguration);
            HttpExchange httpExchange = httpConfiguration.call(httpRequest).get();

            //then
            assertThat(httpExchange.isSuccess()).isTrue();
            assertThat(httpExchange.getHttpResponse().getStatusMessage()).isEqualTo("OK!!");
            assertThat(httpExchange.getHttpResponse().getBodyAsString()).isEqualTo("0123456789");
        }

        @Test
        void test_successful_request_at_second_time() throws ExecutionException, InterruptedException {

            //given
            String scenario = "test_successful_request_at_second_time";
            WireMockRuntimeInfo wmRuntimeInfo = wmHttp.getRuntimeInfo();
            WireMock wireMock = wmRuntimeInfo.getWireMock();
            wireMock
                    .register(WireMock.post("/ping").inScenario(scenario)
                            .whenScenarioStateIs(STARTED)
                            .willReturn(WireMock.aResponse()
                                    .withStatus(500)
                                    .withStatusMessage("Internal Server Error")
                            ).willSetStateTo(INTERNAL_SERVER_ERROR_STATE)
                    );
            wireMock
                    .register(WireMock.post("/ping").inScenario(scenario)
                            .whenScenarioStateIs(INTERNAL_SERVER_ERROR_STATE)
                            .willReturn(WireMock.aResponse()
                                    .withStatus(200)
                                    .withStatusMessage("OK")
                            ).willSetStateTo(AUTHORIZED_STATE)
                    );
            //when
            HttpRequest httpRequest = getDummyHttpRequest(wmHttp.url("/ping"));
            Map<String, String> settings = Maps.newHashMap();
            settings.put("config.dummy.retry.policy.retries","2");
            settings.put("config.dummy.retry.policy.response.code.regex",DEFAULT_DEFAULT_RETRY_RESPONSE_CODE_REGEX);
            HttpConnectorConfig httpConnectorConfig = new HttpConnectorConfig(settings);
            HttpClientConfiguration<OkHttpClient,Request, Response> httpClientConfiguration = new HttpClientConfiguration<>(
                    "dummy",
                    new OkHttpClientFactory(),
                    MapUtils.getMapWithPrefix(httpConnectorConfig.originalsStrings(),"config.dummy."),
                    executorService,
                    getCompositeMeterRegistry()
            );
            HttpConfiguration<OkHttpClient,okhttp3.Request,okhttp3.Response> httpConfiguration = new HttpConfiguration<>(httpClientConfiguration);
            HttpExchange httpExchange = httpConfiguration.call(httpRequest).get();

            //then
            AtomicInteger attempts = httpExchange.getAttempts();
            assertThat(attempts.get()).isEqualTo(2);
            assertThat(httpExchange.isSuccess()).isTrue();
        }

    }

    @NotNull
    private static HttpRequest getDummyHttpRequest(String url) {
        HttpRequest httpRequest = new HttpRequest(url, DUMMY_METHOD);
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put("Content-Type", Lists.newArrayList("application/json"));
        httpRequest.setHeaders(headers);
        httpRequest.setBodyAsString("stuff");
        httpRequest.setBodyAsForm(Maps.newHashMap());
        return httpRequest;
    }





    private static CompositeMeterRegistry getCompositeMeterRegistry() {
        JmxMeterRegistry jmxMeterRegistry = new JmxMeterRegistry(s -> null, Clock.SYSTEM);
        HashSet<MeterRegistry> registries = Sets.newHashSet();
        registries.add(jmxMeterRegistry);
        return new CompositeMeterRegistry(Clock.SYSTEM, registries);
    }



}