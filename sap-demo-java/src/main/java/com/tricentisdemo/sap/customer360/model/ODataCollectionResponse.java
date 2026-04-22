package com.tricentisdemo.sap.customer360.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Envelope for an SAP OData v2 collection response:
 *
 * <pre>
 * {
 *   "d": {
 *     "results": [ { ... }, { ... } ],
 *     "__count": "42",
 *     "__next": "...pagination link..."
 *   }
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataCollectionResponse<T> {

    @JsonProperty("d")
    private Data<T> data;

    public Data<T> getData() {
        return data;
    }

    public void setData(Data<T> data) {
        this.data = data;
    }

    public List<T> getResults() {
        return data == null ? List.of() : data.getResults();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data<T> {

        @JsonProperty("results")
        private List<T> results;

        @JsonProperty("__count")
        private String count;

        @JsonProperty("__next")
        private String nextLink;

        public List<T> getResults() {
            return results == null ? List.of() : results;
        }

        public void setResults(List<T> results) {
            this.results = results;
        }

        public String getCount() {
            return count;
        }

        public void setCount(String count) {
            this.count = count;
        }

        public String getNextLink() {
            return nextLink;
        }

        public void setNextLink(String nextLink) {
            this.nextLink = nextLink;
        }
    }
}
