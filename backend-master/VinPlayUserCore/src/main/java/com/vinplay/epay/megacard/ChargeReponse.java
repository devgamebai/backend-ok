/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.axis.description.ElementDesc
 *  org.apache.axis.description.FieldDesc
 *  org.apache.axis.description.TypeDesc
 *  org.apache.axis.encoding.Deserializer
 *  org.apache.axis.encoding.Serializer
 *  org.apache.axis.encoding.ser.BeanDeserializer
 *  org.apache.axis.encoding.ser.BeanSerializer
 */
package com.vinplay.epay.megacard;

import java.io.Serializable;
import javax.xml.namespace.QName;
import org.apache.axis.description.ElementDesc;
import org.apache.axis.description.FieldDesc;
import org.apache.axis.description.TypeDesc;
import org.apache.axis.encoding.Deserializer;
import org.apache.axis.encoding.Serializer;
import org.apache.axis.encoding.ser.BeanDeserializer;
import org.apache.axis.encoding.ser.BeanSerializer;

public class ChargeReponse
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String amount;
    private String message;
    private String responseamount;
    private String status;
    private String transid;
    private Object __equalsCalc = null;
    private boolean __hashCodeCalc = false;
    private static TypeDesc typeDesc = new TypeDesc(ChargeReponse.class, true);

    public ChargeReponse() {
    }

    public ChargeReponse(String amount, String message, String responseamount, String status, String transid) {
        this.amount = amount;
        this.message = message;
        this.responseamount = responseamount;
        this.status = status;
        this.transid = transid;
    }

    public String getAmount() {
        return this.amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponseamount() {
        return this.responseamount;
    }

    public void setResponseamount(String responseamount) {
        this.responseamount = responseamount;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransid() {
        return this.transid;
    }

    public void setTransid(String transid) {
        this.transid = transid;
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof ChargeReponse)) {
            return false;
        }
        ChargeReponse other = (ChargeReponse)obj;
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (this.__equalsCalc != null) {
            return this.__equalsCalc == obj;
        }
        this.__equalsCalc = obj;
        boolean _equals = (this.amount == null && other.getAmount() == null || this.amount != null && this.amount.equals(other.getAmount())) && (this.message == null && other.getMessage() == null || this.message != null && this.message.equals(other.getMessage())) && (this.responseamount == null && other.getResponseamount() == null || this.responseamount != null && this.responseamount.equals(other.getResponseamount())) && (this.status == null && other.getStatus() == null || this.status != null && this.status.equals(other.getStatus())) && (this.transid == null && other.getTransid() == null || this.transid != null && this.transid.equals(other.getTransid()));
        this.__equalsCalc = null;
        return _equals;
    }

    public synchronized int hashCode() {
        if (this.__hashCodeCalc) {
            return 0;
        }
        this.__hashCodeCalc = true;
        int _hashCode = 1;
        if (this.getAmount() != null) {
            _hashCode += this.getAmount().hashCode();
        }
        if (this.getMessage() != null) {
            _hashCode += this.getMessage().hashCode();
        }
        if (this.getResponseamount() != null) {
            _hashCode += this.getResponseamount().hashCode();
        }
        if (this.getStatus() != null) {
            _hashCode += this.getStatus().hashCode();
        }
        if (this.getTransid() != null) {
            _hashCode += this.getTransid().hashCode();
        }
        this.__hashCodeCalc = false;
        return _hashCode;
    }

    public static TypeDesc getTypeDesc() {
        return typeDesc;
    }

    public static Serializer getSerializer(String mechType, Class _javaType, QName _xmlType) {
        return new BeanSerializer(_javaType, _xmlType, typeDesc);
    }

    public static Deserializer getDeserializer(String mechType, Class _javaType, QName _xmlType) {
        return new BeanDeserializer(_javaType, _xmlType, typeDesc);
    }

    static {
        typeDesc.setXmlType(new QName("http://entities", "ChargeReponse"));
        ElementDesc elemField = new ElementDesc();
        elemField.setFieldName("amount");
        elemField.setXmlName(new QName("http://entities", "amount"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("message");
        elemField.setXmlName(new QName("http://entities", "message"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("responseamount");
        elemField.setXmlName(new QName("http://entities", "responseamount"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("status");
        elemField.setXmlName(new QName("http://entities", "status"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("transid");
        elemField.setXmlName(new QName("http://entities", "transid"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc((FieldDesc)elemField);
    }
}

