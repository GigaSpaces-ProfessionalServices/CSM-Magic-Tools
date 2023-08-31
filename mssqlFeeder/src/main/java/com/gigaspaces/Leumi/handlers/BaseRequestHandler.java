package com.gigaspaces.Leumi.handlers;

import org.openspaces.core.GigaSpace;

public class BaseRequestHandler {
    protected GigaSpace space;

    public BaseRequestHandler(GigaSpace space) {
        this.space = space;
    }

}
