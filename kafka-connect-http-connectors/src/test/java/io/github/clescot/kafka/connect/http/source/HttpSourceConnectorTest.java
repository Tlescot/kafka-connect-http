package io.github.clescot.kafka.connect.http.source;

import com.google.common.collect.Maps;
import org.apache.kafka.common.config.ConfigException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static io.github.clescot.kafka.connect.http.core.queue.ConfigConstants.QUEUE_NAME;
import static io.github.clescot.kafka.connect.http.core.queue.QueueFactory.DEFAULT_QUEUE_NAME;
import static io.github.clescot.kafka.connect.http.source.HttpSourceConfigDefinition.ERROR_TOPIC;
import static io.github.clescot.kafka.connect.http.source.HttpSourceConfigDefinition.SUCCESS_TOPIC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class HttpSourceConnectorTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpSourceConnectorTest.class);
    private HttpSourceConnector httpSourceConnector;

    @BeforeEach
    public void setup(){
        httpSourceConnector = new HttpSourceConnector();
    }

    @Test
    void test_start_nominal_case(){
        Map<String, String> settings = Maps.newHashMap();
        settings.put(SUCCESS_TOPIC, "foo");
        settings.put(ERROR_TOPIC, "foo");
        Assertions.assertDoesNotThrow( () -> httpSourceConnector.start(settings));
    }

    @Test
    void test_start_missing_success_topic(){
        Map < String, String > settings = Maps.newHashMap();
        settings.put(ERROR_TOPIC, "foo");
        Assertions.assertThrows(ConfigException.class, () ->  {
                httpSourceConnector.start(settings);
        });
    }

    @Test
    void test_start_missing_errors_topic(){
        Map < String, String > settings = Maps.newHashMap();
        settings.put(SUCCESS_TOPIC, "foo");
        Assertions.assertThrows(ConfigException.class, () ->  {
                httpSourceConnector.start(settings);
        });
    }

    @Test
    void test_start_with_queue_name(){
        Map < String, String > settings = Maps.newHashMap();
        settings.put(SUCCESS_TOPIC, "foo1");
        settings.put(ERROR_TOPIC, "foo2");
        settings.put(QUEUE_NAME, "myQueue");
        Assertions.assertDoesNotThrow( () -> httpSourceConnector.start(settings));
    }

    @Test
    void test_start_with_default_queue_name(){
        Map < String, String > settings = Maps.newHashMap();
        settings.put(SUCCESS_TOPIC, "foo1");
        settings.put(ERROR_TOPIC, "foo2");
        settings.put(QUEUE_NAME, DEFAULT_QUEUE_NAME);
        Assertions.assertDoesNotThrow( () -> httpSourceConnector.start(settings));
    }



    @Test
    void test_start_empty_settings_map(){
        Map<String,String> settings = Maps.newHashMap();
        Assertions.assertThrows(ConfigException.class, () -> httpSourceConnector.start(settings));
    }

    @Test
    void test_start_null_settings_map(){
        Assertions.assertThrows(NullPointerException.class, () -> httpSourceConnector.start(null));
    }

    @Test
    void test_task_configs_zero_task(){
        Map<String,String> settings = Maps.newHashMap();
        settings.put(SUCCESS_TOPIC,"foo");
        settings.put(ERROR_TOPIC,"foo");
        httpSourceConnector.start(settings);
        List<Map<String, String>> maps = httpSourceConnector.taskConfigs(0);
        assertThat(maps).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
    }

    @Test
    void test_task_configs_1_task(){
        Map<String,String> settings = Maps.newHashMap();
        settings.put(SUCCESS_TOPIC,"foo");
        settings.put(ERROR_TOPIC,"foo");
        httpSourceConnector.start(settings);
        List<Map<String, String>> maps = httpSourceConnector.taskConfigs(1);
        assertThat(maps).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1);

    }
      @Test
      void test_task_configs_10_tasks(){
        Map<String,String> settings = Maps.newHashMap();
          settings.put(SUCCESS_TOPIC,"foo");
          settings.put(ERROR_TOPIC,"foo");
        httpSourceConnector.start(settings);
          List<Map<String, String>> maps = httpSourceConnector.taskConfigs(10);
          assertThat(maps).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(10);
      }


}