Using Debezium Platform built-in Monitoring and Alert
===

This example walks you through running the Debezium Management Platform with **monitoring enabled and testing the alerts on Platform**.

It builds directly on [`../postgresql-kafka-example`](../postgresql-kafka-example), which walks through the UI
in detail (Connections, Sources, Destinations, Pipeline) and explains every field. If you are new to Platform read that one first. Here we
reuse the same PostgreSQL → Kafka pipeline and focus on wiring monitoring into the platform,
reading the panels, making them move, and then alerting on them.

Every step is backed by a script, so you can read along and run them. All scripts source [`env.sh`](./env.sh) for shared configuration like cluster name,
namespaces, chart versions, hostnames so those only need changing in one place.

How a number gets onto a panel
---

```mermaid
flowchart LR
    subgraph SRC["Source"]
        PG[("PostgreSQL")]
    end

    subgraph DST["Destination"]
        KFK[("Kafka (Strimzi)")]
    end

    PG -- "CDC" --> PIPE["Pipeline pod<br/>(Debezium Server, created by<br/>debezium-operator from the Pipeline you seed below)"]
    PIPE -- "change events" --> KFK

    subgraph MON["Monitoring & Observability"]
        OTEL["OpenTelemetry Collector"] -- "Prometheus exporter :8889" --> PROM["Prometheus<br/>(scrapes every 15s)"]
    end

    PIPE -- "OTLP metrics every 5s" --> OTEL

    subgraph USER["User-facing"]
        UI(["Stage UI"])
    end

    COND["Conductor API<br/>(/api/monitoring/*)"]
    UI -- "HTTP" --> COND
    COND -- "PromQL" --> PROM

    classDef store fill:#fff3e0,stroke:#e65100,stroke-width:1.5px
    classDef userFacing fill:#e3f2fd,stroke:#0d47a1,stroke-width:1.5px
    class PG,KFK store
    class UI userFacing
```

Every panel in the UI is the end of that chain: the Stage UI asks the Conductor for a panel, and the Conductor
runs a PromQL query against Prometheus.

Prepare the environment
---

**Prerequisites**

