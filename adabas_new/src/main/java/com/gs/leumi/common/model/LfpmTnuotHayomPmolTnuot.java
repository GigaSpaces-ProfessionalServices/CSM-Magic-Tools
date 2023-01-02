package com.gs.leumi.common.model;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Random;

public class LfpmTnuotHayomPmolTnuot implements ILfpmTnuotHayom, Serializable {

    public static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "LFPM_TNUOT_HAYOM_PMOL-TNUOT";
    public static final String ISN = "ISN";
    public static final String PMOL_TNUOT_INDEX = "PMOL-TNUOT_INDEX";
    public static final String ISN_PMOL_TNUOT_INDEX = "ISN_PMOL-TNUOT_INDEX";
    public static final String PMOL_TAARICH_IBUD = "PMOL-TAARICH-IBUD";
    public static final String PMOL_TAARICH_RIKUZ = "PMOL-TAARICH-RIKUZ";
    public static final String PMOL_TAARICH_ERECH = "PMOL-TAARICH-ERECH";
    public static final String PMOL_OFI_PEULA      = "PMOL-OFI-PEULA"         ;
    public static final String PMOL_ASMACHTA       = "PMOL-ASMACHTA"          ;
    public static final String PMOL_KOD_CHOVA_ZCHUT       = "PMOL-KOD-CHOVA-ZCHUT"      ;
    public static final String PMOL_SCHUM_HA_PEULA       = "PMOL-SCHUM-HA-PEULA";
    public static final String PMOL_KOD_SOFI       = "PMOL-KOD-SOFI";
    public static final String PMOL_SNIF_MESHADER = "PMOL-SNIF-MESHADER";
    public static final String PMOL_MEZAHE = "PMOL-MEZAHE";
    public static final String PMOL_MISPAR_PIKADON = "PMOL-MISPAR-PIKADON" ;

    public Integer isn;
    public Short pmolTnuotIndex;
    public String isnPmolTnuotIndex;
    public Integer pmolTaarichIbud;
    public Integer pmolTaarichRikuz;
    public Integer pmolTaarichErech;
    public Short pmolOfiPeula;
    public Long pmolAsmachta;
    public Byte pmolKodChovaZchut;
    public BigDecimal pmolSchumHaPeula;
    public Byte pmolKodSofi;
    public Integer pmolSnifMeshader;
    public Integer pmolMezahe;
    public Short pmolMisparPikadon;

    public LfpmTnuotHayomPmolTnuot() {
    }

    public Short getPmolTnuotIndex() {
        return pmolTnuotIndex;
    }

    public void setPmolTnuotIndex(Short pmolTnuotIndex) {
        this.pmolTnuotIndex = pmolTnuotIndex;
    }

    public String getIsnPmolTnuotIndex() {
        return isnPmolTnuotIndex;
    }

    public void setIsnPmolTnuotIndex(String isnPmolTnuotIndex) {
        this.isnPmolTnuotIndex = isnPmolTnuotIndex;
    }

    public Integer getPmolTaarichIbud() {
        return pmolTaarichIbud;
    }

    public void setPmolTaarichIbud(Integer pmolTaarichIbud) {
        this.pmolTaarichIbud = pmolTaarichIbud;
    }

    public Integer getPmolTaarichRikuz() {
        return pmolTaarichRikuz;
    }

    public void setPmolTaarichRikuz(Integer pmolTaarichRikuz) {
        this.pmolTaarichRikuz = pmolTaarichRikuz;
    }

    public Integer getPmolTaarichErech() {
        return pmolTaarichErech;
    }

    public void setPmolTaarichErech(Integer pmolTaarichErech) {
        this.pmolTaarichErech = pmolTaarichErech;
    }

    public Short getPmolOfiPeula() {
        return pmolOfiPeula;
    }

    public void setPmolOfiPeula(Short pmolOfiPeula) {
        this.pmolOfiPeula = pmolOfiPeula;
    }

    public Long getPmolAsmachta() {
        return pmolAsmachta;
    }

    public void setPmolAsmachta(Long pmolAsmachta) {
        this.pmolAsmachta = pmolAsmachta;
    }

    public Byte getPmolKodChovaZchut() {
        return pmolKodChovaZchut;
    }

    public void setPmolKodChovaZchut(Byte pmolKodChovaZchut) {
        this.pmolKodChovaZchut = pmolKodChovaZchut;
    }

    public BigDecimal getPmolSchumHaPeula() {
        return pmolSchumHaPeula;
    }

    public void setPmolSchumHaPeula(BigDecimal pmolSchumHaPeula) {
        this.pmolSchumHaPeula = pmolSchumHaPeula;
    }

    public Byte getPmolKodSofi() {
        return pmolKodSofi;
    }

    public void setPmolKodSofi(Byte pmolKodSofi) {
        this.pmolKodSofi = pmolKodSofi;
    }

    public Integer getPmolSnifMeshader() {
        return pmolSnifMeshader;
    }

    public void setPmolSnifMeshader(Integer pmolSnifMeshader) {
        this.pmolSnifMeshader = pmolSnifMeshader;
    }

    public Integer getPmolMezahe() {
        return pmolMezahe;
    }

    public void setPmolMezahe(Integer pmolMezahe) {
        this.pmolMezahe = pmolMezahe;
    }

    public Short getPmolMisparPikadon() {
        return pmolMisparPikadon;
    }

    public void setPmolMisparPikadon(Short pmolMisparPikadon) {
        this.pmolMisparPikadon = pmolMisparPikadon;
    }

    @Override
    public boolean validateNotNull(){
        return (null != isn)&&(null != pmolTnuotIndex)&&(null != pmolAsmachta);
    }

    public static LfpmTnuotHayomPmolTnuot generateRandom(int isn){
        LfpmTnuotHayomPmolTnuot lfpmTnuotHayomPmolTnuot = new LfpmTnuotHayomPmolTnuot();
        Random rand = new Random(System.currentTimeMillis());
        lfpmTnuotHayomPmolTnuot.isn = isn;
        lfpmTnuotHayomPmolTnuot.pmolTnuotIndex = (short)rand.nextInt(1000);
        lfpmTnuotHayomPmolTnuot.pmolAsmachta = rand.nextLong();
        return lfpmTnuotHayomPmolTnuot;
    }

    @Override
    public String toString() {
        return "LfpmTnuotHayomPmolTnuot{" +
                "isn=" + isn +
                ", pmolTnuotIndex=" + pmolTnuotIndex +
                ", isnPmolTnuotIndex='" + isnPmolTnuotIndex + '\'' +
                ", pmolTaarichIbud=" + pmolTaarichIbud +
                ", pmolTaarichRikuz=" + pmolTaarichRikuz +
                ", pmolTaarichErech=" + pmolTaarichErech +
                ", pmolOfiPeula=" + pmolOfiPeula +
                ", pmolAsmachta=" + pmolAsmachta +
                ", pmolKodChovaZchut=" + pmolKodChovaZchut +
                ", pmolSchumHaPeula=" + pmolSchumHaPeula +
                ", pmolKodSofi=" + pmolKodSofi +
                ", pmolSnifMeshader=" + pmolSnifMeshader +
                ", pmolMezahe=" + pmolMezahe +
                ", pmolMisparPikadon=" + pmolMisparPikadon +
                '}';
    }
}
