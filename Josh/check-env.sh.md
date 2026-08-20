# check-env.sh

A single-file Bash tool that runs **health checks against a GigaSpaces / DIH (TAU) environment**
from the pivot/utility host. It exercises the cluster end-to-end: host reachability, the
Manager REST API, the space PU and its containers, the northbound (NB) service tier, pipelines,
sanity logs, Control-M jobs, SpaceDeck, and more.

Running it with no arguments produces a full **daily report**. Individual checks can also be run
one at a time by number (`-c <n>`), which is how it's used most often for ad-hoc diagnosis.

---

## Quick start

```bash
check-env.sh                 # full DAILY report (default)
check-env.sh -lc             # list every individual check + its number
check-env.sh -c 20           # run only check 20 (manager sync)
check-env.sh -v -c 20        # same, VERBOSE (dump the raw per-manager data)
check-env.sh -c 16 program   # query a single service (program_study_service)
check-env.sh -p -c 16 program  # PRINT the curl command instead of running it
check-env.sh -h              # usage
```

The environment is auto-detected from `$ENV_NAME` (set by `~/.bash_profile`): `TAUG` (dev),
`TAUS` (stage/test), `TAUP` (prod). The script picks the matching SSL client certs and the
matching northbound endpoint hostname per environment.

---

## How it gets its bearings (`do_env`)

On startup the script sources `~/.bash_profile` and then resolves everything it needs:

| What | How it's resolved |
|------|-------------------|
| **Manager credentials** | `$GS_USER` / `$GS_PASS` if set; otherwise read from `${ENV_CONFIG}/app.config` (`app.manager.security.username/password`). If `app.vault.use=true`, the password is fetched from the GS vault jar instead of read in clear. Fails fast with a clear error if it can't resolve both. |
| **Manager hosts** (`_MANAGERS`) | `runall -m -l` |
| **All hosts** (`_ALL_HOSTS`) | `runall -A -l`, de-duplicated (drops `===` headers, `None` placeholders, `x.x.x.x`) |
| **DI host** (`_DI_HOST`) | `runall -d -l` (first entry) |
| **Space PU name** | `dih-tau-service` |

After `do_env`, `check_prereqs` verifies that `curl`, `jq`, and `runall` are all on `PATH` and
aborts early if any is missing.

> **Credentials via env vars** is the normalized override: `GS_USER=admin GS_PASS=secret check-env.sh -c 24`.
> This lets the script run without `app.config` / vault access (e.g. from a different host or in CI).

---

## The checks

`check-env.sh -lc` prints this list. Numbers are stable and are what `-c <n>` takes.

| # | Check | What it does |
|---|-------|--------------|
| 1 | `check_ping` | `ping` every host in `_ALL_HOSTS`; reports `Success`/`failed`. |
| 2 | `check_ssh` | `ssh <host> uptime` (5s timeout) on every host; reports per-host failures. |
| 3 | `check_disk_usage_all [pct]` | `df -h` on every host, prints partitions above the threshold (default **70%**). |
| 4 | `show_primary_backup` | Partition count + primary/backup GSC layout via `pb-same-server`. |
| 5 | `service_hc` | Spring actuator `/health` for every service Consul has registered. |
| 6 | `service_query` | For every deployed `*_service` PU (minus notifiers): query it through the NB endpoint and report `data returned` / `empty response` + timing + deploy state. |
| 7 | `check_feeders` | Gilboa full/update + Oracle feeders not in `IN_PROGRESS`/`SUCCESS`/`IDLE`. |
| 8 | `check_pipelines` | Runs `statusPipelines.sh` on the DI host; `Success`/`Failed`. |
| 9 | `check_sanity_errors` | Today's `sanity.log` lines matching `fail\|error\|warn\|down`. |
| 10 | `check_all_sanity_of_today` | Dumps all of today's sanity lines. |
| 11 | `check_ctm` | Validates today's Control-M feeder jobs in groups of lines (start / table / exit-code / `"OK"` / `feeder_exit_code=0`). Skipped on `TAUG`. |
| 12 | `list_types` | Count (and with `-v`, list) of all registered space types. |
| 13 | `run_pipelines_bg` + `show_pipelines_bg` | Run the pipeline status in the background on the DI host and wait for it. |
| 14 | `check_notifiers` | `auto_notifiers.sh -l` — expects exactly 2 intact notifiers. |
| 15 | `list_service_names` | Numbered list of service names parsed from `/giga/microservices/curls`. |
| 16 | `query_one_service <name>` | Query a single service through the NB endpoint. With `-p`, just print the curl. |
| 17 | `check_gigashare` | Confirms the `gigashare` mount is present on every host. |
| 18 | `person_schedule_query_2` | A fixed canned query against `person_schedule_service`. |
| 19 | `check_nbapp_services` | On NB-**app** hosts (`runall -na`): `nginx`, `consul`, `consul-template`, `telegraf`, `northbound.target` all `active`. |
| 20 | `check_sync_of_managers` | **Compares space-PU and container counts across all managers** (see below). |
| 21 | `check_spacedeck_on_managers` | Per manager: is SpaceDeck (`:4200`) returning `200 OK` — `UP`/`DOWN`. |
| 22 | `check_nbagent_services` | On NB-**agent**/space hosts (`runall -s`): `consul`, `telegraf`, `northbound.target` `active`. |
| 23 | `check_indexes [-c\|-l]` | Counts space types carrying the `T_IDKUN` `EQUAL_AND_ORDERED` index. `-c` shows count, `-l` appends to `/gigalogs/check-indexes.log`. |
| 24 | `check_managers_rest` | **Verifies each manager's REST API (`:8090`) answers `200`** (see below). |

