package com.gs.leumi.adabase.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.leumi.adabase.config.Configuration;
import com.gs.leumi.common.model.Envalope;
import com.gs.leumi.common.model.ILfpmTnuotHayom;
import com.gs.leumi.common.model.LfpmTnuotHayom;
import com.gs.leumi.common.model.LfpmTnuotHayomPmolTnuot;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Component
public class Parser {
    private static final Logger logger = LoggerFactory.getLogger(Parser.class);
    private final DocumentBuilderFactory dbf;
    private final LfpmTnuotHayomParser lfpmTnuotHayomParser;
    private final LfpmTnuotHayomTnuotParser lfpmTnuotHayomPmolTnuotParser;
    private final static String METHOD = "method";
    private final static String TABLE_NAME = "tableName";
//    private AtomicLong recordCounter = new AtomicLong(0);
//    private AtomicLong orphanRecordCounter = new AtomicLong(0);
//    private AtomicLong eventCounter = new AtomicLong(0);
    public static boolean STOP = false;

    private MeterRegistry meterRegistry;
    private Counter recordCounter;
    private Counter orphanRecordCounter;
    private Counter eventCounter;

    @Autowired
    private KafkaTemplate<String, Object > kafkaTemplate;

    @Autowired
    private Configuration config;

    public Parser(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initOrderCounters();
        dbf = DocumentBuilderFactory.newInstance();
        lfpmTnuotHayomParser = (LfpmTnuotHayomParser)LfpmTnuotHayomParser.builder()
                .addField(LfpmTnuotHayom.ISN, Integer.class.getName())
                .addField(LfpmTnuotHayom.BANK,Short.class.getName())
                .addField(LfpmTnuotHayom.SNIF,Short.class.getName())
                .addField(LfpmTnuotHayom.CHESBON,Long.class.getName())
                .addField(LfpmTnuotHayom.SUG_CHESBON,Short.class.getName())
                .addField(LfpmTnuotHayom.SUG_MATBEA,Short.class.getName())
                .addField(LfpmTnuotHayom.SHEM_LAKOACH,String.class.getName())
                .addField(LfpmTnuotHayom.ITRA_PTICHA_PIK,BigDecimal.class.getName())
                .addField(LfpmTnuotHayom.ITRA_PTICHA_STF,BigDecimal.class.getName())
                .addField(LfpmTnuotHayom.PMOL_ITRA_PTICHA_BLI_ATD_PIK,BigDecimal.class.getName())
                .addField(LfpmTnuotHayom.PMOL_ITRA_PTICHA_BLI_ATD_STF,BigDecimal.class.getName())
                .addField(LfpmTnuotHayom.PMOL_ITRA_MECHUSH_PIK,BigDecimal.class.getName())
                .addField(LfpmTnuotHayom.PMOL_ITRA_MECHUSH_STF,BigDecimal.class.getName())
                .addField(LfpmTnuotHayom.PMOL_ITRA_MECHUSH_BLI_ATD_PIK,BigDecimal.class.getName())
                .addField(LfpmTnuotHayom.PMOL_ITRA_MECHUSH_BLI_ATD_STF,BigDecimal.class.getName())
                .addField(LfpmTnuotHayom.PMOL_MIS_TNUOT_BE_DAF,Short.class.getName())
                .addField(LfpmTnuotHayom.PMOL_TAARICH_NECHONUT,Integer.class.getName())
                .build();
        lfpmTnuotHayomPmolTnuotParser = (LfpmTnuotHayomTnuotParser)LfpmTnuotHayomTnuotParser.builder()
                .addField(LfpmTnuotHayomPmolTnuot.ISN, Integer.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_TNUOT_INDEX,Short.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.ISN_PMOL_TNUOT_INDEX,String.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_TAARICH_IBUD,Integer.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_TAARICH_RIKUZ,Integer.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_TAARICH_ERECH,Integer.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_OFI_PEULA,Short.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_ASMACHTA,Long.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_KOD_CHOVA_ZCHUT,Byte.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_SCHUM_HA_PEULA, BigDecimal.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_KOD_SOFI,Byte.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_SNIF_MESHADER,Integer.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_MEZAHE,Integer.class.getName())
                .addField(LfpmTnuotHayomPmolTnuot.PMOL_MISPAR_PIKADON,Short.class.getName())
                .build();
    }

