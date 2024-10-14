package com.hideruu.tofutrack1;

import java.util.Date;
import java.util.List;

public class Receipt {
    private List<ReceiptItem> items;
    private double totalCost;
    private Date dateTime;

    // No-argument constructor required by Firestore
    public Receipt() {}

    public Receipt(List<ReceiptItem> items, double totalCost, Date dateTime) {
        this.items = items;
        this.totalCost = totalCost;
        this.dateTime = dateTime;
    }

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
}
