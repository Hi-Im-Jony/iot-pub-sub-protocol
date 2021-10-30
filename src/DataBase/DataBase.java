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
    HashMap<String, HashMap<Integer, Product>> products;

    private void addSection(String sectionName){
        if(!products.containsKey(sectionName)){
            HashMap<Integer, Product> section = new HashMap<>();
            products.put(sectionName, section);

            //TODO send success message to broker
        }
        else{
            //TODO send error message to broker
        }
    }

    private void removeSection(String sectionName){
        products.remove(sectionName);
    }

    private void addProduct( Product product){
        if(products.containsKey(product.section)){
            if(!products.get(product.section).containsKey(product.idCode)){
                products.get(product.section).put(product.idCode, product);
                
                // TODO send success message to broker
            }
            else{
                // TODO send error message to broker "Product exists"
            }
        }
        else{
            // TODO send error message to broker "Section doesn't exist"
        }
    }

    private void updateProduct( Product product){
        if(products.containsKey(product.section)){
            if(products.get(product.section).containsKey(product.idCode)){
                products.get(product.section).put(product.idCode, product);
                
                // TODO send success message to broker
                // TODO ask broker to update subs to this section
            }
            else{
                // TODO send error message to broker "Product doesn't exist"
            }
        }
        else{
            // TODO send error message to broker "Section doesn't exist"
        }
        
    }

    private void removeProduct(Product product){
        if(products.containsKey(product.section))
            products.get(product.section).remove(product.idCode);
    }
}