package io.github.clescot.kafka.connect.http.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

@io.confluent.kafka.schemaregistry.annotations.Schema(value = HttpRequest.SCHEMA_AS_STRING,
        refs = {})
public class HttpRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequest.class);


    //request
    @JsonProperty(required = true)
    private String url;
    @JsonProperty
    private Map<String, List<String>> headers = Maps.newHashMap();
    @JsonProperty(defaultValue = "GET")
    private HttpRequest.Method method;
    @JsonProperty
    private String bodyAsString = "";
    @JsonProperty
    private Map<String,String> bodyAsForm = Maps.newHashMap();
    @JsonProperty
    private String bodyAsByteArray = "";
    @JsonProperty
    private List<String> bodyAsMultipart = Lists.newArrayList();
    @JsonProperty(defaultValue = "STRING")
    private BodyType bodyType;


    public static final String SCHEMA_ID = HttpExchange.BASE_SCHEMA_ID+"http-request.json";
    public static final String SCHEMA_AS_STRING = "{\n" +
            "  \"$id\": \"" + SCHEMA_ID + "\",\n" +
            "\"$schema\": \"http://json-schema.org/draft/2019-09/schema#\",\n" +
            "  \"title\": \"Http Request\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"additionalProperties\": false,\n" +
            "  \"properties\": {\n" +
            "    \"url\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"headers\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"connect.type\": \"map\",\n" +
            "      \"additionalProperties\": {\n" +
            "        \"type\": \"array\",\n" +
            "        \"items\": {\n" +
            "          \"type\": \"string\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"method\": {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"bodyAsString\":\n" +
            "    {\n" +
            "      \"type\": \"string\"\n" +
            "    }\n" +
            "  ,\n" +
            "    \"bodyAsForm\":\n" +
            "    {\n" +
            "      \"type\": \"object\",\n" +
            "      \"connect.type\": \"map\",\n" +
            "      \"additionalProperties\": { \"type\": \"string\" }\n" +
            "    }\n" +
            "  ,\n" +
            "    \"bodyAsByteArray\":  {\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    \"bodyAsMultipart\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": {\n" +
            "        \"type\": \"string\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"bodyType\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"enum\": [\n" +
            "        \"STRING\",\n" +
            "        \"FORM\",\n" +
            "        \"BYTE_ARRAY\",\n" +
            "        \"MULTIPART\"\n" +
            "      ]\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"url\",\n" +
            "    \"method\",\n" +
            "    \"bodyType\"\n" +
            "  ]\n" +
            "}";

    /**
     * only for json deserialization
     */
    protected HttpRequest() {
    }
    public HttpRequest(String url){
        this(url,HttpRequest.Method.GET,BodyType.STRING.name());
    }
    public HttpRequest(String url,HttpRequest.Method method){
        this(url,method,BodyType.STRING.name());
    }
    public HttpRequest(String url,
                       HttpRequest.Method method,
                       String bodyType) {
        Preconditions.checkNotNull(url, "url is required");
        Preconditions.checkNotNull(bodyType, "bodyType is required");
        this.url = url;
        this.method = method;
        this.bodyType = BodyType.valueOf(bodyType);
    }

    public HttpRequest(HttpRequest original){
            this(original.getUrl(),original.getMethod(),original.getBodyType().name());
            this.setHeaders(Maps.newHashMap(original.getHeaders()));
            switch(original.getBodyType()){
                case STRING:this.setBodyAsString(original.getBodyAsString());break;
                case MULTIPART:this.setBodyAsMultipart(Lists.newArrayList(original.getBodyAsMultipart()));break;
                case FORM:this.setBodyAsForm(Maps.newHashMap(original.getBodyAsForm()));break;
                case BYTE_ARRAY:this.setBodyAsByteArray(original.getBodyAsByteArray());
            }
    }

    private List<String> convertMultipart(List<byte[]> bodyAsMultipart) {
        List<String> results = Lists.newArrayList();
        for (byte[] bytes : bodyAsMultipart) {
            results.add(Base64.getEncoder().encodeToString(bytes));
        }
        return results;
    }




    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getUrl() {
        return url;
    }

    public HttpRequest.Method getMethod() {
        return method;
    }

    public String getBodyAsString() {
        return bodyAsString;
    }

    public byte[] getBodyAsByteArray() {
        return this.bodyAsByteArray != null ? Base64.getDecoder().decode(bodyAsByteArray) : new byte[0];
    }

    public Map<String, String> getBodyAsForm() {
        return bodyAsForm;
    }

    public List<byte[]> getBodyAsMultipart() {
        List<byte[]> multipart = Lists.newArrayList();
        for (String encodedPart : this.bodyAsMultipart) {
            multipart.add(Base64.getDecoder().decode(encodedPart));
        }
        return multipart;
    }

    public BodyType getBodyType() {
        return bodyType;
    }

    public void setBodyAsString(String bodyAsString) {
        this.bodyAsString = bodyAsString;
    }

    public void setBodyAsForm(Map<String, String> bodyAsForm) {
        this.bodyAsForm = bodyAsForm;
    }

    public void setBodyAsByteArray( byte[] bodyAsByteArray) {
        this.bodyAsByteArray = Base64.getEncoder().encodeToString(bodyAsByteArray);
    }

    public void setBodyAsMultipart(List<byte[]> bodyAsMultipart) {
        if(bodyAsMultipart!=null) {
            this.bodyAsMultipart = convertMultipart(bodyAsMultipart);
        }
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpRequest that = (HttpRequest) o;
        return url.equals(that.url) && Objects.equals(headers, that.headers) && method.equals(that.method) && Objects.equals(bodyAsString, that.bodyAsString) && Objects.equals(bodyAsForm, that.bodyAsForm) && Objects.equals(bodyAsByteArray, that.bodyAsByteArray) && Objects.equals(bodyAsMultipart, that.bodyAsMultipart) && bodyType == that.bodyType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, headers, method, bodyAsForm, bodyAsByteArray, bodyAsMultipart, bodyType);
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "url='" + url + '\'' +
                ", headers=" + headers +
                ", method='" + method + '\'' +
                ", bodyAsString='" + bodyAsString + '\'' +
                ", bodyAsForm='" + bodyAsForm + '\'' +
                ", bodyAsByteArray='" + bodyAsByteArray + '\'' +
                ", bodyAsMultipart=" + bodyAsMultipart +
                ", bodyType=" + bodyType +
                '}';
    }




    public enum BodyType {
        STRING,
        BYTE_ARRAY,
        FORM,
        MULTIPART;

        @Override
        public String toString() {
            return name();
        }
    }

    public enum Method {
        CONNECT,
        DELETE,
        GET,
        HEAD,
        PATCH,
        POST,
        PUT,
        OPTIONS,
        TRACE
    }

}
