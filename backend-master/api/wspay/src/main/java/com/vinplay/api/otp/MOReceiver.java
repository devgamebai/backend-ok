/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.BasicDBObject
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.model.FindOneAndUpdateOptions
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.usercore.entities.MessageMTResponse
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.vbee.common.messages.OtpMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.api.otp;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.usercore.entities.MessageMTResponse;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.vbee.common.messages.OtpMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

@WebService(name="MOReceiver", targetNamespace="http://wspay.win123.club/")
@SOAPBinding(style=SOAPBinding.Style.RPC)
public class MOReceiver {
    private static final Logger logger = Logger.getLogger(MOReceiver.class);

    @WebMethod
    public String messageReceiver(@WebParam(name="cpid") String cpid, @WebParam(name="sign") String sign, @WebParam(name="smsid") String smsid, @WebParam(name="sender") String sender, @WebParam(name="serviceNumber") String serviceNumber, @WebParam(name="keyword") String keyword, @WebParam(name="content") String content, @WebParam(name="receiverTime") String receiverTime) {
        logger.debug((Object)("OTP message receiver: \n cpid: " + cpid + "\n sign: " + sign + "\n smsid: " + smsid + "\n sender: " + sender + "\n serviceNumber: " + serviceNumber + "\n keyword: " + keyword + "\n content: " + content + "\n receiverTime: " + receiverTime));
        OtpServiceImpl otpService = new OtpServiceImpl();
        MessageMTResponse mtres = null;
        if (sign != null && sender != null && cpid != null && smsid != null && content != null && sender != "" && cpid != "" && smsid != "" && content != "" && sign != "" && receiverTime != null && receiverTime != "") {
            String PrivateKey = "DBE861B65FFC8D920721";
            String hashinput = String.valueOf(cpid) + smsid + content + receiverTime + "DBE861B65FFC8D920721";
            String hashoutput = "";
            try {
                hashoutput = this.getHash(hashinput);
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
            if (sign.equals(hashoutput)) {
                String mobile = otpService.revertMobile(sender);
                OtpMessage message = new OtpMessage(smsid, mobile, keyword, content);
                try {
                    mtres = otpService.genMessageMT(message, mobile);
                    if (mtres != null) {
                        String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<ClientResponse>\n<Message>" + mtres.getOtp() + "</Message>\n<Smsid>" + smsid + "</Smsid>\n<Receiver>" + sender + "</Receiver>\n</ClientResponse>";
                        org.w3c.dom.Document doc = this.convertStringToDocument(xmlStr);
                        otpService.updateOtp(sender, mtres.getOtp(), message.getMessageMO());
                        this.updateOtpSMSInfoProvider(sender, smsid, content, mtres.getMessage(), mtres.getOtp(), "0", receiverTime);
                        AlertServiceImpl service = new AlertServiceImpl();
                        service.SendSMSEsms(mobile, mtres.getOtp());
                        return "";
                    }
                }
                catch (Exception e2) {
                    logger.debug((Object)e2);
                }
            }
        }
        return "";
    }

    private String getHash(String sSrc) throws Exception {
        Charset utf8Charset = Charset.forName("UTF-8");
        byte[] textBytes = sSrc.getBytes(utf8Charset);
        MessageDigest m = MessageDigest.getInstance("MD5");
        byte[] digest = m.digest(textBytes);
        String sEncodedText = Base64.getEncoder().encodeToString(digest);
        return sEncodedText;
    }

    private String GetMobile84(String mobile) {
        if (!mobile.substring(0, 2).equals("84") && !mobile.substring(0, 3).equals("084")) {
            return "84" + mobile.substring(1);
        }
        return mobile;
    }

    private boolean updateOtpSMSInfoProvider(String mobile, String smsid, String content, String messageMO, String otp, String Status, String dt) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_sms_vin_otp");
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("mobile", (Object)mobile);
        updateFields.append("otp", (Object)otp);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
        Date date = new Date();
        try {
            date = format.parse(dt);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        updateFields.append("receivedDate", (Object)date);
        updateFields.append("createDate", (Object)VinPlayUtils.getCurrentDateTime());
        updateFields.append("content", (Object)content);
        updateFields.append("Status", (Object)Status);
        updateFields.append("ResponseMS", (Object)messageMO);
        BasicDBObject conditions = new BasicDBObject();
        conditions.append("mobile", (Object)mobile);
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.upsert(true);
        col.findOneAndUpdate((Bson)conditions, (Bson)new Document("$set", (Object)updateFields), options);
        return true;
    }

    private String convertDocumentToString(org.w3c.dom.Document doc) {
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            Transformer transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString();
            return output;
        }
        catch (TransformerException e) {
            e.printStackTrace();
            return null;
        }
    }

    private org.w3c.dom.Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            return doc;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

