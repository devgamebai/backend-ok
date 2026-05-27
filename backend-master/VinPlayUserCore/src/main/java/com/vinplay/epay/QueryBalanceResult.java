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

public class QueryBalanceResult
implements Serializable {
    private long balance_avaiable;
    private long balance_bonus;
    private long balance_debit;
    private long balance_money;
    private int errorCode;
    private String message;
    private String partnerName;
    private Object __equalsCalc = null;
    private boolean __hashCodeCalc = false;
    private static TypeDesc typeDesc = new TypeDesc(QueryBalanceResult.class, true);

    public QueryBalanceResult() {
    }

    public QueryBalanceResult(long balance_avaiable, long balance_bonus, long balance_debit, long balance_money, int errorCode, String message, String partnerName) {
        this.balance_avaiable = balance_avaiable;
        this.balance_bonus = balance_bonus;
        this.balance_debit = balance_debit;
        this.balance_money = balance_money;
        this.errorCode = errorCode;
        this.message = message;
        this.partnerName = partnerName;
    }

    public long getBalance_avaiable() {
        return this.balance_avaiable;
    }

    public void setBalance_avaiable(long balance_avaiable) {
        this.balance_avaiable = balance_avaiable;
    }

    public long getBalance_bonus() {
        return this.balance_bonus;
    }

    public void setBalance_bonus(long balance_bonus) {
        this.balance_bonus = balance_bonus;
    }

    public long getBalance_debit() {
        return this.balance_debit;
    }

    public void setBalance_debit(long balance_debit) {
        this.balance_debit = balance_debit;
    }

    public long getBalance_money() {
        return this.balance_money;
    }

    public void setBalance_money(long balance_money) {
        this.balance_money = balance_money;
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

    public String getPartnerName() {
        return this.partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof QueryBalanceResult)) {
            return false;
        }
        QueryBalanceResult other = (QueryBalanceResult)obj;
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
        boolean _equals = this.balance_avaiable == other.getBalance_avaiable() && this.balance_bonus == other.getBalance_bonus() && this.balance_debit == other.getBalance_debit() && this.balance_money == other.getBalance_money() && this.errorCode == other.getErrorCode() && (this.message == null && other.getMessage() == null || this.message != null && this.message.equals(other.getMessage())) && (this.partnerName == null && other.getPartnerName() == null || this.partnerName != null && this.partnerName.equals(other.getPartnerName()));
        this.__equalsCalc = null;
        return _equals;
    }

    public synchronized int hashCode() {
        if (this.__hashCodeCalc) {
            return 0;
        }
        this.__hashCodeCalc = true;
        int _hashCode = 1;
        _hashCode += new Long(this.getBalance_avaiable()).hashCode();
        _hashCode += new Long(this.getBalance_bonus()).hashCode();
        _hashCode += new Long(this.getBalance_debit()).hashCode();
        _hashCode += new Long(this.getBalance_money()).hashCode();
        _hashCode += this.getErrorCode();
        if (this.getMessage() != null) {
            _hashCode += this.getMessage().hashCode();
        }
        if (this.getPartnerName() != null) {
            _hashCode += this.getPartnerName().hashCode();
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
        typeDesc.setXmlType(new QName("http://services.epay", "QueryBalanceResult"));
        ElementDesc elemField = new ElementDesc();
        elemField.setFieldName("balance_avaiable");
        elemField.setXmlName(new QName("", "balance_avaiable"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("balance_bonus");
        elemField.setXmlName(new QName("", "balance_bonus"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("balance_debit");
        elemField.setXmlName(new QName("", "balance_debit"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc((FieldDesc)elemField);
        elemField = new ElementDesc();
        elemField.setFieldName("balance_money");
        elemField.setXmlName(new QName("", "balance_money"));
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
        elemField = new ElementDesc();
        elemField.setFieldName("partnerName");
        elemField.setXmlName(new QName("", "partnerName"));
        elemField.setXmlType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc((FieldDesc)elemField);
    }
}

