package com.hideruu.tofutrack1;

import java.util.Date;
import java.util.List;

public class Receipt {
    private List<ReceiptItem> items;
    private double totalCost;
    private Date dateTime;

    public Receipt(List<ReceiptItem> items, double totalCost, Date dateTime) {
        this.items = items;
        this.totalCost = totalCost;
        this.dateTime = dateTime;
    }

    public List<ReceiptItem> getItems() {
        return items;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public Date getDateTime() {
        return dateTime;
    }
}
