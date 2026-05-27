/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.dal.service.LogPortalService
 *  com.vinplay.dal.service.impl.LogPortalServiceImpl
 *  com.vinplay.dichvuthe.service.impl.AlertServiceImpl
 *  com.vinplay.dichvuthe.service.impl.RechargeServiceImpl
 *  com.vinplay.dichvuthe.utils.SMSUtils
 *  com.vinplay.payment.utils.PayUtils
 *  com.vinplay.usercore.entities.MessageMTResponse
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.service.impl.SecurityServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseController
 *  com.vinplay.vbee.common.cp.NoCommandRegistered
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 *  com.vinplay.vbee.common.hazelcast.HazelcastLoader
 *  com.vinplay.vbee.common.messages.OtpMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  com.vinplay.vtcpay.request.CheckAccountRequest
 *  com.vinplay.vtcpay.request.CheckTransRequest
 *  com.vinplay.vtcpay.request.TopupRequest
 *  com.vinplay.vtcpay.response.TopupResponse
 *  com.vinplay.vtcpay.service.impl.VTCPayServiceImpl
 *  javax.servlet.DispatcherType
 *  javax.servlet.ServletException
 *  javax.servlet.ServletInputStream
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.log4j.Logger
 *  org.apache.log4j.PropertyConfigurator
 *  org.bson.Document
 *  org.eclipse.jetty.server.ConnectionFactory
 *  org.eclipse.jetty.server.Connector
 *  org.eclipse.jetty.server.Handler
 *  org.eclipse.jetty.server.HttpConfiguration
 *  org.eclipse.jetty.server.HttpConfiguration$Customizer
 *  org.eclipse.jetty.server.HttpConnectionFactory
 *  org.eclipse.jetty.server.SecureRequestCustomizer
 *  org.eclipse.jetty.server.Server
 *  org.eclipse.jetty.server.ServerConnector
 *  org.eclipse.jetty.server.SslConnectionFactory
 *  org.eclipse.jetty.server.handler.HandlerCollection
 *  org.eclipse.jetty.server.handler.IPAccessHandler
 *  org.eclipse.jetty.servlet.FilterHolder
 *  org.eclipse.jetty.servlet.ServletHandler
 *  org.eclipse.jetty.servlet.ServletHolder
 *  org.eclipse.jetty.util.ssl.SslContextFactory
 *  org.json.JSONObject
 *  org.json.simple.parser.JSONParser
 */
