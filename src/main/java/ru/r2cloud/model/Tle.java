package ru.r2cloud.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.util.Util;

public class Tle {

	private static final Logger LOG = LoggerFactory.getLogger(Tle.class);

	// these 2 fields dont exist in TLE
	private String objectName;
	private String objectId;

	// TLE fields
	private long epoch;
	private double meanMotion;
	private double eccentricity;
	private double inclination;
	private double raan;
	private double pa;
	private double meanAnomaly;
	private int ephemerisType;
	private char classification;
	private int satelliteNumber;
	private int elementNumber;
	private int revolutionNumberAtEpoch;
	private double bstar;
	private double meanMotionFirstDerivative;
	private double meanMotionSecondDerivative;

	// old tle format only
	private int launchYear;
	private int launchNumber;
	private String launchPiece;

	private long lastUpdateTime;
	private String source;

	// @formatter:off
	public Tle(int satelliteNumber, 
			   char classification, 
			   int launchYear, 
			   int launchNumber, 
			   String launchPiece, 
			   int ephemerisType, 
			   int elementNumber, 
			   long epoch, 
			   double meanMotion, 
			   double meanMotionFirstDerivative, 
			   double meanMotionSecondDerivative, 
			   double e, 
			   double i, 
			   double pa, 
			   double raan,
			   double meanAnomaly, 
			   int revolutionNumberAtEpoch, 
			   double bStar) {
		this.satelliteNumber = satelliteNumber;
		this.classification = classification;
		this.launchYear = launchYear;
		this.launchNumber = launchNumber;
		this.launchPiece = launchPiece;
		this.ephemerisType = ephemerisType;
		this.elementNumber = elementNumber;
		this.epoch = epoch;
		this.meanMotion = meanMotion;
		this.meanMotionFirstDerivative = meanMotionFirstDerivative;
		this.meanMotionSecondDerivative = meanMotionSecondDerivative;
		this.eccentricity = e;
		this.inclination = i;
		this.pa = pa;
		this.raan = raan;
		this.meanAnomaly = meanAnomaly;
		this.revolutionNumberAtEpoch = revolutionNumberAtEpoch;
		this.bstar = bStar;
	}
	// @formatter:on

	public Tle() {
		// do nothing
	}

	public int getLaunchNumber() {
		return launchNumber;
	}

	public String getLaunchPiece() {
		return launchPiece;
	}

	public int getLaunchYear() {
		return launchYear;
	}

	public void setLaunchNumber(int launchNumber) {
		this.launchNumber = launchNumber;
	}

	public void setLaunchPiece(String launchPiece) {
		this.launchPiece = launchPiece;
	}

	public void setLaunchYear(int launchYear) {
		this.launchYear = launchYear;
	}

	public double getMeanMotionSecondDerivative() {
		return meanMotionSecondDerivative;
	}

	public void setMeanMotionSecondDerivative(double meanMotionSecondDerivative) {
		this.meanMotionSecondDerivative = meanMotionSecondDerivative;
	}

	public double getMeanMotionFirstDerivative() {
		return meanMotionFirstDerivative;
	}

	public void setMeanMotionFirstDerivative(double meanMotionFirstDerivative) {
		this.meanMotionFirstDerivative = meanMotionFirstDerivative;
	}

	public double getBstar() {
		return bstar;
	}

	public void setBstar(double bstar) {
		this.bstar = bstar;
	}

	public int getRevolutionNumberAtEpoch() {
		return revolutionNumberAtEpoch;
	}

	public void setRevolutionNumberAtEpoch(int revolutionNumberAtEpoch) {
		this.revolutionNumberAtEpoch = revolutionNumberAtEpoch;
	}

	public int getElementNumber() {
		return elementNumber;
	}

	public void setElementNumber(int elementNumber) {
		this.elementNumber = elementNumber;
	}

	public int getSatelliteNumber() {
		return satelliteNumber;
	}

	public void setSatelliteNumber(int satelliteNumber) {
		this.satelliteNumber = satelliteNumber;
	}

	public char getClassification() {
		return classification;
	}

	public void setClassification(char classification) {
		this.classification = classification;
	}

