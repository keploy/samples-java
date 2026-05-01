# restheart-mongo — keploy compat lane sample (work in progress)

Minimum reproducer scaffold for the RESTHeart / MongoDB compat lane. Mirrors the architectural pattern of the [doccano-django sample in `samples-python`](https://github.com/keploy/samples-python/tree/main/doccano-django): the sample owns orchestration (compose / bootstrap / traffic / noise filter / coverage), keploy CI lanes consume it as a thin wrapper.

## Status

**This is a SCAFFOLD.** The compose, bootstrap, and a minimal record-traffic loop work end-to-end against bare RESTHeart without keploy in the picture. The full traffic loop the existing keploy/enterprise lane drives (`compat_trigger_record_traffic` in `enterprise/.ci/scripts/restheart-linux.sh`, ~600 lines covering CRUD on `/<db>/<coll>` + GraphQL + files + ACL + users + bulk + aggregations) has **not been ported** into `flow.sh::restheart_record_traffic` yet. Lanes consuming this sample today should either:

1. Port the missing curls into `flow.sh::restheart_record_traffic` (preferred — that's the migration this scaffold is designed around).
2. Or call into `enterprise/.ci/scripts/restheart-linux.sh::compat_trigger_record_traffic` between `flow.sh bootstrap` and `flow.sh coverage` until the migration completes.

See the migration plan in this PR's description / linked issue.

## Layout

```
restheart-mongo/
├── Dockerfile             # FROM softinstigate/restheart:9.2.1
├── docker-compose.yml     # mongo:7 + restheart:9.2.1, fixed subnet, env-driven
├── flow.sh                # bootstrap | record-traffic | coverage | list-routes
├── keploy.yml.template    # globalNoise for _etag/_oid/lastModified/Date
└── README.md              # this file
```

## Contract

The sample is keploy-independent: `docker compose up && bash flow.sh bootstrap && bash flow.sh record-traffic` runs end-to-end against bare RESTHeart. Lane scripts wrap that exact same path inside `keploy record` / `keploy test`.

* `bootstrap` — wait for RESTHeart to start serving, PUT the test database + collection so subsequent reads have something to find.
* `record-traffic` — drive RESTHeart's REST surface. Every call is logged to `${RESTHEART_FIRED_ROUTES_FILE}` (when set) so `coverage` has a numerator without a keploy recording.
* `coverage` — emits `(method, path)` coverage. Denominator is curated from RESTHeart's pattern-based mount table (see `restheart_list_routes` in `flow.sh`); not file-system-derivable like Next.js, so the list lives in source and must be updated alongside `record-traffic`.
* `list-routes` — diagnostic; prints the route table.

## Local run

```sh
docker compose up -d
bash flow.sh bootstrap 240
RESTHEART_FIRED_ROUTES_FILE=/tmp/fired.log bash flow.sh record-traffic
RESTHEART_FIRED_ROUTES_FILE=/tmp/fired.log bash flow.sh coverage
docker compose down -v
```

## Consumers

Lanes pinning to this sample (pinned via `--branch feat/restheart-mongo-sample` until merge):

* `keploy/enterprise` `.woodpecker/restheart-linux.yml` — being slimmed in a follow-up PR.
* No `keploy/integrations` consumer today; could be added if a RESTHeart-flavoured Mongo wire bug surfaces.
