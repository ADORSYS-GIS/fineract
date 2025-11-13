/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.administration.userstaffext.service;

import java.util.Collection;
import org.apache.fineract.administration.userstaffext.data.UserStaffExtData;

/**
 * Service for reading User-Staff extension data.
 */
public interface UserStaffExtReadPlatformService {

    /**
     * Retrieves all users that have linked staff members.
     *
     * @return collection of UserStaffExtData
     */
    Collection<UserStaffExtData> retrieveAll();

    /**
     * Retrieves a single user-staff entity by user ID.
     *
     * @param userId
     *            the user ID
     * @return UserStaffExtData containing combined user and staff information
     */
    UserStaffExtData retrieveOne(Long userId);

    /**
     * Retrieves template data for creating a new user-staff entity (includes available offices and
     * roles).
     *
     * @return UserStaffExtData template with options
     */
    UserStaffExtData retrieveTemplate();
}
