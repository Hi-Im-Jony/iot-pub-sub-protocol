package DataBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

/*
This class is a pseudo databse, that uses a hashmap to store info on a shops products

Database can:
- Store item info
- Serve item info
- Add/Edit/Remove item info
- Notify subs on any item changes
- Send error messages to broker
*/
public class DataBase {
    HashMap<Integer, Product> products;

    private void addProduct( String name, String section, int idCode, double price){
        
        if(!products.containsKey(idCode)){
            Product product = new Product(name,section,idCode,price);
            products.put(idCode, product);
            
            // TODO send success message to broker
            // TODO ask broker to update subs to this section
        }
        else{
            // TODO send error message to broker "Product exists"
        }
    }

    private void updateProduct( String name, String section, int idCode, double price){
        
        if(products.containsKey(idCode)){
            Product product = new Product(name,section,idCode,price);
            products.put(product.idCode, product);
            
            // TODO send success message to broker
            // TODO ask broker to update subs to this section
        }
        else{
            // TODO send error message to broker "Product doesn't exist"
        }
    }

    private void removeProduct(String section, int idCode){
        if(products.containsKey(section)){
            products.remove(idCode);
            
            // TODO ask broker to update subs to this section
        }

    }

    
    /*
    A class to store information on a product
    */
    private static class Product {
        public String name;
        public String section;
        public int idCode;
        public double price;

        public Product(String name, String section, int idCode, double price){
            this.name = name;
            this.section = section;
            this.idCode = idCode;
            this.price = price;
        }
    }


    // Class that can send and/or receive udp packets
    private static class SenderReceiver{
        
        static final int MTU = 1500;

        private DatagramSocket receiverSocket;
        private DatagramSocket senderSocket;

        private String owner;

        
        public SenderReceiver(int receiverPort, String owner) throws IOException{

            InetAddress address = InetAddress.getLocalHost();

            this.owner = owner;

            senderSocket = new DatagramSocket();
            receiverSocket= new DatagramSocket(receiverPort, address);
            
        }
        public String receive() throws IOException{
            
            // create buffer for data, packet and receiverSocket
            byte[] buffer= new byte[MTU];
            DatagramPacket packet= new DatagramPacket(buffer, buffer.length);
        
            receiverSocket.receive(packet);

            // extract data from packet
            buffer= packet.getData();
            ByteArrayInputStream bstream= new ByteArrayInputStream(buffer);
            ObjectInputStream  ostream= new ObjectInputStream(bstream);

            // print data and end of program
            String data =  ostream.readUTF();
            System.out.println(owner+"received: " + data);
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
            senderSocket.send(packet);
            
        }

        public String buildPayload(String topic, String data){
            String load = topic + data;
            //System.out.println(load);
            return  load;
        }
    }
}