import org.json.JSONException;
import org.json.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;

public class Worker {

    private static final String TASK_QUEUE_NAME = "task_queue";

    public static void main(String[] argv) throws Exception {

        countryNameToISO nameToIso = new countryNameToISO();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connectionIn = factory.newConnection();
        Channel channelIn = connectionIn.createChannel();

        channelIn.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        Connection connectionOut = factory.newConnection();
        Channel channelOut = connectionOut.createChannel();

        channelOut.queueDeclare("eventsWithISO", true, false, false, null);

        channelIn.basicQos(1);

        QueueingConsumer consumer = new QueueingConsumer(channelIn);
        channelIn.basicConsume(TASK_QUEUE_NAME, false, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            String message = new String(delivery.getBody());

            JSONObject jsonObject = new JSONObject(message);
            System.out.println(message);

            String address = parseJson(jsonObject);

            if (address == null || address.equals("")) {
                channelIn.basicAck(delivery.getEnvelope().getDeliveryTag(),
                        false);
                continue;
            }

            int isoCode = nameToIso.start(address);
            // -1 is returned we should skip this message because a isonumber
            // cannot be found.
            if (isoCode == countryNameToISO.NO_COUNTRY_CODE) {
                channelIn.basicAck(delivery.getEnvelope().getDeliveryTag(),
                        false);
                continue;
            }

            // Sent Json object
            JSONObject jsonObjectWithISO = addISOToJson(jsonObject, isoCode);
            String messageOut = jsonObjectWithISO.toString();

            channelOut.basicPublish("", "eventsWithISO",
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    messageOut.getBytes());

            System.out.println("[*] Sent '" + messageOut + "'");

            channelIn.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
    }

    public static String parseJson(JSONObject JsonMessage) {
        String location = "";
        try {
            if (JsonMessage.has("actor")) {
                JSONObject actor = (JSONObject) JsonMessage.get("actor");
                if (actor.has("location")) {
                    Object locationObject = actor.get("location");
                    if (locationObject != null) {
                        location = (String) locationObject;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return location;
    }

    public static JSONObject addISOToJson(JSONObject JsonMessage, int isoNumber) {
        String isoNumberAsString = new Integer(isoNumber).toString();
        try {
            JSONObject actor = (JSONObject) JsonMessage.get("actor");
            actor.put("country_iso", isoNumberAsString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return JsonMessage;
    }
}
