package com.bodymeasure.app.util

/**
 * The "classical" physique proportions of mid-20th-century bodybuilding, as
 * set out by John McCallum in *Keys to Progress* and rooted in the physiques
 * of Eugen Sandow and Steve Reeves. Each site is a fixed fraction of the chest.
 *
 * These are an aesthetic convention from one era of one subculture. They are
 * not a health standard, they carry no medical thresholds, and the waist figure
 * in particular is far narrower than most people carry. The app shows them as a
 * reference to compare against, never as a target to hit — which is why nothing
 * here returns a pass/fail verdict or a "good"/"bad" band.
 *
 * McCallum anchors the chest itself on wrist girth (chest = 6.5 x wrist), the
 * idea being that wrist reflects skeletal frame and does not change with
 * training. The app does not record wrist, so the ratios below are anchored on
 * the recorded chest instead: "given the chest you have, here is where the
 * classical physique puts everything else".
 */
enum class ProportionSite(val label: String, val fractionOfChest: Double) {
    Neck("Neck", 0.37),
    Arm("Arm", 0.36),
    Waist("Waist", 0.70),
    Hip("Hip", 0.85),
    Thigh("Thigh", 0.53),
    Calf("Calf", 0.34)
}

/** One site compared against its classical figure. */
data class ProportionRow(
    val site: ProportionSite,
    val actualCm: Double,
    val classicCm: Double
) {
    val deltaCm: Double get() = actualCm - classicCm

    /** 1.0 means the measurement matches the classical figure exactly. */
    val ratio: Double get() = if (classicCm > 0) actualCm / classicCm else 0.0
}

object ClassicalProportions {

    /** The classical figure for [site] given a chest girth, or null if unusable. */
    fun classicFor(site: ProportionSite, chestCm: Double?): Double? {
        if (chestCm == null || chestCm <= 0) return null
        return chestCm * site.fractionOfChest
    }

    /**
     * Builds a comparison row per site that has both a recorded measurement and
     * a derivable classical figure. Sites the user hasn't measured are skipped.
     */
    fun rows(
        chestCm: Double?,
        neckCm: Double?,
        armCm: Double?,
        waistCm: Double?,
        hipCm: Double?,
        thighCm: Double?,
        calfCm: Double?
    ): List<ProportionRow> {
        val actuals = mapOf(
            ProportionSite.Neck to neckCm,
            ProportionSite.Arm to armCm,
            ProportionSite.Waist to waistCm,
            ProportionSite.Hip to hipCm,
            ProportionSite.Thigh to thighCm,
            ProportionSite.Calf to calfCm
        )
        return ProportionSite.entries.mapNotNull { site ->
            val actual = actuals[site]?.takeIf { it > 0 } ?: return@mapNotNull null
            val classic = classicFor(site, chestCm) ?: return@mapNotNull null
            ProportionRow(site, actual, classic)
        }
    }

    /**
     * The classical symmetry rule holds arm, neck and calf at roughly equal
     * girth. Returns the spread between the largest and smallest of whichever
     * of the three are recorded, or null if fewer than two are.
     */
    fun symmetrySpreadCm(neckCm: Double?, armCm: Double?, calfCm: Double?): Double? {
        val present = listOfNotNull(neckCm, armCm, calfCm).filter { it > 0 }
        if (present.size < 2) return null
        return present.max() - present.min()
    }
}
