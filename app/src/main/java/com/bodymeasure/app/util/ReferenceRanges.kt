package com.bodymeasure.app.util

/** How a zone is coloured. Kept free of Compose so the ranges stay plain data. */
enum class ZoneTone { Low, Mid, High, Alert, Strong, Muted }

/** One band on a reference scale: [from] inclusive, [to] exclusive. */
data class RangeZone(val label: String, val from: Double, val to: Double, val tone: ZoneTone)

/**
 * A drawn scale from [min] to [max] split into contiguous [zones].
 *
 * The drawn bounds are chosen so that no zone is too narrow to label on a phone,
 * not to cover every possible value; anything outside is pinned to the nearest
 * end, and the number printed beside the bar is always the real one.
 */
data class RangeScale(val min: Double, val max: Double, val zones: List<RangeZone>) {

    fun zoneFor(value: Double): RangeZone =
        zones.firstOrNull { value >= it.from && value < it.to }
            ?: if (value < zones.first().from) zones.first() else zones.last()

    /** Position along the bar, 0..1, clamped. */
    fun position(value: Double): Float =
        ((value - min) / (max - min)).coerceIn(0.0, 1.0).toFloat()

    /** Share of the bar a zone occupies, for laying zones out by weight. */
    fun width(zone: RangeZone): Float =
        ((zone.to.coerceAtMost(max) - zone.from.coerceAtLeast(min)) / (max - min)).toFloat()
}

/**
 * Reference scales for the Body composition card.
 *
 * Every boundary here is one the app already uses elsewhere — [Bmi.categorize],
 * [BodyFat.categorize], [BodyAnalysis.ffmiBand] — so the bar can never disagree
 * with the label printed next to it.
 *
 * The middle band does not mean the same thing on each scale, and the labels say
 * so rather than calling all three "average":
 *  - BMI's middle is the WHO *healthy* range. The population average sits above
 *    it — most adults in surveyed countries are in the overweight band.
 *  - Body fat's middle is ACE's "average" band, which is what it sounds like.
 *  - FFMI's middle is the typical untrained range.
 */
object ReferenceRanges {

    fun bmi(): RangeScale = RangeScale(
        min = 15.0, max = 40.0,
        zones = listOf(
            RangeZone("Under", 0.0, 18.5, ZoneTone.Low),
            RangeZone("Healthy", 18.5, 25.0, ZoneTone.Mid),
            RangeZone("Over", 25.0, 30.0, ZoneTone.High),
            RangeZone("Obese", 30.0, Double.MAX_VALUE, ZoneTone.Alert)
        )
    )

    /** ACE categories, collapsed to three: essential/athletes/fitness read as lean. */
    fun bodyFat(sex: Sex): RangeScale = when (sex) {
        Sex.Male -> RangeScale(
            min = 5.0, max = 35.0,
            zones = listOf(
                RangeZone("Lean", 0.0, 18.0, ZoneTone.Low),
                RangeZone("Average", 18.0, 25.0, ZoneTone.Mid),
                RangeZone("High", 25.0, Double.MAX_VALUE, ZoneTone.High)
            )
        )
        Sex.Female -> RangeScale(
            min = 12.0, max = 42.0,
            zones = listOf(
                RangeZone("Lean", 0.0, 25.0, ZoneTone.Low),
                RangeZone("Average", 25.0, 32.0, ZoneTone.Mid),
                RangeZone("High", 32.0, Double.MAX_VALUE, ZoneTone.High)
            )
        )
    }

    /**
     * Above-average, athletic and exceptional are one zone here; the finer band
     * is printed as the caption, so the detail is not lost.
     */
    fun ffmi(sex: Sex): RangeScale = when (sex) {
        Sex.Male -> RangeScale(
            min = 15.0, max = 26.0,
            zones = listOf(
                RangeZone("Below", 0.0, 18.0, ZoneTone.Muted),
                RangeZone("Average", 18.0, 20.0, ZoneTone.Low),
                RangeZone("Above", 20.0, Double.MAX_VALUE, ZoneTone.Strong)
            )
        )
        Sex.Female -> RangeScale(
            min = 12.0, max = 20.0,
            zones = listOf(
                RangeZone("Below", 0.0, 14.0, ZoneTone.Muted),
                RangeZone("Average", 14.0, 15.5, ZoneTone.Low),
                RangeZone("Above", 15.5, Double.MAX_VALUE, ZoneTone.Strong)
            )
        )
    }

    /** Boundary text for a zone, e.g. "18.5–25", "<18.5", "30+". */
    fun rangeText(zone: RangeZone, scale: RangeScale): String {
        fun f(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else BodyAnalysis.format1(v)
        return when {
            zone.from <= scale.min -> "<${f(zone.to)}"
            zone.to >= scale.max -> "${f(zone.from)}+"
            else -> "${f(zone.from)}–${f(zone.to)}"
        }
    }
}
