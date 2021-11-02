package Computer;

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
A pseudo computer, that is used to add/edit/remove items in the database and print item SEL's

Company Computer can:
- Look up item details
- Add/Edit/Remove item in db
- Print item SEL
*/
public class Computer {

    static final int BROKER_PORT = 2;

    private static Transreceiver transreceiver;
    private static String cache;
    public static void main(String[] args) throws IOException, InterruptedException {
        
        System.out.println("Computer turned on");

        transreceiver = new Transreceiver();

        CopmuterReceiverThread receiverThread = new CopmuterReceiverThread(); // create new "back up thread" to receive while we print
        receiverThread.start();

        if(Integer.parseInt(args[0])==0)
            operateManually();
        else
            operateAutomaticlly();
    }

    private static class CopmuterReceiverThread extends Thread{
        @Override
        public void run(){
            try {
                cache = transreceiver.receive();

                CopmuterReceiverThread receiverThread = new CopmuterReceiverThread();
                receiverThread.start();

                
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void interpretInput(String input){
        int count = input.length() - input.replaceAll("/", "").length(); // count of "/" in a our input
        
        if(count==3){
            // input is a product we have requested for
            cache = input;
        }
    }
    // manual operation of "Computer" via terminal
    private static void operateManually(){
    }

    // automatic, hardcoded operation of "Computer"
    private static void operateAutomaticlly() throws IOException, InterruptedException{
        // requestProductDetails(2);
        // TimeUnit.SECONDS.sleep(4);

        // // attempting to edit non-existent product
        // editProduct(2, "name", "section", 9.99);
        // TimeUnit.SECONDS.sleep(4);

        addProduct(0,"A","Games",19.99);

        // add product with duplicate id
        // addProduct(0,"B","Games",19.99);
        // TimeUnit.SECONDS.sleep(4);
        requestProductDetails(0);
        
        printProductSEL(cache);
        TimeUnit.SECONDS.sleep(4);

        // remove non-existent product
        removeProduct(2);
        TimeUnit.SECONDS.sleep(4);

        addProduct(1,"Bb","Games",19.99);
        addProduct(2,"CCC","Toys",19.99);
        addProduct(3,"DDD","Toys",19.99);
        TimeUnit.SECONDS.sleep(4);

        // attempting to add duplicate
        addProduct(3,"DDD","Toys",19.99);
        TimeUnit.SECONDS.sleep(4);

        requestProductDetails(2);
        TimeUnit.SECONDS.sleep(4);


        removeProduct(2);
        TimeUnit.SECONDS.sleep(4);

        // attempting to edit non-existent product
        editProduct(2, "name", "section", 9.99);
        TimeUnit.SECONDS.sleep(4);
        
        addProduct(2,"CCC","Toys",19.99);
        TimeUnit.SECONDS.sleep(4);

        // change price
        editProduct(2, "CCC", "Toys", 9.99);
        TimeUnit.SECONDS.sleep(4);

        // change section
        editProduct(2, "CCC", "Pets", 9.99);
        TimeUnit.SECONDS.sleep(4);
        System.out.println("Auto finished");
        
    }

    
    
    
    private static synchronized void requestProductDetails(int idCode) throws IOException, InterruptedException{
        transreceiver.send("reqprod:"+idCode, BROKER_PORT);
        TimeUnit.MILLISECONDS.sleep(500); // allow time to receive, equivalent to a "loading" screen
    }

    private static void addProduct(int idCode, String name, String section,  double price) throws IOException{
        transreceiver.send("addprod:"+idCode+"/"+name+"/"+section+"/"+price, BROKER_PORT);
    }

    private static void removeProduct(int idCode) throws IOException{
        transreceiver.send("remprod:"+idCode, BROKER_PORT);
    }

    private static void editProduct(int idCode, String name, String section,  double price) throws IOException{
        transreceiver.send("ediprod:"+idCode+"/"+name+"/"+section+"/"+price, BROKER_PORT);
    }

    private static void printProductSEL(String info) throws IOException{
        if(info!=null)
            transreceiver.send("print:"+info, BROKER_PORT);
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
