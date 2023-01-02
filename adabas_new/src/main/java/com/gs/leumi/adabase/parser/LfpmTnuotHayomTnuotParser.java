package com.gs.leumi.adabase.parser;

import com.gs.leumi.common.model.LfpmTnuotHayom;
import com.gs.leumi.common.model.LfpmTnuotHayomPmolTnuot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class LfpmTnuotHayomTnuotParser extends BaseParser{
    private static final Logger logger = LoggerFactory.getLogger(LfpmTnuotHayomTnuotParser.class);

    public LfpmTnuotHayomTnuotParser(BaseParser.Builder builder) {
        super(builder);
    }


    public static LfpmTnuotHayomTnuotParser.Builder builder() {
        return new LfpmTnuotHayomTnuotParser.Builder();
    }
    public static class Builder extends BaseParser.Builder<Builder>{
        public BaseParser build(){
            tableName(LfpmTnuotHayomPmolTnuot.TABLE_NAME);
            return new LfpmTnuotHayomTnuotParser(this);
        }
    }

        public void set(LfpmTnuotHayomPmolTnuot o, String name, String type, String value){
        Object val = getParser(type).parser.apply(value);
        switch (name){
            case LfpmTnuotHayomPmolTnuot.ISN:
                o.isn = (Integer) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_TNUOT_INDEX:
                o.pmolTnuotIndex = (Short)val;
                break;
            case LfpmTnuotHayomPmolTnuot.ISN_PMOL_TNUOT_INDEX:
                o.isnPmolTnuotIndex = (String) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_TAARICH_IBUD:
                o.pmolTaarichIbud = (Integer) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_TAARICH_RIKUZ:
                o.pmolTaarichRikuz = (Integer) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_TAARICH_ERECH:
                o.pmolTaarichErech = (Integer)val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_OFI_PEULA:
                o.pmolOfiPeula = (Short) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_ASMACHTA:
                o.pmolAsmachta = (Long) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_KOD_CHOVA_ZCHUT:
                o.pmolKodChovaZchut = (Byte) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_SCHUM_HA_PEULA:
                o.pmolSchumHaPeula = (BigDecimal)val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_KOD_SOFI:
                o.pmolKodSofi = (Byte) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_SNIF_MESHADER:
                o.pmolSnifMeshader = (Integer) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_MEZAHE:
                o.pmolMezahe = (Integer) val;
                break;
            case LfpmTnuotHayomPmolTnuot.PMOL_MISPAR_PIKADON:
                o.pmolMisparPikadon = (Short) val;
                break;
                default:
                    logger.warn("Name: " + name + " not mapped");
        }
    }
}
