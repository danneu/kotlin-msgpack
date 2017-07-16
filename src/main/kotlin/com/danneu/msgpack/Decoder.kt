package com.danneu.msgpack

import org.msgpack.core.MessageUnpacker
import org.msgpack.value.Value
import org.msgpack.value.ValueType

abstract class Decoder <out T> {
    operator fun invoke(unpacker: MessageUnpacker): T = unpack(unpacker)

    abstract fun unpack (unpacker: MessageUnpacker): T

    fun <T2> andThen (f: (T) -> Decoder<T2>): Decoder<T2> = object : Decoder<T2>() {
        override fun unpack(unpacker: MessageUnpacker): T2 {
            return f(this@Decoder.unpack(unpacker)).unpack(unpacker)
        }
    }

    fun <T2> map (f: (T) -> T2): Decoder<T2> = object : Decoder<T2>() {
        override fun unpack(unpacker: MessageUnpacker): T2 {
            return f(this@Decoder.unpack(unpacker))
        }
    }

    companion object {
        fun <T> nullable(decoder: Decoder<T>): Decoder<T?> = object : Decoder<T?>() {
            override fun unpack(unpacker: MessageUnpacker): T? {
                if (!unpacker.hasNext()) {
                    throw Exception("Ran out of buffer in nullable() decoder.")
                }
                val nextFormat = unpacker.nextFormat.valueType
                return when (nextFormat) {
                    ValueType.NIL ->
                        unpacker.unpackNil().let { null }
                    else ->
                        decoder.unpack(unpacker)
                }
            }
        }

        val bool: Decoder<Boolean> = object : Decoder<Boolean>() {
            override fun unpack(unpacker: MessageUnpacker): Boolean {
                return unpacker.unpackBoolean()
            }
        }

        val str: Decoder<String> = object : Decoder<String>() {
            override fun unpack(unpacker: MessageUnpacker) = unpacker.unpackString()
        }

        val float: Decoder<Float> = object : Decoder<Float>() {
            override fun unpack(unpacker: MessageUnpacker) = unpacker.unpackFloat()
        }

        val int: Decoder<Int> = object : Decoder<Int>() {
            override fun unpack(unpacker: MessageUnpacker): Int {
                return unpacker.unpackInt()
            }
        }

        fun <T> listOf(decoder: Decoder<T>): Decoder<List<T>> = object : Decoder<List<T>>() {
            override fun unpack(unpacker: MessageUnpacker): List<T> {
                val count = unpacker.unpackArrayHeader()
                return (0 until count).map {
                    decoder.unpack(unpacker)
                }
            }
        }

        fun <T> succeed(value: T): Decoder<T> = object : Decoder<T>() {
            override fun unpack(unpacker: MessageUnpacker): T = value
        }

        // VALUE

        val value = object : Decoder<Any?>() {
            fun decodeValue(v: Value): Any? {
                return when (v.valueType) {
                    ValueType.NIL ->
                        null
                    ValueType.BOOLEAN ->
                        v.asBooleanValue().boolean
                    ValueType.INTEGER ->
                        v.asIntegerValue().let { iv ->
                            when {
                                iv.isInIntRange ->
                                    iv.toInt()
                                iv.isInLongRange ->
                                    iv.toLong()
                                else ->
                                    iv.toBigInteger()
                            }
                        }
                    ValueType.FLOAT ->
                        v.asFloatValue().toFloat()
                    ValueType.STRING ->
                        v.asStringValue().asString()
                    ValueType.BINARY ->
                        v.asBinaryValue().asByteArray()
                    ValueType.EXTENSION ->
                        throw Exception("ValueType.EXTENSION not supported")
                    ValueType.ARRAY ->
                        listOf(v.asArrayValue().map { ev -> decodeValue(ev) })
                    ValueType.MAP ->
                        v.asMapValue().asMapValue().keyValueArray.let { arr ->
                            (0 until arr.size step 2).map {
                                decodeValue(arr[it]!!) to decodeValue(arr[it + 1]!!)
                            }.toMap()
                        }
                }
            }

            override fun unpack(unpacker: MessageUnpacker): Any? {
                val v = unpacker.unpackValue()
                return decodeValue(v)
            }
        }

        // MAPPING

        fun <V1, T> map(f: (V1) -> T, d1: Decoder<V1>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker)) } }
        fun <V1, V2, T> map(f: (V1, V2) -> T, d1: Decoder<V1>, d2: Decoder<V2>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker)) } }
        fun <V1, V2, V3, T> map(f: (V1, V2, V3) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker)) } }
        fun <V1, V2, V3, V4, T> map(f: (V1, V2, V3, V4) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>, d4: Decoder<V4>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker), d4.unpack(unpacker)) } }
        fun <V1, V2, V3, V4, V5, T> map(f: (V1, V2, V3, V4, V5) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>, d4: Decoder<V4>, d5: Decoder<V5>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker), d4.unpack(unpacker), d5.unpack(unpacker)) } }
        fun <V1, V2, V3, V4, V5, V6, T> map(f: (V1, V2, V3, V4, V5, V6) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>, d4: Decoder<V4>, d5: Decoder<V5>, d6: Decoder<V6>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker), d4.unpack(unpacker), d5.unpack(unpacker), d6.unpack(unpacker)) } }
        fun <V1, V2, V3, V4, V5, V6, V7, T> map(f: (V1, V2, V3, V4, V5, V6, V7) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>, d4: Decoder<V4>, d5: Decoder<V5>, d6: Decoder<V6>, d7: Decoder<V7>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker), d4.unpack(unpacker), d5.unpack(unpacker), d6.unpack(unpacker), d7.unpack(unpacker)) } }
        fun <V1, V2, V3, V4, V5, V6, V7, V8, T> map(f: (V1, V2, V3, V4, V5, V6, V7, V8) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>, d4: Decoder<V4>, d5: Decoder<V5>, d6: Decoder<V6>, d7: Decoder<V7>, d8: Decoder<V8>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker), d4.unpack(unpacker), d5.unpack(unpacker), d6.unpack(unpacker), d7.unpack(unpacker), d8.unpack(unpacker)) } }
        fun <V1, V2, V3, V4, V5, V6, V7, V8, V9, T> map(f: (V1, V2, V3, V4, V5, V6, V7, V8, V9) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>, d4: Decoder<V4>, d5: Decoder<V5>, d6: Decoder<V6>, d7: Decoder<V7>, d8: Decoder<V8>, d9: Decoder<V9>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker), d4.unpack(unpacker), d5.unpack(unpacker), d6.unpack(unpacker), d7.unpack(unpacker), d8.unpack(unpacker), d9.unpack(unpacker)) } }
        fun <V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, T> map(f: (V1, V2, V3, V4, V5, V6, V7, V8, V9, V10) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>, d4: Decoder<V4>, d5: Decoder<V5>, d6: Decoder<V6>, d7: Decoder<V7>, d8: Decoder<V8>, d9: Decoder<V9>, d10: Decoder<V10>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker), d4.unpack(unpacker), d5.unpack(unpacker), d6.unpack(unpacker), d7.unpack(unpacker), d8.unpack(unpacker), d9.unpack(unpacker), d10.unpack(unpacker)) } }
        fun <V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, T> map(f: (V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>, d4: Decoder<V4>, d5: Decoder<V5>, d6: Decoder<V6>, d7: Decoder<V7>, d8: Decoder<V8>, d9: Decoder<V9>, d10: Decoder<V10>, d11: Decoder<V11>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker), d4.unpack(unpacker), d5.unpack(unpacker), d6.unpack(unpacker), d7.unpack(unpacker), d8.unpack(unpacker), d9.unpack(unpacker), d10.unpack(unpacker), d11.unpack(unpacker)) } }
        fun <V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, T> map(f: (V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12) -> T, d1: Decoder<V1>, d2: Decoder<V2>, d3: Decoder<V3>, d4: Decoder<V4>, d5: Decoder<V5>, d6: Decoder<V6>, d7: Decoder<V7>, d8: Decoder<V8>, d9: Decoder<V9>, d10: Decoder<V10>, d11: Decoder<V11>, d12: Decoder<V12>): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(d1.unpack(unpacker), d2.unpack(unpacker), d3.unpack(unpacker), d4.unpack(unpacker), d5.unpack(unpacker), d6.unpack(unpacker), d7.unpack(unpacker), d8.unpack(unpacker), d9.unpack(unpacker), d10.unpack(unpacker), d11.unpack(unpacker), d12.unpack(unpacker)) } }
    }
}