	public int getEphemerisType() {
		return ephemerisType;
	}

	public void setEphemerisType(int ephemerisType) {
		this.ephemerisType = ephemerisType;
	}

	public double getMeanAnomaly() {
		return meanAnomaly;
	}

	public void setMeanAnomaly(double meanAnomaly) {
		this.meanAnomaly = meanAnomaly;
	}

	public double getPa() {
		return pa;
	}

	public void setPa(double pa) {
		this.pa = pa;
	}

	public double getRaan() {
		return raan;
	}

	public void setRaan(double raan) {
		this.raan = raan;
	}

	public double getInclination() {
		return inclination;
	}

	public void setInclination(double inclination) {
		this.inclination = inclination;
	}

	public double getEccentricity() {
		return eccentricity;
	}

	public void setEccentricity(double eccentricity) {
		this.eccentricity = eccentricity;
	}

	public long getEpoch() {
		return epoch;
	}

	public void setEpoch(long epoch) {
		this.epoch = epoch;
	}

	public double getMeanMotion() {
		return meanMotion;
	}

	public void setMeanMotion(double meanMotion) {
		this.meanMotion = meanMotion;
	}

	public String getObjectName() {
		return objectName;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		// fields exist in Celestrak only
		json.add("OBJECT_NAME", getObjectName());
		if (getObjectId() != null) {
			json.add("OBJECT_ID", getObjectId());
		}

		// fields exist in TLE and Celestrak
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		json.add("EPOCH", sdf.format(new Date(getEpoch())));
		json.add("MEAN_MOTION", getMeanMotion());
		json.add("ECCENTRICITY", getEccentricity());
		json.add("INCLINATION", getInclination());
		json.add("RA_OF_ASC_NODE", getRaan());
		json.add("ARG_OF_PERICENTER", getPa());
		json.add("MEAN_ANOMALY", getMeanAnomaly());
		json.add("EPHEMERIS_TYPE", getEphemerisType());
		json.add("CLASSIFICATION_TYPE", new String(new char[] { getClassification() }));
		json.add("NORAD_CAT_ID", getSatelliteNumber());
		json.add("ELEMENT_SET_NO", getElementNumber());
		json.add("REV_AT_EPOCH", getRevolutionNumberAtEpoch());
		json.add("BSTAR", getBstar());
		json.add("MEAN_MOTION_DOT", getMeanMotionFirstDerivative());
		json.add("MEAN_MOTION_DDOT", getMeanMotionSecondDerivative());

		// fields exist in TLE only
		json.add("launchYear", getLaunchYear());
		json.add("launchNumber", getLaunchNumber());
		if (getLaunchPiece() != null) {
			json.add("launchPiece", getLaunchPiece());
		}

		// extra r2cloud fields
		json.add("updated", lastUpdateTime);
		if (source != null) {
			json.add("source", source);
		}
		return json;
	}

