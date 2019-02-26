/*
 * Copyright 2016 Crown Copyright
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

package stroom.volume;

import stroom.node.shared.VolumeEntity;

import java.util.ArrayList;
import java.util.List;

class VolumeListUtil {
    static List<VolumeEntity> removeFullVolumes(final List<VolumeEntity> list) {
        final List<VolumeEntity> newList = new ArrayList<>(list.size());
        for (final VolumeEntity volume : list) {
            if (!volume.isFull()) {
                newList.add(volume);
            }
        }
        return newList;
    }

    static List<VolumeEntity> removeVolumesWithoutValidState(final List<VolumeEntity> list) {
        final List<VolumeEntity> newList = new ArrayList<>(list.size());
        for (final VolumeEntity volume : list) {
            if (volume.getVolumeState() != null && volume.getVolumeState().getBytesUsed() != null
                    && volume.getVolumeState().getBytesFree() != null
                    && volume.getVolumeState().getBytesTotal() != null) {
                newList.add(volume);
            }
        }
        return newList;
    }
}
