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
package com.vinplay.epay;

import java.io.Serializable;
import javax.xml.namespace.QName;
import org.apache.axis.description.ElementDesc;
import org.apache.axis.description.FieldDesc;
import org.apache.axis.description.TypeDesc;
import org.apache.axis.encoding.Deserializer;
import org.apache.axis.encoding.Serializer;
import org.apache.axis.encoding.ser.BeanDeserializer;
import org.apache.axis.encoding.ser.BeanSerializer;

public class CheckOrdesrCDVResult
implements Serializable {
    private long amountTopupSuccess;
    private int errorCode;
    private String message;
    private Object __equalsCalc = null;
    private boolean __hashCodeCalc = false;
    private static TypeDesc typeDesc = new TypeDesc(CheckOrdesrCDVResult.class, true);

    public CheckOrdesrCDVResult() {
    }

    public CheckOrdesrCDVResult(long amountTopupSuccess, int errorCode, String message) {
        this.amountTopupSuccess = amountTopupSuccess;
        this.errorCode = errorCode;
        this.message = message;
    }

    public long getAmountTopupSuccess() {
        return this.amountTopupSuccess;
    }

    public void setAmountTopupSuccess(long amountTopupSuccess) {
        this.amountTopupSuccess = amountTopupSuccess;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof CheckOrdesrCDVResult)) {
            return false;
        }
        CheckOrdesrCDVResult other = (CheckOrdesrCDVResult)obj;
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
        boolean _equals = this.amountTopupSuccess == other.getAmountTopupSuccess() && this.errorCode == other.getErrorCode() && (this.message == null && other.getMessage() == null || this.message != null && this.message.equals(other.getMessage()));
        this.__equalsCalc = null;
        return _equals;
    }

    public synchronized int hashCode() {
        if (this.__hashCodeCalc) {
            return 0;
        }
        this.__hashCodeCalc = true;
        int _hashCode = 1;
        _hashCode += new Long(this.getAmountTopupSuccess()).hashCode();
        _hashCode += this.getErrorCode();
        if (this.getMessage() != null) {
            _hashCode += this.getMessage().hashCode();
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
        typeDesc.setXmlType(new QName("http://services.epay", "CheckOrdesrCDVResult"));
        ElementDesc elemField = new ElementDesc();
        elemField.setFieldName("amountTopupSuccess");
        elemField.setXmlName(new QName("", "amountTopupSuccess"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("errorCode");
        elemField.setXmlName(new QName("", "errorCode"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("message");
        elemField.setXmlName(new QName("", "message"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc((FieldDesc)elemField);
    }
}

