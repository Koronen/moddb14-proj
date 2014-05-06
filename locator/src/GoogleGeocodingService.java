import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GoogleGeocodingService {

    public static final String API_BASE_URL = "http://maps.google.com/maps/api/geocode/xml";

    /**
     * Country code used to indicate resolver error.
     */
    public static final String NO_COUNTRY = null;

    public static final void main(String[] argv) throws Exception {
        GoogleGeocodingService cnti = new GoogleGeocodingService();
        System.out.println(cnti.locationToCountryName("Maluku"));
    }

    private DocumentBuilder documentBuilder;
    private XPath xpath;

    public GoogleGeocodingService() {
        try {
            documentBuilder = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            documentBuilder = null;
        }
        xpath = XPathFactory.newInstance().newXPath();
    }

    private String extractCountryName(Document geocoderResultDocument) {
        try {
            // obtain the formatted_address field for every result
            NodeList resultNodeList = (NodeList) xpath.evaluate(
                    "/GeocodeResponse/result/formatted_address",
                    geocoderResultDocument, XPathConstants.NODESET);

            // if google does not find any location
            if (resultNodeList.getLength() == 0) {
                return NO_COUNTRY;
            }

            String n = resultNodeList.item(0).getTextContent();
            String[] array = n.split(",");

            String countryPlusSpace = array[array.length - 1];
            String country = countryPlusSpace.trim();

            return country;
        } catch (XPathExpressionException e) {
            return NO_COUNTRY;
        }
    }

    public String locationToCountryName(String location) throws IOException {
        Document geocoderResultDocument = sendApiRequest(location);
        if (geocoderResultDocument != null) {
            return extractCountryName(geocoderResultDocument);
        } else {
            return NO_COUNTRY;
        }
    }

    private Document sendApiRequest(String location) throws IOException {
        URL apiUrl = new URL(API_BASE_URL + "?address="
                + URLEncoder.encode(location, "UTF-8") + "&sensor=false");
        HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
        try {
            conn.connect();
            return documentBuilder
                    .parse(new InputSource(conn.getInputStream()));
        } catch (SAXException sex) {
            return null;
        } finally {
            conn.disconnect();
        }
    }

}