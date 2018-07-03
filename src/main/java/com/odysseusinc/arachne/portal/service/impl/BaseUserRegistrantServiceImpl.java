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
 * Created: September 19, 2017
 *
 */

package com.odysseusinc.arachne.portal.service.impl;

import com.odysseusinc.arachne.portal.model.UserRegistrant;
import com.odysseusinc.arachne.portal.repository.UserRegistrantRepository;
import com.odysseusinc.arachne.portal.service.BaseUserRegistrantService;
import com.odysseusinc.arachne.portal.service.mail.RegistrationMailMessage;
import com.odysseusinc.arachne.portal.service.mail.UserRegistrantMailMessage;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseUserRegistrantServiceImpl<UR extends UserRegistrant> implements BaseUserRegistrantService<UR> {

    private UserRegistrantRepository<UR> userRegistrantRepository;

    @Autowired
    public BaseUserRegistrantServiceImpl(UserRegistrantRepository<UR> userRegistrantRepository) {

        this.userRegistrantRepository = userRegistrantRepository;
    }

    public Optional<UR> findByToken(String token) {
        return userRegistrantRepository.findOneByToken(token);
    }

    @Override
    public void customizeUserRegistrantMailMessage(UR userRegistrant, String callbackUrl, UserRegistrantMailMessage mail) {

        mail.setFromPersonal(userRegistrant.getSenderName());
        mail.setSubject(userRegistrant.getSubject());
        mail.setTemplate(userRegistrant.getTemplate());
        mail.setCallbackUrl(callbackUrl);
    }
}
