/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.payment.entities.PaywellNotifyRequest
 *  com.vinplay.payment.service.impl.RechargePayWellServiceImpl
 *  javax.servlet.ServletException
 *  javax.servlet.http.HttpServlet
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.commons.fileupload.FileUploadException
 *  org.apache.commons.fileupload.servlet.ServletFileUpload
 *  org.apache.commons.fileupload.util.Streams
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.paywell;

import com.vinplay.payment.entities.PaywellNotifyRequest;
import com.vinplay.payment.service.impl.RechargePayWellServiceImpl;
import com.vinplay.response.PayResponse;
import com.vinplay.utils.RequestUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.log4j.Logger;

public class PayWellNotifyServlet
extends HttpServlet {
    private static final Logger logger = Logger.getLogger(PayWellNotifyServlet.class);
    private static final long serialVersionUID = 1L;
    private static final List<String> IP_PAYWELL = Arrays.asList("127.0.0.1", "0:0:0:0:0:0:0:1");

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.onExecute(request, response);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     */
    private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        String remoteAddr = RequestUtil.getIpAddress(request);
        if (!PayWellNotifyServlet.IP_PAYWELL.contains(remoteAddr)) {
            PayWellNotifyServlet.logger.error("Remote IP Address IP_PAYWELL_NOTALLOW = " + remoteAddr);
            return;
        }
        PayWellNotifyServlet.logger.info("Remote IP Address PAYWELL " + remoteAddr);
        PaywellNotifyRequest requestObj = new PaywellNotifyRequest();
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (isMultipart) {
            ServletFileUpload upload = new ServletFileUpload();
            try {
                org.apache.commons.fileupload.FileItemIterator iter = upload.getItemIterator(request);
                while (iter.hasNext()) {
                    org.apache.commons.fileupload.FileItemStream item = iter.next();
                    String name = item.getFieldName().toLowerCase();
                    InputStream stream = null;
                    try {
                        stream = item.openStream();
                        if (!item.isFormField()) continue;
                        String value = Streams.asString(stream);
                        switch (name) {
                            case "merchantcode": {
                                requestObj.setMerchantCode(value);
                                break;
                            }
                            case "cartid": {
                                requestObj.setCartId(value);
                                break;
                            }
                            case "referenceid": {
                                requestObj.setReferenceId(value);
                                break;
                            }
                            case "amount": {
                                requestObj.setAmount(Double.valueOf(Double.parseDouble(value)));
                                break;
                            }
                            case "amountfee": {
                                requestObj.setAmountFee(Double.valueOf(Double.parseDouble(value)));
                                break;
                            }
                            case "currencycode": {
                                requestObj.setCurrencyCode(value);
                                break;
                            }
                            case "status": {
                                requestObj.setStatus(Integer.parseInt(value));
                                break;
                            }
                            case "requesttime": {
                                requestObj.setRequestTime(Long.parseLong(value));
                                break;
                            }
                            case "signature": {
                                requestObj.setSignature(value);
                                break;
                            }
                            default:
                                break;
                        }
                    }
                    finally {
                        if (stream != null) {
                            stream.close();
                        }
                    }
                }
            }
            catch (FileUploadException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        String res = "";
        PayWellNotifyServlet.logger.info("Request notify paywell " + requestObj.toJson());
        try {
            RechargePayWellServiceImpl service = new RechargePayWellServiceImpl();
            com.vinplay.dichvuthe.response.RechargePaywellResponse rechargeResponse = service.notification(requestObj);
            PayWellNotifyServlet.logger.info("Response notify paywell " + rechargeResponse.toJson());
            res = rechargeResponse.getCode() == 0 ? new PayResponse(1, "success").toJson() : new PayResponse(0, rechargeResponse.getData()).toJson();
        }
        catch (Exception e) {
            PayWellNotifyServlet.logger.error(e);
            res = new PayResponse(0, e.getMessage()).toJson();
        }
        response.getWriter().println(res);
    }
}

