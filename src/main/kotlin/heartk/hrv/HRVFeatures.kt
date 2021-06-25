package heartk.hrv

import com.google.common.reflect.TypeToken
import com.google.gson.Gson

data class HRVFeatures(
    var HR: DoubleArray? = null,
    var meanHR: Double? = null,
    val stdHR: Double? = null,
    var medianHR: Double? = null,
    var varHR: Double? = null,
    var maxHR: Double? = null,
    var minHR: Double? = null,
    var rangeHR: Double? = null,
    var GI: Double? = null,
    var SI: Double? = null,
    var PI: Double? = null,
    var AI: Double? = null,
    var C1d: Double? = null,
    var C1a: Double? = null,
    var SD1a: Double? = null,
    var SD1d: Double? = null,
    var C2d: Double? = null,
    var C2a: Double? = null,
    var SD2d: Double? = null,
    var SD2a: Double? = null,
    var Cd: Double? = null,
    var Ca: Double? = null,
    var SDNNd: Double? = null,
    var SDNNa: Double? = null,
    var SD1: Double? = null,
    var SD2: Double? = null,
    var SD1SD2: Double? = null,
    var S: Double? = null,
    var CSI: Double? = null,
    var CVI: Double? = null,
    var CSI_Modified: Double? = null,
    var PIP: Double? = null,
    var IALS: Double? = null,
    var PSS: Double? = null,
    var PAS: Double? = null,
    var RMSSD: Double? = null,
    var MeanNN: Double? = null,
    var SDNN: Double? = null,
    var SDSD: Double? = null,
    var CVNN: Double? = null,
    var CVSD: Double? = null,
    var MedianNN: Double? = null,
    var MadNN: Double? = null,
    var MCVNN: Double? = null,
    var IQR: Double? = null,
    var pNN50: Double? = null,
    var pNN20: Double? = null,
    var LF: Double? = null,
    var HF: Double? = null,
    var ULF: Double? = null,
    var VLF: Double? = null,
    var VHF: Double? = null,
    var LFHF: Double? = null,
    var LFn: Double? = null,
    var HFn: Double? = null,
    var LnHF: Double? = null
) {

    companion object {
        fun getCSVHeader(): String = "AI,C1a,C1d,C2a,C2d,CSI,CSI_Modified,CVI,CVNN,CVSD,Ca,Cd,GI,HF,HFn,HR,IALS,IQR,LF,LFHF,LFn,LnHF,MCVNN,MadNN,MeanNN,MedianNN,PAS,PI,PIP,PSS,RMSSD,S,SD1,SD1SD2,SD1a,SD1d,SD2,SD2a,SD2d,SDNN,SDNNa,SDNNd,SDSD,SI,ULF,VHF,VLF,pNN20,pNN50"
    }

    fun toCSV(): String  {
        return arrayOf(
            (this.AI ?: 0.0).toString(),
            (this.C1a ?: 0.0).toString(),
            (this.C1d ?: 0.0).toString(),
            (this.C2a ?: 0.0).toString(),
            (this.C2d ?: 0.0).toString(),
            (this.CSI ?: 0.0).toString(),
            (this.CSI_Modified ?: 0.0).toString(),
            (this.CVI ?: 0.0).toString(),
            (this.CVNN ?: 0.0).toString(),
            (this.CVSD ?: 0.0).toString(),
            (this.Ca ?: 0.0).toString(),
            (this.Cd ?: 0.0).toString(),
            (this.GI ?: 0.0).toString(),
            (this.HF ?: 0.0).toString(),
            (this.HFn ?: 0.0).toString(),
            (this.HR?.average() ?: 0.0).toString(),
            (this.IALS ?: 0.0).toString(),
            (this.IQR ?: 0.0).toString(),
            (this.LF ?: 0.0).toString(),
            (this.LFHF ?: 0.0).toString(),
            (this.LFn ?: 0.0).toString(),
            (this.LnHF ?: 0.0).toString(),
            (this.MCVNN ?: 0.0).toString(),
            (this.MadNN ?: 0.0).toString(),
            (this.MeanNN ?: 0.0).toString(),
            (this.MedianNN ?: 0.0).toString(),
            (this.PAS ?: 0.0).toString(),
            (this.PI ?: 0.0).toString(),
            (this.PIP ?: 0.0).toString(),
            (this.PSS ?: 0.0).toString(),
            (this.RMSSD ?: 0.0).toString(),
            (this.S ?: 0.0).toString(),
            (this.SD1 ?: 0.0).toString(),
            (this.SD1SD2 ?: 0.0).toString(),
            (this.SD1a ?: 0.0).toString(),
            (this.SD1d ?: 0.0).toString(),
            (this.SD2 ?: 0.0).toString(),
            (this.SD2a ?: 0.0).toString(),
            (this.SD2d ?: 0.0).toString(),
            (this.SDNN ?: 0.0).toString(),
            (this.SDNNa ?: 0.0).toString(),
            (this.SDNNd ?: 0.0).toString(),
            (this.SDSD ?: 0.0).toString(),
            (this.SI ?: 0.0).toString(),
            (this.ULF ?: 0.0).toString(),
            (this.VHF ?: 0.0).toString(),
            (this.VLF ?: 0.0).toString(),
            (this.pNN20 ?: 0.0).toString(),
            (this.pNN50 ?: 0.0).toString()
        ).joinToString(",")
    }

    fun asMap(): Map<String, Any> {
        return mapOf() // Gson().fromJson(Gson().toJson(this), object : TypeToken<Map<String, Any>>() {}.type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HRVFeatures
        val thisHR = this.HR
        val otherHR = other.HR
        if (thisHR != null) {
            if (otherHR == null) return false
            if (!thisHR.contentEquals(otherHR)) return false
        } else if (other.HR != null) return false
        if (meanHR != other.meanHR) return false
        if (stdHR != other.stdHR) return false
        if (medianHR != other.medianHR) return false
        if (varHR != other.varHR) return false
        if (maxHR != other.maxHR) return false
        if (minHR != other.minHR) return false
        if (rangeHR != other.rangeHR) return false
        if (GI != other.GI) return false
        if (SI != other.SI) return false
        if (PI != other.PI) return false
        if (AI != other.AI) return false
        if (C1d != other.C1d) return false
        if (C1a != other.C1a) return false
        if (SD1a != other.SD1a) return false
        if (SD1d != other.SD1d) return false
        if (C2d != other.C2d) return false
        if (C2a != other.C2a) return false
        if (SD2d != other.SD2d) return false
        if (SD2a != other.SD2a) return false
        if (Cd != other.Cd) return false
        if (Ca != other.Ca) return false
        if (SDNNd != other.SDNNd) return false
        if (SDNNa != other.SDNNa) return false
        if (SD1 != other.SD1) return false
        if (SD2 != other.SD2) return false
        if (SD1SD2 != other.SD1SD2) return false
        if (S != other.S) return false
        if (CSI != other.CSI) return false
        if (CVI != other.CVI) return false
        if (CSI_Modified != other.CSI_Modified) return false
        if (PIP != other.PIP) return false
        if (IALS != other.IALS) return false
        if (PSS != other.PSS) return false
        if (PAS != other.PAS) return false
        if (RMSSD != other.RMSSD) return false
        if (MeanNN != other.MeanNN) return false
        if (SDNN != other.SDNN) return false
        if (SDSD != other.SDSD) return false
        if (CVNN != other.CVNN) return false
        if (CVSD != other.CVSD) return false
        if (MedianNN != other.MedianNN) return false
        if (MadNN != other.MadNN) return false
        if (MCVNN != other.MCVNN) return false
        if (IQR != other.IQR) return false
        if (pNN50 != other.pNN50) return false
        if (pNN20 != other.pNN20) return false
        if (LF != other.LF) return false
        if (HF != other.HF) return false
        if (ULF != other.ULF) return false
        if (VLF != other.VLF) return false
        if (VHF != other.VHF) return false
        if (LFHF != other.LFHF) return false
        if (LFn != other.LFn) return false
        if (HFn != other.HFn) return false
        if (LnHF != other.LnHF) return false

        return true
    }

    override fun hashCode(): Int {
        var result = HR?.contentHashCode() ?: 0
        result = 31 * result + (meanHR?.hashCode() ?: 0)
        result = 31 * result + (stdHR?.hashCode() ?: 0)
        result = 31 * result + (medianHR?.hashCode() ?: 0)
        result = 31 * result + (varHR?.hashCode() ?: 0)
        result = 31 * result + (maxHR?.hashCode() ?: 0)
        result = 31 * result + (minHR?.hashCode() ?: 0)
        result = 31 * result + (rangeHR?.hashCode() ?: 0)
        result = 31 * result + (GI?.hashCode() ?: 0)
        result = 31 * result + (SI?.hashCode() ?: 0)
        result = 31 * result + (PI?.hashCode() ?: 0)
        result = 31 * result + (AI?.hashCode() ?: 0)
        result = 31 * result + (C1d?.hashCode() ?: 0)
        result = 31 * result + (C1a?.hashCode() ?: 0)
        result = 31 * result + (SD1a?.hashCode() ?: 0)
        result = 31 * result + (SD1d?.hashCode() ?: 0)
        result = 31 * result + (C2d?.hashCode() ?: 0)
        result = 31 * result + (C2a?.hashCode() ?: 0)
        result = 31 * result + (SD2d?.hashCode() ?: 0)
        result = 31 * result + (SD2a?.hashCode() ?: 0)
        result = 31 * result + (Cd?.hashCode() ?: 0)
        result = 31 * result + (Ca?.hashCode() ?: 0)
        result = 31 * result + (SDNNd?.hashCode() ?: 0)
        result = 31 * result + (SDNNa?.hashCode() ?: 0)
        result = 31 * result + (SD1?.hashCode() ?: 0)
        result = 31 * result + (SD2?.hashCode() ?: 0)
        result = 31 * result + (SD1SD2?.hashCode() ?: 0)
        result = 31 * result + (S?.hashCode() ?: 0)
        result = 31 * result + (CSI?.hashCode() ?: 0)
        result = 31 * result + (CVI?.hashCode() ?: 0)
        result = 31 * result + (CSI_Modified?.hashCode() ?: 0)
        result = 31 * result + (PIP?.hashCode() ?: 0)
        result = 31 * result + (IALS?.hashCode() ?: 0)
        result = 31 * result + (PSS?.hashCode() ?: 0)
        result = 31 * result + (PAS?.hashCode() ?: 0)
        result = 31 * result + (RMSSD?.hashCode() ?: 0)
        result = 31 * result + (MeanNN?.hashCode() ?: 0)
        result = 31 * result + (SDNN?.hashCode() ?: 0)
        result = 31 * result + (SDSD?.hashCode() ?: 0)
        result = 31 * result + (CVNN?.hashCode() ?: 0)
        result = 31 * result + (CVSD?.hashCode() ?: 0)
        result = 31 * result + (MedianNN?.hashCode() ?: 0)
        result = 31 * result + (MadNN?.hashCode() ?: 0)
        result = 31 * result + (MCVNN?.hashCode() ?: 0)
        result = 31 * result + (IQR?.hashCode() ?: 0)
        result = 31 * result + (pNN50?.hashCode() ?: 0)
        result = 31 * result + (pNN20?.hashCode() ?: 0)
        result = 31 * result + (LF?.hashCode() ?: 0)
        result = 31 * result + (HF?.hashCode() ?: 0)
        result = 31 * result + (ULF?.hashCode() ?: 0)
        result = 31 * result + (VLF?.hashCode() ?: 0)
        result = 31 * result + (VHF?.hashCode() ?: 0)
        result = 31 * result + (LFHF?.hashCode() ?: 0)
        result = 31 * result + (LFn?.hashCode() ?: 0)
        result = 31 * result + (HFn?.hashCode() ?: 0)
        result = 31 * result + (LnHF?.hashCode() ?: 0)
        return result
    }
}
