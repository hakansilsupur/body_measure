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
 * training. That is the faithful form and the app uses it whenever a wrist
 * measurement exists, falling back to the recorded chest otherwise.
 *
 * The fallback is noticeably harsher for anyone whose chest is below the
 * classical figure for their frame: every other target is a fraction of the
 * chest, so a smaller chest drags them all down together. Wrist anchoring
 * removes that feedback loop, which is also why the targets stop moving as
 * training changes the chest.
 */
enum class ProportionSite(val label: String, val fractionOfChest: Double) {
    Chest("Chest", 1.00),
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

/** Where a wrist sits relative to the span McCallum's rule was written for. */
enum class AnchorRange { Below, Calibrated, Above }

object ClassicalProportions {

    /** McCallum derives the classical chest from wrist girth. */
    const val CHEST_PER_WRIST = 6.5

    /**
     * McCallum wrote the 6.5x rule for the trainees in front of him, whose
     * wrists ran roughly 6.5-7.5 in. It is a single linear coefficient with no
     * claim to hold outside that span.
     *
     * This matters more here than a normal extrapolation would, because the
     * chest it produces is the denominator for every other site. One wrist
     * outside the span does not skew one row, it moves all seven the same way
     * at once — and seven bars agreeing looks like a finding about the body
     * rather than an artifact of the anchor. So the card says so outright.
     */
    const val WRIST_CALIBRATED_MIN_CM = 16.5
    const val WRIST_CALIBRATED_MAX_CM = 19.0

    fun wristRange(wristCm: Double): AnchorRange = when {
        wristCm < WRIST_CALIBRATED_MIN_CM -> AnchorRange.Below
        wristCm > WRIST_CALIBRATED_MAX_CM -> AnchorRange.Above
        else -> AnchorRange.Calibrated
    }

    /** Where the ratios are being scaled from, and which measurement supplied it. */
    sealed interface Anchor {
        val chestCm: Double

        /** The faithful form: chest derived from frame size. */
        data class FromWrist(val wristCm: Double, override val chestCm: Double) : Anchor

        /** Fallback when no wrist is recorded. */
        data class FromChest(override val chestCm: Double) : Anchor
    }

    /** Prefers wrist; falls back to the recorded chest. Null if neither is usable. */
    fun anchor(wristCm: Double?, chestCm: Double?): Anchor? {
        wristCm?.takeIf { it > 0 }?.let {
            return Anchor.FromWrist(it, it * CHEST_PER_WRIST)
        }
        return chestCm?.takeIf { it > 0 }?.let { Anchor.FromChest(it) }
    }

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
        anchor: Anchor,
        chestCm: Double?,
        neckCm: Double?,
        armCm: Double?,
        waistCm: Double?,
        hipCm: Double?,
        thighCm: Double?,
        calfCm: Double?
    ): List<ProportionRow> {
        val actuals = mapOf(
            ProportionSite.Chest to chestCm,
            ProportionSite.Neck to neckCm,
            ProportionSite.Arm to armCm,
            ProportionSite.Waist to waistCm,
            ProportionSite.Hip to hipCm,
            ProportionSite.Thigh to thighCm,
            ProportionSite.Calf to calfCm
        )
        return ProportionSite.entries.mapNotNull { site ->
            // Chest is the anchor when no wrist was recorded, so comparing it
            // against itself would always read exactly 1.00 and tell nobody
            // anything. With a wrist it is a real comparison and worth showing.
            if (site == ProportionSite.Chest && anchor is Anchor.FromChest) {
                return@mapNotNull null
            }
            val actual = actuals[site]?.takeIf { it > 0 } ?: return@mapNotNull null
            val classic = classicFor(site, anchor.chestCm) ?: return@mapNotNull null
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
