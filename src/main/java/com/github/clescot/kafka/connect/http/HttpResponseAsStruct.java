package com.github.clescot.kafka.connect.http;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;

public class HttpResponseAsStruct {
    private static final Integer VERSION = 1;

    private static final String STATUS_CODE = "statusCode";
    private static final String STATUS_MESSAGE = "statusMessage";
    private static final String PROTOCOL = "protocol";
    private static final String HEADERS = "headers";
    private static final String BODY = "body";

    public static final Schema SCHEMA = SchemaBuilder
            .struct()
            .name(HttpResponse.class.getName())
            .version(VERSION)
            .field(STATUS_CODE,Schema.INT64_SCHEMA)
            .field(STATUS_MESSAGE,Schema.STRING_SCHEMA)
            .field(PROTOCOL,Schema.OPTIONAL_STRING_SCHEMA)
            .field(HEADERS, SchemaBuilder.map(Schema.STRING_SCHEMA, SchemaBuilder.array(Schema.STRING_SCHEMA)).build())
            .field(BODY,Schema.OPTIONAL_STRING_SCHEMA);

    private HttpResponse httpResponse;

    public HttpResponseAsStruct(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public Struct toStruct() {
        return new Struct(SCHEMA)
                .put(STATUS_CODE,httpResponse.getStatusCode().longValue())
                .put(STATUS_MESSAGE,httpResponse.getStatusMessage())
                .put(HEADERS,httpResponse.getResponseHeaders())
                .put(BODY,httpResponse.getResponseBody());


    }
}