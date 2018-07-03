/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
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
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: August 21, 2017
 *
 */

package com.odysseusinc.arachne.portal.api.v1.dto.converters;

import com.odysseusinc.arachne.portal.api.v1.dto.SearchDataCatalogDTO;
import com.odysseusinc.arachne.portal.service.BaseDataSourceService;
import com.odysseusinc.arachne.portal.service.impl.solr.FieldList;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class SearchDataCatalogDTOToSolrQueryConverter
        extends SearchDTOToSolrQuery implements Converter<SearchDataCatalogDTO, SolrQuery>, InitializingBean {

    @Autowired
    private GenericConversionService conversionService;

    @Autowired
    private BaseDataSourceService dataSourceService;

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    protected FieldList getSolrFields() {

        return dataSourceService.getSolrFields();
    }

    @Override
    public SolrQuery convert(SearchDataCatalogDTO source) {

        return source.isFullFacetsQuery() ? super.convertToFullFacetsQuery(source) : super.convert(source);
    }

}
