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
import re
import sys
import time

import requests
import yaml


VARIABLES = {}


def substitute_vars(value):
    """Recursively substitute ${VAR} placeholders with values from VARIABLES."""
    if isinstance(value, str):
        # find and replace placeholders like ${my_var}
        def replacer(match):
            var_name = match.group(1)
            if var_name not in VARIABLES or VARIABLES[var_name] is None:
                return match.group(0)
            return str(VARIABLES[var_name])
        return re.sub(r"\$\{([A-Za-z0-9_]+)\}", replacer, value)
    elif isinstance(value, dict):
        return {k: substitute_vars(v) for k, v in value.items()}
    elif isinstance(value, list):
        return [substitute_vars(item) for item in value]
    return value


def extract_json_value(data, path):
    """Extract a value from a JSON dictionary using a dot-separated path."""
    if not path:
        return data
    actual = data
    try:
        for key in path.split("."):
            if key == "{FIRST_KEY}" and isinstance(actual, dict):
                actual = list(actual.keys())[0] if actual else None
            elif key.isdigit() and isinstance(actual, list):
                actual = actual[int(key)]
            else:
                actual = actual[key]
        return actual
    except (KeyError, IndexError, TypeError):
        return None


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


def step_docker_compose_build(step, config):
    cmd = compose_cmd(config, "build")
    services = step.get("services", [])
    if isinstance(services, list):
        cmd += services
    elif isinstance(services, str):
        cmd.append(services)
    run_cmd(cmd)

def step_docker_compose_stop(step, config):
    service = step["service"]
    cmd = compose_cmd(config, "stop", service)
    run_cmd(cmd)


def step_docker_compose_exec(step, config):
    service = substitute_vars(step["service"])
    command = substitute_vars(step["command"])
    
    shell = substitute_vars(step.get("shell", "bash"))
    # Use a configurable shell so shell operators like >, |, && still work.
    # We default to `bash` for full backward compatibility with existing tests,
    # but it can be overridden to `sh` for Alpine-based images.
    cmd = compose_cmd(config, "exec", "-T", service, shell, "-c", command)
    
    run_cmd(cmd)


def _http_request(method, step, config):
    url = substitute_vars(step["url"])
    body_file = step.get("body_file")
    body = step.get("body")
    headers = step.get("headers", {})
    expected_statuses = step.get("expected_status", [200, 201])
    capture_json = step.get("capture_json", {})
    
    if isinstance(expected_statuses, int):
        expected_statuses = [expected_statuses]

    if body_file:
        with open(body_file) as f:
            body = json.load(f)

    body = substitute_vars(body)
    headers = substitute_vars(headers)

    print(f"  {method} {url}")
    resp = requests.request(method, url, json=body, headers=headers, timeout=30)
    
    if resp.status_code not in expected_statuses:
        raise RuntimeError(
            f"{method} {url} returned {resp.status_code}, expected one of {expected_statuses}.\n"
            f"Response: {resp.text}"
        )
    print(f"  -> {resp.status_code}")

    if capture_json:
        try:
            data = resp.json()
        except Exception:
            data = {}
        for var_name, path in capture_json.items():
            val = extract_json_value(data, path)
            VARIABLES[var_name] = val
            print(f"  -> Captured {var_name}={val}")


def step_http_put(step, config):
    _http_request("PUT", step, config)


def step_http_post(step, config):
    _http_request("POST", step, config)


def step_http_delete(step, config):
    _http_request("DELETE", step, config)


def step_http_wait(step, config):
    url = substitute_vars(step["url"])
    timeout = step.get("timeout_seconds", 60)
    interval = step.get("interval_seconds", 5)
    json_path = step.get("expected_json_path")
    expected_value = substitute_vars(step.get("expected_value")) if step.get("expected_value") is not None else None
    headers = substitute_vars(step.get("headers", {}))
    expected_statuses = step.get("expected_status")
    capture_json = step.get("capture_json", {})
    restart_url = substitute_vars(step.get("restart_on_fail_url")) if step.get("restart_on_fail_url") else None
    restart_interval = step.get("restart_interval_seconds", 30)
    if isinstance(expected_statuses, int):
        expected_statuses = [expected_statuses]

    deadline = time.time() + timeout
    last_restart_time = 0
    print(f"  Polling {url} (timeout={timeout}s)")

    while True:
        try:
            resp = requests.get(url, headers=headers, timeout=10)
            
            is_success = False
            if expected_statuses is not None:
                is_success = resp.status_code in expected_statuses
            else:
                is_success = 200 <= resp.status_code < 300

            if is_success:
                if capture_json:
                    try:
                        data = resp.json()
                    except Exception:
                        data = {}
                    for var_name, path in capture_json.items():
                        val = extract_json_value(data, path)
                        VARIABLES[var_name] = val
                        print(f"  -> Captured {var_name}={val}")

                if json_path is not None and expected_value is not None:
                    try:
                        data = resp.json()
                    except Exception:
                        data = {"_error": "Response not valid JSON", "_body": resp.text}
                        
                    actual = extract_json_value(data, json_path)
                    
                    if expected_value == "{ANY}" and actual is not None:
                        print(f"  -> {json_path}={actual} (OK)")
                        return
                    elif str(actual) == str(expected_value):
                        print(f"  -> {json_path}={actual} (OK)")
                        return
                    
                    # Value doesn't match — try restarting if configured
                    if restart_url and (time.time() - last_restart_time >= restart_interval):
                        try:
                            print(f"  -> {json_path}={actual}, sending restart to {restart_url}")
                            requests.post(restart_url, timeout=10)
                            last_restart_time = time.time()
                        except Exception as re:
                            print(f"  -> restart request failed: {re}")
                    else:
                        print(f"  -> {json_path}={actual}, waiting for {expected_value}... (Body: {resp.text[:100]})")
                else:
                    print(f"  -> {resp.status_code} (OK)")
                    return
            else:
                print(f"  -> not ready (HTTP {resp.status_code})")
        except Exception as e:
            print(f"  -> not ready ({e})")

        if time.time() >= deadline:
            raise RuntimeError(f"Timed out waiting for {url} after {timeout}s")
        time.sleep(interval)


