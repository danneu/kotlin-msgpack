package com.danneu.msgpack

import org.msgpack.core.MessagePacker
import java.math.BigInteger

// Couldn't think of better names
typealias Runner = (MessagePacker) -> Unit
typealias Factory <T> = (T) -> Runner

class Encoder <in T> (val init: T.() -> Runner) {
    operator fun invoke(item: T) = item.init()

    companion object {
        val nil: Runner = { packer -> packer.packNil() }
        val str: Factory<String> = { v -> { packer -> packer.packString(v) }}
        val bool: Factory<Boolean> = { v -> { packer -> packer.packBoolean(v) }}
        val int: Factory<Int> = { v -> { packer -> packer.packInt(v) }}
        val long: Factory<Long> = { v -> { packer -> packer.packLong(v) }}
        val short: Factory<Short> = { v -> { packer -> packer.packShort(v) }}
        val bigint: Factory<BigInteger> = { v -> { packer -> packer.packBigInteger(v) }}
        val double: Factory<Double> = { v -> { packer -> packer.packDouble(v) }}
        val float: Factory<Float> = { v -> { packer -> packer.packFloat(v) }}
        val bytes: Factory<ByteArray> = { v -> { packer -> packer.packBinaryHeader(v.size); packer.writePayload(v) }}
        val mapOf: Factory<Map<Runner, Runner>> = { v -> { packer ->
            packer.packMapHeader(v.size)
            v.forEach { (a, b) -> a(packer); b(packer) }
        }}

        fun listOf(runners: List<Runner>): Runner = { packer ->
            packer.packArrayHeader(runners.size)
            runners.forEach { it(packer) }
        }

        fun listOf(vararg runners: Runner) = listOf(runners.toList())

        // Higher order

        fun <T> nullable(encoder: Factory<T>): Factory<T?> = { v -> { packer ->
            when (v) {
                null -> nil(packer)
                else -> encoder(v)(packer)
            }}}

        // Special

        val value: Factory<Any?> by lazy {
            { v: Any? -> { packer: MessagePacker ->
                println("v was  $v")
                when (v) {
                    null -> nil(packer)
                    is Boolean -> bool(v)(packer)
                    is BigInteger -> bigint(v)(packer)
                    is Long -> long(v)(packer)
                    is Int -> int(v)(packer)
                    is Float -> float(v)(packer)
                    is String -> str(v)(packer)
                    is ByteArray -> bytes(v)(packer)
                    is List<*> -> Encoder.listOf(v.map { n -> value(n) })(packer)
                    is Map<*, *> -> mapOf(v.map { (a, b) -> value(a) to value(b) }.toMap())(packer)
                    else -> throw Error("unexpected value(v): $v")
                }
            }}
        }
    }
}
