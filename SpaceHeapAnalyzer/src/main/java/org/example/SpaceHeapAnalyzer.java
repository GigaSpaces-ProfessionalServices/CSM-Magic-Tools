package org.example;

import com.gigaspaces.heap.HeapUtils;
import com.gigaspaces.heap.formatters.HeapReportFormatter;
import com.gigaspaces.heap.formatters.JSonHeapReportFormatter;
import com.gigaspaces.heap.formatters.TextHeapReportFormatter;
import com.gigaspaces.heap.space.*;
import com.gigaspaces.internal.jvm.HeapUsageEstimator;
import org.netbeans.lib.profiler.heap.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

public class SpaceHeapAnalyzer {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Dump path was not specified");
            System.exit(1);
        }

        Path dumpPath = Paths.get(args[0]);
        String reportPath = args.length >= 2 ? args[1] : null;
        boolean verbose = args.length >= 3 ? Boolean.parseBoolean(args[2]) : false;

        if (!Files.exists(dumpPath)) {
            System.out.println("File not found: " + dumpPath);
            System.exit(1);
        }

        System.out.println("Analyzing " + dumpPath + " - this may take a while...");
        try {
            GigaSpacesHeapReport report = SpaceHeapAnalyzer.analyze(dumpPath, verbose);
            if (reportPath == null)
                System.out.print(report.toString());
            else {
                System.out.println("Analysis completed - saving report at " + reportPath);
                HeapReportFormatter formatter = reportPath.endsWith(".json")
                        ? new JSonHeapReportFormatter()
                        : new TextHeapReportFormatter();
                try (Writer writer = new FileWriter(reportPath)) {
                    writer.write(formatter.format(report));
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static GigaSpacesHeapReport analyze(Path path, boolean verbose) throws IOException {
        GigaSpacesHeapReport.Builder builder = GigaSpacesHeapReport.builder()
                .dumpPath(path);
        Heap heap = HeapFactory.createHeap(path.toFile());
        builder.dumpedOn(Instant.ofEpochMilli(heap.getSummary().getTime()));
        builder.summary(GigaSpacesHeapSummary.analyze(heap));
        JavaClass spaceEngineClass = heap.getJavaClassByName(GigaSpacesClasses.SpaceEngine);
        if (spaceEngineClass != null) {
            HeapUtils.forEachInstance(spaceEngineClass,
                    instance -> builder.add(analyzeSpace(instance, verbose)));
        }

        return builder.build();
    }

    private static SpaceReport analyzeSpace(Instance spaceInstance, boolean verbose) {
        SpaceTypeAnalyzer.Builder builder = SpaceTypeAnalyzer.builder();
        builder.verbose(verbose);
        String spaceName = HeapUtils.getStringValue(HeapUtils.getNestedValue(spaceInstance, "_spaceName"));
        String instanceId = HeapUtils.getStringValue(HeapUtils.getNestedValue(spaceInstance, "_spaceImpl", "_instanceId"));
        HeapUsageEstimator heapUsageEstimator = initHeapUsageEstimator(HeapUtils.getNestedValue(spaceInstance, "_spaceImpl", "_heapUsageEstimator"));
        builder.heapUsageEstimator(heapUsageEstimator);
        SpaceReport report = new SpaceReport(spaceName, instanceId, heapUsageEstimator != null ? heapUsageEstimator.getDesc() : "None");
        List<Instance> types = ((ObjectArrayInstance) HeapUtils.getNestedValue(spaceInstance, "_cacheManager", "_typeDataMap", "elementData")).getValues();
        for (Instance typeInstance : types) {
            if (typeInstance != null) {
                //System.out.printf("Analyzing instance #%s (class %s) %n", typeInstance.getInstanceId(), typeInstance.getJavaClass().getName());
                String typeName = HeapUtils.getStringValue((Instance) typeInstance.getValueOfField("_className"));
                SpaceTypeAnalyzer typeDataAnalyzer = builder.typeName(typeName).build();
                HeapUtils.walk(typeInstance, typeDataAnalyzer);
                report.add(typeDataAnalyzer.getReport());
            }
        }

        return report;
    }

    private static HeapUsageEstimator initHeapUsageEstimator(Instance instance) {
        if (instance == null)
            return null;
        return new HeapUsageEstimator.Builder()
                .desc(HeapUtils.getStringValue(HeapUtils.getNestedValue(instance, "desc")))
                .arrayHeaderSize((Integer) instance.getValueOfField("arrayHeaderSize"))
                .objectHeaderSize((Integer) instance.getValueOfField("objectHeaderSize"))
                .objectPadding((Integer) instance.getValueOfField("objectPadding"))
                .referenceSize((Integer) instance.getValueOfField("referenceSize"))
                .superclassFieldPadding((Integer) instance.getValueOfField("superclassFieldPadding"))
                .build();
    }
}
