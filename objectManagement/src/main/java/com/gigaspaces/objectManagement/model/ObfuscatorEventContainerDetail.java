package com.gigaspaces.objectManagement.model;

public class ObfuscatorEventContainerDetail {
    String typeName;
    String srcPropName;
    String destPropName;
    String obfuscatePropName;
    String obfuscationType;
    String spaceId;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getSrcPropName() {
        return srcPropName;
    }

    public void setSrcPropName(String srcPropName) {
        this.srcPropName = srcPropName;
    }

    public String getDestPropName() {
        return destPropName;
    }

    public void setDestPropName(String destPropName) {
        this.destPropName = destPropName;
    }

    public String getObfuscatePropName() {
        return obfuscatePropName;
    }

    public void setObfuscatePropName(String obfuscatePropName) {
        this.obfuscatePropName = obfuscatePropName;
    }

    public String getObfuscationType() {
        return obfuscationType;
    }

    public void setObfuscationType(String obfuscationType) {
        this.obfuscationType = obfuscationType;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    @Override
    public String toString() {
        return "ObfuscatorEventContainerDetail{" +
                "typeName='" + typeName + '\'' +
                ", srcPropName='" + srcPropName + '\'' +
                ", destPropName='" + destPropName + '\'' +
                ", obfuscatePropName='" + obfuscatePropName + '\'' +
                ", obfuscationType='" + obfuscationType + '\'' +
                ", spaceId='" + spaceId + '\'' +
                '}';
    }
}
