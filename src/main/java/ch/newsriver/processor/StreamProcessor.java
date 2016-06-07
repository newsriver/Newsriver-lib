package ch.newsriver.processor;

import org.apache.kafka.streams.KeyValue;

/**
 * Created by eliapalme on 02/06/16.
 */
public interface StreamProcessor<I,O> {


    public KeyValue<String,O> process (String key, I value);
    public void close();
}
