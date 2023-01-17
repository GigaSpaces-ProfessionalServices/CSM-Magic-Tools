package com.gigaspaces.tierdirectcall.dto;

public class UpdateTypeDetail {
    String partialMemberName;
    String spaceName;
    String isBackup;

    public String getPartialMemberName() {
        return partialMemberName;
    }

    public void setPartialMemberName(String partialMemberName) {
        this.partialMemberName = partialMemberName;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public String getIsBackup() {
        return isBackup;
    }

    public void setIsBackup(String isBackup) {
        this.isBackup = isBackup;
    }
}
