package org.ohdsi.webapi.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.helper.ResourceHelper;
import org.ohdsi.webapi.model.Cohort;
import org.ohdsi.webapi.model.CohortDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * 
 * TODO replace all cohort_definition lookups with that service, once available
 *
 */
@Path("/cohort/")
@Component
public class CohortService {
    
    @Value("${datasource.cdm.schema}")
    private String cdmSchema;
    
    @Value("${datasource.ohdsi.schema}")
    private String ohdsiSchema;
    
    @Value("${datasource.dialect}")
    private String dialect;
    
    @Value("${datasource.dialect.source}")
    private String sourceDialect;
    
    private final JdbcTemplate jdbcTemplate;
    
    private final RowMapper<Cohort> cohortMapper = new RowMapper<Cohort>() {
        
        @Override
        public Cohort mapRow(final ResultSet rs, final int arg1) throws SQLException {
            final Cohort cohort = new Cohort();
            cohort.setCohortDefinitionId(rs.getInt(Cohort.COHORT_DEFINITION_ID));
            cohort.setCohortStartDate(rs.getDate(Cohort.COHORT_START_DATE));
            cohort.setCohortEndDate(rs.getDate(Cohort.COHORT_END_DATE));
            cohort.setSubjectId(rs.getInt(Cohort.SUBJECT_ID));
            return cohort;
        }
    };
    
    private final RowMapper<CohortDefinition> cohortDefinitionMapper = new RowMapper<CohortDefinition>() {
        
        @Override
        public CohortDefinition mapRow(final ResultSet rs, final int arg1) throws SQLException {
            final CohortDefinition definition = new CohortDefinition();
            definition.setCohortDefinitionDescription(rs.getString(CohortDefinition.COHORT_DEFINITION_DESCRIPTION));
            definition.setCohortDefinitionId(rs.getInt(CohortDefinition.COHORT_DEFINITION_ID));
            definition.setCohortDefinitionName(rs.getString(CohortDefinition.COHORT_DEFINITION_NAME));
            definition.setCohortDefinitionSyntax(rs.getString(CohortDefinition.COHORT_DEFINITION_SYNTAX));
            definition.setCohortInitiationDate(rs.getDate(CohortDefinition.COHORT_INITIATION_DATE));
            return definition;
        }
    };
    
    @Autowired
    public CohortService(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CohortDefinition> getCohortDefinitionList() {
        
        String sql = ResourceHelper.GetResourceAsString("/resources/cohort/sql/getCohortDefinitions.sql");
        sql = SqlRender.renderSql(sql, new String[] { "CDM_schema" }, new String[] { this.cdmSchema });
        sql = SqlTranslate.translateSql(sql, this.sourceDialect, this.dialect);
        
        return this.jdbcTemplate.query(sql, this.cohortDefinitionMapper);
    }
}
