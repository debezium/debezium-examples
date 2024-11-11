#!/bin/bash

cd "$PWD/src/main/resources/go"
tinygo build -target=wasi -o ../compiled/cdc.wasm cdc.go
cd "$OLDPWD"
