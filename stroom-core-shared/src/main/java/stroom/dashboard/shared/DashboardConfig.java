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

import stroom.docref.HasDisplayValue;
import stroom.query.api.v2.TimeRange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonPropertyOrder({"parameters", "timeRange", "components", "layout", "tabVisibility"})
@JsonInclude(Include.NON_NULL)
@XmlRootElement(name = "dashboard")
@XmlType(name = "DashboardConfig", propOrder = {"parameters", "timeRange", "components", "layout", "tabVisibility"})
public class DashboardConfig {

    @XmlElement(name = "parameters")
    @JsonProperty("parameters")
    private String parameters;
    @XmlElement(name = "timeRange")
    @JsonProperty("timeRange")
    private TimeRange timeRange;
    @XmlElementWrapper(name = "components")
    @XmlElements({@XmlElement(name = "component", type = ComponentConfig.class)})
    @JsonProperty("components")
    private List<ComponentConfig> components;
    @XmlElements({@XmlElement(name = "splitLayout", type = SplitLayoutConfig.class),
            @XmlElement(name = "tabLayout", type = TabLayoutConfig.class)})
    @JsonProperty("layout")
    private LayoutConfig layout;
    @XmlElement(name = "tabVisibility")
    @JsonProperty("tabVisibility")
    private TabVisibility tabVisibility;

    public DashboardConfig() {
        tabVisibility = TabVisibility.SHOW_ALL;
    }

    @JsonCreator
    public DashboardConfig(@JsonProperty("parameters") final String parameters,
                           @JsonProperty("timeRange") final TimeRange timeRange,
                           @JsonProperty("components") final List<ComponentConfig> components,
                           @JsonProperty("layout") final LayoutConfig layout,
                           @JsonProperty("tabVisibility") final TabVisibility tabVisibility) {
        this.parameters = parameters;
        this.timeRange = timeRange;
        this.components = components;
        this.layout = layout;
        this.tabVisibility = tabVisibility;

        if (this.tabVisibility == null) {
            this.tabVisibility = TabVisibility.SHOW_ALL;
        }
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(final TimeRange timeRange) {
        this.timeRange = timeRange;
    }

    public List<ComponentConfig> getComponents() {
        return components;
    }

    public void setComponents(final List<ComponentConfig> components) {
        this.components = components;
    }

    public LayoutConfig getLayout() {
        return layout;
    }

    public void setLayout(final LayoutConfig layout) {
        this.layout = layout;
    }

    public TabVisibility getTabVisibility() {
        return tabVisibility;
    }

    public void setTabVisibility(final TabVisibility tabVisibility) {
        this.tabVisibility = tabVisibility;
    }

    public enum TabVisibility implements HasDisplayValue {
        SHOW_ALL("Show All"),
        HIDE_SINGLE("Hide Single Tabs"),
        HIDE_ALL("Hide All");

        private final String displayValue;

        TabVisibility(final String displayValue) {
            this.displayValue = displayValue;
        }

        @Override
        public String getDisplayValue() {
            return displayValue;
        }
    }
}
