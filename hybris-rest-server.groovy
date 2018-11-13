import groovy.json.*;
import com.sun.net.httpserver.*; 
import de.hybris.platform.core.Registry;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.hybris.platform.servicelayer.search.FlexibleSearchService; 
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.product.ProductService;
def portNumber =  8900;

logWriter("Starting HybrisArchitect.com REST Server on "+portNumber);
HttpServer server = HttpServer.create(new InetSocketAddress(portNumber),0); 
server.createContext("/product", new ProductHandler(server:server));
server.createContext("/test", new TestHandler(server:server)); 
server.createContext("/shutdown", new ShutdownHandler(server:server));
server.start(); 
logWriter("HybrisArchitect.com REST Server Has Started on "+portNumber);


abstract class AdvancedHttpHandler implements HttpHandler { 
 
  public void responseREST(HttpExchange exchange, String response) throws IOException {
    exchange.responseHeaders['Content-Type'] = 'application/json' 
    response = groovy.json.JsonOutput.prettyPrint(response)
    exchange.sendResponseHeaders(200, response.length());
    exchange.getResponseBody().write(response.bytes);
    exchange.close();
  }
  
  public void logWriter(String logEntry) {   
    def Logger logger = LoggerFactory.getLogger("HybrisArchitectLogger")
    logger.info(logEntry)
  }  
}

class ProductHandler extends AdvancedHttpHandler {
    def server
    public void handle(HttpExchange exchange) throws IOException {

           def builder   = new JsonBuilder();
         
          try{   
               
                def productCode      = getProductCodeFromRequestURI(exchange.getRequestURI());
                if ("".equals(productCode)) {
                     builder {
                         message  "No Product Code Found in URL Path"
                     } 
                     responseREST(exchange,builder.toString());
                     return;
                }

                def catalogName      = getCatalogNameFromRequestURI(exchange.getRequestURI());
                if ("".equals(catalogName)) {
                     builder {
                         message  "No Catalog Name Found in URL Path"
                     } 
                     responseREST(exchange,builder.toString());
                     return;
                }

                def catalogVersion      = getCatalogVersionFromRequestURI(exchange.getRequestURI());
                if ("".equals(catalogVersion)) {
                     builder {
                         message  "No Catalog Version Found in URL Path"
                     } 
                     responseREST(exchange,builder.toString());
                     return;
                }

                CatalogVersionService cvs = Registry.getApplicationContext().getBean("catalogVersionService", CatalogVersionService.class);
                CatalogVersionModel cvm = cvs.getCatalogVersion(catalogName,catalogVersion) 
                ProductService productService = Registry.getApplicationContext().getBean("productService", ProductService.class);
                cvs.addSessionCatalogVersion(cvm); 
                ProductModel productModel = productService.getProductForCode( cvs.getSessionCatalogVersions().getAt(0), productCode);
                 
                /* build the JSON message **/  
                 builder {
                     code           productModel.getCode()
                     name           productModel.getName()
                     description    productModel.getDescription()
                     summary        productModel.getSummary()
                     approvalStatus productModel.getApprovalStatus()
                   
                     priceRows  productModel.getEurope1Prices().collect { 
                          [
                          price:          it.getPrice(),
                          currencyName:   it.getCurrency().getName(),
                          unit:           it.getUnit().getCode() 
                         ]     
                      } 
                    }  
                 responseREST(exchange,builder.toString());
             
              }catch(Exception exception){
                builder {
                     message        exception.toString()
                 }    
                responseREST(exchange,builder.toString());
                logWriter(exception.toString()); 
             } 
       }

        public String getProductCodeFromRequestURI(URI uri) {  
          String urlPath = uri.getPath();  
          def code = ""; 
          if (urlPath.contains('/')) { 
              def pathArray = urlPath.split('/');
              
              if ("code".equals(pathArray[2].toString())) {
                  code = pathArray[3].toString();
              }
          }  
          return code;
   } 

   public String getCatalogNameFromRequestURI(URI uri) {  
          String urlPath = uri.getPath();  
          def code = ""; 
          if (urlPath.contains('/')) { 
              def pathArray = urlPath.split('/'); 
              if ("catalogname".equals(pathArray[4].toString())) {
                  code = pathArray[5].toString();
              }
          }  
          return code;
   } 

   public String getCatalogVersionFromRequestURI(URI uri) {  
          String urlPath = uri.getPath();  
          def code = ""; 
          if (urlPath.contains('/')) { 
              def pathArray = urlPath.split('/'); 
              if ("version".equals(pathArray[6].toString())) {
                  code = pathArray[7].toString();
              }
          }  
          return code;
   }  
}  

class TestHandler extends AdvancedHttpHandler {
    def server
   public void handle(HttpExchange exchange) throws IOException {
     try {
        Date date = new Date();
        def builder = new JsonBuilder()
        builder {
            message "This is a test" 
            datetime date.toString()
        } 
        responseREST(exchange,builder.toString());
     
        }catch(Exception ex){
          responseREST(exchange,ex.toString());
          logWriter(ex.printStackTrace());
        }
    }
} 
 

class ShutdownHandler extends AdvancedHttpHandler {
 
    def server;
  
    public void handle(HttpExchange exchange) throws IOException {
      
        def builder = new JsonBuilder();
        builder { 
            message "Shutting down HybrisArchitect.com REST API Server..."
        }  
        responseREST(exchange,builder.toString());
        server.stop(2); 
    }
} 

 def logWriter(String logEntry) {  
    def date = new Date();  
    def Logger logger = LoggerFactory.getLogger("HybrisArchitectLogger");
    logger.info(date.toString() + ": " + logEntry);
  }
 
