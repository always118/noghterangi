package com.noghterangi.cart;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Session state contains identifiers and quantities only. */
public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<Long, Integer> items = new LinkedHashMap<>();
    public synchronized Map<Long, Integer> items() { return new LinkedHashMap<>(items); }
    synchronized void put(long id, int quantity) { items.put(id, quantity); }
    public synchronized void remove(long id) { items.remove(id); }
    public synchronized void clear() { items.clear(); }
    public synchronized long getCount() { return items.values().stream().mapToLong(Integer::longValue).sum(); }
}
