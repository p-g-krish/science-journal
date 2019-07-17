/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.google.android.apps.forscience.whistlepunk.metadata;

import static org.junit.Assert.assertEquals;

import com.google.android.apps.forscience.whistlepunk.data.nano.GoosciSensorConfig.BleSensorConfig.ScaleTransform;
import com.google.android.apps.forscience.whistlepunk.devicemanager.SensorTypeProvider;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ValueFilter;
import com.google.android.apps.forscience.whistlepunk.sensors.BluetoothSensor;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.migration.nano2lite.runtime.MigrateAs;
import com.google.protobuf.migration.nano2lite.runtime.MigrateAs.Destination;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BleSensorTest {
  @Test
  public void testPresetsVsCustom() {
    BleSensorSpec bleSensor = new BleSensorSpec("address", "name");
    bleSensor.setCustomFrequencyEnabled(true);
    bleSensor.setCustomPin("A7");

    assertEquals(SensorTypeProvider.TYPE_RAW, bleSensor.getSensorType());
    assertEquals(false, bleSensor.getFrequencyEnabled());
    assertEquals("A0", bleSensor.getPin());

    bleSensor.setSensorType(SensorTypeProvider.TYPE_CUSTOM);
    assertEquals(SensorTypeProvider.TYPE_CUSTOM, bleSensor.getSensorType());
    assertEquals(true, bleSensor.getFrequencyEnabled());
    assertEquals("A7", bleSensor.getPin());

    bleSensor.setSensorType(SensorTypeProvider.TYPE_ROTATION);
    assertEquals(SensorTypeProvider.TYPE_ROTATION, bleSensor.getSensorType());
    assertEquals(true, bleSensor.getFrequencyEnabled());
    assertEquals("A0", bleSensor.getPin());

    // Custom pin is unused, but still stored
    assertEquals("A7", bleSensor.getCustomPin());

    // Custom settings have not been overridden
    bleSensor.setSensorType(SensorTypeProvider.TYPE_CUSTOM);
    assertEquals(SensorTypeProvider.TYPE_CUSTOM, bleSensor.getSensorType());
    assertEquals(true, bleSensor.getFrequencyEnabled());
    assertEquals("A7", bleSensor.getPin());
  }

  @Test
  public void testScaleFilter() {
    BleSensorSpec bleSensor = new BleSensorSpec("address", "name");
    bleSensor.setCustomFrequencyEnabled(false);

    @MigrateAs(Destination.BUILDER)
    ScaleTransform transform = new ScaleTransform();
    transform.sourceBottom = 0;
    transform.sourceTop = 10;
    transform.destBottom = 0;
    transform.destTop = 100;

    bleSensor.setSensorType(SensorTypeProvider.TYPE_CUSTOM);
    bleSensor.setCustomScaleTransform(transform);
    assertEquals(transform, bleSensor.getScaleTransform());

    bleSensor.setSensorType(SensorTypeProvider.TYPE_ROTATION);
    assertEquals(null, bleSensor.getScaleTransform());

    bleSensor.setSensorType(SensorTypeProvider.TYPE_RAW);
    ValueFilter computedFilter =
        new BluetoothSensor(
                "sensorId",
                bleSensor,
                BluetoothSensor.ANNING_SERVICE_SPEC,
                MoreExecutors.directExecutor())
            .getDeviceDefaultValueFilter();
    assertEquals(0.0, computedFilter.filterValue(0, 0.0), 0.001);
    assertEquals(50.0, computedFilter.filterValue(0, 511.5), 0.001);
    assertEquals(100.0, computedFilter.filterValue(0, 1023.0), 0.001);
  }

  @Test
  public void loggingId() {
    BleSensorSpec spec = new BleSensorSpec("address", "name");
    spec.setSensorType(SensorTypeProvider.TYPE_ROTATION);
    assertEquals("bluetooth_le:rot", spec.getLoggingId());
    spec.setSensorType(SensorTypeProvider.TYPE_CUSTOM);
    assertEquals("bluetooth_le:cus", spec.getLoggingId());
    spec.setSensorType(SensorTypeProvider.TYPE_RAW);
    assertEquals("bluetooth_le:raw", spec.getLoggingId());
  }
}
