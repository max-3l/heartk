package heartk

import heartk.hrv.HrvHr
import heartk.ppg.PPG
import heartk.hrv.HRV
import heartk.hrv.HrvFrequency
import heartk.hrv.HrvNonlinear
import heartk.utils.std
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import java.io.File
import java.util.Arrays
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class PPGTest {
    var timestamps: MutableList<Long> = mutableListOf()
    var ppg: MutableList<Float> = mutableListOf()
    var peaks: MutableList<Boolean> = mutableListOf()
    private val withOutput = true
    private val outDir = File("./output/")

    fun writeOutput(name: String, content: String) {
        if (withOutput) {
            File(outDir, name).writeText(content)
        }
    }

    @Before
    fun loadPPGSamples(){
        val reader = PPGTest::class.java.classLoader.getResourceAsStream("ppg_sample.csv")!!.bufferedReader()
        reader.readLine()
        reader.forEachLine { line ->
            val splittedLine = line.split(",")
            this.timestamps.add(splittedLine[0].toLong())
            this.ppg.add(splittedLine[1].toFloat())
            this.peaks.add(splittedLine[2].toBoolean())
        }
    }

    @Before
    fun initializeDirectories(){
        if (withOutput) {
            this.outDir.mkdirs()
        }
    }

    @Test fun shouldLoadCSV() {
        assertTrue(this.ppg.size > 0, "read ppg signal should be longer than 0")
    }

    @Test fun shouldFindPeaks() {
        val output = PPG.processSignal(this.ppg.toFloatArray(), 1000.0)
        // Allow to find peaks with an error of +-1
        assertTrue(abs(155 - output.sumBy{ if (it) 1 else 0 }) < 2)
    }

    @Test fun shoulFindPeaksFast() {
        val ppgSignal = this.ppg.toFloatArray()
        var processingSum = 0L
        for (i in (0 .. 1000)) {
            val before = System.nanoTime()
            PPG.processSignal(ppgSignal, 1000.0)
            val after = System.nanoTime()
            processingSum += after - before
        }
        println("Time needed to process signal: ${(processingSum / 1000) / 1e6} ms")
        assertTrue(processingSum / 1000 < 1e8.toLong())
        writeOutput("Peaks.csv", fString)
    }

    @Test fun shouldComputeHR() {
        val peaksSignal = PPG.processSignal(this.ppg.toFloatArray(), 1000.0)
        val hrSignal = HrvHr.computeHR(peaksSignal, 1000.0)
        val fString = hrSignal.fold("") {
                current, element -> current + "\n" + element
        }
        writeOutput("HR.csv", fString)
    }

    @Test fun shouldComputeRRIntervals() {
        val peaksSignal = PPG.processSignal(this.ppg.toFloatArray(), 1000.0)
        val rri = HRV.getRRIntervals(peaksSignal, 1000.0, true)
        val fString = rri.fold("") {
                current, element -> current + "\n" + element
        }
        writeOutput("RRI.csv", fString)
    }

    @Test fun shouldOutputCorrectBins() {
        val bins = doubleArrayOf(
            0.0 ,   0.5,   1.0 ,   1.5,   2.0 ,   2.5,   3.0 ,   3.5,   4.0 ,
            4.5,   5.0 ,   5.5,   6.0 ,   6.5,   7.0 ,   7.5,   8.0 ,   8.5,
            9.0 ,   9.5,  10.0 ,  10.5,  11.0 ,  11.5,  12.0 ,  12.5,  13.0 ,
            13.5,  14.0 ,  14.5,  15.0 ,  15.5,  16.0 ,  16.5,  17.0 ,  17.5,
            18.0 ,  18.5,  19.0 ,  19.5,  20.0 ,  20.5,  21.0 ,  21.5,  22.0 ,
            22.5,  23.0 ,  23.5,  24.0 ,  24.5, -25.0 , -24.5, -24.0 , -23.5,
            -23.0 , -22.5, -22.0 , -21.5, -21.0 , -20.5, -20.0 , -19.5, -19.0 ,
            -18.5, -18.0 , -17.5, -17.0 , -16.5, -16.0 , -15.5, -15.0 , -14.5,
            -14.0 , -13.5, -13.0 , -12.5, -12.0 , -11.5, -11.0 , -10.5, -10.0 ,
            -9.5,  -9.0 ,  -8.5,  -8.0 ,  -7.5,  -7.0 ,  -6.5,  -6.0 ,  -5.5,
            -5.0 ,  -4.5,  -4.0 ,  -3.5,  -3.0 ,  -2.5,  -2.0 ,  -1.5,  -1.0 ,
            -0.5)
       assertArrayEquals(bins.toTypedArray(), HrvFrequency.fftFrequencies(100, 50.0).toTypedArray())
    }

    @Test fun shouldComputePSDUsingWelch() {
        val peaksSignal = PPG.processSignal(this.ppg.toFloatArray(), 1000.0)
        val rri = HRV.getRRIntervals(peaksSignal, 1000.0, true)
        val (frequencies, power) = HrvFrequency.psdWelch(rri, 1000.0, 10000, normalize=false, returnOneSided = true, windowType = "hannCSV")
        val psd = power.fold("welch\n") {
                current, element -> current + element + "\n"
        }
        val freqs = frequencies.fold("freqs\n") {
                current, element -> current + element + "\n"
        }
        writeOutput("welch.csv", psd)
        writeOutput("freqs.csv", freqs)
    }

    @Test fun shouldComputeFrequencyFeatures() {
        val peaksSignal = PPG.processSignal(this.ppg.toFloatArray(), 1000.0)
        val rri = HRV.getRRIntervals(peaksSignal, 1000.0, true, "quadratic")
        val frequencyFeatures = HrvFrequency.getFeatures(rri, 1000.0)
        val freqFeatures = frequencyFeatures.entries.fold("feature, value\n") {
                current, element -> current + element.key + ", " + element.value + "\n"
        }
        writeOutput("freqFeatures.csv", freqFeatures)
    }

    @Test fun shouldComputeNonLinearFeatures() {
        val peaksSignal = PPG.processSignal(this.ppg.toFloatArray(), 1000.0)
        val rri = HRV.getRRIntervals(peaksSignal, 1000.0, false)
        val nonLinearFeatures = HrvNonlinear.getFeatures(rri)
        val nonLinearFeaturesString = nonLinearFeatures.entries.fold("feature, value\n") {
                current, element -> current + element.key + ", " + element.value + "\n"
        }
        writeOutput("nonLinearFeatures.csv", nonLinearFeaturesString)
    }

    @Test fun shouldComputeAverages() {
        val originalArray = FloatArray(100) { 1F }
        assertArrayEquals(PPG.movingAverage(originalArray, 10).toTypedArray(), originalArray.toTypedArray())
    }

    @Test fun shouldComputeCorrectStd() {
        val array = DoubleArray(10) { it.plus(1.0) }
        assertEquals(2.8722813232690143, std(array, 0))
        assertEquals(3.0276503540974917, std(array, 1))
        assertEquals(3.2113081446662823, std(array, 2))
    }

    @Test fun shouldReturnHannWindow() {
        val tolerance = 1e-8
        val window = HrvFrequency.hannWindow(20)
        val expectedWindow = doubleArrayOf(
            0.0       , 0.02709138, 0.10542975, 0.22652592, 0.37725726,
            0.54128967, 0.70084771, 0.83864079, 0.93973688, 0.99318065,
            0.99318065, 0.93973688, 0.83864079, 0.70084771, 0.54128967,
            0.37725726, 0.22652592, 0.10542975, 0.02709138, 0.0
        )
        window.forEachIndexed() { index, element ->
            assertTrue(abs(element - expectedWindow[index]) <= tolerance,
                "Window at [$index] should be ${expectedWindow[index]} but was $element. The difference of ${abs(element - expectedWindow[index])} is greater" +
                        " than the allowed tolerance of $tolerance" )
        }
    }
}
