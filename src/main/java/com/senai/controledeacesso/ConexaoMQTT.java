import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
public class ConexaoMQTT {

    private static MqttClient client;

    public ConexaoMQTT(String brokerUrl) {
        String clientId = MqttClient.generateClientId();
        MqttConnectOptions options = new MqttConnectOptions();
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(5);

            // Callback para gerenciar eventos de conexão
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Conexão perdida: " + cause.getMessage());
                    // O método de reconexão automática será tratado pelo próprio cliente
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("Mensagem recebida no tópico " + topic + ": " + new String(message.getPayload(), StandardCharsets.UTF_8));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Não utilizado para assinatura de tópicos
                }
            });

            System.out.println("Conectando ao broker MQTT: " + brokerUrl);
            client.connect(options);

        } catch (MqttException e) {
            System.out.println("Tentando reconectar ao broker...");
            try {
                Thread.sleep(5000);
                client.connect(options);
            } catch (MqttException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            System.out.println("Reconectado ao broker!");
        }
        System.out.println("Conectado.");
    }

    // Método para publicar mensagem
    public void publicarMensagem(String topico, String mensagem) {
        try {
            MqttMessage mqttMessage = new MqttMessage(mensagem.getBytes());
            mqttMessage.setQos(1); // Define QoS 1 (pelo menos uma vez)
            client.publish(topico, mqttMessage);
            System.out.println("Mensagem publicada no tópico " + topico + ": " + mensagem);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Método para assinar um tópico de forma contínua e tratar mensagens
    public void assinarTopico(String topico, MensagemListener listener) {
        try {
            client.subscribe(topico, (topic, message) -> {
                String mensagemRecebida = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
                System.out.println("Mensagem recebida no tópico " + topic + ": " + mensagemRecebida);
                // Chama o listener para tratar a mensagem recebida
                listener.onMensagemRecebida(mensagemRecebida);
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Método para desconectar do broker
    public static void desconectar() {
        try {
            client.disconnect();
            System.out.println("Desconectado do broker MQTT.");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Interface para tratar as mensagens recebidas
    public interface MensagemListener {
        void onMensagemRecebida(String mensagem);
    }
}
