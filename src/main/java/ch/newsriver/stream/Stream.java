package ch.newsriver.stream;

import ch.newsriver.executable.poolExecution.BatchInterruptibleWithinExecutorPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by eliapalme on 05/06/16.
 */
public class Stream<I, O> extends BatchInterruptibleWithinExecutorPool implements Runnable {

    private static final Logger logger = LogManager.getLogger(Stream.class);
    private static final int POOLING_TIMEOUT = 500;
    private static final ObjectMapper mapper = new ObjectMapper();
    Consumer<String, String> consumer;
    Producer<String, String> producer;

    private Class<I> inputClass;
    private Class<O> outputClass;
    private ArrayList<String> inboundTopics = new ArrayList<>();
    private int batchSize;
    private Function<I, O> processor;
    private HashMap<String, Predicate<O>> outboundTopics = new HashMap<>();
    private String name;

    private boolean run = true;

    protected Stream(String name, int batchSize, int poolSize, int maxQueueSize, Duration duration) {
        super(poolSize, maxQueueSize, duration);
        this.name = name;
        this.batchSize = batchSize;
    }

    protected void from(String topicName) {
        this.inboundTopics.add(topicName);
    }

    protected void setProcessor(Function<I, O> processor) {
        this.processor = processor;
    }

    protected void to(String topicName, Predicate<O> predicate) {
        this.outboundTopics.put(topicName, predicate);
    }

    public void setInputClass(Class<I> inputClass) {
        this.inputClass = inputClass;
    }

    public void setOutputClass(Class<O> outputClass) {
        this.outputClass = outputClass;
    }

    protected void initialize() {

        Properties props = new Properties();
        InputStream inputStream = null;
        try {

            String propFileName = "kafka.properties";
            inputStream = Stream.class.getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                props.load(inputStream);
            } else {
                throw new FileNotFoundException("Error in stream" + name + " property file '" + propFileName + "' not found in the classpath");
            }
        } catch (java.lang.Exception e) {
            logger.error("Error in stream" + name + " unable to load kafka properties", e);
        } finally {
            try {
                inputStream.close();
            } catch (java.lang.Exception e) {
            }
        }


        consumer = new KafkaConsumer(props);
        consumer.subscribe(inboundTopics);
        producer = new KafkaProducer(props);
    }


    @Override
    public void run() {
        while (run) {
            try {
                this.waitFreeBatchExecutors(batchSize);
                //TODO: decide if we want to keep this.
                //metrics.logMetric("processing batch");

                ConsumerRecords<String, String> records = consumer.poll(POOLING_TIMEOUT);

                for (ConsumerRecord<String, String> record : records) {


                    supplyAsyncInterruptExecutionWithin(() -> {


                        I input = deserialize(record.value());

                        if (input == null) return null;

                        O output = processor.apply(input);
                        if (output != null) {
                            for (String topicName : outboundTopics.keySet()) {
                                Predicate<O> predicate = outboundTopics.get(topicName);
                                if (predicate.test(output)) {
                                    producer.send(new ProducerRecord<String, String>(topicName, record.key(), serialize(output)));
                                }
                            }
                        }

                        return null;
                    }, this)
                            .exceptionally(throwable -> {
                                logger.error("Error in stream " + name + " unrecoverable error.", throwable);
                                return null;
                            });
                }
            } catch (InterruptedException ex) {
                logger.warn("Main job interrupted stream name " + name, ex);
                run = false;
                return;
            } catch (BatchSizeException ex) {
                logger.fatal("Error in stream " + name + " requested a batch size bigger than pool capability.");
                run = false;
                return;
            }
        }
    }

    public void shutdown() {
        super.shutdown();
        run = false;
        if (consumer != null) consumer.close();
        if (producer != null) producer.close();
    }


    private I deserialize(String data) {
        I input;
        try {
            input = mapper.readValue(data, inputClass);
        } catch (IOException e) {
            logger.error("Error in stream" + name + " unable to deserialize " + inputClass.getName(), e);
            return null;
        }
        return input;
    }

    private String serialize(O output) {
        String data;
        try {
            data = mapper.writeValueAsString(output);
        } catch (IOException e) {
            logger.error("Error in stream" + name + " unable to serialize " + outputClass.getName(), e);
            return null;
        }
        return data;
    }

    public static class BuilderFinal<I, O> extends BuilderTo<I, O> {


        protected BuilderFinal(String name, int batchSize, int poolSize, int maxQueueSize, Duration duration) {
            this.stream = new Stream(name, batchSize, poolSize, maxQueueSize, duration);
        }

        public Stream<I, O> build() {
            stream.initialize();
            return stream;
        }

    }

    public static class BuilderTo<I, O> extends BuilderProcess<I, O> {

        public BuilderFinal to(String topicName, Predicate<O> predicate) {
            this.stream.to(topicName, predicate);
            return (BuilderFinal) this;
        }

    }

    public static class BuilderProcess<I, O> extends BuilderClass<I, O> {

        public BuilderTo setProcessor(Function<I, O> processor) {
            this.stream.setProcessor(processor);
            return (BuilderTo) this;
        }

    }

    public static class BuilderClass<I, O> extends Builder<I, O> {

        public BuilderProcess withClasses(Class<I> inputClass, Class<O> outputClass) {
            this.stream.setInputClass(inputClass);
            this.stream.setOutputClass(outputClass);
            return (BuilderProcess) this;
        }

    }

    public static class Builder<I, O> {

        protected Stream<I, O> stream;

        private Builder() {
        }

        public static Builder with(String name, int batchSize, int poolSize, int maxQueueSize, Duration duration) {

            BuilderFinal builder = new BuilderFinal(name, batchSize, poolSize, maxQueueSize, duration);

            return builder;
        }

        public BuilderClass from(String topicName) {
            this.stream.from(topicName);
            return (BuilderClass) this;
        }

    }

}
