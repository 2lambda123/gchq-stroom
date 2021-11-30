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

package stroom.ui.config.shared;

import stroom.util.shared.AbstractConfig;
import stroom.util.shared.validation.ValidRegex;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;
import javax.inject.Singleton;
import javax.validation.constraints.Pattern;

@JsonPropertyOrder(alphabetic = true)
@JsonInclude(Include.NON_NULL)
public class UiConfig extends AbstractConfig {

    @JsonProperty
    @JsonPropertyDescription("The welcome message that is displayed in the welcome tab when logging in to Stroom. " +
            "The welcome message is in HTML format.")
    private String welcomeHtml;

    @JsonProperty
    @JsonPropertyDescription("The about message that is displayed when selecting Help -> About. " +
            "The about message is in HTML format.")
    private String aboutHtml;

    @JsonProperty
    @JsonPropertyDescription("Provide a warning message to users about an outage or other significant event.")
    private String maintenanceMessage;

    @JsonProperty
    @JsonPropertyDescription("The default maximum number of search results to return to the dashboard, unless the " +
            "user requests lower values.")
    private String defaultMaxResults;

    @JsonProperty
    private ProcessConfig process;

    @JsonProperty
    @JsonPropertyDescription("The URL of hosted help files.")
    private String helpUrl;

    @JsonProperty
    private ThemeConfig theme;

    @JsonProperty
    private QueryConfig query;

    @ValidRegex
    @JsonProperty
    @JsonPropertyDescription("The regex pattern for entity names.")
    private String namePattern;

    @JsonProperty
    @JsonPropertyDescription("The title to use for the application in the browser.")
    private String htmlTitle;

    @Pattern(regexp = "^return (true|false);$")
    @JsonProperty
    @JsonPropertyDescription("Determines the behaviour of the browser built-in context menu. This property is " +
            "for developer use only. Set to 'return false;' to see Stroom's context menu. Set to 'return true;' " +
            "to see the standard " +
            "browser menu.")
    private String oncontextmenu;

    @JsonProperty
    private SplashConfig splash;

    @JsonProperty
    private ActivityConfig activity;

    @JsonProperty
    private UiPreferences uiPreferences;

    @JsonProperty
    private SourceConfig source;

    @JsonProperty
    @JsonPropertyDescription("The Stroom GWT UI is now wrapped in a new React UI that provides some additional " +
            "features. To use the React UI the GWT UI must be wrapped in an IFrame which is hosted at the root URL. " +
            "If a user navigates to the GWT UI directly via `stroom/ui` then the React additions will not function. " +
            "When this property is set to true that will be prevented as the user will be redirected back to the " +
            "root URL. This behaviour is configurable as development of the GWT UI still requires direct access via " +
            "`stroom/ui`")
    private Boolean requireReactWrapper;

    public UiConfig() {
        setDefaults();
    }

    @JsonCreator
    public UiConfig(@JsonProperty("welcomeHtml") final String welcomeHtml,
                    @JsonProperty("aboutHtml") final String aboutHtml,
                    @JsonProperty("maintenanceMessage") final String maintenanceMessage,
                    @JsonProperty("defaultMaxResults") final String defaultMaxResults,
                    @JsonProperty("process") final ProcessConfig process,
                    @JsonProperty("helpUrl") final String helpUrl,
                    @JsonProperty("theme") final ThemeConfig theme,
                    @JsonProperty("query") final QueryConfig query,
                    @JsonProperty("namePattern") @ValidRegex final String namePattern,
                    @JsonProperty("htmlTitle") final String htmlTitle,
                    @JsonProperty("oncontextmenu") final String oncontextmenu,
                    @JsonProperty("splash") final SplashConfig splash,
                    @JsonProperty("activity") final ActivityConfig activity,
                    @JsonProperty("uiPreferences") final UiPreferences uiPreferences,
                    @JsonProperty("source") final SourceConfig source,
                    @JsonProperty("requireReactWrapper") Boolean requireReactWrapper) {
        this.welcomeHtml = welcomeHtml;
        this.aboutHtml = aboutHtml;
        this.maintenanceMessage = maintenanceMessage;
        this.defaultMaxResults = defaultMaxResults;
        this.process = process;
        this.helpUrl = helpUrl;
        this.theme = theme;
        this.query = query;
        this.namePattern = namePattern;
        this.htmlTitle = htmlTitle;
        this.oncontextmenu = oncontextmenu;
        this.splash = splash;
        this.activity = activity;
        this.uiPreferences = uiPreferences;
        this.source = source;
        this.requireReactWrapper = requireReactWrapper;

        setDefaults();
    }

    private void setDefaults() {
        if (welcomeHtml == null) {
            welcomeHtml = "<h1>About Stroom</h1><p>Stroom is designed to receive data from multiple systems.</p>";
        }
        if (aboutHtml == null) {
            aboutHtml = "<h1>About Stroom</h1><p>Stroom is designed to receive data from multiple systems.</p>";
        }
        if (defaultMaxResults == null) {
            defaultMaxResults = "1000000,100,10,1";
        }
        if (process == null) {
            process = new ProcessConfig();
        }
        if (helpUrl == null) {
            helpUrl = "https://gchq.github.io/stroom-docs";
        }
        if (theme == null) {
            theme = new ThemeConfig();
        }
        if (query == null) {
            query = new QueryConfig();
        }
        if (namePattern == null) {
            namePattern = "^[a-zA-Z0-9_\\- \\.\\(\\)]{1,}$";
        }
        if (htmlTitle == null) {
            htmlTitle = "Stroom";
        }
        if (oncontextmenu == null) {
            oncontextmenu = "return false;";
        }
        if (splash == null) {
            splash = new SplashConfig();
        }
        if (activity == null) {
            activity = new ActivityConfig();
        }
        if (uiPreferences == null) {
            uiPreferences = new UiPreferences();
        }
        if (source == null) {
            source = new SourceConfig();
        }
        if (requireReactWrapper == null) {
            requireReactWrapper = true;
        }
    }

