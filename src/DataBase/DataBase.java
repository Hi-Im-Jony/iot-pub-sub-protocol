package DataBase;

import java.util.HashMap;
import Product.Product;
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

    private void addProduct( Product product){
        
        if(!products.containsKey(product.idCode)){
            products.put(product.idCode, product);
            
            // TODO send success message to broker
            // TODO ask broker to update subs to this section
        }
        else{
            // TODO send error message to broker "Product exists"
        }
    }

    private void updateProduct( Product product){
        
        if(products.containsKey(product.idCode)){
            products.put(product.idCode, product);
            
            // TODO send success message to broker
            // TODO ask broker to update subs to this section
        }
        else{
            // TODO send error message to broker "Product doesn't exist"
        }
    }

    private void removeProduct(Product product){
        if(products.containsKey(product.section)){
            products.remove(product.idCode);
            
            // TODO ask broker to update subs to this section
        }

    }
}