package com.vinplay.pay.server;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.api.brandname.Service;
import com.vinplay.dal.service.LogPortalService;
import com.vinplay.dal.service.impl.LogPortalServiceImpl;
import com.vinplay.dichvuthe.service.impl.AlertServiceImpl;
import com.vinplay.dichvuthe.service.impl.RechargeServiceImpl;
import com.vinplay.dichvuthe.utils.SMSUtils;
import com.vinplay.payment.utils.PayUtils;
import com.vinplay.usercore.entities.MessageMTResponse;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.SecurityServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.cp.BaseController;
import com.vinplay.vbee.common.cp.NoCommandRegistered;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.messages.OtpMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vtcpay.request.CheckAccountRequest;
import com.vinplay.vtcpay.request.CheckTransRequest;
import com.vinplay.vtcpay.request.TopupRequest;
import com.vinplay.vtcpay.response.TopupResponse;
import com.vinplay.vtcpay.service.impl.VTCPayServiceImpl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Endpoint;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.IPAccessHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class WSPay {
    private static final Logger logger = Logger.getLogger(WSPay.class);
    private static final Logger blackListIpLogger = Logger.getLogger((String)"BlackListIpLogger");
    private static String LOG_PROPERTIES_FILE = "config/log4j.properties";
    private static LogPortalService service = new LogPortalServiceImpl();
    private static int HTTP_PORT = 8081;
    private static int SSL_PORT = 443;
    private static long IDLE_TIMEOUT = 30000L;
    private static BaseController<HttpServletRequest, String> controller;
    private static String basePath;

    private static void initializeLogger() {
        Properties logProperties = new Properties();
        try {
            File file = new File(basePath.concat(LOG_PROPERTIES_FILE));
            logProperties.load(new FileInputStream(file));
            PropertyConfigurator.configure((Properties)logProperties);
            logger.info((Object)"Logging initialized.");
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to load logging property config/log4j.properties");
        }
    }

    public static void main(String[] args) {
        try {
            basePath = VBeePath.initBasePath(WSPay.class);
            WSPay.initializeLogger();
            logger.info((Object)"STARTING WEBSERVICE PAY API .... !!!!");
            WSPay.loadConfig();
            RMQApi.start((String)"config/rmq.properties");
            HazelcastLoader.start();
            MongoDBConnectionFactory.init();
            GameCommon.init();
            PayUtils.init();
            SMSUtils.init();
            Server server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(HTTP_PORT);
            connector.setIdleTimeout(IDLE_TIMEOUT);
            ServletHandler handler = new ServletHandler();
            handler.addFilterWithMapping(CorsFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
            handler.addServletWithMapping(JeetyServlet.class, "/api");
            handler.addServletWithMapping(TopupServlet.class, "/topup");
            handler.addServletWithMapping(NapasServlet.class, "/i2b");
            handler.addServletWithMapping(EmailServlet.class, "/activeEmail");
            handler.addServletWithMapping(SMSPlusCheckMOServlet.class, "/smsPlus/checkMO");
            handler.addServletWithMapping(SMS8xRequestServlet.class, "/sms8x/request");
            handler.addServletWithMapping(SMSPlusRequestServlet.class, "/smsPlus/request");
            handler.addServletWithMapping(MOReceiver.class, "/receive_mo");
            handler.addServletWithMapping(Iwin99BuyCardServerlet.class, "/api/gachthe/callback");
            handler.addServletWithMapping(Iwin99BankCallbackServlet.class, "/api/bank/callback");
            handler.addServletWithMapping(Iwin99MomoCallbackServlet.class, "/api/momo/callback");

            IPAccessHandler ipAccessHandler = new IPAccessHandler();
            File fXmlFile = new File(basePath.concat("config/ip_access.xml"));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("uri");
            for (int temp = 0; temp < nList.getLength(); ++temp) {
                Element eElement = (Element)nList.item(temp);
                String uri = eElement.getAttribute("id");
                Element eElementWhitelist = (Element)eElement.getElementsByTagName("whitelist").item(0);
                NodeList nListWhite = eElementWhitelist.getElementsByTagName("ip");
                for (int i = 0; i < nListWhite.getLength(); ++i) {
                    Element ipWhile = (Element)nListWhite.item(i);
                    ipAccessHandler.addWhite(String.valueOf(ipWhile.getTextContent()) + "|" + uri);
                }
                Element eElementBlack = (Element)eElement.getElementsByTagName("blacklist").item(0);
                NodeList nListBlack = eElementBlack.getElementsByTagName("ip");
                for (int j = 0; j < nListBlack.getLength(); ++j) {
                    Element ipBlack = (Element)nListBlack.item(j);
                    ipAccessHandler.addBlack(String.valueOf(ipBlack.getTextContent()) + "|" + uri);
                }
            }
            ipAccessHandler.setHandler((Handler)handler);
            HandlerCollection handlerCollection = new HandlerCollection();
            handlerCollection.setHandlers(new Handler[]{ipAccessHandler});
            server.setHandler((Handler)handlerCollection);
            server.setConnectors(new Connector[]{connector});
            server.start();
            logger.info((Object)"WEBSERVICE PAY API Started ...!!!");
            server.join();
        }
        catch (Exception e) {
            logger.error((Object)("WEBSERVICE PAY API Start error: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    public static class Iwin99BuyCardServerlet
            extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            RechargeServiceImpl service = new RechargeServiceImpl();
            logger.info(request.getParameterMap());
            String result = service.receiveResultFromIwin99BuyCard(request.getParameterMap());
            response.getWriter().println(result);
        }
    }

    public static class Iwin99BankCallbackServlet
            extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            RechargeServiceImpl service = new RechargeServiceImpl();
            logger.info(request.getParameterMap());
            String result = service.rechargeByBankIwin99(request.getParameterMap());
            response.getWriter().println(result);
        }
    }

    public static class Iwin99MomoCallbackServlet
            extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            RechargeServiceImpl service = new RechargeServiceImpl();
            logger.info(request.getParameterMap());
            String result = service.rechargeByMomoIwin99(request.getParameterMap());
            response.getWriter().println(result);
        }
    }

    private static void otpServiceSOAP() {
        try {
            logger.info((Object)("Publish OTP Service url " + GameCommon.OTP_URL_RECEIVE_MO));
            Endpoint.publish(GameCommon.OTP_URL_RECEIVE_MO, (Object)new MOReceiver());
            logger.info((Object)("Publish OTP Service url " + GameCommon.OTP_URL_RECEIVE_MO + " success"));
        }
        catch (Exception e) {
            logger.info((Object)("Publish OTP Service url " + GameCommon.OTP_URL_RECEIVE_MO + " error: " + e.getMessage()));
            System.out.println("Publish  OTP Service url " + GameCommon.OTP_URL_RECEIVE_MO + " error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void brandnameServiceSOAP() {
        try {
            Endpoint.publish(GameCommon.BRANDNAME_URL_REPORT_FROM_ST, new Service());
        }
        catch (Exception e) {
            logger.info((Object)("Publish Brandname url error: " + e.getMessage()));
            System.out.println("Publish Brandname url error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadConfig() throws Exception {
        File file = new File(basePath.concat("config/api_portal.xml"));
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
        org.w3c.dom.Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("portal");
        Element el = (Element)nodeList.item(0);
        HTTP_PORT = Integer.parseInt(el.getElementsByTagName("http_port").item(0).getTextContent());
        SSL_PORT = Integer.parseInt(el.getElementsByTagName("ssl_port").item(0).getTextContent());
        IDLE_TIMEOUT = Integer.parseInt(el.getElementsByTagName("idle_timeout").item(0).getTextContent());
        Element cmds = (Element)el.getElementsByTagName("commands").item(0);
        NodeList cmdList = cmds.getElementsByTagName("command");
        HashMap<Integer, String> commandsMap = new HashMap<Integer, String>();
        for (int i = 0; i < cmdList.getLength(); ++i) {
            Element eCmd = (Element)cmdList.item(i);
            Integer id = Integer.parseInt(eCmd.getElementsByTagName("id").item(0).getTextContent());
            String path = eCmd.getElementsByTagName("path").item(0).getTextContent();
            logger.debug((Object)(id + " <-> " + path));
            System.out.println(id + " <-> " + path);
            commandsMap.put(id, path);
        }
        controller = new BaseController();
        controller.initCommands(commandsMap);
    }

    public static class SMSOTPChargingServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            String res = "";
            response.getWriter().println("");
        }
    }

    public static class SMSPlusRequestServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            RechargeServiceImpl service = new RechargeServiceImpl();
            String res = service.smsPlusRequest(request.getParameterMap());
            response.getWriter().println(res);
        }
    }

    public static class SMS8xRequestServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            RechargeServiceImpl service = new RechargeServiceImpl();
            String res = service.sms8xRequest(request.getParameterMap());
            response.getWriter().println(res);
        }
    }

    public static class SMSPlusCheckMOServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            RechargeServiceImpl service = new RechargeServiceImpl();
            String res = service.smsPlusCheckMO(request.getParameterMap());
            response.getWriter().println(res);
        }
    }

    public static class NapasServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            RechargeServiceImpl service = new RechargeServiceImpl();
            service.receiveResultFromBank(request.getParameterMap());
            String web = "";
            try {
                web = GameCommon.getValueStr((String)"WEB");
            }
            catch (KeyNotFoundException e) {
                e.printStackTrace();
            }
            response.sendRedirect(web);
        }
    }

    public static class EmailServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            String token = request.getParameter("token");
            SecurityServiceImpl secure = new SecurityServiceImpl();
            String web = "";
            try {
                web = GameCommon.getValueStr((String)"WEB");
            }
            catch (KeyNotFoundException e) {
                e.printStackTrace();
            }
            String res = secure.receiveActiveEmail(token) + "</br><a href='" + web + "'>Quay l\u1ea1i trang ch\u1ee7</a>";
            response.getWriter().println(res);
        }
    }

    public static class MOReceiver
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            block7 : {
                response.setContentType("text/html");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(200);
                String remoteAddr = request.getRemoteAddr();
                logger.info((Object)("IP:" + remoteAddr));
                Object message = null;
                try {
                    String sign = request.getParameter("sign");
                    String cpid = request.getParameter("cpid");
                    String smsid = request.getParameter("smsid");
                    String sender = request.getParameter("sender");
                    String serviceNumber = request.getParameter("serviceNumber");
                    String keyword = request.getParameter("keyword");
                    String content = request.getParameter("content");
                    String receiverTime = request.getParameter("receiverTime");
                    OtpServiceImpl otpService = new OtpServiceImpl();
                    MessageMTResponse mtres = null;
                    if (sign == null || sender == null || cpid == null || smsid == null || content == null || sender == "" || cpid == "" || smsid == "" || content == "" || sign == "" || receiverTime == null || receiverTime == "") break block7;
                    String PrivateKey = "DBE861B65FFC8D920721";
                    String hashinput = String.valueOf(cpid) + smsid + content + receiverTime + "DBE861B65FFC8D920721";
                    String hashoutput = "";
                    try {
                        hashoutput = this.getHash(hashinput);
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    String mobile = otpService.revertMobile(sender);
                    OtpMessage message2 = new OtpMessage(smsid, mobile, keyword, content.trim().replace("+", " ").toUpperCase());
                    try {
                        mtres = otpService.genMessageMT(message2, mobile);
                        if (mtres != null) {
                            String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<ClientResponse>\n<Message>Cam on ban,chung toi da nhan dc tin nhan.Ma se duoc gui trong vai giay nua.</Message>\n<Smsid>" + smsid + "</Smsid>\n<Receiver>" + sender + "</Receiver>\n</ClientResponse>";
                            org.w3c.dom.Document doc = this.convertStringToDocument(xmlStr);
                            otpService.updateOtp(mobile, mtres.getOtp(), message2.getMessageMO());
                            this.updateOtpSMSInfoProvider(sender, smsid, content, mtres.getMessage(), mtres.getOtp(), "0", receiverTime);
                            AlertServiceImpl service = new AlertServiceImpl();
                            this.SendSMSEsms(mobile, mtres.getMessage());
                            response.getWriter().println(this.convertDocumentToString(doc));
                        }
                    }
                    catch (Exception e2) {
                        logger.debug((Object)e2);
                    }
                }
                catch (Exception sign) {
                    // empty catch block
                }
            }
            response.getWriter().println("");
        }

        public boolean SendSMSEsms(String phone, String messge) {
            try {
                String messageText = URLEncoder.encode(messge, "UTF-8");
                URL url = new URL("http://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_get?Phone=" + phone + "&Content=" + messageText + "&ApiKey=71AF87BEB55955929C764A6B86FDB0&SecretKey=A47665D6FCCF81EDC53B7538701290&IsUnicode=0&SmsType=2&brandname=Verify");
                HttpURLConnection request = (HttpURLConnection)url.openConnection();
                request.setConnectTimeout(90000);
                request.setUseCaches(false);
                request.setDoOutput(true);
                request.setDoInput(true);
                HttpURLConnection.setFollowRedirects(true);
                request.setInstanceFollowRedirects(true);
                request.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                request.setRequestMethod("GET");
                BufferedReader rd = new BufferedReader(new InputStreamReader(request.getInputStream(), Charset.forName("UTF-8")));
                String result = "";
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result = result.concat(line);
                }
                JSONObject json = (JSONObject)new JSONParser().parse(result);
                return json != null && json.get("CodeResult") != null && String.valueOf(json.get("CodeResult")).equals("100");
            }
            catch (Exception ex) {
                return false;
            }
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
            Document updateFields = new Document();
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
            col.insertOne((Object)updateFields);
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

    public static class JeetyServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            Map requestMap = request.getParameterMap();
            String remoteAddr = request.getRemoteAddr();
            logger.info((Object)("IP:" + remoteAddr));
            if (requestMap.containsKey("c")) {
                String command = request.getParameter("c");
                if (command == null || command.equalsIgnoreCase("")) {
                    blackListIpLogger.debug((Object)remoteAddr);
                    return;
                }
                Param param = new Param();
                param.set((Object)request);
                logger.debug((Object)("command: " + command));
                try {
                    response.getWriter().println((String)controller.processCommand(Integer.valueOf(Integer.parseInt(command)), param));
                    service.log(command);
                }
                catch (NoCommandRegistered e2) {
                    logger.debug((Object)"COMMAND NOT FOUND");
                    response.getWriter().println("COMMAND NOT FOUND");
                    service.log("CMD_404");
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                    System.out.println(e1);
                    response.getWriter().println("EXCEPTION: " + e1.getMessage());
                }
            } else {
                blackListIpLogger.debug((Object)remoteAddr);
                response.getWriter().println("NO COMMAND PARAMETERS");
                service.log("NO_CMD");
            }
        }
    }

    public static class TopupServlet
    extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            this.onExecute(request, response);
        }

        private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(200);
            String res = "";
            try {
                TopupResponse topupResponse = new TopupResponse();
                String body = TopupServlet.getBody(request);
                String timeResponse = DateTimeUtils.getCurrentTime((String)"yyyyMMddHHmmss");
                if (body.contains("command")) {
                    JSONObject jsonObj = new JSONObject(body);
                    String command = jsonObj.getString("command");
                    if (command != null && (command.equals("11") || command.equals("12") || command.equals("13"))) {
                        VTCPayServiceImpl service = new VTCPayServiceImpl();
                        if (command.equals("11")) {
                            if (body.contains("partnerId") && body.contains("requestId") && body.contains("nickName") && body.contains("price") && body.contains("timeRequest") && body.contains("sign")) {
                                TopupRequest topupInput = new TopupRequest(command, jsonObj.getString("partnerId"), jsonObj.getString("requestId"), jsonObj.getString("nickName"), jsonObj.getString("price"), jsonObj.getString("timeRequest"), jsonObj.getString("sign"));
                                res = service.topup(topupInput);
                            } else {
                                topupResponse.setStatus(-1);
                                topupResponse.setResponseCode("-1");
                                topupResponse.setDescription("Param truy\u00e1\u00bb\ufffdn v\u00c3\u00a0o kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang");
                                topupResponse.setTimeResponse(timeResponse);
                                res = topupResponse.toJson();
                            }
                        } else if (command.equals("12")) {
                            if (body.contains("partnerId") && body.contains("requestId") && body.contains("sign")) {
                                CheckTransRequest checkTransInput = new CheckTransRequest(command, jsonObj.getString("partnerId"), jsonObj.getString("requestId"), jsonObj.getString("sign"));
                                res = service.checkTrans(checkTransInput);
                            } else {
                                topupResponse.setStatus(-1);
                                topupResponse.setResponseCode("-1");
                                topupResponse.setDescription("Param truy\u00e1\u00bb\ufffdn v\u00c3\u00a0o kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang");
                                topupResponse.setTimeResponse(timeResponse);
                                res = topupResponse.toJson();
                            }
                        } else if (command.equals("13")) {
                            if (body.contains("partnerId") && body.contains("nickName") && body.contains("sign")) {
                                CheckAccountRequest checkAccountInput = new CheckAccountRequest(command, jsonObj.getString("partnerId"), jsonObj.getString("nickName"), jsonObj.getString("sign"));
                                res = service.checkAccount(checkAccountInput);
                            } else {
                                topupResponse.setStatus(-1);
                                topupResponse.setResponseCode("-1");
                                topupResponse.setDescription("Param truy\u00e1\u00bb\ufffdn v\u00c3\u00a0o kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang");
                                topupResponse.setTimeResponse(timeResponse);
                                res = topupResponse.toJson();
                            }
                        }
                    } else {
                        topupResponse.setStatus(-1);
                        topupResponse.setResponseCode("-7");
                        topupResponse.setDescription("Command kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang");
                        topupResponse.setTimeResponse(timeResponse);
                        res = topupResponse.toJson();
                    }
                } else {
                    topupResponse.setStatus(-1);
                    topupResponse.setResponseCode("-7");
                    topupResponse.setDescription("Command kh\u00c3\u00b4ng \u00c4\u2018\u00c3\u00bang");
                    topupResponse.setTimeResponse(timeResponse);
                    res = topupResponse.toJson();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            response.getWriter().println(res);
        }

        private static String getBody(HttpServletRequest request) throws IOException {
            String body = null;
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = null;
            try {
                ServletInputStream inputStream = request.getInputStream();
                if (inputStream != null) {
                    bufferedReader = new BufferedReader(new InputStreamReader((InputStream)inputStream));
                    char[] charBuffer = new char[128];
                    int bytesRead = -1;
                    while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                        stringBuilder.append(charBuffer, 0, bytesRead);
                    }
                } else {
                    stringBuilder.append("");
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
                throw ex;
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            body = stringBuilder.toString();
            return body;
        }
    }

}

