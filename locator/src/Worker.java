import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import redis.clients.jedis.Jedis;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class Worker implements Runnable {

    private static final String IN_QUEUE_NAME = "raw-events";
    private static final String OUT_QUEUE_NAME = "events-with-iso";

    public static String extractLocationFromEvent(JSONObject event) {
        try {
            if (event.has("actor")) {
                JSONObject actor = (JSONObject) event.get("actor");
                if (actor.has("location") && !actor.isNull("location")) {
                    Object locationObject = actor.get("location");
                    return locationObject.toString();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] argv) throws Exception {
        Worker worker = new Worker("localhost");
        worker.connect();
        worker.run();
    }

    private ConnectionFactory amqpConnectionFactory;
    private Channel channelIn;
    private Channel channelOut;
    private GoogleGeocodingService geocodingService;
    private Jedis redis;

    public Worker(String amqpHost) {
        geocodingService = new GoogleGeocodingService();
        amqpConnectionFactory = new ConnectionFactory();
        amqpConnectionFactory.setHost(amqpHost);
    }

    public void connect() throws IOException {
        Connection connectionIn = amqpConnectionFactory.newConnection();
        channelIn = connectionIn.createChannel();
        channelIn.queueDeclare(IN_QUEUE_NAME, true, false, false, null);
        channelIn.basicQos(1);

        Connection connectionOut = amqpConnectionFactory.newConnection();
        channelOut = connectionOut.createChannel();
        channelOut.queueDeclare(OUT_QUEUE_NAME, true, false, false, null);

        redis = new Jedis("localhost");
    }

    private void processEvent(JSONObject event) throws IOException {
        String location = extractLocationFromEvent(event);
        if (location == null || location.equals("")) {
            return;
        }

        String countryAlpha2 = locationToAlpha2(location);
        if (countryAlpha2 == GoogleGeocodingService.NO_COUNTRY) {
            return;
        }

        CountryCodeToISO country = CountryCodeToISO.getByCode(countryAlpha2);
        if (country != null) {
            try {
                JSONObject actor = (JSONObject) event.get("actor");
                actor.put("country_alpha2", countryAlpha2);
                actor.put("country_name", country.getName());
                actor.put("country_iso", country.getNumeric());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            String messageOut = event.toString();
            channelOut.basicPublish("", OUT_QUEUE_NAME,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    messageOut.getBytes());

            try {
                System.out.println("[*] Sent: actor=" + event.get("actor"));
            } catch (JSONException e) {
            }
        }
    }

    private String locationToAlpha2(String location) throws IOException {
        String key = locationToRedisKey(location);
        String alpha2 = redis.get(key);

        if (alpha2 != null) {
            System.out.println("[*] Got location from Redis");
        } else {
            alpha2 = geocodingService.locationToCountryAlpha2(location);
            System.out.println("[*] Got location from Google");

            if (alpha2 != null && !location.equals("")) {
                redis.set(key, alpha2);
                redis.expire(key, 2592000); // 30d
            }

        }
        return alpha2;
    }

    public String locationToRedisKey(String location) {
        try {
            return "google:geocode:location:"
                    + URLEncoder.encode(location.toLowerCase(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void run() {
        try {
            QueueingConsumer consumer = new QueueingConsumer(channelIn);
            channelIn.basicConsume(IN_QUEUE_NAME, false, consumer);

            while (true) {
                QueueingConsumer.Delivery delivery;
                try {
                    delivery = consumer.nextDelivery();
                } catch (ShutdownSignalException e) {
                    e.printStackTrace();
                    break;
                } catch (ConsumerCancelledException e) {
                    e.printStackTrace();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                channelIn.basicAck(delivery.getEnvelope().getDeliveryTag(),
                        false);

                String message = new String(delivery.getBody());
                try {
                    JSONObject event = new JSONObject(message);
                    System.out.println("[*] Received: actor="
                            + event.get("actor"));
                    processEvent(event);
                } catch (JSONException e) {
                    System.err.println("Invalid event JSON!");
                    e.printStackTrace();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }
    }
}
