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
package com.vinplay.brandname.enties;

import java.io.Serializable;
import javax.xml.namespace.QName;
import org.apache.axis.description.ElementDesc;
import org.apache.axis.description.FieldDesc;
import org.apache.axis.description.TypeDesc;
import org.apache.axis.encoding.Deserializer;
import org.apache.axis.encoding.Serializer;
import org.apache.axis.encoding.ser.BeanDeserializer;
import org.apache.axis.encoding.ser.BeanSerializer;

public class DataSms
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String message;
    private String phone;
    private Object __equalsCalc = null;
    private boolean __hashCodeCalc = false;
    private static TypeDesc typeDesc = new TypeDesc(DataSms.class, true);

    public DataSms() {
    }

    public DataSms(String message, String phone) {
        this.message = message;
        this.phone = phone;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof DataSms)) {
            return false;
        }
        DataSms other = (DataSms)obj;
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
        boolean _equals = (this.message == null && other.getMessage() == null || this.message != null && this.message.equals(other.getMessage())) && (this.phone == null && other.getPhone() == null || this.phone != null && this.phone.equals(other.getPhone()));
        this.__equalsCalc = null;
        return _equals;
    }

    public synchronized int hashCode() {
        if (this.__hashCodeCalc) {
            return 0;
        }
        this.__hashCodeCalc = true;
        int _hashCode = 1;
        if (this.getMessage() != null) {
            _hashCode += this.getMessage().hashCode();
        }
        if (this.getPhone() != null) {
            _hashCode += this.getPhone().hashCode();
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
        typeDesc.setXmlType(new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "DataSms"));
        ElementDesc elemField = new ElementDesc();
        elemField.setFieldName("message");
        elemField.setXmlName(new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "Message"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("phone");
        elemField.setXmlName(new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "Phone"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc((FieldDesc)elemField);
    }
}

