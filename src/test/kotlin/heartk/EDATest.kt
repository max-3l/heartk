package heartk

import heartk.eda.EDA
import heartk.eda.EdaPhasicTonicFeatures
import org.junit.Test
import java.io.File

class EDATest {
    private val withOutput = true
    private val outDir = "./output"

    private fun writeOutput(name: String, content: String) {
        if (withOutput) {
            File(outDir, name).writeText(content)
        }
    }

    @Test fun splitPhasicTonic() {
        val eda = mutableListOf<Double>()
        val reader = PPGTest::class.java.classLoader.getResourceAsStream("gsr_sample.csv")!!.bufferedReader()
        reader.readLine()
        reader.forEachLine { line ->
            val splittedLine = line.split(",")
            eda.add(splittedLine[1].toDouble().toDouble())
        }
        val cleanedSignal = EdaPhasicTonicFeatures.cleanSignal(eda.toDoubleArray(), 51.2)
        val phasicSignal = EdaPhasicTonicFeatures.phasicSignal(cleanedSignal, 51.2)
        val tonicSignal = EdaPhasicTonicFeatures.tonicSignal(cleanedSignal, 51.2)
        val peaks = EdaPhasicTonicFeatures.filterPeaks(EdaPhasicTonicFeatures.findPeaks(phasicSignal), 51.2)

        val signalString = phasicSignal.foldIndexed("phasic,tonic,eda,cleanedEda\n") { index, current, el ->
            "$current$el,${tonicSignal[index]},${eda[index]},${cleanedSignal[index]}\n"
        }
        val peaksString = peaks.peaks.foldIndexed("peak, start, end, height\n") { index, current, el ->
            "$current$el,${peaks.peaksStart[index]},${peaks.peaksEnd[index]},${peaks.peaksHeight[index]}\n"
        }
        writeOutput("processedEdaPhasicTonic.csv", signalString)
        writeOutput("edaPeaks.csv", peaksString)
    }

    @Test fun shouldComputeFeatures() {
        val eda = mutableListOf<Double>()
        val reader = PPGTest::class.java.classLoader.getResourceAsStream("gsr_sample.csv")!!.bufferedReader()
        reader.readLine()
        reader.forEachLine { line ->
            val splittedLine = line.split(",")
            eda.add(splittedLine[1].toDouble().toDouble())
        }
        val features = EDA.processFeatures(eda.toDoubleArray(), 51.2)
    }
}