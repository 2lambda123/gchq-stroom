package stroom.query.common.v2;

public class BasicKeyFactoryConfig implements KeyFactoryConfig {

    @Override
    public int getTimeFieldIndex() {
        return -1;
    }

    @Override
    public boolean addTimeToKey() {
        return false;
    }
}
