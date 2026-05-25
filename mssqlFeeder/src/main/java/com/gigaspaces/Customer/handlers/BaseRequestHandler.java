package com.gigaspaces.Customer.handlers;

import org.openspaces.core.GigaSpace;

public class BaseRequestHandler {
    protected GigaSpace space;

    public BaseRequestHandler(GigaSpace space) {
        this.space = space;
    }

}
