
const arity = 12 

function templateOneLiners (n) {
  // 'V1, ..., V20' -- does not end in comma
  const gen1 = Array.apply(null, { length: n }).map((_, i) => `V${i+1}`).join(', ')
  // 'd1: Decoder<V1>, ..., d20: Decoder<V20>'
  const gen2 = Array.apply(null, { length: n }).map((_, i) => {
    return `d${i+1}: Decoder<V${i+1}>`
  }).join(', ')
  // the return, 'd1.unpack(unpacker), ... d5.unpack(unpacker)'
  const gen3 = Array.apply(null, { length: n }).map((_, i) => {
    return `d${i+1}.unpack(unpacker)`
  }).join(', ')

  return `
fun <${gen1}, T> map(f: (${gen1}) -> T, ${gen2}): Decoder<T> = object : Decoder<T>() { override fun unpack(unpacker: MessageUnpacker): T { unpacker.unpackArrayHeader(); return f(${gen3}) } }`.trim()
}

////////////////////////////////////////////////

function templateIndent (n) {
  // 'V1, ..., V20' -- does not end in comma
  const gen1 = Array.apply(null, { length: n }).map((_, i) => `V${i+1}`).join(', ')
  // 'd1: Decoder<V1>, ..., d20: Decoder<V20>'
  const gen2 = Array.apply(null, { length: n }).map((_, i) => {
    return `d${i+1}: Decoder<V${i+1}>`
  }).join(', ')
  // the return, 'd1.unpack(unpacker), ... d5.unpack(unpacker)'
  const gen3 = Array.apply(null, { length: n }).map((_, i) => {
    return `d${i+1}.unpack(unpacker)`
  }).join(', ')

  return `
fun <${gen1}, T> map(f: (${gen1}) -> T, ${gen2}): Decoder<T> = object : Decoder<T>() {
    override fun unpack(unpacker: MessageUnpacker): T {
        unpacker.unpackArrayHeader()
        return f(${gen3})
    }
}`.trim()

}

function gen () {
  const allCode = Array.apply(null, { length: arity }).map((_, i) => {
    return templateOneLiners(i+1)
  }).join('\n')

  console.log(allCode)
}

gen()
