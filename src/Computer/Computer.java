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
    private static SenderReceiver transreceiver;
    public static void main(String[] args) throws IOException {
        
        System.out.println("Computer turned on");

        int receiverPort =  Integer.parseInt(args[0]);
        transreceiver = new SenderReceiver(receiverPort, "Printer");

        CopmuterReceiverThread receiverThread = new CopmuterReceiverThread(); // create new "back up thread" to receive while we print
        receiverThread.start();

        if(Integer.parseInt(args[1])==0)
            operateManually();
        else
            operateAutomaticlly();
    }

    // manual operation of "Computer" via terminal
    private static void operateManually(){
    }

    // automatic, hardcoded operation of "Computer"
    private static void operateAutomaticlly() throws IOException{
        addProduct(0,"A","Games",19.99);
        addProduct(1,"Bb","Games",19.99);
        addProduct(2,"CCC","Toys",19.99);
        addProduct(3,"DDD","Toys",19.99);

        requestProductDetails(2);


    }

    private static void viewReceivedData(String data){

        String[] splitData = data.split(":");
        String request = splitData[0];

        switch(request){
            case "showprod":
                String[] productDetails = splitData[1].split("/");
                System.out.println("\n\n\n----------------------");
                System.out.println("      |  Product details   ");
                System.out.println("      | -------------------");
                System.out.println("      | Name:"+productDetails[1]);
                System.out.println("      | Section:"+productDetails[2]);
                System.out.println("      | Price:"+productDetails[3]);
                System.out.println("      | -------------------|");
                break;
        }
    }
    private static class CopmuterReceiverThread extends Thread{
        @Override
        public void run(){
            try {
                String data = transreceiver.receive();

                CopmuterReceiverThread receiverThread = new CopmuterReceiverThread();
                receiverThread.start();
                viewReceivedData(data);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void requestProductDetails(int idCode) throws IOException{
        transreceiver.send("reqprod:"+idCode, 2);
    }

    private static void addProduct(int idCode, String name, String section,  double price) throws IOException{
        transreceiver.send("addprod:"+idCode+"/"+name+"/"+section+"/"+price, 2);
    }

    private void removeProduct(int idCode) throws IOException{
        transreceiver.send("remprod:"+idCode, 2);
    }

    private void editProduct(int idCode, String name, String section,  double price) throws IOException{
        transreceiver.send("ediprod:"+idCode+"/"+name+"/"+section+"/"+price, 2);
    }

    private void printProductSEL(int idCode) throws IOException{
        transreceiver.send("print:"+idCode, 2);
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
