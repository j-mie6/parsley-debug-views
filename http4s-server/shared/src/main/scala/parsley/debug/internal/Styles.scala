/*
 * Copyright 2023 Parsley Debug View Contributors <https://github.com/j-mie6/parsley-debug-views/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debug.internal

private [debug] object Styles {
  val primaryStylesheet: String = """
      |* {
      |  border-spacing: 0 !important;
      |}
      |
      |body {
      |  background-color: #f0f0f0;
      |  font-family: monospace;
      |  font-size: 12px;
      |}
      |
      |th {
      |  text-align: right;
      |  min-width: max-content;
      |}
      |
      |td {
      |  padding: 0;
      |  margin: 0;
      |}
      |
      |h1 {
      |  font-size: 48px;
      |}
      |
      |a {
      |  font-weight: normal !important;
      |}
      |
      |.folds-btn {
      |  font-family: monospace;
      |  font-weight: bold;
      |  font-size: 14px;
      |  text-align: center;
      |  margin-bottom: 2em;
      |}
      |
      |.large {
      |  font-size: 24px;
      |}
      |
      |.toc {
      |  display: flex;
      |  flex-direction: row;
      |  gap: 1em;
      |  align-content: flex-start;
      |  max-width: 40em;
      |  flex-wrap: wrap;
      |}
      |
      |.parser {
      |  margin: 0;
      |  padding: 0;
      |}
      |
      |.nickname {
      |  position: absolute;
      |  display: block;
      |
      |  align-self: center;
      |  text-align: center;
      |
      |  padding-left: 1.25em;
      |  padding-right: 1.25em;
      |
      |  top: -70%;
      |  left: 50%;
      |  margin-right: -50%;
      |  margin-bottom: -170%;
      |  transform: translate(-50%, 50%);
      |
      |  background-color: #f0f0f0;
      |  font-weight: bold;
      |  font-size: 14px;
      |
      |  z-index: 99 !important;
      |}
      |
      |.unloaded {
      |  min-width: 4em !important;
      |  min-height: 2em !important;
      |  background-color: #878787 !important;
      |  line-height: 1.75 !important;
      |  text-align: center !important;
      |  font-weight: bold !important;
      |}
      |
      |.dotted {
      |  border: 2px dashed black;
      |  padding: 1em;
      |  margin: 0.5em;
      |}
      |
      |.info {
      |  position: fixed;
      |  display: flex;
      |  visibility: hidden;
      |
      |  background-color: #ffffff;
      |  border: 2px solid;
      |  border-color: #212223;
      |
      |  opacity: 0;
      |  transition: visibility 0.25s, opacity 0.25s ease-in-out;
      |
      |  padding: 0.7em;
      |  left: 3em;
      |  bottom: 3em;
      |
      |  z-index: 999 !important;
      |  font-size: 18px !important;
      |
      |  max-height: 50vh;
      |  max-width: 75vw;
      |  overflow: scroll;
      |}
      |
      |.info th {
      |  padding-right: 1em !important;
      |}
      |
      |.info * {
      |  vertical-align: baseline;
      |}
      |
      |.attempt {
      |  position: relative;
      |  display: block;
      |
      |  margin: 0.5em;
      |  padding-top: 0.125em;
      |  padding-bottom: 0.125em;
      |  padding-left: 1em;
      |  padding-right: 1em;
      |}
      |
      |.attempt:hover {
      |  background-color: #ffff00 !important;
      |}
      |
      |.attempt:hover > .info, .info:hover {
      |  position: fixed;
      |  display: flex;
      |  visibility: visible;
      |  opacity: 1;
      |}
      |
      |.children:last-child > .attempt:hover .info {
      |  display: none !important;
      |}
      |
      |.overview {
      |  width: 100%;
      |  text-align: center;
      |  font-weight: bold;
      |  line-height: 1.75;
      |}
      |
      |.success {
      |  background-color: #87ff87;
      |}
      |
      |.failure {
      |  background-color: #ff8787;
      |}
      |
      |.children {
      |  padding: 0;
      |}
      |
      |.children * {
      |  vertical-align: top;
      |}
      |
      |.children > tr {
      |  padding: 0;
      |}""".stripMargin
}
