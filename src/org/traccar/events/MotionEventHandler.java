/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.traccar.BaseEventHandler;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class MotionEventHandler extends BaseEventHandler {

    private static final double SPEED_THRESHOLD = 0.01;
    private int suppressRepeated;

    public MotionEventHandler() {
        suppressRepeated = Context.getConfig().getInteger("event.suppressRepeated", 60);
    }

    @Override
    protected Collection<Event> analyzePosition(Position position) {

        Device device = Context.getIdentityManager().getDeviceById(position.getDeviceId());
        if (device == null) {
            return null;
        }
        if (!Context.getDeviceManager().isLatestPosition(position) || !position.getValid()) {
            return null;
        }

        Collection<Event> result = null;
        double speed = position.getSpeed();
        double oldSpeed = 0;
        Position lastPosition = Context.getDeviceManager().getLastPosition(position.getDeviceId());
        if (lastPosition != null) {
            oldSpeed = lastPosition.getSpeed();
        }
        try {
            if (speed > SPEED_THRESHOLD && oldSpeed <= SPEED_THRESHOLD) {
                result = new ArrayList<>();
                result.add(new Event(Event.TYPE_DEVICE_MOVING, position.getDeviceId(), position.getId()));
            } else if (speed <= SPEED_THRESHOLD && oldSpeed > SPEED_THRESHOLD) {
                result = new ArrayList<>();
                result.add(new Event(Event.TYPE_DEVICE_STOPPED, position.getDeviceId(), position.getId()));
            }

            if (result != null && !result.isEmpty()) {
                for (Event event : result) {
                    if (!Context.getDataManager().getLastEvents(position.getDeviceId(),
                            event.getType(), suppressRepeated).isEmpty()) {
                        event = null;
                    }
                }
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
        return result;
    }

}
