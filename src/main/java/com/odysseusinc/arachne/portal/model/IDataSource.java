/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
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
 * Created: November 07, 2016
 *
 */

package com.odysseusinc.arachne.portal.model;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonCDMVersionDTO;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonHealthStatus;
import com.odysseusinc.arachne.commons.api.v1.dto.CommonModelType;
import com.odysseusinc.arachne.portal.model.security.Tenant;
import com.odysseusinc.arachne.portal.model.solr.SolrFieldAnno;
import com.odysseusinc.arachne.portal.security.ArachnePermission;
import com.odysseusinc.arachne.portal.security.HasArachnePermissions;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.hibernate.validator.constraints.NotBlank;

public interface IDataSource {

    boolean equals(final Object obj);

    int hashCode();

    Long getId();

    void setId(Long id);

    String getUuid();

    void setUuid(String uuid);

    String getName();

    void setName(String name);

    DataNode getDataNode();

    void setDataNode(DataNode dataNode);

    Set<ArachnePermission> getPermissions();

    void setPermissions(Set<ArachnePermission> permissions);

    CommonModelType getModelType();

    void setModelType(CommonModelType modelType);

    CommonHealthStatus getHealthStatus();

    void setHealthStatus(CommonHealthStatus healthStatus);

    String getHealthStatusDescription();

    void setHealthStatusDescription(String healthStatusDescription);

    Date getCreated();

    void setCreated(Date created);

    Date getDeleted();

    void setDeleted(Date deletedAt);

    CommonCDMVersionDTO getCdmVersion();

    void setCdmVersion(CommonCDMVersionDTO cdmVersion);

    String getOrganization();

    void setOrganization(String organization);

    Set<Tenant> getTenants();

    void setTenants(Set<Tenant> tenants);

    Boolean getPublished();

    void setPublished(Boolean published);
}
