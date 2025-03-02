package io.github.clescot.kafka.connect.http.source.queue;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.clescot.kafka.connect.http.core.HttpExchange;
import io.github.clescot.kafka.connect.http.core.HttpRequest;
import io.github.clescot.kafka.connect.http.core.HttpResponse;
import io.github.clescot.kafka.connect.http.core.queue.KafkaRecord;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.clescot.kafka.connect.http.client.HttpClient.*;

/**
 * produce some dummy KafkaRecord for tests only.
 */
public class QueueProducer implements Runnable {
    public static final String APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE = "Content-Type";
    private final Queue<KafkaRecord> transferQueue;

    private final long numberOfSuccessfulMessages;
    private final long numberOfErrorMessages;

    private static final AtomicInteger numberOfProducedMessages = new AtomicInteger();


    public QueueProducer(Queue<KafkaRecord> transferQueue, long numberOfSuccessfulMessages, long numberOfErrorMessages) {
        this.transferQueue = transferQueue;
        this.numberOfSuccessfulMessages = numberOfSuccessfulMessages;
        this.numberOfErrorMessages = numberOfErrorMessages;
    }

    @Override
    public void run() {
        for (int i = 0; i < numberOfSuccessfulMessages; i++) {
            transferQueue.offer(toKafkaRecord(getHttpExchange(SUCCESS)));
            numberOfProducedMessages.incrementAndGet();
        }
        for (int i = 0; i < numberOfErrorMessages; i++) {
            transferQueue.offer(toKafkaRecord(getHttpExchange(FAILURE)));
            numberOfProducedMessages.incrementAndGet();
        }
    }

    public static KafkaRecord toKafkaRecord(HttpExchange httpExchange){
        return new KafkaRecord(Lists.newArrayList(),null,null,httpExchange);
    }

    private static HttpExchange getHttpExchange(boolean success) {
        Map<String, List<String>> requestheaders = Maps.newHashMap();
        requestheaders.put("X-Request-ID", Lists.newArrayList("sdqd-qsdqd-446564"));
        requestheaders.put("X-Correlation-ID",Lists.newArrayList("222-qsdqd-446564"));
        requestheaders.put(CONTENT_TYPE,Lists.newArrayList(APPLICATION_JSON));
        HttpRequest httpRequest = new HttpRequest("http://www.toto.com",HttpRequest.Method.POST);
        httpRequest.setHeaders(requestheaders);
        httpRequest.setBodyAsString("fummy body");
        return success? getSuccessfulHttpExchange(httpRequest): getErrorHttpExchange(httpRequest);
    }

    @NotNull
    private static HttpExchange getSuccessfulHttpExchange(HttpRequest httpRequest) {
        Map<String,List<String>> responseHeaders = Maps.newHashMap();
        responseHeaders.put(CONTENT_TYPE,Lists.newArrayList(APPLICATION_JSON));
        HttpResponse httpResponse = new HttpResponse(200,"OK");
        httpResponse.setBodyAsString("body");
        httpResponse.setHeaders(responseHeaders);
        return HttpExchange.Builder.anHttpExchange()
                //tracing headers
                //request
                .withHttpRequest(httpRequest)
                //response
                .withHttpResponse(httpResponse)
                //technical metadata
                //time elapsed during http call
                .withDuration(469878798L)
                //at which moment occurs the beginning of the http call
                .at(OffsetDateTime.now(ZoneId.of(UTC_ZONE_ID)))
                .withAttempts(new AtomicInteger(1))
                .withSuccess(SUCCESS)
                .build();
    }

    @NotNull
    private static HttpExchange getErrorHttpExchange(HttpRequest httpRequest) {
        Map<String,List<String>> responseHeaders = Maps.newHashMap();
        responseHeaders.put(CONTENT_TYPE,Lists.newArrayList(APPLICATION_JSON));
        HttpResponse httpResponse = new HttpResponse(500,"Internal Server Error");
        httpResponse.setBodyAsString("Houston, we've got a problem....");
        httpResponse.setHeaders(responseHeaders);
        return HttpExchange.Builder.anHttpExchange()
                //tracing headers
                .withHttpRequest(httpRequest)
                //response
                .withHttpResponse(httpResponse)
                //technical metadata
                //time elapsed during http call
                .withDuration(465558798L)
                //at which moment occurs the beginning of the http call
                .at(OffsetDateTime.now(ZoneId.of(UTC_ZONE_ID)))
                .withAttempts(new AtomicInteger(1))
                .withSuccess(FAILURE)
                .build();
    }
}
