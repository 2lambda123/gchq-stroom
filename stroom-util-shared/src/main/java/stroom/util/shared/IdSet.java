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

package stroom.util.shared;

import javax.xml.bind.annotation.XmlTransient;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class IdSet extends CriteriaSet<Long> {
    private static final long serialVersionUID = 1L;

    public IdSet() {
        super(new TreeSet<>());
    }

    @Override
    @XmlTransient
    public Set<Long> getSet() {
        return super.getSet();
    }

    @Override
    public void setSet(final Set<Long> set) {
        super.setSet(set);
    }

    /**
     * THIS IS HERE ONLY FOR BACKWARD COMPATIBILITY WITH OLD SERIALISED VERSIONS
     * <p>
     * DO NOT USE BUT DO NOT REMOVE EITHER
     */
    @Deprecated
    public List<Long> getIdSet() {
        return null;
    }

    /**
     * THIS IS HERE ONLY FOR BACKWARD COMPATIBILITY WITH OLD SERIALISED VERSIONS
     * <p>
     * DO NOT USE BUT DO NOT REMOVE EITHER
     */
    @Deprecated
    public void setIdSet(final List<Long> newSet) {
        setId(newSet);
    }

    /**
     * HERE FOR XML JAXB serialisation ..... DO NOT REMOVE
     */
    public Collection<Long> getId() {
        return getSet();
    }

    /**
     * HERE FOR XML JAXB serialisation ..... DO NOT REMOVE
     */
    public void setId(final Collection<Long> newSet) {
        if (newSet == null) {
            setSet(null);
        } else {
            setSet(new TreeSet<>(newSet));
        }
    }

    public Long getMaxId() {
        Long max = null;
        for (final Long id : getSet()) {
            if (max == null) {
                max = id;
            } else {
                if (id.longValue() > max.longValue()) {
                    max = id;
                }
            }
        }
        return max;
    }

    /**
     * @param id update so as not to change JAXB
     */
    public void updateSingleId(final Long id) {
        clear();
        if (id != null) {
            add(id);
        }
    }

    public Long getSingleId() {
        if (!isConstrained()) {
            return null;
        }
        if (getSet().size() != 1) {
            throw new RuntimeException("Single is state invalid");
        }
        return getSet().iterator().next();
    }
}
