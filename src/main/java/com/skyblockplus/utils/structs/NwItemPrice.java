package com.skyblockplus.utils.structs;

public class NwItemPrice {

    public final double price;
    public final String json;

    public NwItemPrice(double price, String json) {
        this.price = price;
        this.json = json;
    }

    @Override
    public String toString() {
        return "NwItemPrice{" + "price=" + price + ", json='" + json + '\'' + '}';
    }
}
