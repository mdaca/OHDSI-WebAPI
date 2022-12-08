select c2.concept_id as concept_id,
       CONCAT(c2.concept_name, ': ', ar1.stratum_3) as concept_name,
       sum(ar1.count_value) as count_value
from (
       select cast(CASE WHEN isNumeric(stratum_1) = 1 THEN stratum_1 ELSE null END as int) stratum_1, 
              cast(CASE WHEN isNumeric(stratum_2) = 1 THEN stratum_2 ELSE null END as int) stratum_2, stratum_3, count_value
       FROM @results_database_schema.achilles_results
       where analysis_id = 1818
       GROUP BY analysis_id, stratum_1, stratum_2, stratum_3, count_value
     ) ar1
  inner join @vocab_database_schema.concept c1 on ar1.stratum_1 = c1.concept_id
  inner join @vocab_database_schema.concept c2 on ar1.stratum_2 = c2.concept_id
where
  c1.concept_id in (@conceptIds)
GROUP BY c2.concept_id, c2.concept_name