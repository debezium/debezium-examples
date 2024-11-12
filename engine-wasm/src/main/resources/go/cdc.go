package main

import (
    "encoding/json"
    "fmt"
    "unsafe"
)

// alloc/free implementation from:
// https://github.com/tinygo-org/tinygo/blob/2a76ceb7dd5ea5a834ec470b724882564d9681b3/src/runtime/arch_tinygowasm_malloc.go#L7
var allocs = make(map[uintptr][]byte)

//go:wasm-module cdc
//export malloc
func libc_malloc(size uintptr) unsafe.Pointer {
	if size == 0 {
		return nil
	}
	buf := make([]byte, size)
	ptr := unsafe.Pointer(&buf[0])
	allocs[uintptr(ptr)] = buf
	return ptr
}

//go:wasm-module cdc
//export free
func libc_free(ptr unsafe.Pointer) {
	if ptr == nil {
		return
	}
	if _, ok := allocs[uintptr(ptr)]; ok {
		delete(allocs, uintptr(ptr))
	} else {
		panic("free: invalid pointer")
	}
}

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

