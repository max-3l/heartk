package heartk.configuration

/**
 * Generates and sanitizes configurations
 */
object Configurator {

    /**
     * Sanitizes the parameters for the butterworth filter.
     *
     * The sampling rate should be lower than double the highcut.
     *
     * Reimplementation of neurokits _signal_filter_sanitize method.
     * All intellectual properties belong to neurokit2.
     *
     * [https://github.com/neuropsychology/NeuroKit]
     *
     * @param lowcut
     * @param highcut
     * @param sampling_rate
     * @param normalize
     *
     * @return a pair of frequencies and a filter type
     */
    fun signalFilterSanitize(lowcut: Double? = null, highcut: Double? = null, sampling_rate: Double = 1000.0, normalize: Boolean = false): Pair<Array<Double>, String> {

        var lowcutPar = lowcut
        var highcutPar = highcut

        var freqs: Array<Double> = arrayOf()
        var filterType = ""

        if (lowcutPar == 0.0) {
            lowcutPar = null
        }
        if (highcutPar == 0.0) {
            highcutPar = null
        }

        if (lowcutPar != null && highcutPar != null) {
            if (lowcutPar > highcutPar) {
                filterType = "bandstop"
            } else {
                filterType = "bandpass"
            }
            freqs = arrayOf(lowcutPar, highcutPar)
        } else if (lowcutPar != null) {
            freqs = arrayOf(lowcutPar)
            filterType = "highpass"
        } else if (highcutPar != null) {
            freqs = arrayOf(highcutPar)

            filterType = "lowpass"
        }

        if (normalize) {
            freqs.map { it / (sampling_rate / 2) }
        }
        return Pair(freqs, filterType)
    }
        
}