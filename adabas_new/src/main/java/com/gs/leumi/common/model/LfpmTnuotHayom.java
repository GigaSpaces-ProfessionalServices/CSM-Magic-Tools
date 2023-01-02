package com.gs.leumi.common.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class LfpmTnuotHayom implements ILfpmTnuotHayom{
    public static final String TABLE_NAME = "LFPM_TNUOT_HAYOM";
    public static final String ISN = "ISN";
    public static final String BANK = "BANK";
    public static final String SNIF = "SNIF";
    public static final String CHESBON = "CHESBON";
    public static final String SUG_CHESBON      = "SUG-CHESBON"         ;
    public static final String SUG_MATBEA       = "SUG-MATBEA"          ;
    public static final String SHEM_LAKOACH       = "SHEM-LAKOACH"      ;
    public static final String ITRA_PTICHA_STF       = "ITRA-PTICHA-STF";
    public static final String ITRA_PTICHA_PIK       = "ITRA-PTICHA-PIK";
    public static final String PMOL_ITRA_PTICHA_BLI_ATD_STF = "PMOL-ITRA-PTICHA-BLI-ATD-STF";
    public static final String PMOL_ITRA_PTICHA_BLI_ATD_PIK = "PMOL-ITRA-PTICHA-BLI-ATD-PIK";
    public static final String PMOL_ITRA_MECHUSH_STF = "PMOL-ITRA-MECHUSH-STF" ;
    public static final String PMOL_ITRA_MECHUSH_PIK = "PMOL-ITRA-MECHUSH-PIK" ;
    public static final String PMOL_ITRA_MECHUSH_BLI_ATD_STF = "PMOL-ITRA-MECHUSH-BLI-ATD-STF";
    public static final String PMOL_ITRA_MECHUSH_BLI_ATD_PIK = "PMOL-ITRA-MECHUSH-BLI-ATD-PIK";
    public static final String PMOL_MIS_TNUOT_BE_DAF = "PMOL-MIS-TNUOT-BE-DAF";
    public static final String PMOL_TAARICH_NECHONUT = "PMOL-TAARICH-NECHONUT";
    private Integer isn;
    private Short bank;
    private Short snif;
    private Long chesbon;
    private Short sugChesbon;
    private Short sugMatbea;
    private String shemLakoach;
    private BigDecimal itraPtichaSTF;
    private BigDecimal itraPtichaPIK;
    private BigDecimal pmolPtichaBliAtdSTF;
    private BigDecimal pmolPtichaBliAtdPIK;
    private BigDecimal pmolMechushSTF;
    private BigDecimal pmolMechushPIK;
    private BigDecimal pmolMechushBliAtdSTF;
    private BigDecimal pmolMechushBliAtdPIK;
    private Short pmolMisTnuotBeDaf;
    private Integer pmolTaarichNechonut;

    public List<LfpmTnuotHayomPmolTnuot> getLfpmTnuotHayomPmolTnuot() {
        return lfpmTnuotHayomPmolTnuot;
    }

    public void setLfpmTnuotHayomPmolTnuot(List<LfpmTnuotHayomPmolTnuot> lfpmTnuotHayomPmolTnuot) {
        this.lfpmTnuotHayomPmolTnuot = lfpmTnuotHayomPmolTnuot;
    }

    public List<LfpmTnuotHayomPmolTnuot> lfpmTnuotHayomPmolTnuot;

    public Integer getIsn() {
        return isn;
    }

    public void setIsn(Integer isn) {
        this.isn = isn;
    }

    public Short getBank() {
        return bank;
    }

    public void setBank(Short bank) {
        this.bank = bank;
    }

    public Short getSnif() {
        return snif;
    }

    public void setSnif(Short snif) {
        this.snif = snif;
    }

    public Long getChesbon() {
        return chesbon;
    }

    public void setChesbon(Long chesbon) {
        this.chesbon = chesbon;
    }

    public Short getSugChesbon() {
        return sugChesbon;
    }

    public void setSugChesbon(Short sugChesbon) {
        this.sugChesbon = sugChesbon;
    }

    public Short getSugMatbea() {
        return sugMatbea;
    }

    public void setSugMatbea(Short sugMatbea) {
        this.sugMatbea = sugMatbea;
    }

    public String getShemLakoach() {
        return shemLakoach;
    }

    public void setShemLakoach(String shemLakoach) {
        this.shemLakoach = shemLakoach;
    }

    public BigDecimal getItraPtichaSTF() {
        return itraPtichaSTF;
    }

    public void setItraPtichaSTF(BigDecimal itraPtichaSTF) {
        this.itraPtichaSTF = itraPtichaSTF;
    }

    public BigDecimal getItraPtichaPIK() {
        return itraPtichaPIK;
    }

    public void setItraPtichaPIK(BigDecimal itraPtichaPIK) {
        this.itraPtichaPIK = itraPtichaPIK;
    }

    public BigDecimal getPmolPtichaBliAtdSTF() {
        return pmolPtichaBliAtdSTF;
    }

    public void setPmolPtichaBliAtdSTF(BigDecimal pmolPtichaBliAtdSTF) {
        this.pmolPtichaBliAtdSTF = pmolPtichaBliAtdSTF;
    }

    public BigDecimal getPmolPtichaBliAtdPIK() {
        return pmolPtichaBliAtdPIK;
    }

    public void setPmolPtichaBliAtdPIK(BigDecimal pmolPtichaBliAtdPIK) {
        this.pmolPtichaBliAtdPIK = pmolPtichaBliAtdPIK;
    }

    public BigDecimal getPmolMechushSTF() {
        return pmolMechushSTF;
    }

    public void setPmolMechushSTF(BigDecimal pmolMechushSTF) {
        this.pmolMechushSTF = pmolMechushSTF;
    }

    public BigDecimal getPmolMechushPIK() {
        return pmolMechushPIK;
    }

    public void setPmolMechushPIK(BigDecimal pmolMechushPIK) {
        this.pmolMechushPIK = pmolMechushPIK;
    }

    public BigDecimal getPmolMechushBliAtdSTF() {
        return pmolMechushBliAtdSTF;
    }

    public void setPmolMechushBliAtdSTF(BigDecimal pmolMechushBliAtdSTF) {
        this.pmolMechushBliAtdSTF = pmolMechushBliAtdSTF;
    }

    public BigDecimal getPmolMechushBliAtdPIK() {
        return pmolMechushBliAtdPIK;
    }

    public void setPmolMechushBliAtdPIK(BigDecimal pmolMechushBliAtdPIK) {
        this.pmolMechushBliAtdPIK = pmolMechushBliAtdPIK;
    }

    public Short getPmolMisTnuotBeDaf() {
        return pmolMisTnuotBeDaf;
    }

    public void setPmolMisTnuotBeDaf(Short pmolMisTnuotBeDaf) {
        this.pmolMisTnuotBeDaf = pmolMisTnuotBeDaf;
    }

    public Integer getPmolTaarichNechonut() {
        return pmolTaarichNechonut;
    }

    public void setPmolTaarichNechonut(Integer pmolTaarichNechonut) {
        this.pmolTaarichNechonut = pmolTaarichNechonut;
    }

    public LfpmTnuotHayom() {
        setLfpmTnuotHayomPmolTnuot(new ArrayList<>());
    }

    @Override
    public String toString() {
        return "LfpmTnuotHayom{" +
                "isn=" + isn +
                "bank=" + bank +
                ", snif=" + snif +
                ", chesbon=" + chesbon +
                ", sugChesbon=" + sugChesbon +
                ", sugMatbea=" + sugMatbea +
                ", shemLakoach='" + shemLakoach + '\'' +
                ", itraPtichaSTF=" + itraPtichaSTF +
                ", itraPtichaPIK=" + itraPtichaPIK +
                ", pmolPtichaBliAtdSTF=" + pmolPtichaBliAtdSTF +
                ", pmolPtichaBliAtdPIK=" + pmolPtichaBliAtdPIK +
                ", pmolMechushSTF=" + pmolMechushSTF +
                ", pmolMechushPIK=" + pmolMechushPIK +
                ", pmolMechushBliAtdSTF=" + pmolMechushBliAtdSTF +
                ", pmolMechushBliAtdPIK=" + pmolMechushBliAtdPIK +
                ", pmolMisTnuotBeDaf=" + pmolMisTnuotBeDaf +
                ", pmolTaarichNechonut=" + pmolTaarichNechonut +
                ",\nlfpmTnuotHayomPmolTnuot=" + Arrays.toString(lfpmTnuotHayomPmolTnuot.toArray()) +
                '}';
    }
@Override
    public boolean validateNotNull(){
        return (null != bank)&&(null != snif)&&(null != chesbon)&&(null != sugChesbon)&&(null != sugMatbea);
    }

    public static LfpmTnuotHayom generateRandom(){
        LfpmTnuotHayom lfpmTnuotHayom = new LfpmTnuotHayom();
        Random rand = new Random(System.currentTimeMillis());
        lfpmTnuotHayom.isn = rand.nextInt(10000);
        lfpmTnuotHayom.bank = (short)rand.nextInt(1000);
        lfpmTnuotHayom.snif = (short)rand.nextInt(1000);
        lfpmTnuotHayom.chesbon = rand.nextLong();
        lfpmTnuotHayom.sugChesbon = (short)rand.nextInt(1000);
        lfpmTnuotHayom.sugMatbea = (short)rand.nextInt(1000);
        lfpmTnuotHayom.shemLakoach = "shem" + rand.nextInt();
        lfpmTnuotHayom.pmolMisTnuotBeDaf = (short)rand.nextInt(1000);
        lfpmTnuotHayom.pmolTaarichNechonut = rand.nextInt();
        return lfpmTnuotHayom;
    }
}
