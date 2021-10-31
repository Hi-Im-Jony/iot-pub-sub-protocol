package Broker;

import java.util.HashMap;
import java.io.IOException;
import java.util.ArrayList;

import SenderReceiver.SenderReceiver;
/* 
The broker is the middle man in this network. 
It receives packets with requests from various clients, and tries to fulfill those requests.

Broker can:
- Send/Receive packets
- Store list of subs to topics
- Publish data to subs of topic
*/
public class Broker {
    
	static final int MTU = 1500;

    static SenderReceiver sender;

    static HashMap<Integer, ArrayList<Integer>> topicSubscribers;

    public static void main(String[] args) throws IOException {

        sender = new SenderReceiver(0, "Broker"); // hardcoded receiver port because there is only one broker
        
        for(int i = 0; i<10;i++){
            String payload = sender.buildPayload("Random Topic", "Random Data");
            sender.send(payload, 2);
        }

       
    }

    private static void executeRequest(int requestor_port, String data) throws IOException{
        int request = data.charAt(0) - 48; // turn ASCII value into int value
        int topic = data.charAt(1) - 48;
        String info = data.substring(2, data.length()-1);
        //System.out.println("Request is: "+ request);
        switch(request){
            case 0:
                // subscribe to a topic
                // System.out.print("Subbing");
                subscibe(requestor_port, topic);
                break;
            case 1:
                // unsubscribe from a topic
               // System.out.print("Un-subbing");
                unsubscribe(requestor_port, topic);
                break;
            case 2:
                // publish data about a topic
                // System.out.print("Publishing");
                publish(topic, info);
                break;
        }
    }

    private static void subscibe(int requestor_port, int topic){
        //System.out.println("Executing 'subscribe'");
        ArrayList<Integer> subs = topicSubscribers.get(topic);
        if(subs != null)
            subs.add(requestor_port);
        else{
            subs = new ArrayList<Integer>();
            subs.add(requestor_port);
            topicSubscribers.put(topic, subs);
        }
    }

    private static void unsubscribe(int requestor_port, int topic){
        //System.out.println("Executing 'unsub'");
        ArrayList<Integer> subs = topicSubscribers.get(topic);

        if(subs != null)
            for(int i = 0; i<subs.size(); i++){
                if(subs.get(i) == requestor_port){
                    subs.remove(i);
                    break;
                }
            }
    }

    private static void publish(int topic, String data) throws IOException{
       // TODO publish data to subs of topic
    }
}
