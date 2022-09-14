package com.gigaspaces.objectManagement.utils;

import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.objectManagement.model.RecordOutcome;
import com.gigaspaces.objectManagement.model.ReportData;
import com.gigaspaces.objectManagement.model.TableOutcome;

import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ReportWriter {
    StringBuilder additionalInfoBuilder;
    Formatter additionalInfoFormatter;

    public ReportWriter() {
        additionalInfoBuilder = new StringBuilder();
        additionalInfoFormatter = new Formatter(additionalInfoBuilder);
    }

    static Logger logger = Logger.getLogger(ReportWriter.class.getName());

    public String getAdditionalInfo() {
        return additionalInfoBuilder.toString();
    }

    public String produceReport(ReportData reportData, Map<String, SpaceTypeDescriptor> baseTypeDescriptorMap) {
        StringBuilder reportStringBuilder = new StringBuilder();
        Formatter reportFormatter = new Formatter(reportStringBuilder);

        for (SpaceTypeDescriptor typeDescriptor : baseTypeDescriptorMap.values()) {
            TableOutcome tsResults = reportData.getTsResults(typeDescriptor.getTypeName());
            TableOutcome csResults = reportData.getCsResults(typeDescriptor.getTypeName());

            reportFormatter.format(" Table :  %40s\n", typeDescriptor.getTypeName());
            reportFormatter.format("===========================================================================================\n");

            additionalInfoFormatter.format(" Table :  %40s\n", typeDescriptor.getTypeName());
            additionalInfoFormatter.format("===========================================================================================\n");

            reportFormatter.format(" %10s  %12s %12s %12s %12s %12s %12s\n", "Id", "Cache Write", "TS Write", "Cache Read", "TS Read", "Compared Equal?", "Additional Info?");
            List<RecordOutcome> csRecords = csResults.records();
            List<RecordOutcome> tsRecords = tsResults.records();
            int i = 0;
            for (RecordOutcome csRecord : csResults.records()) {
                RecordOutcome tsRecord = tsRecords.get(i++);
                reportFormatter.format(String.format(" %10s  %12b %12b %12b %12b %12b %12b\n", csRecord.getIdValue(), tsRecord.recordWritten, csRecord.recordWritten, tsRecord.recordRead, csRecord.recordRead, csRecord.comparedEqual, getAdditionalInfo(csRecord, tsRecord)));
            }

            additionalInfoFormatter.format("CS Remarks: %70s\n", getAdditionalInfo(csResults));
            additionalInfoFormatter.format("TS Remarks: %70s\n", getAdditionalInfo(tsResults));
        }
        return reportStringBuilder.toString();
    }


    private boolean getAdditionalInfo(RecordOutcome csRecord, RecordOutcome tsRecord) {
        boolean isAvailable = false;
        String str = getAdditionalInfo(csRecord);
        if (str.length() > 0) {
            isAvailable = true;
            this.additionalInfoFormatter.format("CS Remarks: %70s\n", str);
        }
        str = getAdditionalInfo(tsRecord);
        if (str.length() > 0) {
            isAvailable = true;
            this.additionalInfoFormatter.format("TS Remarks %70s\n", str);
        }
        return isAvailable;
    }

    private String getAdditionalInfo(RecordOutcome record) {
        StringBuffer info = new StringBuffer("");
        if (record.additionalInfo.size() > 0) {
            info.append("  Record id : " + record.getIdValue());
            for (Map.Entry<String, Object> entry : record.additionalInfo.entrySet()) {
                String entryValue = "";
                if (entry.getValue() instanceof Throwable) {
                    entryValue = getExceptionInfo((Throwable) entry.getValue());
                } else {
                    entryValue = entry.getValue().toString();
                }
                info.append(" {[ " + entry.getKey() + " : " + entryValue + " ]} ");
            }
        }
        return info.toString();
    }

    private String getAdditionalInfo(TableOutcome table) {
        StringBuffer info = new StringBuffer(" - ");
        for (Map.Entry<String, Object> entry : table.additionalInfo.entrySet()) {
            String entryValue = "";
            if (entry.getValue() instanceof Throwable) {
                entryValue = getExceptionInfo((Throwable) entry.getValue());
            } else {
                entryValue = entry.getValue().toString();
            }
            info.append(" {[ " + entry.getKey() + " : " + entryValue + " ]} ");
        }
        return info.toString();
    }

    private String getExceptionInfo(Throwable th) {
        StringBuffer info = new StringBuffer(th.getLocalizedMessage());
        info.append(" Stacktrace: ");
        StackTraceElement[] stackTrace = th.getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            info.append(" :-> " + ste.toString());
        }
        if (th.getCause() != null) {
            info.append(" Caused by: " + getExceptionInfo(th.getCause()));
        }
        return info.toString();
    }
}
