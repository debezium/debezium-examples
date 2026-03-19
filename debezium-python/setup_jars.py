#!/usr/bin/env python3
"""
Setup script to copy Debezium JARs to the pydbzengine package installation directory.

This script:
1. Downloads Debezium 3.0+ JARs using Maven
2. Finds the pydbzengine installation directory
3. Copies the JARs to pydbzengine's debezium/libs folder

Usage:
    python3 setup_jars.py
"""

import os
import shutil
import subprocess
import sys
from pathlib import Path


def run_command(cmd, cwd=None):
    """Run a shell command and return the result."""
    print(f"\n>>> Running: {cmd}")
    result = subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error: {result.stderr}")
        return False
    print(result.stdout)
    return True


def main():
    print("="*80)
    print("Debezium JAR Setup for pydbzengine Connect Mode")
    print("="*80)
    
    # Step 1: Check Maven is installed
    print("\n[1/5] Checking Maven installation...")
    result = subprocess.run("mvn --version", shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(" Maven not found. Please install Maven first:")
        print("    sudo apt update && sudo apt install maven")
        sys.exit(1)
    print("✓ Maven is installed")
    
    # Step 2: Download JARs using Maven
    print("\n[2/5] Downloading Debezium 3.0+ JARs...")
    project_dir = Path(__file__).parent
    temp_libs_dir = project_dir / "temp_libs"
    
    if not run_command(
        f'mvn clean dependency:copy-dependencies -DoutputDirectory="{temp_libs_dir}"',
        cwd=project_dir
    ):
        print(" Failed to download JARs")
        sys.exit(1)
    
    # Step 3: Find pydbzengine installation
    print("\n[3/5] Finding pydbzengine installation...")
    try:
        import pydbzengine
        pydbz_path = Path(pydbzengine.__file__).parent
        print(f"✓ Found pydbzengine at: {pydbz_path}")
    except ImportError:
        print(" pydbzengine not found. Please install it first:")
        print("    pip install pydbzengine>=3.4.1.0")
        sys.exit(1)
    
    # Step 4: Create debezium/libs directory in pydbzengine
    print("\n[4/5] Setting up pydbzengine JAR directory...")
    debezium_libs_dir = pydbz_path / "debezium" / "libs"
    
    # Clean old JARs to prevent version conflicts
    if debezium_libs_dir.exists():
        print(f"⚠ Cleaning old JARs to prevent version conflicts...")
        old_jar_count = len(list(debezium_libs_dir.glob("*.jar")))
        if old_jar_count > 0:
            print(f"  Removing {old_jar_count} old JAR files...")
            for old_jar in debezium_libs_dir.glob("*.jar"):
                old_jar.unlink()
            print(f"  ✓ Cleaned {old_jar_count} old JARs")
    
    debezium_libs_dir.mkdir(parents=True, exist_ok=True)
    print(f"✓ Target directory: {debezium_libs_dir}")
    
    # Step 5: Copy JARs
    print("\n[5/5] Copying JARs to pydbzengine...")
    jar_count = 0
    for jar_file in temp_libs_dir.glob("*.jar"):
        shutil.copy2(jar_file, debezium_libs_dir)
        jar_count += 1
        print(f"  ✓ Copied: {jar_file.name}")
    
    # Cleanup temp directory
    shutil.rmtree(temp_libs_dir)
    
    print("\n" + "="*80)
    print(f" SUCCESS! Copied {jar_count} JAR files to pydbzengine")
    print("="*80)
    print("\nYou can now run the Connect mode test:")
    print("    python3 connect_mode_test.py")
    print()


if __name__ == "__main__":
    main()
