package com.gs.leumi.adabase;

import com.gs.leumi.adabase.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseClient {
    @Autowired
    protected Parser parser;

    public abstract void run() throws Exception;
}