    public String getWelcomeHtml() {
        return welcomeHtml;
    }

    public void setWelcomeHtml(final String welcomeHtml) {
        this.welcomeHtml = welcomeHtml;
    }

    public String getAboutHtml() {
        return aboutHtml;
    }

    public void setAboutHtml(final String aboutHtml) {
        this.aboutHtml = aboutHtml;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage;
    }

    public void setMaintenanceMessage(final String maintenanceMessage) {
        this.maintenanceMessage = maintenanceMessage;
    }

    public String getDefaultMaxResults() {
        return defaultMaxResults;
    }

    public void setDefaultMaxResults(final String defaultMaxResults) {
        this.defaultMaxResults = defaultMaxResults;
    }

    public ProcessConfig getProcess() {
        return process;
    }

    public void setProcess(final ProcessConfig process) {
        this.process = process;
    }

    public String getHelpUrl() {
        return helpUrl;
    }

    public void setHelpUrl(final String helpUrl) {
        this.helpUrl = helpUrl;
    }

    public ThemeConfig getTheme() {
        return theme;
    }

    public void setTheme(final ThemeConfig theme) {
        this.theme = theme;
    }

    public QueryConfig getQuery() {
        return query;
    }

    public void setQuery(final QueryConfig query) {
        this.query = query;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setNamePattern(final String namePattern) {
        this.namePattern = namePattern;
    }

    public SplashConfig getSplash() {
        return splash;
    }

    public void setSplash(final SplashConfig splash) {
        this.splash = splash;
    }

    public ActivityConfig getActivity() {
        return activity;
    }

    public void setActivity(final ActivityConfig activity) {
        this.activity = activity;
    }

    public String getHtmlTitle() {
        return htmlTitle;
    }

    public void setHtmlTitle(final String htmlTitle) {
        this.htmlTitle = htmlTitle;
    }

    public String getOncontextmenu() {
        return oncontextmenu;
    }

    public void setOncontextmenu(final String oncontextmenu) {
        this.oncontextmenu = oncontextmenu;
    }

    public UiPreferences getUiPreferences() {
        return uiPreferences;
    }

    public void setUiPreferences(final UiPreferences uiPreferences) {
        this.uiPreferences = uiPreferences;
    }

    public SourceConfig getSource() {
        return source;
    }

    public void setSource(final SourceConfig source) {
        this.source = source;
    }

    public Boolean getRequireReactWrapper() {
        return requireReactWrapper;
    }

    public void setRequireReactWrapper(final Boolean requireReactWrapper) {
        this.requireReactWrapper = requireReactWrapper;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UiConfig uiConfig = (UiConfig) o;
        return Objects.equals(welcomeHtml, uiConfig.welcomeHtml)
                && Objects.equals(aboutHtml, uiConfig.aboutHtml)
                && Objects.equals(maintenanceMessage, uiConfig.maintenanceMessage)
                && Objects.equals(defaultMaxResults, uiConfig.defaultMaxResults)
                && Objects.equals(process, uiConfig.process)
                && Objects.equals(helpUrl, uiConfig.helpUrl)
                && Objects.equals(theme, uiConfig.theme)
                && Objects.equals(query, uiConfig.query)
                && Objects.equals(namePattern, uiConfig.namePattern)
                && Objects.equals(htmlTitle, uiConfig.htmlTitle)
                && Objects.equals(oncontextmenu, uiConfig.oncontextmenu)
                && Objects.equals(splash, uiConfig.splash)
                && Objects.equals(activity, uiConfig.activity)
                && Objects.equals(uiPreferences, uiConfig.uiPreferences)
                && Objects.equals(source, uiConfig.source)
                && Objects.equals(requireReactWrapper, uiConfig.requireReactWrapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(welcomeHtml,
                aboutHtml,
                maintenanceMessage,
                defaultMaxResults,
                process,
                helpUrl,
                theme,
                query,
                namePattern,
                htmlTitle,
                oncontextmenu,
                splash,
                activity,
                uiPreferences,
                source,
                requireReactWrapper);
    }

    @Override
    public String toString() {
        return "UiConfig{" +
                "welcomeHtml='" + welcomeHtml + '\'' +
                ", aboutHtml='" + aboutHtml + '\'' +
                ", maintenanceMessage='" + maintenanceMessage + '\'' +
                ", defaultMaxResults='" + defaultMaxResults + '\'' +
                ", process=" + process +
                ", helpUrl='" + helpUrl + '\'' +
                ", theme=" + theme +
                ", query=" + query +
                ", namePattern='" + namePattern + '\'' +
                ", htmlTitle='" + htmlTitle + '\'' +
                ", oncontextmenu='" + oncontextmenu + '\'' +
                ", splash=" + splash +
                ", activity=" + activity +
                ", uiPreferences=" + uiPreferences +
                ", source=" + source +
                ", requireReactWrapper=" + requireReactWrapper +
                '}';
    }
}
