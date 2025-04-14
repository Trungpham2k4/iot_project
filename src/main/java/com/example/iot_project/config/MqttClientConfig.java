package com.example.iot_project.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttClientConfig{
    private static final Dotenv dotenv = Dotenv.load();
    private static final String USERNAME = dotenv.get("ADA_FRUIT_USER");
    private static final String PASSWORD = dotenv.get("ADA_FRUIT_KEY");
    private static final String BROKER = dotenv.get("ADA_FRUIT_SERVER_URI");

    @Bean
    public MqttClient mqttClient() throws MqttException {
        MqttClient mqttClient = new MqttClient(BROKER, MqttClient.generateClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        options.setKeepAliveInterval(60);

        mqttClient.connect(options);
        return mqttClient;
    }
}
