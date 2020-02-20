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

package stroom.dashboard.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonPropertyOrder({"width", "height"})
@JsonInclude(Include.NON_DEFAULT)
@XmlRootElement(name = "size")
@XmlType(name = "Size", propOrder = {"width", "height"})
public class Size {
    @XmlTransient
    @JsonIgnore
    private int[] size = new int[]{200, 200};

    public Size() {
    }

    @XmlElement(name = "width")
    @JsonProperty("width")
    public int getWidth() {
        return size[0];
    }

    @JsonProperty("width")
    public void setWidth(final int width) {
        size[0] = width;
    }

    @XmlElement(name = "height")
    @JsonProperty("height")
    public int getHeight() {
        return size[1];
    }

    @JsonProperty("height")
    public void setHeight(final int height) {
        size[1] = height;
    }

    @JsonIgnore
    public void set(final int dimension, final int size) {
        this.size[dimension] = size;
    }

    @JsonIgnore
    public void set(final int[] size) {
        this.size[0] = size[0];
        this.size[1] = size[1];
    }

    @JsonIgnore
    public int get(final int dimension) {
        return this.size[dimension];
    }

    @JsonIgnore
    public int[] get() {
        return size;
    }

    @Override
    public String toString() {
        return Arrays.toString(size);
    }
}