- [minikube](https://minikube.sigs.k8s.io/docs/)
- `kubectl`, `helm`
- `curl` and [`jq`](https://jqlang.org/) (used by the seed/verify scripts)

> **_NOTE:_** This example reuses the same minikube profile name (`debezium`) and `/etc/hosts` entry
> (`platform.debezium.io`) as [`../postgresql-kafka-example`](../postgresql-kafka-example). Run `./clean-up.sh`
> in that example first if you still have it running, or edit `CLUSTER` in [`env.sh`](./env.sh) to use a
> different profile.

`create-environment.sh` provisions a local minikube cluster with an ingress controller and points
`platform.debezium.io` at it via `/etc/hosts`. The monitoring stack is heavier than the base example, so this
cluster is sized up (`--cpus=6 --memory=8192`).

```sh
./create-environment.sh
```

> **_NOTE:_** On macOS, keep the printed `sudo minikube tunnel -p debezium` command running in a separate
> terminal for the rest of this example — the ingress isn't reachable from the host without it.

Install the platform with monitoring
---

### 1. Install the monitoring stack

Monitoring depends on two operators, which must exist before the platform is installed:

- the **OpenTelemetry Operator**, which turns the `OpenTelemetryCollector` resource the platform chart creates
  into a running collector pod,
- **kube-prometheus-stack**, which provides the Prometheus Operator (it consumes `ServiceMonitor` resources)
  and the Prometheus server the Conductor queries.

```sh
./setup-monitoring.sh
```

> **_NOTE:_** The script installs `kube-prometheus-stack` under that exact release name.
> Both `monitoring.prometheus.url` and the `ServiceMonitor` label in the next steps are derived from it.

```sh
$ kubectl get pods -n monitoring

NAME                                                     READY   STATUS    RESTARTS   AGE
prometheus-kube-prometheus-stack-prometheus-0            2/2     Running   0          2m10s
kube-prometheus-stack-operator-6c6d4f8d9-abcde           1/1     Running   0          2m40s
kube-prometheus-stack-kube-state-metrics-7f9d8c6-fghij   1/1     Running   0          2m40s
alertmanager-kube-prometheus-stack-alertmanager-0        2/2     Running   0          2m10s
```

### 2. Install the source database and destination Kafka

Same as the base example: a PostgreSQL instance pre-seeded with the `inventory` schema as the source, and a
Strimzi-managed Kafka cluster as the destination.

```sh
./setup-infra.sh
```

### 3. Deploy the Debezium Platform

Here we install the same `debezium-platform` Helm chart, but
[`values.yaml`](./values.yaml) adds a `monitoring` block on top of the usual `domain`/`database`/`ingress`
settings:

```yaml
monitoring:
  otel:
    enabled: true
    collector:
      image: "ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector-contrib:0.152.0"
  prometheus:
    url: "http://kube-prometheus-stack-prometheus.monitoring.svc.cluster.local:9090"
    serviceMonitor:
      labels:
        release: kube-prometheus-stack
```

In short:

- `otel.enabled: true` is what makes the chart create the collector and point new pipelines at it.
- `otel.collector.image` must be a distribution that bundles the **Prometheus exporter** (`contrib`, not the
  operator's default `otelcol-k8s`), or the collector pod crash-loops.
- `prometheus.url` must be **fully qualified**, because Prometheus and the Conductor are in different namespaces.
- `serviceMonitor.labels.release` must match the Prometheus Helm release name.

```sh
./setup-platform.sh
```

```sh
$ kubectl get pods -n debezium-platform

NAME                                                READY   STATUS    RESTARTS   AGE
conductor-7c48c54c5c-rmjw9                          1/1     Running   0          2m
stage-6c64f68df6-cfhjs                              1/1     Running   0          2m
debezium-operator-666f7b44d9-6tf4n                  1/1     Running   0          2m
postgres-69c4c64ff5-2tfmw                           1/1     Running   0          2m
debezium-platform-otel-collector-collector-xxxxx    1/1     Running   0          90s
```

The Stage UI is now reachable at `http://platform.debezium.io/`.

Explore the monitoring
---

### 4. Create the pipeline

Monitoring needs something to monitor, so the next step is the usual Connection → Source → Destination →
Pipeline. Build it by hand in the [Stage UI](http://platform.debezium.io/) if you want the detail — the base
example [walks through every field](../postgresql-kafka-example/README.md#using-the-debezium-platform-stageui-for-setting-up-our-data-pipeline)
— or skip ahead with [`seed-pipeline.sh`](./seed-pipeline.sh), which creates `postgres-connection`,
`kafka-connection`, `test-source`, `test-destination` and `test-pipeline` against the Conductor API:

```sh
./seed-pipeline.sh
```


It prints the direct link to the Monitoring tab, e.g. `http://platform.debezium.io/pipeline/1/monitoring`.

### 5. Open the Monitoring tab

Open the link printed by `seed-pipeline.sh` (or **Pipelines** → `test-pipeline` → **Monitoring**) and leave it
open for the rest of this example.

![Pipeline monitoring tab](./resources/monitoring-overview.png)

The tab has two sections, matching the `category` of each panel.

#### Streaming — ongoing change data capture

| Panel | What it shows |
| --- | --- |
| **Streaming Event Count Rate** | Change events/sec, split by `create` / `update` / `delete`, plus a `total` line. |
| **Connection Status** | Whether the connector is connected to the source database. |
| **Committed Transactions Rate** | Rate of committed source transactions being processed. |
| **Time Since Last Event** | Seconds since the last change event. |
| **Source Lag** | Time between a change in the source database and Debezium processing it. |
| **Queue Utilization** | % of the internal event queue (by event count) in use. |
| **Queue Size Utilization** | % of the internal event queue's byte capacity in use. |
| **Erroneous Events Rate** | Rate of events that errored during processing. |
| **Events Filtered Rate** | Rate of events excluded by include/exclude list configuration. |

#### Snapshot — the one-time initial copy

| Panel | What it shows |
| --- | --- |
| **Snapshot Status** | Lifecycle state: `running`, `completed`, `aborted` or `skipped`. |
| **Snapshot Table Count** | Number of tables included in the snapshot. |
| **Snapshot Table Progress** | Tables still remaining. |
| **Snapshot Duration** | Elapsed time of the current or most recent snapshot. |
| **Snapshot Event Count Rate** | Rate of snapshot (initial-load) change events/sec. |

The snapshot ran once, right after the pipeline was created, so these look flat. Scenario C below makes them
move again.



### 6. Make the panels move

Keep the Monitoring tab open while you run them, and give each one 30–45 seconds.


#### A. Watch the event chart climb

A single burst gives one bump that decays; a steady drip gives a chart that climbs and holds:

```sh
./generate-traffic.sh            # defaults: 60 batches x 20 rows, one batch every 5s
./generate-traffic.sh 20 50      # or: batches, rows-per-batch
```

**Watch:** **Streaming Event Count Rate** climbs then plateaus, **Time Since Last Event** stays near 0, and
**Committed Transactions Rate** lifts.



#### B. Watch a stalled pipeline

Stop `generate-traffic.sh` with `Ctrl+C`, do nothing for two minutes, then insert a single row:

```sh
kubectl exec -n debezium-platform deploy/postgresql -- psql -U debezium -d debezium -c \
  "INSERT INTO inventory.products (name, description, weight) VALUES ('wakeup', 'one row', 1);"
```

**Watch:** **Time Since Last Event** climbs as a diagonal line, then drops straight back to ~0 on the insert.

#### C. Make the Snapshot panels light up

The snapshot finished the moment the pipeline was created, so these panels look flat. Debezium records how far
it got in an offset table in the platform's own database; clearing it and restarting the pipeline makes it
snapshot from scratch:

```sh
./replay-snapshot.sh
```

**Watch:** switch to the **Snapshot** section straight away. **Snapshot Status** goes `running` → `completed`,
**Snapshot Table Progress** counts down, and **Snapshot Duration** and **Snapshot Event Count Rate** fill in.
It's quick on this dataset, so don't blink.

#### D. Flip Connection Status to disconnected

The clearest "something is wrong" visual on the dashboard. The script repeatedly kills the source database's
replication connection; Debezium reconnects on its own and the pipeline pod never restarts.

```sh
./break-connection.sh
```

**Watch:** **Connection Status** flips to **disconnected** partway through the loop and returns to
**connected** about 30s after it ends. Keep your eyes on it — Debezium reconnects on its own, so the dip
typically lasts only about a minute, and if you look away the panel reads **connected** again as though
nothing ever happened. (That is precisely the gap the [alerting section](#alerting-get-told-instead-of-watching)
below closes.)

> **_NOTE:_** Don't demo this by scaling `deploy/postgresql` to 0. The source database has no persistent
> volume, so that wipes the `inventory` data and the replication slot, and the pipeline then crash-loops on a
> WAL position that no longer exists. (If that does happen, `./replay-snapshot.sh` is the fix.)

Alerting: get told instead of watching
---

Everything so far is passive: the panels only help while somebody is looking at them. Alerting closes that
gap. The evaluation engine runs inside the Conductor, queries the same Prometheus the panels use, and keeps
rules and incidents in the platform's own database — nothing extra to install, and no flag to turn on. A rule
is built from a **monitoring panel** rather than a PromQL expression, so anything on the Monitoring tab can be
alerted on.

> **_NOTE:_** `kube-prometheus-stack` also installed an `alertmanager-...` pod back in step 1. The platform
> does not use it — rules live in the Conductor and only *read* from Prometheus.

### 7. Create an alert rule

Open **Alerts** → **Rules** in the left navigation and create a rule. The form reads as a sentence.

| Field | Value | Why |
| --- | --- | --- |
| **Name** | `source-disconnected` | Lowercase letters, digits and hyphens. |
| **Severity** | *Critical* | Critical and Warning count towards the header badge; Info does not. |
| **When** | *Connection Status* | Any panel from the Monitoring tab can be picked here. |
| **Reduce** | *last* | The default, and the right choice for a gauge. |
| **Is** | *equal to* `0` | 1 is connected, 0 is not — no threshold to tune. |
| **For** | *Immediately* | Fire on the first evaluation that sees a 0, with no pending state. |

![Create alert rule form](./resources/alert-rule-form.png)

Save it, and the rule shows up in the list with its condition rendered back as `last == 0`, plus a toggle
that disables it without deleting it.

> **_NOTE:_** Rules are global — one rule covers every pipeline, and each breaching pipeline gets its own
> incident. There is only one pipeline here, but you won't have to revisit the rule when you add more.

Then break the connection, for longer than the default:

```sh
./break-connection.sh 30
```

The default of 14 kills covers about 70 seconds, which can slip between two evaluations and never be noticed.
30 kills covers roughly two and a half minutes, so at least one evaluation is guaranteed to land inside the
window.

**Watch:** the rules list marks `source-disconnected` as firing, the header badge turns red, and a new
incident appears under **Alerts** → **History**. About a minute after the loop ends Debezium reconnects and
the incident resolves on its own.

![Alert rules list with a firing rule](./resources/alert-rules-list.png)

The panel is a poor record of the same event: **Connection Status** dips for about a minute and then reads
connected again, so looking away loses it entirely. The rule catches it either way.

### 8. Read the history

**Alerts** → **History** is the record of everything that happened while you weren't watching. Each incident
carries its rule, pipeline, severity, the value that tripped it, the threshold it crossed, when it fired and
how long it lasted. Rows expand for the detail, and the table filters by severity, status, pipeline, rule and
date range.

Incidents that are still firing are pinned to the top, so the page opens on what is broken right now, with the
closed ones underneath. The pipeline name in each row links back to that pipeline's Monitoring tab, which is
usually where you were heading anyway.

![Alert history](./resources/alert-history.png)

After the scenario above you should have one closed incident, marked critical, with a short duration.

### Notifying outside the platform

Alerts always appear in the UI, which is why the walkthrough above needed no setup. To also notify a person,
add a channel under **Alerts** → **Channels** and attach it to a rule through the rule's *Additional channels*
field. One channel can serve several rules, and each has a **Test** button.

![Notification channels](./resources/alert-channels.png)

Both types need something this example does not install, so treat this as a pointer rather than a step:

- **Email** needs an SMTP server, set at install time through `alerting.email.host`, `alerting.email.from` and
  a secret referenced by `alerting.email.auth.existingSecret`.
- **Webhook** posts JSON to a URL (Slack, PagerDuty, Teams). The Conductor rejects private, loopback and
  link-local addresses by default, so an in-cluster receiver needs
  `alerting.webhook.allowPrivateNetworks: true` in [`values.yaml`](./values.yaml) and a `helm upgrade`.

See the [`debezium-platform` Helm chart README](https://github.com/debezium/debezium-platform/blob/main/helm/README.md#alerting)
for the full set of `alerting.*` values.


Cleanup
---
```sh
./clean-up.sh
```

This deletes the `debezium` minikube profile, and everything deployed in it goes with it. The script also
prints the command to drop the `/etc/hosts` entry it added.
