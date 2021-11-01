package Computer;

import Product.Product;

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
}
