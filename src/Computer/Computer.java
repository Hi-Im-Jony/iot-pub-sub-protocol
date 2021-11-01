package Computer;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
/*
A pseudo computer, that is used to add/edit/remove items in the database and print item SEL's

Company Computer can:
- Look up item details
- Add/Edit/Remove item in db
- Print item SEL
*/
public class Computer {
    
    private void requestProductDetails(int idCode){
        // TODO request product details from db, via Broker
        // print item details to console (ie, displaying details on computer screen)
    }

    private void addProduct(int idCode, String name, String section,  double price){
        // TODO add product to db via broker
        // print success message to console
    }

    private void removeProduct(int idCode){
        // TODO remove product from db via broker
        // print success message to console
    }

    private void editProduct(int idCode, String name, String section,  double price){
        // TODO edit product from db via broker
        // print success message to console
    }

    private void printSEL(int idCode){
        // TODO use Printer to print SEL
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
