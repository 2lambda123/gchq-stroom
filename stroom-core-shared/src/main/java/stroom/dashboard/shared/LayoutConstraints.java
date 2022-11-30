package stroom.dashboard.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({
        "fitWidth",
        "fitHeight"
})
@JsonInclude(Include.NON_NULL)
public class LayoutConstraints {

    @JsonProperty
    private final boolean fitWidth;
    @JsonProperty
    private final boolean fitHeight;

    public LayoutConstraints() {
        fitWidth = true;
        fitHeight = true;
    }

    @JsonCreator
    public LayoutConstraints(@JsonProperty("fitWidth") final boolean fitWidth,
                             @JsonProperty("fitHeight") final boolean fitHeight) {
        this.fitWidth = fitWidth;
        this.fitHeight = fitHeight;
    }

    public boolean isFitWidth() {
        return fitWidth;
    }

    public boolean isFitHeight() {
        return fitHeight;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LayoutConstraints that = (LayoutConstraints) o;
        return fitWidth == that.fitWidth && fitHeight == that.fitHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fitWidth, fitHeight);
    }
}
