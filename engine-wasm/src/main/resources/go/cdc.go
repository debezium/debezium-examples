package main

import (
    "encoding/json"
    "fmt"
    "unsafe"
)

func readBufferFromMemory(bufferPosition *uint32, length uint32) []byte {
    subjectBuffer := make([]byte, length)
    pointer := uintptr(unsafe.Pointer(bufferPosition))
    for i := 0; i < int(length); i++ {
        s := *(*int32)(unsafe.Pointer(pointer + uintptr(i)))
        subjectBuffer[i] = byte(s)
    }
    return subjectBuffer
}

//go:wasm-module cdc
//export change
func change(destinationPtr *uint32, destinationLength uint32, keyPtr *uint32, keyLength uint32, valuePtr *uint32, valueLength uint32) {
    destination := string(readBufferFromMemory(destinationPtr, destinationLength))
    keyBytes := readBufferFromMemory(keyPtr, keyLength)
    value := string(readBufferFromMemory(valuePtr, valueLength))

    var keyJson map[string] interface{}
    json.Unmarshal(keyBytes, &keyJson)
    fmt.Printf("Received message for destination '%s', with id = '%.0f' and content %s\n", destination, keyJson["id"].(float64), value)
}

func main() {
}

