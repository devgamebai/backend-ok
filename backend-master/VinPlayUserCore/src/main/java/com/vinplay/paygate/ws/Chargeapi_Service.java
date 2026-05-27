/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import com.vinplay.paygate.ws.Chargeapi;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name="chargeapi", targetNamespace="http://ws.payapi/", wsdlLocation="http://45.77.253.113:8182/paygate/chargeapi?WSDL")
public class Chargeapi_Service
extends Service {
    private static final URL CHARGEAPI_WSDL_LOCATION;
    private static final WebServiceException CHARGEAPI_EXCEPTION;
    private static final QName CHARGEAPI_QNAME;

    public Chargeapi_Service() {
        super(Chargeapi_Service.__getWsdlLocation(), CHARGEAPI_QNAME);
    }

    public Chargeapi_Service(WebServiceFeature ... features) {
        super(Chargeapi_Service.__getWsdlLocation(), CHARGEAPI_QNAME, features);
    }

    public Chargeapi_Service(URL wsdlLocation) {
        super(wsdlLocation, CHARGEAPI_QNAME);
    }

    public Chargeapi_Service(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, CHARGEAPI_QNAME, features);
    }

    public Chargeapi_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Chargeapi_Service(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }

    @WebEndpoint(name="chargeapiPort")
    public Chargeapi getChargeapiPort() {
        return super.getPort(new QName("http://ws.payapi/", "chargeapiPort"), Chargeapi.class);
    }

    @WebEndpoint(name="chargeapiPort")
    public Chargeapi getChargeapiPort(WebServiceFeature ... features) {
        return super.getPort(new QName("http://ws.payapi/", "chargeapiPort"), Chargeapi.class, features);
    }

    private static URL __getWsdlLocation() {
        if (CHARGEAPI_EXCEPTION != null) {
            throw CHARGEAPI_EXCEPTION;
        }
        return CHARGEAPI_WSDL_LOCATION;
    }

    static {
        CHARGEAPI_QNAME = new QName("http://ws.payapi/", "chargeapi");
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://45.77.253.113:8182/paygate/chargeapi?WSDL");
        }
        catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        CHARGEAPI_WSDL_LOCATION = url;
        CHARGEAPI_EXCEPTION = e;
    }
}

