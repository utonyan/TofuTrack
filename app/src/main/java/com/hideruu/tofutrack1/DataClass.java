package com.hideruu.tofutrack1;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.Date;

@IgnoreExtraProperties
public class DataClass {
    private int productId;  // Changed productId to int
    private String prodName;
    private String prodDesc;
    private String prodGroup;
    private int prodQty;
    private double prodCost;
    private double prodTotalPrice;
    private String dataImage;
    private Date dateAdded;  // Field to track the date when the product was added
    private String prodUnitType;  // New unit type field

    // Default constructor required for calls to DataSnapshot.getValue(DataClass.class)
    public DataClass() {
    }

    // Parameterized constructor including prodUnitType
    public DataClass(int productId, String prodName, String prodDesc, String prodGroup, int prodQty, double prodCost, double prodTotalPrice, String dataImage, Date dateAdded, String prodUnitType) {
        this.productId = productId;
        this.prodName = prodName;
        this.prodDesc = prodDesc;
        this.prodGroup = prodGroup;
        this.prodQty = prodQty;
        this.prodCost = prodCost;
        this.prodTotalPrice = prodTotalPrice;
        this.dataImage = dataImage;
        this.dateAdded = dateAdded;
        this.prodUnitType = prodUnitType;
    }

    // Getters
    public int getProductId() {
        return productId;
    }

    public String getProdName() {
        return prodName;
    }

    public String getProdDesc() {
        return prodDesc;
    }

    public String getProdGroup() {
        return prodGroup;
    }

    public int getProdQty() {
        return prodQty;
    }

    public double getProdCost() {
        return prodCost;
    }

    public double getProdTotalPrice() {
        return prodTotalPrice;
    }

    public String getDataImage() {
        return dataImage;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    public String getProdUnitType() {
        return prodUnitType;
    }

    // Setters
    public void setProductId(int productId) {
        this.productId = productId;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    public void setProdDesc(String prodDesc) {
        this.prodDesc = prodDesc;
    }

    public void setProdGroup(String prodGroup) {
        this.prodGroup = prodGroup;
    }

    public void setProdQty(int prodQty) {
        this.prodQty = prodQty;
    }

    public void setProdCost(double prodCost) {
        this.prodCost = prodCost;
    }

    public void setProdTotalPrice(double prodTotalPrice) {
        this.prodTotalPrice = prodTotalPrice;
    }

    public void setDataImage(String dataImage) {
        this.dataImage = dataImage;
    }

    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }

    public void setProdUnitType(String prodUnitType) {
        this.prodUnitType = prodUnitType;
    }
}
