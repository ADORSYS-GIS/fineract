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

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

/**
 * Service for creating and updating User-Staff entities together in atomic transactions.
 */
public interface UserStaffExtWritePlatformService {

    /**
     * Creates both a Staff member and a User, linking them together in a single atomic
     * transaction.
     *
     * @param command
     *            JSON command containing both user and staff data
     * @return CommandProcessingResult containing both user and staff IDs
     */
    CommandProcessingResult createUserStaffExt(JsonCommand command);

    /**
     * Updates both the User and Staff information in a single atomic transaction.
     *
     * @param userId
     *            the ID of the user to update
     * @param command
     *            JSON command containing updated user and staff data
     * @return CommandProcessingResult with changes
     */
    CommandProcessingResult updateUserStaffExt(Long userId, JsonCommand command);
}
