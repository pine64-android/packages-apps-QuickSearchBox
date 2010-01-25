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

import java.util.ArrayList;


/**
 * Interface for logging implementations.
 */
public interface Logger {

    public static final int SEARCH_METHOD_BUTTON = 0;
    public static final int SEARCH_METHOD_KEYBOARD = 1;

    /**
     * Called when QSB has started.
     *
     * @param latency User-visible start-up latency in milliseconds.
     */
    void logStart(int latency, String intentSource, Source currentSearchSource,
            ArrayList<Source> orderedSources);

    /**
     * Called when a suggestion is clicked.
     *
     * @param position 0-based position of the suggestion in the UI.
     * @param suggestionCursor all the suggestions shown in the UI.
     * @param queriedSources all sources that were queried to produce the suggestions in
     *        {@code suggestionCursor}, ordered by rank.
     */
    void logSuggestionClick(int position, SuggestionCursor suggestionCursor,
            ArrayList<Source> queriedSources);

    /**
     * The user launched a search.
     *
     * @param searchSource The search source. {@code null} means web search.
     * @param startMethod One of {@link #SEARCH_METHOD_BUTTON} or {@link #SEARCH_METHOD_KEYBOARD}.
     * @param numChars The number of characters in the query.
     */
    void logSearch(Source searchSource, int startMethod, int numChars);

    /**
     * The user launched a voice search.
     *
     * @param searchSource The search source. {@code null} means web search.
     */
    void logVoiceSearch(Source searchSource);

    /**
     * The user left QSB without performing any action (click suggestions, search or voice search).
     *
     * @param suggestionCursor all the suggestions shown in the UI when the user left
     */
    void logExit(SuggestionCursor suggestionCursor);

    void logWebLatency();

}