select CONCEPT_ID, CONCEPT_NAME, ISNULL(STANDARD_CONCEPT,'N') STANDARD_CONCEPT, ISNULL(INVALID_REASON,'V') INVALID_REASON, CONCEPT_CODE, CONCEPT_CLASS_ID, DOMAIN_ID, VOCABULARY_ID, CR.RELATIONSHIP_ID
from @CDM_schema.concept_recommended cr
join @CDM_schema.concept c on c.concept_id = cr.concept_id_2
where cr.concept_id_1 in (@identifiers)
