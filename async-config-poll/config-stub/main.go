// Command config-stub is a stand-in for a central config service. It backs the
// app's boot-blocking config fetch and its background watch long-poll:
//
//   - GET /v1/buckets/{name}                        -> current config (version 1)
//   - GET /v1/buckets/app-config?watch=true&version=N -> long-poll: returns the
//     NEXT version (N+1), simulating a config change on each watch poll.
//
// By default a watch poll returns immediately (the periodic-poller scenario).
// Set POLL_HOLD_SECONDS>0 to model a real long-poll with a server timeout: the
// watch=true request is HELD open that long before delivering the next version
// (the httpPoll scenario), so Keploy records its open-duration as pollDurationMs.
//
// It is hit only during `keploy record`. At replay time Keploy serves the
// recorded responses instead, so this stub does not need to be running.
package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

// pollHold is the long-poll server timeout: a watch=true request is held open
// this long before delivering the next version. Default 0 (respond
// immediately); override with POLL_HOLD_SECONDS.
func pollHold() time.Duration {
	if s := os.Getenv("POLL_HOLD_SECONDS"); s != "" {
		if n, err := strconv.Atoi(s); err == nil {
			return time.Duration(n) * time.Second
		}
	}
	return 0
}

func main() {
	hold := pollHold()
	http.HandleFunc("/v1/buckets/", func(w http.ResponseWriter, r *http.Request) {
		name := strings.TrimPrefix(r.URL.Path, "/v1/buckets/")
		q := r.URL.Query()

		version := 1
		if q.Get("watch") == "true" {
			cur, _ := strconv.Atoi(q.Get("version"))
			if hold > 0 {
				// Long-poll: hold the connection open until the server timeout,
				// then deliver the next version. Abort if the client disconnects.
				select {
				case <-time.After(hold):
				case <-r.Context().Done():
					return
				}
			}
			version = cur + 1 // deliver the next version -> a "change" per poll
		}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_ = json.NewEncoder(w).Encode(map[string]interface{}{
			"name":    name,
			"version": version,
			"keys": map[string]string{
				"feature.enabled": "true",
			},
		})
	})
	log.Println("config-stub listening on :9100")
	log.Fatal(http.ListenAndServe(":9100", nil))
}
