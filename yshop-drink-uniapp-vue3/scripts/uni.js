#!/usr/bin/env node
/**
 * 本项目是 HBuilderX 风格的 uni-app 工程，源码位于项目根目录；
 * uni-app CLI 默认从 src/ 读取，因此这里先把 UNI_INPUT_DIR 指向根目录，
 * 再交给官方 uni CLI。
 */
const path = require('path')

process.env.UNI_INPUT_DIR = path.resolve(__dirname, '..')

require('@dcloudio/vite-plugin-uni/bin/uni.js')
