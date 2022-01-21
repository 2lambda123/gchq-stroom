package stroom.activity.impl;

import stroom.activity.api.ActivityService;
import stroom.activity.api.CurrentActivity;
import stroom.util.guice.RestResourcesBinder;

import com.google.inject.AbstractModule;

public class ActivityModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivityService.class).to(ActivityServiceImpl.class);
        bind(CurrentActivity.class).to(CurrentActivityImpl.class);

        RestResourcesBinder.create(binder())
                .bind(ActivityResourceImpl.class);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
