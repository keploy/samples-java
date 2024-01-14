package main

import (
	"io"
	"log"
	"net/http"
	"strconv"
	"strings"
)

// counter for requests
var counter int

func generateTraceID(testNo string) string {
	// Remove hyphens and spaces from testNo
	testNoClean := strings.ReplaceAll(testNo, "-", "")
	testNoClean = strings.ReplaceAll(testNoClean, " ", "")

	// Calculate the remaining length for padding
	remainingLength := 32 - len(testNoClean)

	// Pad the remaining string with zeros
	traceID := testNoClean + strings.Repeat("a", remainingLength)
	return traceID
}

func main() {
	targetServerURL := "http://localhost:9966" // Replace with your target server URL

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		counter++
		// log the request
		log.Printf("received request %s %s", r.Method, r.URL.Path)
		client := &http.Client{}

		// Create a new request
		req, err := http.NewRequest(r.Method, targetServerURL+r.URL.Path, r.Body)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}

		// Copy the headers from the original request to the new request
		for key, values := range r.Header {
			for _, value := range values {
				req.Header.Add(key, value)
			}
		}

		// Add the traceparent header
		testNo := "00" + strconv.Itoa(counter)
		traceID := generateTraceID(testNo)

		// create a 32 hex digit trace-id. The string should contain the entire testNo but the rest is random
		// the trace-id should not contain - or spaces

		req.Header.Add("traceparent", "00-"+traceID+"-c532cb4098ac3dd2-01")
		// req.Header.Add("traceparent", "00-5bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")

		// Forward the request to the target server
		resp, err := client.Do(req)
		if err != nil {
			http.Error(w, err.Error(), http.StatusBadGateway)
			return
		}
		defer resp.Body.Close()

		// Copy the response headers and body to the original response writer
		for key, values := range resp.Header {
			for _, value := range values {
				w.Header().Add(key, value)
			}
		}
		w.WriteHeader(resp.StatusCode)
		io.Copy(w, resp.Body)
	})

	log.Println("Starting proxy server on :8080")
	log.Fatal(http.ListenAndServe(":8080", nil))
}
