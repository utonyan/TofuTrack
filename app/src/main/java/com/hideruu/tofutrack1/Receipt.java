package com.hideruu.tofutrack1;

import java.util.Date;
import java.util.List;

public class Receipt {
    private List<ReceiptItem> items;
    private double totalCost;
    private Date dateTime;
    private String documentName; // Field for the document name
    private double payment; // Field for payment amount
    private double change;   // Field for change amount

    // No-argument constructor required by Firestore
    public Receipt() {}

    public Receipt(List<ReceiptItem> items, double totalCost, Date dateTime, String documentName, double payment, double change) {
        this.items = items;
        this.totalCost = totalCost;
        this.dateTime = dateTime;
        this.documentName = documentName; // Initialize document name
        this.payment = payment; // Initialize payment
        this.change = change;   // Initialize change
    }

    // Getters and setters for all fields
    public List<ReceiptItem> getItems() {
        return items;
    }

    public void setItems(List<ReceiptItem> items) {
        this.items = items;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public String getDocumentName() { // Getter for document name
        return documentName;
    }

    public void setDocumentName(String documentName) { // Setter for document name
        this.documentName = documentName;
    }

    // Getters and setters for payment and change
    public double getPayment() {
        return payment;
    }

    public void setPayment(double payment) {
        this.payment = payment;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }
}
