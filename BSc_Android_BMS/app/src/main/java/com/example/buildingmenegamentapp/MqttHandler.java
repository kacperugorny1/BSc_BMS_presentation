package com.example.buildingmenegamentapp;


import androidx.compose.runtime.snapshots.SnapshotStateList;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Arrays;

public class MqttHandler implements MqttCallback {

    private MqttClient client;
    public String Url;
    private final String Id;
    public MyViewModel viewModel;


    public MqttHandler(String BrokerUrl, String clientId){
        Url = BrokerUrl;
        Id = clientId;
    }

    public void connect(MyViewModel viewmodel) {
        try {
            this.viewModel = viewmodel;
            // Set up the persistence layer
            MemoryPersistence persistence = new MemoryPersistence();

            // Initialize the MQTT client
            client = new MqttClient(Url, Id, persistence);

            // Set up the connection options
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);

            client.setCallback(this);
            // Connect to the broker
            client.connect(connectOptions);
            viewModel.setMqttOnline(true);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            client.disconnect();
            viewModel.setMqttOnline(false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message,MyViewModel viewmodel) {
        try {
            if(!client.isConnected()){
                this.connect(viewmodel);
            }
            byte[] bytes = message.getBytes();
            MqttMessage mqttMessage = new MqttMessage(bytes);
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        viewModel.setMqttOnline(false);
        System.out.println("Connection lost: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        //LOGIKA
        System.out.println("t: " + topic + " m:" + message.toString());

        viewModel.setServerOnline(topic, !message.toString().equals("LOC:OFFLINE"));

        if(message.toString().contains("SYNCDEV:")){
           viewModel.syncDevices(message.toString().substring(8), topic);
        }
        if(message.toString().contains("SYNCCOND:")){
            viewModel.syncCond(message.toString().substring(9), topic);
        }
        else if(message.toString().contains("LOC:D")){
            viewModel.syncState(message.getPayload(), message.toString().substring(4), topic);

        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("Delivery complete. Topic: " + token.getTopics()[0]);
    }

    public void subscribe(String topic) {
        try {
            client.subscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}