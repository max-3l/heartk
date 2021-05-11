package heartk

import heartk.ppg.PPG
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import java.io.File
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class PPGTest {
    var timestamps: MutableList<Long> = mutableListOf()
    var ppg: MutableList<Float> = mutableListOf()
    var peaks: MutableList<Boolean> = mutableListOf()

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
    }

    @Test fun shouldComputeAverages() {
        val originalArray = FloatArray(100) { 1F }
        assertArrayEquals(PPG.movingAverage(originalArray, 10).toTypedArray(), originalArray.toTypedArray())
    }
}
