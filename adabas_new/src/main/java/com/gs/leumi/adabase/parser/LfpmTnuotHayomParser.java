package com.gs.leumi.adabase.parser;

import com.gs.leumi.common.model.LfpmTnuotHayom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class LfpmTnuotHayomParser extends BaseParser{
    private static final Logger logger = LoggerFactory.getLogger(LfpmTnuotHayomParser.class);

    public LfpmTnuotHayomParser(BaseParser.Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder extends BaseParser.Builder<Builder>{
        public BaseParser build(){
            this.tableName(LfpmTnuotHayom.TABLE_NAME);
            return new LfpmTnuotHayomParser(this);
        }
    }

    public void set(LfpmTnuotHayom o, String name, String type, String value){
        Object val = getParser(type).parser.apply(value);
        switch (name){
            case LfpmTnuotHayom.ISN:
                o.setIsn((Integer) val);
                break;
            case LfpmTnuotHayom.BANK:
                o.setBank((Short) val);
                break;
            case LfpmTnuotHayom.SNIF:
                o.setSnif((Short) val);
                break;
            case LfpmTnuotHayom.CHESBON:
                o.setChesbon((Long) val);
                break;
            case LfpmTnuotHayom.SUG_CHESBON:
                o.setSugChesbon((Short) val);
                break;
            case LfpmTnuotHayom.SUG_MATBEA:
                o.setSugMatbea((Short) val);
                break;
            case LfpmTnuotHayom.SHEM_LAKOACH:
                o.setShemLakoach((String) val);
                break;
            case LfpmTnuotHayom.ITRA_PTICHA_PIK:
                o.setItraPtichaPIK((BigDecimal) val);
                break;
            case LfpmTnuotHayom.ITRA_PTICHA_STF:
                o.setItraPtichaSTF((BigDecimal) val);
                break;
            case LfpmTnuotHayom.PMOL_ITRA_MECHUSH_BLI_ATD_PIK:
                o.setPmolMechushBliAtdPIK((BigDecimal) val);
                break;
            case LfpmTnuotHayom.PMOL_ITRA_MECHUSH_BLI_ATD_STF:
                o.setPmolMechushBliAtdSTF((BigDecimal) val);
                break;
            case LfpmTnuotHayom.PMOL_ITRA_MECHUSH_PIK:
                o.setPmolMechushPIK((BigDecimal) val);
                break;
            case LfpmTnuotHayom.PMOL_ITRA_MECHUSH_STF:
                o.setPmolMechushSTF((BigDecimal) val);
                break;
            case LfpmTnuotHayom.PMOL_ITRA_PTICHA_BLI_ATD_PIK:
                o.setPmolPtichaBliAtdPIK((BigDecimal) val);
                break;
            case LfpmTnuotHayom.PMOL_ITRA_PTICHA_BLI_ATD_STF:
                o.setPmolPtichaBliAtdSTF((BigDecimal) val);
                break;
            case LfpmTnuotHayom.PMOL_MIS_TNUOT_BE_DAF:
                o.setPmolMisTnuotBeDaf((Short) val);
                break;
            case LfpmTnuotHayom.PMOL_TAARICH_NECHONUT:
                o.setPmolTaarichNechonut((Integer) val);
                break;
            default:
                logger.warn("Name: " + name + " not mapped");

        }
    }
}
