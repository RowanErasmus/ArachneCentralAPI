/*
 *
 * Copyright 2018 Observational Health Data Sciences and Informatics
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
 * Authors: Pavel Grafkin
 * Created: February 2, 2017
 *
 */

package com.odysseusinc.arachne.portal.service.impl;

import com.odysseusinc.arachne.portal.model.security.Tenant;
import com.odysseusinc.arachne.portal.repository.BaseTenantRepository;
import com.odysseusinc.arachne.portal.service.BaseTenantService;
import java.util.Set;

public abstract class BaseTenantServiceImpl<T extends Tenant> implements BaseTenantService<T> {

    protected final BaseTenantRepository<T> tenantRepository;

    public BaseTenantServiceImpl(final BaseTenantRepository<T> tenantRepository) {

        this.tenantRepository = tenantRepository;
    }

    @Override
    public Set<T> getDefault() {

        return tenantRepository.findAllByIsDefaultTrue();
    }
    
    @Override
    public T findById(final Long tenantId) {
        
        return tenantRepository.getOne(tenantId);
    }
}
