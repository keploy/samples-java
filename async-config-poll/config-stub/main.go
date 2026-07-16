// Command config-stub is a stand-in for a central config service. It backs the
// app's boot-blocking config fetch and its background watch long-poll:
//
//   - GET /v1/buckets/{name}                        -> current config (version 1)
//   - GET /v1/buckets/app-config?watch=true&version=N -> long-poll: returns the
//     NEXT version (N+1), simulating a config change on each watch poll.
//
// It is hit only during `keploy record`. At replay time Keploy serves the
// recorded responses instead, so this stub does not need to be running.
package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"strings"
)

func main() {
	http.HandleFunc("/v1/buckets/", func(w http.ResponseWriter, r *http.Request) {
		name := strings.TrimPrefix(r.URL.Path, "/v1/buckets/")
		q := r.URL.Query()

		version := 1
		if q.Get("watch") == "true" {
			cur, _ := strconv.Atoi(q.Get("version"))
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
