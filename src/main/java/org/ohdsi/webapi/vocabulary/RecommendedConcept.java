package org.ohdsi.webapi.vocabulary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import org.ohdsi.circe.vocabulary.Concept;

/**
 *
 * @author fdefalco
 */
@JsonInclude(Include.NON_NULL)
public class RecommendedConcept extends Concept {

    @JsonProperty("RELATIONSHIPS")
    public ArrayList<String> relationships;
		
}