### Check 24 — `check_managers_rest`
Hits `http://<manager>:8090/v2/info` on every manager and asserts HTTP `200`. A manager whose
REST process is down (the classic onprem1 `.52` failure) is reported explicitly. This runs
*before* the sync check in the daily report because a down REST endpoint would otherwise distort
the sync comparison.

### Check 20 — `check_sync_of_managers`
For each manager it pulls:
- `GET /v2/pus/dih-tau-service` → counts the space PU's instances (`space_pus`)
- `GET /v2/containers` → counts containers **with** instances and **empty** containers

It prints one line per manager, e.g.:

```
  192.168.64.46    space_pus=36     containers=36     empty=36
  192.168.64.52    space_pus=36     containers=36     empty=36
  192.168.64.51    space_pus=36     containers=36     empty=36

manager sync: Success
```

Then it string-compares every manager's counts against the first manager; any mismatch — or any
`ERR` — fails the check. Empty containers raise a non-fatal `WARNING`. `-v` dumps the raw
associative arrays.

> **This is the check the normalized version fixed.** Previously a missing PU returned a
> *plaintext* 404 body and an unreachable manager returned an empty body; both were piped straight
> into `jq`, which spat `parse error: Invalid numeric literal...` and counted them as `0`. When
> every manager came back `0`, the counts "matched" and the check falsely printed **Success**.
> Now `is_json()` guards every REST body: a non-JSON / empty response becomes `ERR`, which is
> always printed and always fails the check.

---

## Options

| Option | Meaning |
|--------|---------|
| `-d` | Daily report (default when no args). |
| `-r` | Regular report — **TBD / placeholder**. |
| `-c <n>` | Run a single check by number, then exit. |
| `-lc` | List all checks with their numbers. |
| `-ls` | List service names (`list_service_names`). |
| `-v` | Verbose — dump raw data / full responses. |
| `-q` | Quiet — no pauses between checks. |
| `-p` | Print the command instead of executing (single-service queries). |
| `-h` | Usage. |

---

## Internal helpers

| Helper | Purpose |
|--------|---------|
| `mng_rest <host> <path>` | All manager REST calls go through this. Adds `-k`, basic-auth, and an 8s timeout (`_CURL_TIMEOUT`). Returns the body; the caller validates it. |
| `is_json <text>` | True only if the argument parses as JSON (`jq -e .`). Used to reject plaintext 404s / empty bodies before counting. |
| `check_prereqs` | Aborts if `curl` / `jq` / `runall` aren't on `PATH`. |
| `do_env` | Bootstraps creds + host lists (above). |

---

## Daily report flow (`do_daily`)

Run order when invoked with no arguments:

```
run_sanity_bg ─┐ (background)
run_pipelines_bg ─┐ (background, not on TAUG)
check_ping
check_nbapp_services
check_nbagent_services
check_managers_rest
check_spacedeck_on_managers
check_sync_of_managers
check_notifiers
show_primary_backup
service_hc
service_query
check_ssh
check_feeders
check_disk_usage_all
check_ctm
show_pipelines_bg            (not on TAUG)
check_gigashare
check_sanity_errors
show_sanity_bg
```

The sanity and pipeline status jobs are kicked off in the background early and harvested near the
end so their (slow) runtime overlaps the rest of the report.

---

## Requirements & assumptions

- Run from the **pivot / utility host** where `runall`, `pb-same-server`, `auto_*` helpers,
  `/giga/microservices/curls`, and the per-environment SSL client certs under `/giga/josh/ssl/<env>`
  all exist.
- `$ENV_NAME` is one of `TAUG` / `TAUS` / `TAUP`; service-query checks abort on any other env.
- Passwordless `ssh` to every cluster host (the script `ssh`'s into hosts for disk, gigashare,
  uptime, NB-service status).
- Manager REST API reachable on `:8090`; SpaceDeck on `:4200`; NB endpoints on `:8443`.
- `curl`, `jq`, `runall` on `PATH`.

---

## Provenance

This repo copy is the **normalized merge** of two diverged versions: the newer
`onprem1:/giga/utils/check-env.sh` and this repo's richer copy. The merge kept the repo-only
checks (`-p` print mode, separate NB-app/NB-agent checks, `check_indexes`), adopted the cluster's
fixes (notifiers on `TAUG`), and hardened the script: env-var credentials, prerequisite/credential
validation, a single `mng_rest()` for all REST calls, the `check_sync_of_managers` JSON-guard fix
described above, and the new `check_managers_rest`. See the header comment block in `check-env.sh`
for the full per-item merge log.
