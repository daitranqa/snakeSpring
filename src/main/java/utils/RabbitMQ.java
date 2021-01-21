package utils;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import exceptions.NoSuchDataException;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RabbitMQ {
    private static final Logger LOGGER = LoggerManager.getInstance();
    private static final Gson gson = GsonManager.getInstance();
    private static final ConnectionFactory FACTORY = new ConnectionFactory();

    private RabbitMQ() {
    }

    public static int getTotalMessageInQueue(String queueName) throws TimeoutException {
        int totalMessage = countMessageOnQueue(queueName);
        int timeAttempt = 20;
        LOGGER.info("Starting to get message on RabbitMQ");
        LOGGER.info(" Waiting for the message");
        while (totalMessage == 0 && timeAttempt > 0) {
            totalMessage = countMessageOnQueue(queueName);
            timeAttempt--;
        }
        return totalMessage;
    }

    public static int countMessageOnQueue(String queueName) throws TimeoutException {
        try (Connection conn = FACTORY.newConnection();
             Channel channel = conn.createChannel()) {
            AMQP.Queue.DeclareOk queueStatus = channel.queueDeclarePassive(queueName);
            return queueStatus.getMessageCount();
        } catch (IOException e) {
            LOGGER.error("countMessageOnQueue: Cannot create connection to channel", e);
        }
        return 0;
    }

    public static void purseMessage(String queueName) throws TimeoutException {
        try (Connection conn = FACTORY.newConnection();
             Channel channel = conn.createChannel()) {
            channel.queuePurge(queueName);
        } catch (IOException e) {
            LOGGER.error("purseMessage: Cannot create connection to channel", e);
        }
    }

    public static String getFirstMessageInQueue(String queueName) throws TimeoutException {
        try (Connection conn = FACTORY.newConnection();
             Channel channel = conn.createChannel()) {
            return new String(channel.basicGet(queueName, true).getBody());
        } catch (IOException e) {
            LOGGER.error("getFirstMessageInQueue: Cannot create connection to channel", e);
        }

        throw new NoSuchDataException();
    }

    public static List<String> getMessagesFromQueue(String queueName) throws TimeoutException {
        int totalMessage = getTotalMessageInQueue(queueName);
        List<String> listMessage = new ArrayList<>();
        for (int i = 1; i <= totalMessage; i++) {
            String body = getFirstMessageInQueue(queueName);
            listMessage.add(body);
        }

        if (listMessage.isEmpty()) {
            LOGGER.warn("There is no message on queue");
        } else {
            listMessage.forEach(s -> LOGGER.info("Message on Rabbit MQ : {}", listMessage));
        }
        return listMessage;
    }

    @SneakyThrows
    public static <T> T parseSpecificMessageOnQueueToModel(List<String> listMessages, Class<T> clazz, String expectedContain) {
        for (String message : listMessages) {
            if (message.contains(expectedContain)) {
                return gson.fromJson(message, clazz);
            }
        }
        return null;
    }

    public void setURI(String uri) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        FACTORY.setUri(uri);
    }
}

