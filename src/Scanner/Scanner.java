package Scanner;

/*
A pseudo scanning device, that "scans" a qr/bar code to look up an item in the database

After finding an item in the db, Scanner can:
    - "Scan" qr/bar codes to check item details
    - Print item SEL
    - Subscribe to a "section" of the store (ie, sub to a topic)
    - Get notified when item in section gets added/edited/removed
*/
public class Scanner {

    static final int DB_ADDRESS = 1;
    static final int BROKER_ADDRESS = 2;
    
    private void scan(int code){
        // TODO
    }

    private void printSEL(int code){
        // TODO
    }

    private void subToSection(String section){
        //  TODO
    }

    private void receiveAlert(){
        // TODO
    }
}
