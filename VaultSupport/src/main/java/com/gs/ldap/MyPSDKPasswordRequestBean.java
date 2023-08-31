package com.gs.ldap;

import javapasswordsdk.PSDKPasswordRequest;
import javapasswordsdk.exceptions.PSDKException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PostConstruct;

public class MyPSDKPasswordRequestBean extends PSDKPasswordRequest implements InitializingBean, DisposableBean{

    private String appId;
    private String safe;
    private String folder;
    private String object;
    private String reason;
    private String password;
    private String userName;


    public String getAppId() { return appId; }
    public String getSafe() { return safe; }
    public String getFolder() { return folder; }
    public String getObject() { return object; }
    public String getReason() { return reason; }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() throws PSDKException
    {
        return javapasswordsdk.PasswordSDK.getPassword(this).getUserName();
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getPassword() throws PSDKException {
        return javapasswordsdk.PasswordSDK.getPassword(this).getContent();
    }

    public MyPSDKPasswordRequestBean() throws PSDKException { super();
    }

    @PostConstruct
    public void construct() {
        System.out.println("########## MyPSDKPasswordRequestBean.construct() has been initialised 0.4 ##########");
    }

    public void setAppId(String appId) throws PSDKException { super.setAppID(appId);
    this.appId = appId;}

    @Override
    public void setSafe(String safe) throws PSDKException {
        super.setSafe(safe);
        this.safe = safe;
    }

    @Override
    public void setFolder(String folder) throws PSDKException {
        super.setFolder(folder);
        this.folder = folder;
    }

    @Override
    public void setObject(String object) throws PSDKException {
           super.setObject(object);
           this.object = object;
    }

    @Override
    public void setReason(String reason) throws PSDKException {
        super.setReason(reason);
        this.reason = reason;
    }

    @Override
    public void destroy() throws Exception {    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("########## MyPSDKPasswordRequestBean.afterPropertiesSet() has been initialised 0.4 ##########");
    }
}