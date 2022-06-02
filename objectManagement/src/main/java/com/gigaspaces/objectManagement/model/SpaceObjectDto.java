package com.gigaspaces.objectManagement.model;

public class SpaceObjectDto {
    private String spaceName;
    private String objName;
    private String objtype;
    private String index;
    private String spaceId;
    private String spaceRouting;

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public String getObjName() {
        return objName;
    }

    public void setObjName(String objName) {
        this.objName = objName;
    }

    public String getObjtype() {
        return objtype;
    }

    public void setObjtype(String objtype) {
        this.objtype = objtype;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getSpaceRouting() {
        return spaceRouting;
    }

    public void setSpaceRouting(String spaceRouting) {
        this.spaceRouting = spaceRouting;
    }

    @Override
    public String toString() {
        return "SpaceObjectDto{" +
                "spaceName='" + spaceName + '\'' +
                ", objName='" + objName + '\'' +
                ", objtype='" + objtype + '\'' +
                ", index='" + index + '\'' +
                ", spaceId='" + spaceId + '\'' +
                ", spaceRouting='" + spaceRouting + '\'' +
                '}';
    }
}
