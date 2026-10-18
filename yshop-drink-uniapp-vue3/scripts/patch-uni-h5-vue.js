/**
 * 修复 @dcloudio/uni-h5-vue 与 @vue/shared 3.4.21+ 的兼容问题：
 * updateSlots 中 Object.assign 向只读的 slots._ 赋值，运行时报
 * “Cannot assign to read only property '_' of object”。
 * 官方修复为 def(children, "_", type, true)，即让 "_" 可写。
 */
const fs = require('fs')
const path = require('path')

const files = [
  'node_modules/@dcloudio/uni-h5-vue/dist/vue.runtime.esm.js',
  'node_modules/@dcloudio/uni-h5-vue/dist/vue.runtime.cjs.js',
]
const from = 'def(children, "_", type)'
const to = 'def(children, "_", type, true)'

for (const file of files) {
  const target = path.resolve(__dirname, '..', file)
  if (!fs.existsSync(target)) {
    continue
  }
  const source = fs.readFileSync(target, 'utf8')
  if (!source.includes(from)) {
    continue
  }
  fs.writeFileSync(target, source.replace(from, to))
  console.log(`[patch-uni-h5-vue] patched ${file}`)
}
