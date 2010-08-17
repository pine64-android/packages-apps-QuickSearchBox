/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quicksearchbox;

import com.google.common.annotations.VisibleForTesting;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains all {@link SuggestionCursor} objects that have been reported.
 */
public class BlendedSuggestions extends Suggestions {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.BlendedSuggestions";

    private static int sId = 0;
    // Object ID for debugging
    private final int mId;

    /** The sources that are expected to report. */
    private final List<Corpus> mExpectedCorpora;

    private Corpus mSingleCorpusFilter;

    /**
     * All {@link SuggestionCursor} objects that have been published so far,
     * in the order that they were published.
     * This object may only be accessed on the UI thread.
     * */
    private final ArrayList<CorpusResult> mCorpusResults;

    private final Promoter<CorpusResult> mPromoter;

    /**
     * Creates a new empty Suggestions.
     *
     * @param expectedCorpora The sources that are expected to report.
     */
    public BlendedSuggestions(Promoter<CorpusResult> promoter, int maxPromoted,
            String query, List<Corpus> expectedCorpora) {
        super(query, maxPromoted);
        mPromoter = promoter;
        mExpectedCorpora = expectedCorpora;
        mCorpusResults = new ArrayList<CorpusResult>(mExpectedCorpora.size());
        mId = sId++;
        if (DBG) {
            Log.d(TAG, "new Suggestions [" + mId + "] query \"" + query
                    + "\" expected corpora: " + mExpectedCorpora);
        }
    }

    public List<Corpus> getExpectedCorpora() {
        return mExpectedCorpora;
    }

    /**
     * Gets the number of corpora that are expected to report.
     */
    @VisibleForTesting
    public int getExpectedResultCount() {
        return mExpectedCorpora.size();
    }

    /**
     * Gets the set of corpora that have reported results to this suggestions set.
     *
     * @return A collection of corpora.
     */
    public Set<Corpus> getIncludedCorpora() {
        HashSet<Corpus> corpora = new HashSet<Corpus>();
        for (CorpusResult result : mCorpusResults) {
            corpora.add(result.getCorpus());
        }
        return corpora;
    }

    /**
     * Closes all the source results and unregisters all observers.
     */
    @Override
    public void close() {
        if (DBG) Log.d(TAG, "close() [" + mId + "]");

        for (CorpusResult result : mCorpusResults) {
            result.close();
        }
        mCorpusResults.clear();
        super.close();
    }

    /**
     * Checks whether all sources have reported.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     */
    @Override
    public boolean isDone() {
        // TODO: Handle early completion because we have all the results we want.
        return mCorpusResults.size() >= mExpectedCorpora.size();
    }

    /**
     * Adds a list of corpus results. Must be called on the UI thread, or before this
     * object is seen by the UI thread.
     */
    public void addCorpusResults(List<CorpusResult> corpusResults) {
        if (isClosed()) {
            for (CorpusResult corpusResult : corpusResults) {
                corpusResult.close();
            }
            return;
        }

        for (CorpusResult corpusResult : corpusResults) {
            if (DBG) {
                Log.d(TAG, "addCorpusResult["+ mId + "] corpus:" +
                        corpusResult.getCorpus().getName() + " results:" + corpusResult.getCount());
            }
            if (!mQuery.equals(corpusResult.getUserQuery())) {
              throw new IllegalArgumentException("Got result for wrong query: "
                    + mQuery + " != " + corpusResult.getUserQuery());
            }
            mCorpusResults.add(corpusResult);
        }
        notifyDataSetChanged();
    }

    @Override
    protected SuggestionCursor buildPromoted() {
        if (mSingleCorpusFilter == null) {
            ListSuggestionCursor promoted = new ListSuggestionCursorNoDuplicates(mQuery);
            if (mPromoter == null) {
                return promoted;
            }
            mPromoter.pickPromoted(getShortcuts(), mCorpusResults, getMaxPromoted(), promoted);
            if (DBG) {
                Log.d(TAG, "pickPromoted(" + getShortcuts() + "," + mCorpusResults + ","
                        + getMaxPromoted() + ") = " + promoted);
            }
            return promoted;
        } else {
            SuggestionCursor promoted = getCorpusResult(mSingleCorpusFilter);
            if (promoted == null) {
                promoted = new ListSuggestionCursor(mQuery);
            }
            return promoted;
        }
    }

    public CorpusResult getCorpusResult(Corpus corpus) {
        for (CorpusResult result : mCorpusResults) {
            if (result.getCorpus().equals(corpus)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Gets the number of source results.
     * Must be called on the UI thread, or before this object is seen by the UI thread.
     */
    public int getResultCount() {
        if (isClosed()) {
            throw new IllegalStateException("Called getSourceCount() when closed.");
        }
        return mCorpusResults == null ? 0 : mCorpusResults.size();
    }

    public void filterByCorpus(Corpus singleCorpus) {
        if (mSingleCorpusFilter == singleCorpus) {
            return;
        }
        mSingleCorpusFilter = singleCorpus;
        if ((mExpectedCorpora.size() == 1) && (mExpectedCorpora.get(0) == singleCorpus)) {
            return;
        }
        notifyDataSetChanged();
    }

    @Override
    public String toString() {
        return "Suggestions{expectedCorpora=" + mExpectedCorpora
                + ",mCorpusResults.size()=" + mCorpusResults.size() + "}";
    }

}