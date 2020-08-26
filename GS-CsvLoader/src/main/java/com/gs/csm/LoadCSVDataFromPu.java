package com.gs.csm;

import com.gigaspaces.utils.CsvReader;
import org.openspaces.core.GigaSpace;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static com.gs.csm.GsFactory.*;

public class LoadCSVDataFromPu {

//    static InputStream inputStream = LoadCSVData.class.getClassLoader().getResourceAsStream(GsFactory.csvFile);

    public static void loadCsvData(GigaSpace gigaSpace,InputStream inputStream) throws IOException, ClassNotFoundException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateFormatTs = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        CsvReader reader = CsvReader.builder().addParser(String.class.getName(), String.class, s -> s.replace("'", "")).addParser(
                BigDecimal.class.getName(), BigDecimal.class, s -> new BigDecimal(s)).addParser(
                Timestamp.class.getName(), Timestamp.class, s -> {
                    try {
                        return new Timestamp(dateFormatTs.parse(s).getTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).addParser(
                Date.class.getName(), Date.class, s -> {
                    try {
                        return new Date(dateFormat.parse(s).getTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .build();
            reader.read(inputStream, Class.forName(csvPojo)).limit(limitRows).forEach(gigaSpace::write);
    }
}
