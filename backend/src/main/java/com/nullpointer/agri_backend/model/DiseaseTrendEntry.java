package com.nullpointer.agri_backend.model;

/**
 * One disease's report count within the trend window for a district, plus
 * whether that count crosses the alert threshold.
 *
 * This is what turns the regional-alerts feature from a plain running
 * total into an actual outbreak signal: {@code alert = true} means enough
 * farmers in this district reported the same disease within the window to
 * be worth surfacing, not just "this disease has ever been seen here."
 *
 * @param count      number of matching diagnosis reports within the window
 * @param alert      true if count >= the configured alert threshold
 * @param windowDays size of the trailing window this count covers, so
 *                   clients don't have to hardcode it to interpret count
 */
public record DiseaseTrendEntry(long count, boolean alert, int windowDays) {
}
