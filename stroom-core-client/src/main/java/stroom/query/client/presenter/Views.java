package stroom.query.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.view.shared.ViewResource;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.web.bindery.event.shared.EventBus;

import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Views implements HasHandlers {

    private static final ViewResource VIEW_RESOURCE = GWT.create(ViewResource.class);

    private final EventBus eventBus;
    private final RestFactory restFactory;

    private List<String> viewNames;


    @Inject
    Views(final EventBus eventBus,
          final RestFactory restFactory) {
        this.eventBus = eventBus;
        this.restFactory = restFactory;
    }

    public void fetchViews(final Consumer<List<String>> consumer) {
        if (viewNames != null) {
            consumer.accept(viewNames);
        } else {
            final Rest<List<String>> rest = restFactory.create();
            rest
                    .onSuccess(result -> {
                        viewNames = result;
                        consumer.accept(result);
                    })
                    .onFailure(throwable -> AlertEvent.fireError(
                            this,
                            throwable.getMessage(),
                            null))
                    .call(VIEW_RESOURCE)
                    .list();
        }
    }

    @Override
    public void fireEvent(final GwtEvent<?> event) {
        eventBus.fireEvent(event);
    }
}