	public static Tle fromJson(JsonObject json) {
		String line1 = json.getString("line1", null);
		String line2 = json.getString("line2", null);
		String line3 = json.getString("line3", null);
		Tle result;
		// old format
		if (line1 != null && line2 != null && line3 != null) {
			result = Util.fromOldFormat(line2, line3);
			result.setObjectName(line1);
		} else {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			String epoch = json.getString("EPOCH", null);
			long epochTime = 0;
			try {
				int milllisecondIndex = epoch.lastIndexOf('.');
				// cut-off microsecond precision
				if (milllisecondIndex != -1 && epoch.length() - milllisecondIndex + 1 > 3) {
					epoch = epoch.substring(0, milllisecondIndex + 4);
				}
				epochTime = sdf.parse(epoch).getTime();
			} catch (ParseException e) {
				LOG.error("invalid epoch format", e);
				return null;
			}
			JsonValue classificationTypeValue = json.get("CLASSIFICATION_TYPE");
			char classificationType;
			if (classificationTypeValue.isString()) {
				classificationType = (char) classificationTypeValue.asString().charAt(0);
			} else {
				// satnogs format
				classificationType = (char) classificationTypeValue.asInt();
			}
			// @formatter:off
			result = new Tle(json.getInt("NORAD_CAT_ID", 0), 
					classificationType, 
							 json.getInt("launchYear", 0),
							 json.getInt("launchNumber", 0),
							 json.getString("launchPiece", null),
							 json.getInt("EPHEMERIS_TYPE", 0), 
							 json.getInt("ELEMENT_SET_NO", 0), 
							 epochTime,
							 json.getDouble("MEAN_MOTION", 0.0), 
							 json.getDouble("MEAN_MOTION_DOT", 0.0), 
							 json.getDouble("MEAN_MOTION_DDOT", 0.0), 
							 json.getDouble("ECCENTRICITY", 0.0), 
							 json.getDouble("INCLINATION", 0.0), 
							 json.getDouble("ARG_OF_PERICENTER", 0.0), 
							 json.getDouble("RA_OF_ASC_NODE", 0.0), 
							 json.getDouble("MEAN_ANOMALY", 0.0), 
							 json.getInt("REV_AT_EPOCH", 0),
							 json.getDouble("BSTAR", 0.0));
			result.setObjectName(json.getString("OBJECT_NAME", null));
			JsonValue objectIdValue = json.get("OBJECT_ID");
			if( objectIdValue != null && objectIdValue.isString() ) {
				result.setObjectId(objectIdValue.asString());
			}
			// @formatter:on
		}
		result.setLastUpdateTime(json.getLong("updated", 0));
		result.setSource(json.getString("source", null));
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(bstar);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + classification;
		temp = Double.doubleToLongBits(eccentricity);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + elementNumber;
		result = prime * result + ephemerisType;
		result = prime * result + (int) (epoch ^ (epoch >>> 32));
		temp = Double.doubleToLongBits(inclination);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + launchNumber;
		result = prime * result + ((launchPiece == null) ? 0 : launchPiece.hashCode());
		result = prime * result + launchYear;
		temp = Double.doubleToLongBits(meanAnomaly);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(meanMotion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(meanMotionFirstDerivative);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(meanMotionSecondDerivative);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((objectName == null) ? 0 : objectName.hashCode());
		temp = Double.doubleToLongBits(pa);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(raan);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + revolutionNumberAtEpoch;
		result = prime * result + satelliteNumber;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tle other = (Tle) obj;
		if (Double.doubleToLongBits(bstar) != Double.doubleToLongBits(other.bstar))
			return false;
		if (classification != other.classification)
			return false;
		if (Double.doubleToLongBits(eccentricity) != Double.doubleToLongBits(other.eccentricity))
			return false;
		if (elementNumber != other.elementNumber)
			return false;
		if (ephemerisType != other.ephemerisType)
			return false;
		if (epoch != other.epoch)
			return false;
		if (Double.doubleToLongBits(inclination) != Double.doubleToLongBits(other.inclination))
			return false;
		if (launchNumber != other.launchNumber)
			return false;
		if (launchPiece == null) {
			if (other.launchPiece != null)
				return false;
		} else if (!launchPiece.equals(other.launchPiece))
			return false;
		if (launchYear != other.launchYear)
			return false;
		if (Double.doubleToLongBits(meanAnomaly) != Double.doubleToLongBits(other.meanAnomaly))
			return false;
		if (Double.doubleToLongBits(meanMotion) != Double.doubleToLongBits(other.meanMotion))
			return false;
		if (Double.doubleToLongBits(meanMotionFirstDerivative) != Double.doubleToLongBits(other.meanMotionFirstDerivative))
			return false;
		if (Double.doubleToLongBits(meanMotionSecondDerivative) != Double.doubleToLongBits(other.meanMotionSecondDerivative))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (objectName == null) {
			if (other.objectName != null)
				return false;
		} else if (!objectName.equals(other.objectName))
			return false;
		if (Double.doubleToLongBits(pa) != Double.doubleToLongBits(other.pa))
			return false;
		if (Double.doubleToLongBits(raan) != Double.doubleToLongBits(other.raan))
			return false;
		if (revolutionNumberAtEpoch != other.revolutionNumberAtEpoch)
			return false;
		if (satelliteNumber != other.satelliteNumber)
			return false;
		return true;
	}

}
