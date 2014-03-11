package example;

import java.util.Date;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu.composite.CodingDt;
import ca.uhn.fhir.model.dstu.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu.composite.PeriodDt;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.dstu.valueset.AdministrativeGenderCodesEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.InstantDt;

public class FhirDataModel {

	public static void datatypes() {
// START SNIPPET: datatypes
Observation obs = new Observation();

// These are all equivalent
obs.setIssued(new InstantDt(new Date()));
obs.setIssued(new Date(), TemporalPrecisionEnum.MILLI);
obs.setIssuedWithMillisPrecision(new Date());

/* 
 * The InstantDt also lets you work with the instant as a Java Date
 * object or as a FHIR String.
 */
Date date = obs.getIssued().getValue(); // A date object
String dateString = obs.getIssued().getValueAsString(); // "2014-03-08T12:59:58.068-05:00"
// END SNIPPET: datatypes

System.out.println(date);
System.out.println(dateString);

	}
	
@SuppressWarnings("unused")
public void nonNull() {
// START SNIPPET: nonNull
Observation observation = new Observation();

/*
 * None of these calls will return null, but instead create their respective
 * child elements.
 */
IdentifierDt identifierDt = observation.getIdentifier();
PeriodDt periodDt         = observation.getIdentifier().getPeriod();
DateTimeDt activeDt       = observation.getIdentifier().getPeriod().getStart();

/*
 * DateTimeDt is a FHIR primitive however, so the following will return null
 * unless a value has been placed there.
 */
Date active = observation.getIdentifier().getPeriod().getStart().getValue();
// END SNIPPET: nonNull


	
	
}
	
	
public static void codes() {
// START SNIPPET: codes
Patient patient = new Patient();

// Coded types can naturally be set using plain Strings
CodingDt genderCoding = patient.getGender().addCoding();
genderCoding.setSystem("http://hl7.org/fhir/v3/AdministrativeGender");
genderCoding.setCode("M");

// This is equivalent to the three statements above
patient.setGender(AdministrativeGenderCodesEnum.M);
// END SNIPPET: codes

}

	public static void main(String[] args) {
		datatypes();
		
	}
		

}