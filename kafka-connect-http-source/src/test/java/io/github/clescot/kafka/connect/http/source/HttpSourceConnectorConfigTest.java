package io.github.clescot.kafka.connect.http.source;

import com.google.common.collect.Maps;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static io.github.clescot.kafka.connect.http.source.HttpSourceConfigDefinition.ERROR_TOPIC;
import static io.github.clescot.kafka.connect.http.source.HttpSourceConfigDefinition.SUCCESS_TOPIC;


public class HttpSourceConnectorConfigTest {



    @Test
    public void test_null_map(){
        Assertions.assertThrows(NullPointerException.class,()->new HttpSourceConnectorConfig(null));
    }
    @Test
    public void test_empty_map(){
        Assertions.assertThrows(ConfigException.class,()->
        new HttpSourceConnectorConfig(Maps.newHashMap()));
    }

    @Test
    public void test_nominal_case(){
        HashMap<Object, Object> config = Maps.newHashMap();
        config.put(SUCCESS_TOPIC,"success.topic");
        config.put(ERROR_TOPIC,"error.topic");
        new HttpSourceConnectorConfig(config);
    }




    @Test
    public void test_missing_ack_topic(){
        HashMap<Object, Object> config = Maps.newHashMap();
        Assertions.assertThrows(ConfigException.class,()->
        new HttpSourceConnectorConfig(config));
    }


}