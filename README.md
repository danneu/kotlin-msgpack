
# kotlin-msgpack

A simple library for encoding/decoding [Msgpack][msgpack]
without reflection.

[msgpack]: http://msgpack.org/index.html

## Quickstart

I'll show a round-trip from Kotlin → Msgpack → Kotlin.

Here's, our `User` class with an encoder and decoder defined so that
we can send and receive users over the wire.

```kotlin
import com.danneu.msgpack.Encoder
import com.danneu.msgpack.Decoder

data class User(
  val uname: String,
  val colors: List<String>,
  val age: Int,
  val something: Boolean? = null
) {
  companion object {
    val decoder: Decoder<User> = Decoder.map(
      ::User,
      Decoder.str,
      Decoder.listOf(Decoder.str),
      Decoder.int,
      Decoder.nullable(Decoder.bool)
    )

    val encoder = Encoder<User> {
      Encoder.listOf(
        Encoder.str(uname),
        Encoder.listOf(colors.map { Encoder.str(it) }),
        Encoder.int(age),
        Encoder.nullable(Encoder.bool)(something)
      )
    }
  }
}
```

Here's the round-trip.

```kotlin
import org.msgpack.core.MessagePack

fun main(args: Array<String>) {
  val user1 = User("dan", listOf("black", "orange"), 28, null)

  val bytes = MessagePack.newDefaultBufferPacker().use { packer ->
    User.encoder(user1)(packer)
    packer.toByteArray()
  }

  println("bytes packed: ${bytes.size}") // bytes packed: 21

  val user2 = MessagePack.newDefaultUnpacker(bytes).use { unpacker ->
    User.decoder.unpack(unpacker)
  }

  println("user1: $user1") // user1: User(uname=dan, colors=[black, orange], age=28, something=null)
  println("user2: $user2") // user2: User(uname=dan, colors=[black, orange], age=28, something=null))
  println("are they equal? ${user1 == user2}") // are the equal? true
}
```

Here's how the encoder / decoder code above would look in vanilla Msgpack:

```kotlin
data class User(val uname: String, val colors: List<String>, val age: Int, val something: Boolean? = null) {
  fun encode(): ByteArray {
    MessagePack.newDefaultBufferPacker().use { packer ->
      packer.packArrayHeader(5)
      packer.packString(uname)
      packer.packArrayHeader(colors.size)
      colors.forEach { packer.packString(it) }
      packer.packInt(age)
      if (something == null) {
        packer.packNil()
      } else {
        packer.packBoolean(something)
      }
      packer.toByteArray()
    }
  }

  fun decode(bytes: ByteArray): User {
    MessagePack.newDefaultUnpacker(bytes).use { unpacker ->
      unpacker.unpackArrayHeader()
      val uname = unpacker.unpackString()
      val colorCount = unpacker.unpackArrayHeader()
      val colors = mutableListOf<String>()
      for (i in 0 until colorCount) {
        colors.add(unpacker.unpackString())
      }
      val age = unpacker.unpackInt()
      val something: Boolean? = unpacker.unpackValue().let { value ->
        when (value.valueType) {
          ValueType.BOOLEAN ->
            value.asBooleanValue()
          else ->
            value.asNilValue()
        }
      }

      User(uname, colors, age, something)
    }
  }
```

## Encoders

**TODO**

## Decoders

**TODO**

### `Decoder.value`

Here's an example that generates a random `Any?` value for the `user.weird`
property and then round-trips it with the `Decoder.value` decoder.


```kotlin
import com.danneu.msgpack.Encoder
import com.danneu.msgpack.Decoder

data class User(
  val uname: String,
  val colors: List<String>,
  val age: Int,
  val something: Boolean? = null,
  val weird: Any? = null
) {
  companion object {
    val decoder: Decoder<User> = Decoder.map(
      ::User,
      Decoder.str,
      Decoder.listOf(Decoder.str),
      Decoder.int,
      Decoder.nullable(Decoder.bool),
      Decoder.value
    )

    val encoder = Encoder<User> {
      Encoder.listOf(
        Encoder.str(uname),
        Encoder.listOf(colors.map { Encoder.str(it) }),
        Encoder.int(age),
        Encoder.nullable(Encoder.bool)(something),
        Encoder.value(weird)
      )
    }

    fun randomValue(): Any? {
      val random = Random()
      val min = 1
      val max = 5
      var roll = random.nextInt(max + 1 - min) + min
      return when (roll) {
        1 -> null
        2 -> random.nextLong()
        3 -> random.nextBoolean()
        4 -> mapOf("foo" to 42, "bar" to mapOf("key" to "hell"))
        else -> listOf(random.nextInt(), random.nextInt(), random.nextInt())
      }
    }
  }
}
```
