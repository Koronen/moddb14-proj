import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;

public class countryNameToISO {

  // URL prefix to the geocoder
  private static final String GEOCODER_REQUEST_PREFIX_FOR_XML = "http://maps.google.com/maps/api/geocode/xml";
  public static int ISOcode;
  public HashMap<String, Integer> mapAddressToISO = new HashMap<String, Integer>();

  public static final void main (String[] argv) throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {
	  countryNameToISO cnti = new countryNameToISO();
	  String Address = "Maluku";
	  int iso = cnti.start(Address);
  }
  public int start(String address) throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
	  
	  //System.out.println("address: "+address);
    
	  Boolean inMap = checkIfInMap(address);
   
	  //if the address was not in the hashmap, do the google search.
	  if(inMap == false){

		  // prepare a URL to the geocoder
		  URL url = new URL(GEOCODER_REQUEST_PREFIX_FOR_XML + "?address=" + URLEncoder.encode(address, "UTF-8") + "&sensor=false");

		  // prepare an HTTP connection to the geocoder
		  HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		  Document geocoderResultDocument = null;
		  try {
		  // open the connection and get results as InputSource.
		  conn.connect();
		  InputSource geocoderResultInputSource = new InputSource(conn.getInputStream());
		
		  // read result and parse into XML Document
		  geocoderResultDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(geocoderResultInputSource);
		} finally {
		  conn.disconnect();
		}
		
		  //prepare XPath
		  XPath xpath = XPathFactory.newInstance().newXPath();
			
		  //extract the result
		  NodeList resultNodeList = null;
			
		  //obtain the formatted_address field for every result
		  resultNodeList = (NodeList) xpath.evaluate("/GeocodeResponse/result/formatted_address", geocoderResultDocument, XPathConstants.NODESET);
			
		  //if google does not find any location, return -1.
		  if(resultNodeList.item(0) ==(null)){
			  ISOcode = -1;
			  System.out.println("google cannot find location");
			  return ISOcode;
		  }
		  String n = resultNodeList.item(0).getTextContent();
		  String[] array = n.split(",");
			  
		  String countryPlusSpace = array[array.length-1];
		  String country = countryPlusSpace.trim();
		  System.out.println("countryName: "+country);
			   
		
		  //fetching the country code
			
		  CountryNameToCountryCode cntc = new CountryNameToCountryCode();
		  String code = cntc.getCode(country);
		  
		  //if the country code is not found , return -1
		  if(code.equals("")){
			  System.out.println("No country code found");
			  ISOcode = -1;
			  return ISOcode;
		  }
			
		  //System.out.println("CountryCode: "+code);
			  
		  //Feching the iso code by creating an object of the  CountryCodeToISO class
		  CountryCodeToISO cc = CountryCodeToISO.getByCode(code);
			  
		  //if the country is not in the countryNameToCountryCode class, return -1
		  if(cc == null){
			  ISOcode = -1;
			  return ISOcode;
		  }
			  
		  //getting the numeric iso code
		  ISOcode = cc.getNumeric();
		  System.out.println("got iso by doing google search");
			  
		  //save mapping in cache
		  addToMap(address, ISOcode);
	  }
    
      //else, get the iso from the hashmap
	  else{
    	
		  ISOcode = getISOFromMap(address);
		  System.out.println("got iso from hashmap");
	  }
      

      System.out.println("ISO 3166-1 numeric code = " + ISOcode);
      return ISOcode;
   
  }
  public int getISOFromMap(String checkAddress){
	  
	  String key = checkAddress;
	  Integer value = mapAddressToISO.get(key);
	  return value;
  }
  
  public void addToMap(String newaddress, int newISO){
	  mapAddressToISO.put(newaddress, newISO);
  }
	
  public Boolean checkIfInMap(String getAddress){
	  Boolean b = mapAddressToISO.containsKey(getAddress);
	  return b;
  }


}