    public static void registerTypes(){
//        gigaSpace.getTypeManager().registerTypeDescriptor(LfpmTnuotHayom.class);
//        gigaSpace.getTypeManager().registerTypeDescriptor(LfpmTnuotHayomPmolTnuot.class);
    }

    private void initOrderCounters() {
        recordCounter = this.meterRegistry.counter("adabase", "type", "records"); // 1 - create a counter
        orphanRecordCounter = this.meterRegistry.counter("adabase", "type", "orphans"); // 1 - create a counter
        eventCounter = this.meterRegistry.counter("adabase", "type", "events"); // 1 - create a counter

        logger.info("Create meter counters");
    }


    public boolean consumeEvent(String event) throws IOException, SAXException, ParserConfigurationException {
        this.eventCounter.increment();
        if (logger.isDebugEnabled()) logger.debug("Debug event: " + event);
        Envalope o = parse(event);
        String topic = null;
        if(o.getOperation().equalsIgnoreCase("CREATE")){
            topic = config.getKafka().getSchemaTopic().replace("$tableName$", o.getType());
        } else {
            topic = config.getKafka().getTopic().replace("$tableName$", o.getType());
            this.recordCounter.increment();
        }
//        String msg = new ObjectMapper().writeValueAsString(o);
        kafkaTemplate.send(topic, o);
        return true;
    }

    public Envalope parse(String event) throws ParserConfigurationException, IOException, SAXException {
        if (event == null) {
            logger.error("null event " );
            return null;
        }
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(event)));
        Element element = doc.getDocumentElement();
        element.normalize();
        if (!element.getNodeName().equalsIgnoreCase("artdocument")) {
            logger.error("Root node is not the expected artdocument node. it is {}\n Msg:\n{}",
                    element.getNodeName(), event);
            return null;
        }
        String method = element.getElementsByTagName(METHOD).item(0).getTextContent();
        if (!(method.equalsIgnoreCase("update") || method.equalsIgnoreCase("insert") ||
                method.equalsIgnoreCase("create"))) {
           logger.warn("Method will not be handled: {}\n Msg:\n{}", method, event);
            return null;
        }

        String tableName = element.getElementsByTagName(TABLE_NAME).item(0).getTextContent();
//        tableName=tableName.replace("-","_");
        JSONObject json = XML.toJSONObject(event);
        if(method.equalsIgnoreCase("create")){
            Path path = Paths.get(config.getSchemasPath());
            Files.createDirectories(path);
            Path schemaFile = path.resolve(tableName);
            Files.write(schemaFile, json.toString(4).getBytes());
        }

/*        if (tableName.equalsIgnoreCase(LfpmTnuotHayom.TABLE_NAME)) {
            return parseTnuotHayom(new LfpmTnuotHayom(), element.getChildNodes(), tableName, method);
        } else if (tableName.equalsIgnoreCase(LfpmTnuotHayomPmolTnuot.TABLE_NAME)) {
            return parseTnuotHayom(new LfpmTnuotHayomPmolTnuot(), element.getChildNodes(), tableName, method);
        }
*/
        return new Envalope(tableName, method.toUpperCase(), json.toMap());
    }


    private Envalope parseTnuotHayom(ILfpmTnuotHayom target, NodeList nodes, String table, String method){

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equalsIgnoreCase("fields")) {
                NodeList fieldList = node.getChildNodes();
                for (int j = 0; j < fieldList.getLength(); j++) {
                    Node fieldNode = fieldList.item(j);
                    NamedNodeMap map = fieldNode.getAttributes();
                    String fieldName = map.getNamedItem("name").getNodeValue();
                    String fieldValue = map.getNamedItem("value").getNodeValue();
                    if(target instanceof LfpmTnuotHayom) {
                        String type = lfpmTnuotHayomParser.nameTypeMap.get(fieldName);
                        lfpmTnuotHayomParser.set((LfpmTnuotHayom) target, fieldName, type, fieldValue);
                    }
                    else {
                        String type = lfpmTnuotHayomPmolTnuotParser.nameTypeMap.get(fieldName);
                        lfpmTnuotHayomPmolTnuotParser.set((LfpmTnuotHayomPmolTnuot) target, fieldName, type, fieldValue);
                    }
                }
            }
        }
        Envalope envalope = new Envalope(table, method, table);
        return envalope;
    }

    @Override
    public String toString() {
        return "Parser{" +
                "recordCounter=" + recordCounter.count() +
                "orphanRecordCounter=" + orphanRecordCounter.count() +
                ", eventCounter=" + eventCounter.count() +
                '}';
    }
}
