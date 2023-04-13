package stroom.search.elastic;

import stroom.datasource.api.v2.AbstractField;
import stroom.search.elastic.shared.ElasticIndexDoc;
import stroom.search.elastic.shared.ElasticIndexField;

import java.util.List;
import java.util.Map;

public interface ElasticIndexService {

    List<AbstractField> getDataSourceFields(ElasticIndexDoc index);

    List<ElasticIndexField> getFields(ElasticIndexDoc index);

    Map<String, ElasticIndexField> getFieldsMap(ElasticIndexDoc index);
}
