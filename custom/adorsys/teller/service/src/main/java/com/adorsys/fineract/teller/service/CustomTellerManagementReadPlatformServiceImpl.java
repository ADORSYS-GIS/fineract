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
package com.adorsys.fineract.teller.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.SqlValidator;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.organisation.teller.data.CashierData;
import org.apache.fineract.organisation.teller.service.TellerManagementReadPlatformServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@Primary
public class CustomTellerManagementReadPlatformServiceImpl extends TellerManagementReadPlatformServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(CustomTellerManagementReadPlatformServiceImpl.class);
    private final JdbcTemplate jdbcTemplate;

    public CustomTellerManagementReadPlatformServiceImpl(final JdbcTemplate jdbcTemplate, final PlatformSecurityContext context,
            final OfficeReadPlatformService officeReadPlatformService, final StaffReadPlatformService staffReadPlatformService,
            final CurrencyReadPlatformService currencyReadPlatformService, final DatabaseSpecificSQLGenerator sqlGenerator,
            final PaginationHelper paginationHelper, final SqlValidator sqlValidator) {
        super(jdbcTemplate, context, officeReadPlatformService, staffReadPlatformService, currencyReadPlatformService, sqlGenerator,
                paginationHelper, sqlValidator);
        this.jdbcTemplate = jdbcTemplate;
        LOG.info("Custom Teller Management Service has been initialized and is overriding the default implementation.");
    }

    @Override
    public Collection<CashierData> getCashierData(Long officeId, Long tellerId, Long staffId, LocalDate date) {
        LOG.info("Executing custom getCashierData method with officeId: {}, tellerId: {}, staffId: {}, date: {}", officeId, tellerId,
                staffId, date);
        final CashierMapper cm = new CashierMapper();
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select ").append(cm.schema()).append(" where 1=1 ");

        final List<Object> params = new ArrayList<>();

        if (officeId != null) {
            sqlBuilder.append(" and t.office_id = ? ");
            params.add(officeId);
        }
        if (tellerId != null) {
            sqlBuilder.append(" and c.teller_id = ? ");
            params.add(tellerId);
        }
        if (staffId != null) {
            sqlBuilder.append(" and c.staff_id = ? ");
            params.add(staffId);
        }
        if (date != null) {
            sqlBuilder.append(" and ? between c.start_date and c.end_date ");
            params.add(date);
        }

        return this.jdbcTemplate.query(sqlBuilder.toString(), cm, params.toArray());
    }

    private static final class CashierMapper implements RowMapper<CashierData> {

        public String schema() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("c.id as id,c.teller_id as teller_id, t.name as teller_name, c.description as description, ");
            sqlBuilder.append("c.staff_id as staff_id, s.display_name as staff_name,  ");
            sqlBuilder.append("c.start_date as start_date, c.end_date as end_date,  ");
            sqlBuilder.append("c.full_day as full_day, c.start_time as start_time, c.end_time as end_time ");
            sqlBuilder.append("from m_cashiers c ");
            sqlBuilder.append("join m_tellers t on t.id = c.teller_id ");
            sqlBuilder.append("join m_staff s on s.id = c.staff_id ");
            return sqlBuilder.toString();
        }

        @Override
        public CashierData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long tellerId = rs.getLong("teller_id");
            final String tellerName = rs.getString("teller_name");
            final Long staffId = rs.getLong("staff_id");
            final String staffName = rs.getString("staff_name");
            final String description = rs.getString("description");
            final LocalDate startDate = JdbcSupport.getLocalDate(rs, "start_date");
            final LocalDate endDate = JdbcSupport.getLocalDate(rs, "end_date");
            final Boolean fullDay = rs.getBoolean("full_day");
            final String startTime = rs.getString("start_time");
            final String endTime = rs.getString("end_time");
            return CashierData.instance(id, null, null, staffId, staffName, tellerId, tellerName, description, startDate, endDate,
                    fullDay, startTime, endTime);
        }
    }
}