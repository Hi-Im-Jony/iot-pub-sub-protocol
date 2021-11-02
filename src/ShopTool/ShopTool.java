package ShopTool;


import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
/*
A pseudo tool used by workers in a shop to check prices of items, 
get notified of product changes, and print product SEL

ShopTool can:
- "Scan" qr/bar codes to check item details
- Print item SEL
- Subscribe to a "section" of the store (ie, sub to a topic)
- Get notified when item in section gets added/edited/removed
*/
public class ShopTool {
    

    static final int BROKER_PORT = 2;

    private static Transreceiver transreceiver;
    private static String cache;
    public static void main(String[] args) throws IOException, InterruptedException {
            
        System.out.println("ShopTool turned on");

        transreceiver = new Transreceiver();

        ReceiverThread receiverThread = new ReceiverThread(); // create new "back up thread" to receive while we print
        receiverThread.start();

        demo(args[0]); // scanner will sub to the topic given when run in cmd

    }

    private static void demo(String topic) throws IOException, InterruptedException{

        // TimeUnit.SECONDS.sleep(10); // wait for Computer to add some items to db

        // // "scan" some codes instore
        // scan(0);
        // TimeUnit.SECONDS.sleep(5);
        // scan(1);
        // TimeUnit.SECONDS.sleep(5);

        // // "scan" incorrect code - see what happens
        // scan(1089); 
        // TimeUnit.SECONDS.sleep(5);
        // scan(4);
        // TimeUnit.SECONDS.sleep(5);

        sub(topic);
        TimeUnit.SECONDS.sleep(30);

        unsub(topic);
        TimeUnit.SECONDS.sleep(10);

        scan(1);
        TimeUnit.SECONDS.sleep(1);
        printProductSEL(cache);
        TimeUnit.SECONDS.sleep(2);
        System.out.println("Demo over");
    }

    private static class ReceiverThread extends Thread{
        @Override
        public void run(){
            try {
                String data = transreceiver.receive();

                ReceiverThread receiverThread = new ReceiverThread();
                receiverThread.start();

                interpret(data);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void interpret(String data){
        String[] splitData = data.split(":"); // data = dataType:info
        String dataType = splitData[0];

        switch(dataType){
            case "prod": // data represents a product
                cache = splitData[1];
                break;
            default : // data is a notification
                System.out.println("NOTIFICATION: "+data);
                break;
        }
    }

    private static void scan(int idCode) throws IOException, InterruptedException{
        transreceiver.send("reqprod:"+idCode, BROKER_PORT);
        TimeUnit.MILLISECONDS.sleep(500); // allow time to receive, equivalent to a "loading" screen
    }

    private static void printProductSEL(String info) throws IOException{
        if(info!=null)
            transreceiver.send("print:"+info, BROKER_PORT);
    }
    
    private static void sub(String topic) throws IOException{
        transreceiver.send("sub:"+topic, BROKER_PORT);
    }

    private static void unsub(String topic) throws IOException{
         transreceiver.send("unsub:"+topic, BROKER_PORT);
    }

    // Class that can send and/or receive udp packets
    private static class Transreceiver{
        
        static final int MTU = 1500;

        private DatagramSocket transreceiver;
        
        public Transreceiver() throws IOException{
            transreceiver= new DatagramSocket();
        }

        public String receive() throws IOException{
            
            // create buffer for data, packet and transreceiver
            byte[] buffer= new byte[MTU];
            DatagramPacket packet= new DatagramPacket(buffer, buffer.length);
        
            transreceiver.receive(packet);

            // extract data from packet
            buffer= packet.getData();
            ByteArrayInputStream bstream= new ByteArrayInputStream(buffer);
            ObjectInputStream  ostream= new ObjectInputStream(bstream);

            String data =  ostream.readUTF();
            System.out.println("Received: \""+data+",\" from port:"+packet.getPort());
            return data;
        }

        public void send(String payload, int dest) throws IOException{
            
            InetAddress address= InetAddress.getLocalHost();   
            int port= dest;                       
        
            ByteArrayOutputStream bstream= new ByteArrayOutputStream();
            ObjectOutputStream ostream= new ObjectOutputStream(bstream);

            ostream.writeUTF(payload);
            ostream.flush();
            
            byte[] buffer = bstream.toByteArray();
            // create packet addressed to destination
            DatagramPacket packet= new DatagramPacket(buffer, buffer.length, address, port);
            transreceiver.send(packet);
        }
    }
}
