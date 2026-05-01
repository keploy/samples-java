# restheart-mongo — keploy compat lane sample

A complete, self-contained sample that drives the RESTHeart 9.x REST surface keploy needs to gate on its compat lanes. Mirrors the architectural pattern of the [doccano-django sample in `samples-python`](https://github.com/keploy/samples-python/tree/main/doccano-django): the sample owns orchestration (compose / bootstrap / traffic / noise filter / coverage), and keploy CI lanes consume it as a thin wrapper.

The traffic loop exercises the surfaces that keploy parsers and matchers have to handle correctly across record + replay:

* **CRUD** on `/<db>/<coll>` and `/<db>/<coll>/<docid>` — including `_size`, `_meta`, `_indexes`, ETag conditional requests, `writeMode=insert/update/upsert`, and `$inc / $push / $addToSet / $pull / $unset / $rename / $currentDate` PATCH operators.
* **HAL** representations via `Accept: application/hal+json` and `?rep=hal&hal=full` on documents, collections, indexes, and bulk responses.
* **Aggregations** via `_meta.aggrs` — group / count / sort / project / facet / lookup / unwind plus `avars` variable interpolation (scalars, arrays, nested objects, missing / malformed inputs).
* **Bulk writes** — array-body POST, filter-bound PATCH and DELETE, larger 25-doc batches, mixed valid / invalid documents.
* **GraphQL** apps — `gql-apps` registration, query / mutation / fragment / alias / multi-op forms, BSON scalar coercion (`BsonObjectId`, `BsonDecimal128`, `BsonLong`, `BsonDate`, `BsonBinary`) on outputs and inputs, introspection.
* **Files / GridFS** — buckets (`<coll>.files`), multipart upload, binary download with `Range` requests, metadata fetch, delete.
* **ACL** rules (`/acl`) — predicate evaluation (`method`, `path-prefix`, `qparams-whitelist`, `qparams-blacklist`, `qparams-contain`, `qparams-size`, `bson-request-whitelist/blacklist/contains`, `equals[%U,...]`, `in[%h, ...]`), `mongo` permission interceptors (`readFilter`, `writeFilter`, `projectResponse`, `mergeRequest`, `filterOperatorsBlacklist`, `propertiesBlacklist`, `allowBulk*`).
* **Users** (`/users`) — non-admin user creation with the bcrypt password hasher; reader / writer roles authenticating via Basic + Bearer; wrong-password denial.
* **Sessions / transactions** (`/_sessions`, `/_sessions/<id>/_txns/<txnid>`) — open, write inside, commit (PATCH), abort (DELETE), and re-read.
* **Auth services** — `/token` form grants (password, client_credentials, refresh_token, unsupported), JWT bearer (valid + invalid signature), Auth-Token, Digest, OAuth metadata under `/.well-known/oauth-*`.
* **Diagnostics** — `/ping`, `/metrics` (json / prometheus / openmetrics, per-db, per-coll), `/health/db`, OPTIONS preflight, gzip request encoding, Accept-Encoding negotiation.
* **MongoMountResolver** — multiple databases, collections with dashes / dots / encoded slashes, root `/_size` and `/_meta`, trailing-slash and double-slash variants.

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

* `bootstrap` — wait for RESTHeart to start serving and PUT the seed collections (`items`, `people`, `places`, `halpeople`, `relpeople`, `gql-apps`, `acl`, `_schemas`, `avatars.files`, `range_files.files`, `imported_csv`) so subsequent record-traffic calls have something to find.
* `record-traffic` — drive the full RESTHeart REST surface listed above. Every call is logged to `${RESTHEART_FIRED_ROUTES_FILE}` (when set) so `coverage` has a numerator without a keploy recording, and every call is fault-tolerant (`|| true`) so a single transient 4xx never aborts the run. keploy is the assertion layer.
* `coverage` — emits `(method, path)` coverage. The denominator is curated from RESTHeart's pattern-based mount table (see `restheart_list_routes` in `flow.sh`); RESTHeart routes are not file-system-derivable like Next.js, so the list lives in source and stays in lockstep with `record-traffic`.
* `list-routes` — diagnostic; prints the route table the coverage report uses as its denominator.

## Local run

```sh
docker compose up -d
bash flow.sh bootstrap 240
RESTHEART_FIRED_ROUTES_FILE=/tmp/fired.log bash flow.sh record-traffic
RESTHEART_FIRED_ROUTES_FILE=/tmp/fired.log bash flow.sh coverage
docker compose down -v
```

## Consumers

* `keploy/enterprise` `.woodpecker/restheart-linux.yml` — the RESTHeart compat lane delegates compose + traffic + coverage to this sample and wraps them in `keploy record` / `keploy test`.
