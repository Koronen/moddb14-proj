import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

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

    public static JSONObject addIsoToEvent(JSONObject event, int isoNumber) {
        String isoNumberAsString = new Integer(isoNumber).toString();
        try {
            JSONObject actor = (JSONObject) event.get("actor");
            actor.put("country_iso", isoNumberAsString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return event;
    }

    public static String extractLocationFromEvent(JSONObject event) {
        try {
            if (event.has("actor")) {
                JSONObject actor = (JSONObject) event.get("actor");
                if (actor.has("location") && !actor.isNull("location")) {
                    Object locationObject = actor.get("location");
                    return JSONObject.valueToString(locationObject);
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
    }

    private void processEvent(JSONObject jsonObject) throws IOException {
        String location = extractLocationFromEvent(jsonObject);
        if (location == null || location.equals("")) {
            return;
        }

        String countryName = geocodingService.locationToCountryName(location);
        if (countryName == GoogleGeocodingService.NO_COUNTRY) {
            return;
        }

        List<CountryCodeToISO> countries = CountryCodeToISO
                .findByName(countryName);
        int isoCode = -1;
        if (!countries.isEmpty()) {
            isoCode = countries.get(0).getNumeric();
        }

        JSONObject jsonObjectWithISO = addIsoToEvent(jsonObject, isoCode);
        String messageOut = jsonObjectWithISO.toString();
        channelOut.basicPublish("", OUT_QUEUE_NAME,
                MessageProperties.PERSISTENT_TEXT_PLAIN, messageOut.getBytes());

        try {
            System.out.println("[*] Sent: actor=" + jsonObjectWithISO.get("actor"));
        } catch (JSONException e) {
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
                } catch (ShutdownSignalException | ConsumerCancelledException
                        | InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                channelIn.basicAck(delivery.getEnvelope().getDeliveryTag(),
                        false);

                String message = new String(delivery.getBody());
                try {
                    JSONObject event = new JSONObject(message);
                    System.out.println("[*] Received: actor=" + event.get("actor"));
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
