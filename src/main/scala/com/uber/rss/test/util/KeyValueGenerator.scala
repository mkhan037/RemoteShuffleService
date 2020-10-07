/*
 * Copyright (c) 2020 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.rss.test.util

import org.apache.spark.internal.Logging
import org.apache.spark.util.LongAccumulator

class KeyValueGenerator(val mapId: Int,
                        val testValues: Seq[String],
                        val totalBytes: Long = 1024L*1024L,
                        val bytesAccumulator: LongAccumulator,
                        val recordsAccumulator: LongAccumulator)
  extends Iterator[(String, String)] with Logging {

  logInfo(s"Key value generator: mapId: $mapId, totalBytes: $totalBytes")

  private var startTime = 0L
  private val random = new java.util.Random()
  private var currentBytes = 0L
  private var lastLogTime = 0L
  private var lastLogBytes = 0L

  override def hasNext: Boolean = {
    if (startTime == 0) {
      startTime = System.currentTimeMillis()
    }
    
    val result = currentBytes < totalBytes
    if (!result) {
      logInfo(s"MapId: $mapId finished generating data")
      logStatus()
    }
    result
  }

  override def next(): (String, String) = {
    if (System.currentTimeMillis() - lastLogTime > 30000) {
      logStatus()
    }
    
    val key = testValues(random.nextInt(testValues.size))
    val value = testValues(random.nextInt(testValues.size))
    currentBytes = currentBytes + key.size + value.size
    bytesAccumulator.add(key.size + value.size)
    recordsAccumulator.add(1)

    key -> value
  }

  def logStatus() = {
    val progress = if (totalBytes == 0) {
      100
    } else {
      Math.min( currentBytes, totalBytes ) * 100 / totalBytes
    }
    val duration = System.currentTimeMillis() - lastLogTime
    val throughput = if (duration == 0) {
      "(unknown)"
    } else {
      (currentBytes - lastLogBytes).toDouble / (1024 * 1024) / (duration.toDouble / 1000.0) + " mb/s"
    }
    logInfo( s"MapId: $mapId, generated bytes: $currentBytes, progress: $progress%, throughput: $throughput" )
    lastLogTime = System.currentTimeMillis()
    lastLogBytes = currentBytes
  }
}