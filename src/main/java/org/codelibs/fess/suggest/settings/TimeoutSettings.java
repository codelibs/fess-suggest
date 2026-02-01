/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.suggest.settings;

/**
 * Timeout settings for various operations in the suggest system.
 * This class provides configuration for search, index, bulk, indices, cluster, and scroll timeouts.
 */
public class TimeoutSettings {
    /** Search timeout. */
    protected String searchTimeout = "15s";
    /** Index timeout. */
    protected String indexTimeout = "1m";
    /** Bulk timeout. */
    protected String bulkTimeout = "1m";
    /** Indices timeout. */
    protected String indicesTimeout = "1m";
    /** Cluster timeout. */
    protected String clusterTimeout = "1m";
    /** Scroll timeout. */
    protected String scrollTimeout = "1m";

    /**
     * Constructs a new {@link TimeoutSettings} with default values.
     */
    public TimeoutSettings() {
        // nothing
    }

    /**
     * Gets the search timeout.
     * @return The search timeout.
     */
    public String getSearchTimeout() {
        return searchTimeout;
    }

    /**
     * Sets the search timeout.
     * @param timeout The search timeout.
     */
    public void setSearchTimeout(final String timeout) {
        this.searchTimeout = timeout;
    }

    /**
     * Gets the index timeout.
     * @return The index timeout.
     */
    public String getIndexTimeout() {
        return indexTimeout;
    }

    /**
     * Sets the index timeout.
     * @param timeout The index timeout.
     */
    public void setIndexTimeout(final String timeout) {
        this.indexTimeout = timeout;
    }

    /**
     * Gets the bulk timeout.
     * @return The bulk timeout.
     */
    public String getBulkTimeout() {
        return bulkTimeout;
    }

    /**
     * Sets the bulk timeout.
     * @param timeout The bulk timeout.
     */
    public void setBulkTimeout(final String timeout) {
        this.bulkTimeout = timeout;
    }

    /**
     * Gets the indices timeout.
     * @return The indices timeout.
     */
    public String getIndicesTimeout() {
        return indicesTimeout;
    }

    /**
     * Sets the indices timeout.
     * @param timeout The indices timeout.
     */
    public void setIndicesTimeout(final String timeout) {
        this.indicesTimeout = timeout;
    }

    /**
     * Gets the cluster timeout.
     * @return The cluster timeout.
     */
    public String getClusterTimeout() {
        return clusterTimeout;
    }

    /**
     * Sets the cluster timeout.
     * @param timeout The cluster timeout.
     */
    public void setClusterTimeout(final String timeout) {
        this.clusterTimeout = timeout;
    }

    /**
     * Gets the scroll timeout.
     * @return The scroll timeout.
     */
    public String getScrollTimeout() {
        return scrollTimeout;
    }

    /**
     * Sets the scroll timeout.
     * @param timeout The scroll timeout.
     */
    public void setScrollTimeout(final String timeout) {
        this.scrollTimeout = timeout;
    }
}
