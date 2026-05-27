/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringEscapeUtils
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.vinplay.usercore.service.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SmsBrandNameService {
    static String url = "http://210.211.109.117/apibrandname/send?wsdl";
    private String apiUrl;
    private String username;
    private String password;
    private String brand;

    public SmsBrandNameService(String apiUrl, String username, String password, String brand) {
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        this.brand = brand;
    }

    public String SendGet(String mesage, String phone, String IDREQ) {
        String otp = "(" + this.brand + "): Your verify code is " + mesage;
        String text = StringEscapeUtils.escapeXml((String)otp);
        String xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:send=\"http://send_sms.vienthongdidong.vn/\">\n   <soapenv:Header/>\n   <soapenv:Body>\n      <send:send>\n         <!--Optional:-->\n         <USERNAME>" + this.username + "</USERNAME>\n         <!--Optional:-->\n         <PASSWORD>" + this.password + "</PASSWORD>\n         <!--Optional:-->\n         <BRANDNAME>" + this.brand + "</BRANDNAME>\n         <!--Optional:-->\n         <MESSAGE>" + text + "</MESSAGE>\n         <!--Optional:-->\n         <TYPE>1</TYPE>\n         <!--Optional:-->\n         <PHONE>" + phone + "</PHONE>\n         <!--Optional:-->\n         <IDREQ>" + IDREQ + "</IDREQ>\n      </send:send>\n   </soapenv:Body>\n</soapenv:Envelope>";
        String val = this.sendPost(this.apiUrl, xml);
        val = this.parser(val);
        return val;
    }

    public String parser(String xmlRecords) {
        System.out.println("xmlRecords:" + xmlRecords);
        String reval = "";
        JSONObject json = new JSONObject();
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xmlRecords));
            Document doc = db.parse(is);
            NodeList nodes = doc.getElementsByTagName("return");
            for (int i = 0; i < nodes.getLength(); ++i) {
                Element element = (Element)nodes.item(i);
                NodeList name = element.getElementsByTagName("result");
                Element line = (Element)name.item(0);
                reval = SmsBrandNameService.getCharacterDataFromElement(line);
                System.out.println("result: " + reval);
                json.put("result", reval);
                name = element.getElementsByTagName("receipt");
                line = (Element)name.item(0);
                reval = SmsBrandNameService.getCharacterDataFromElement(line);
                System.out.println("receipt: " + reval);
                json.put("receipt", reval);
                name = element.getElementsByTagName("detail");
                line = (Element)name.item(0);
                reval = SmsBrandNameService.getCharacterDataFromElement(line);
                System.out.println("detail: " + reval);
                json.put("detail", reval);
                name = element.getElementsByTagName("idTran");
                line = (Element)name.item(0);
                reval = SmsBrandNameService.getCharacterDataFromElement(line);
                System.out.println("idTran: " + reval);
                json.put("idtrans", reval);
            }
        }
        catch (ParserConfigurationException ex) {
            ex.printStackTrace();
            reval = "";
        }
        catch (SAXException ex) {
            ex.printStackTrace();
            reval = "";
        }
        catch (IOException ex) {
            ex.printStackTrace();
            reval = "";
        }
        catch (JSONException ex) {
            Logger.getLogger(SmsBrandNameService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return json.toString();
    }

    public static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData)child;
            return cd.getData();
        }
        return "";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String sendPost(String url, String urlParameters) {
        String reval = "";
        try {
            String inputLine;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection)obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Host", "210.211.109.118");
            con.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            con.setRequestProperty("Content-Length", "length");
            con.setRequestProperty("SOAPAction", "http://send_sms.vienthongdidong.vn/send/sendRequest");
            con.setReadTimeout(15000);
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            reval = response.toString();
            return reval;
        }
        catch (Exception e) {
            System.out.print("==>> Excetion : " + e.getMessage());
            reval = "";
            return reval;
        }
        finally {
            return reval;
        }
    }
}

