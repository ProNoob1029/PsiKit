// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package org.psilynx.psikit.core;

import edu.wpi.first.networktables.*;
import java.util.HashMap;
import java.util.Map;

/** Publishes log data using NT4. */
public class NT4Publisher implements LogDataReceiver {
  private final NetworkTable akitTable;
  private LogTable lastTable = new LogTable(0);
  private final IntegerPublisher timestampPublisher;
  private final Map<String, GenericPublisher> publishers = new HashMap<>();
  private final Map<String, String> units = new HashMap<>();

  /** Creates a new NT4Publisher. */
  public NT4Publisher() {
    akitTable = NetworkTableInstance.getDefault().getTable("/AdvantageKit");
    timestampPublisher =
        akitTable.getIntegerTopic(timestampKey.substring(1)).publish(PubSubOption.sendAll(true));
  }

  public void putTable(LogTable table) {
    // Send timestamp
    timestampPublisher.set((long) table.getTimestamp(), (long) table.getTimestamp());

    // Get old and new data
    Map<String, LogTable.LogValue> newMap = table.getAll(false);
    Map<String, LogTable.LogValue> oldMap = lastTable.getAll(false);

    // Encode new/changed fields
    for (Map.Entry<String, LogTable.LogValue> field : newMap.entrySet()) {
      // Check if field has changed
      LogTable.LogValue newValue = field.getValue();
      if (newValue.equals(oldMap.get(field.getKey()))) {
        continue;
      }

      // Create publisher if necessary
      String key = field.getKey().substring(1);
      String unit = /*field.getValue().unitStr*/ "";
      GenericPublisher publisher = publishers.get(key);
      if (publisher == null) {
        publisher =
            akitTable
                .getTopic(key)
                .genericPublish(field.getValue().getNT4Type(), PubSubOption.sendAll(true));
        publishers.put(key, publisher);

        // Set initial unit
        if (unit != null) {
          akitTable.getTopic(key).setProperty("unit", "\"" + unit + "\"");
          units.put(key, unit);
        }
      }

      // Check if unit changed
      if (unit != null && !unit.equals(units.get(key))) {
        akitTable.getTopic(key).setProperty("unit", "\"" + unit + "\"");
        units.put(key, unit);
      }

      // Write new data
      switch (field.getValue().type) {
        case Raw:
          publisher.setRaw(field.getValue().getRaw(), (long) table.getTimestamp());
          break;
        case Boolean:
          publisher.setBoolean(field.getValue().getBoolean(), (long) table.getTimestamp());
          break;
        case BooleanArray:
          publisher.setBooleanArray(field.getValue().getBooleanArray(), (long) table.getTimestamp());
          break;
        case Integer:
          publisher.setInteger(field.getValue().getInteger(), (long) table.getTimestamp());
          break;
        case IntegerArray:
          publisher.setIntegerArray(field.getValue().getIntegerArray(), (long) table.getTimestamp());
          break;
        case Float:
          publisher.setFloat(field.getValue().getFloat(), (long) table.getTimestamp());
          break;
        case FloatArray:
          publisher.setFloatArray(field.getValue().getFloatArray(), (long) table.getTimestamp());
          break;
        case Double:
          publisher.setDouble(field.getValue().getDouble(), (long) table.getTimestamp());
          break;
        case DoubleArray:
          publisher.setDoubleArray(field.getValue().getDoubleArray(), (long) table.getTimestamp());
          break;
        case String:
          publisher.setString(field.getValue().getString(), (long) table.getTimestamp());
          break;
        case StringArray:
          publisher.setStringArray(field.getValue().getStringArray(), (long) table.getTimestamp());
          break;
      }
    }

    // Update last table
    lastTable = table;
  }
}
