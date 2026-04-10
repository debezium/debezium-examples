#!/usr/bin/env python3
"""
Run a Debezium example test using a YAML DSL specification.

Usage:
    python scripts/run-example-test.py <example-directory>

The example directory must contain a test.yaml file describing the test steps.
"""

import json
import os
import subprocess
import sys
import time

import requests
import yaml


def load_env_file(path):
    """Load KEY=VALUE pairs from an env file into os.environ."""
    if not os.path.exists(path):
        print(f"[WARN] env_file not found: {path}")
        return
    with open(path) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            os.environ.setdefault(key.strip(), value.strip())


def run_cmd(cmd, check=True, capture_output=False):
    """Run a shell command, printing it first."""
    print(f"  $ {' '.join(cmd)}")
    return subprocess.run(
        cmd,
        check=check,
        capture_output=capture_output,
        text=True,
        env=os.environ,
    )


def compose_cmd(config, *args):
    """Build the base docker compose command from config."""
    cmd = ["docker", "compose"]
    env_file = config.get("env_file")
    if env_file:
        cmd += ["--env-file", env_file]
    compose_file = config.get("compose_file")
    if compose_file:
        cmd += ["-f", compose_file]
    cmd += list(args)
    return cmd


# ---------------------------------------------------------------------------
# Step handlers
# ---------------------------------------------------------------------------

def step_docker_compose_up(step, config):
    cmd = compose_cmd(config, "up")
    if step.get("detach", False):
        cmd.append("-d")
    services = step.get("services", [])
    cmd += services
    run_cmd(cmd)


def step_docker_compose_down(step, config):
    cmd = compose_cmd(config, "down")
    if step.get("volumes", False):
        cmd.append("-v")
    run_cmd(cmd, check=False)


def step_docker_compose_stop(step, config):
    service = step["service"]
    cmd = compose_cmd(config, "stop", service)
    run_cmd(cmd)


def step_docker_compose_exec(step, config):
    service = step["service"]
    command = step["command"]
    cmd = compose_cmd(config, "exec", "-T", service, "bash", "-c", command)
    run_cmd(cmd)


def step_http_put(step, config):
    url = step["url"]
    body_file = step["body_file"]
    expected_statuses = step.get("expected_status", [200, 201])
    if isinstance(expected_statuses, int):
        expected_statuses = [expected_statuses]

    with open(body_file) as f:
        body = json.load(f)

    print(f"  PUT {url}")
    resp = requests.put(url, json=body, timeout=30)
    if resp.status_code not in expected_statuses:
        raise RuntimeError(
            f"PUT {url} returned {resp.status_code}, expected one of {expected_statuses}.\n"
            f"Response: {resp.text}"
        )
    print(f"  -> {resp.status_code}")


def step_http_wait(step, config):
    url = step["url"]
    timeout = step.get("timeout_seconds", 60)
    interval = step.get("interval_seconds", 5)
    json_path = step.get("expected_json_path")
    expected_value = step.get("expected_value")

    deadline = time.time() + timeout
    print(f"  Polling {url} (timeout={timeout}s)")

    while True:
        try:
            resp = requests.get(url, timeout=10)
            if resp.status_code < 500:
                if json_path and expected_value:
                    data = resp.json()
                    # resolve simple dot-separated path
                    actual = data
                    for key in json_path.split("."):
                        actual = actual[key]
                    if str(actual) == str(expected_value):
                        print(f"  -> {json_path}={actual} (OK)")
                        return
                    print(f"  -> {json_path}={actual}, waiting for {expected_value}...")
                else:
                    print(f"  -> {resp.status_code} (OK)")
                    return
        except Exception as e:
            print(f"  -> not ready ({e})")

        if time.time() >= deadline:
            raise RuntimeError(f"Timed out waiting for {url} after {timeout}s")
        time.sleep(interval)


def step_kafka_consume(step, config):
    topic = step["topic"]
    timeout_seconds = step.get("timeout_seconds", 30)
    expected_content = step.get("expected_content")
    timeout_ms = timeout_seconds * 1000

    cmd = compose_cmd(
        config,
        "exec", "-T", "kafka",
        "/kafka/bin/kafka-console-consumer.sh",
        "--bootstrap-server", "kafka:9092",
        "--topic", topic,
        "--from-beginning",
        "--max-messages", "100",
        "--timeout-ms", str(timeout_ms),
    )
    result = run_cmd(cmd, check=False, capture_output=True)
    output = result.stdout + result.stderr

    if expected_content and expected_content not in output:
        raise RuntimeError(
            f"Expected '{expected_content}' not found in Kafka topic '{topic}'.\n"
            f"Consumer output:\n{output[-2000:]}"
        )
    if expected_content:
        print(f"  -> Found '{expected_content}' in topic output (OK)")


def step_env_override(step, config):
    vars_ = step.get("vars", {})
    for key, value in vars_.items():
        print(f"  export {key}={value}")
        os.environ[key] = str(value)


def step_wait(step, config):
    seconds = step.get("seconds", 5)
    print(f"  Sleeping {seconds}s...")
    time.sleep(seconds)


STEP_HANDLERS = {
    "docker_compose_up": step_docker_compose_up,
    "docker_compose_down": step_docker_compose_down,
    "docker_compose_stop": step_docker_compose_stop,
    "docker_compose_exec": step_docker_compose_exec,
    "http_put": step_http_put,
    "http_wait": step_http_wait,
    "kafka_consume": step_kafka_consume,
    "env_override": step_env_override,
    "wait": step_wait,
}


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def run_step(step, config):
    step_type = step["type"]
    handler = STEP_HANDLERS.get(step_type)
    if handler is None:
        raise RuntimeError(f"Unknown step type: '{step_type}'")
    handler(step, config)


def main():
    if len(sys.argv) < 2:
        print("Usage: run-example-test.py <example-directory>", file=sys.stderr)
        sys.exit(1)

    example_dir = sys.argv[1]

    # Resolve paths relative to the repo root (script lives in scripts/)
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    example_path = os.path.join(repo_root, example_dir)

    if not os.path.isdir(example_path):
        print(f"Error: directory not found: {example_path}", file=sys.stderr)
        sys.exit(1)

    test_file = os.path.join(example_path, "test.yaml")
    if not os.path.exists(test_file):
        print(f"Error: test.yaml not found in {example_path}", file=sys.stderr)
        sys.exit(1)

    with open(test_file) as f:
        spec = yaml.safe_load(f)

    print(f"[TEST] {spec.get('name', example_dir)}")
    if spec.get("description"):
        print(f"       {spec['description']}")

    # Change into the example directory so relative paths in test.yaml resolve correctly
    os.chdir(example_path)

    # Load env vars from env_file
    env_file = spec.get("env_file")
    if env_file:
        load_env_file(env_file)

    steps = spec.get("steps", [])
    cleanup_step = spec.get("cleanup")

    failed = False
    try:
        for i, step in enumerate(steps, 1):
            name = step.get("name", step["type"])
            print(f"\n[{i}/{len(steps)}] {name}")
            run_step(step, spec)
    except Exception as e:
        print(f"\n[FAIL] {e}", file=sys.stderr)
        failed = True
    finally:
        if cleanup_step:
            print(f"\n[CLEANUP] Running cleanup...")
            try:
                run_step(cleanup_step, spec)
            except Exception as e:
                print(f"[WARN] Cleanup failed: {e}", file=sys.stderr)

    if failed:
        sys.exit(1)

    print("\n[PASS] All steps completed successfully.")


if __name__ == "__main__":
    main()