def step_kafka_consume(step, config):
    topic = step["topic"]
    timeout_seconds = step.get("timeout_seconds", 30)
    expected_content = step.get("expected_content")
    
    if expected_content is None:
        expected_contents = []
    elif isinstance(expected_content, list):
        expected_contents = [str(item) for item in expected_content]
    else:
        expected_contents = [str(expected_content)]

    timeout_ms = timeout_seconds * 1000
    service = step.get("service", "kafka")

    cmd = compose_cmd(
        config,
        "exec", "-T", service,
        "/kafka/bin/kafka-console-consumer.sh",
        "--bootstrap-server", "kafka:9092",
        "--topic", topic,
        "--from-beginning",
        "--max-messages", "100",
        "--timeout-ms", str(timeout_ms),
    )
    result = run_cmd(cmd, check=False, capture_output=True)
    output = result.stdout + result.stderr

    if not expected_contents:
        return

    # Split output into individual lines (each representing a single message)
    lines = [line.strip() for line in output.splitlines() if line.strip()]

    # Look for a single message containing all expected substrings
    found = False
    for line in lines:
        if all(expected in line for expected in expected_contents):
            found = True
            break

    expected_str = ", ".join(f"'{e}'" for e in expected_contents)
    if not found:
        raise RuntimeError(
            f"Could not find any single message containing all expected substrings [{expected_str}] in Kafka topic '{topic}'.\n"
            f"Consumer output:\n{output[-2000:]}"
        )
    print(f"  -> Found all expected substrings [{expected_str}] in a single message (OK)")


def step_env_override(step, config):
    vars_ = step.get("vars", {})
    for key, value in vars_.items():
        print(f"  export {key}={value}")
        os.environ[key] = str(value)


def step_wait(step, config):
    seconds = step.get("seconds", 5)
    print(f"  Sleeping {seconds}s...")
    time.sleep(seconds)


def step_wait_for_log(step, config):
    service = substitute_vars(step["service"])
    expected_content = step.get("expected_content")
    if not expected_content or not isinstance(expected_content, str):
        raise RuntimeError(f"Step 'wait_for_log' requires 'expected_content' to be a non-empty string, but got {type(expected_content).__name__}")
    
    timeout_seconds = step.get("timeout_seconds", 30)
    interval = step.get("interval_seconds", 5)

    deadline = time.time() + timeout_seconds
    # Use --tail to keep log inspection efficient as suggested by Copilot
    cmd = compose_cmd(config, "logs", "--tail", "200", service)
    print(f"  Waiting for '{expected_content}' in {service} logs (timeout={timeout_seconds}s)")

    while True:
        # run directly to avoid spamming the console
        result = subprocess.run(cmd, check=False, capture_output=True, text=True, env=os.environ)
        output = result.stdout + result.stderr
        
        if expected_content in output:
            print(f"  -> Found '{expected_content}' in logs (OK)")
            return
            
        if time.time() >= deadline:
            raise RuntimeError(
                f"Expected '{expected_content}' not found in logs of service '{service}' within {timeout_seconds}s.\n"
                f"Logs (last 200 lines):\n{output}"
            )
        
        time.sleep(interval)


STEP_HANDLERS = {
    "docker_compose_up": step_docker_compose_up,
    "docker_compose_down": step_docker_compose_down,
    "docker_compose_build": step_docker_compose_build,
    "docker_compose_stop": step_docker_compose_stop,
    "docker_compose_exec": step_docker_compose_exec,
    "http_put": step_http_put,
    "http_post": step_http_post,
    "http_delete": step_http_delete,
    "http_wait": step_http_wait,
    "kafka_consume": step_kafka_consume,
    "env_override": step_env_override,
    "wait": step_wait,
    "wait_for_log": step_wait_for_log,
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

    steps = spec.get("steps") or []
    cleanup_step = spec.get("cleanup")

    failed = False
    name = "Unknown"
    step = {}
    try:
        for i, step_item in enumerate(steps, 1):
            step = step_item
            name = step.get("name", step.get("type", "Unknown"))
            print(f"\n[{i}/{len(steps)}] {name}")
            run_step(step, spec)
    except Exception as e:
        print(f"\n[FAIL] Step '{name}' failed: {e}")
        service = step.get("service", "connect")
        if service:
            print(f"\n[LOGS] Last 100 lines for service '{service}':")
            try:
                subprocess.run(compose_cmd(spec, "logs", "--tail", "100", service), check=False)
            except:
                pass
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
