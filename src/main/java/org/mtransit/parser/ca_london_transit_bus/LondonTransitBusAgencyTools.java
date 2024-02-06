package org.mtransit.parser.ca_london_transit_bus;

import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://www.londontransit.ca/open-data/
public class LondonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new LondonTransitBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "London Transit";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@NotNull
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		//noinspection deprecation
		return gRoute.getRouteId(); // use by GTFS-RT provider
	}

	@NotNull
	@Override
	public String cleanRouteShortName(@NotNull String routeShortName) {
		routeShortName = String.valueOf(Integer.parseInt(routeShortName)); // remove leading 0
		return super.cleanRouteShortName(routeShortName);
	}

	private static final Pattern STARTS_WITH_ROUTE_RSN = Pattern.compile("(^route \\d+\\s?)", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, routeLongName, getIgnoredWords());
		routeLongName = STARTS_WITH_ROUTE_RSN.matcher(routeLongName).replaceAll(EMPTY);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR_GREEN = "009F60"; // GREEN (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern STARTS_WITH_LETTER = Pattern.compile("(^[a-z] )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		if (!fromStopName) {
			if ("Masonville via Natural Science".equals(directionHeadSign)) { // route 34
				return "CW via Natural Science";
			} else if ("Masonville via Alumni Hall".equals(directionHeadSign)) { // route 34
				return "CWW via Alumni Hall";
			}
		}
		directionHeadSign = super.cleanDirectionHeadsign(directionId, fromStopName, directionHeadSign);
		directionHeadSign = STARTS_WITH_LETTER.matcher(directionHeadSign).replaceAll(EMPTY);
		return directionHeadSign;
	}

	private static final Pattern AREA = Pattern.compile("((^|\\W)(area)(\\W|$))", Pattern.CASE_INSENSITIVE);

	private static final Pattern ARGYLE_ = Pattern.compile("((^|\\W)(agyle)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ARGYLE_REPLACEMENT = "$2" + "Argyle" + "$4";

	private static final Pattern DEVERON_ = Pattern.compile("((^|\\W)(deverion)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String DEVERON_REPLACEMENT = "$2" + "Deveron" + "$4";

	private static final Pattern FIX_HOSPITAL_ = Pattern.compile("((^|\\W)(hosptial|hos)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String FIX_HOSPITAL_REPLACEMENT = "$2" + "hospital" + "$4";

	private static final Pattern FIX_MASONVILLE_ = Pattern.compile("((^|\\W)(masonvile|masvonille|masonvillel|masonville)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String FIX_MASONVILLE_REPLACEMENT = "$2" + "Masonville" + "$4";

	private static final Pattern ONLY_ = Pattern.compile("((^|\\W)(only)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ONLY_REPLACEMENT = "$2" + EMPTY + "$4";

	private static final Pattern UNIVERSITY_OF_WESTERN_ONTARIO = Pattern.compile("((^|\\W)(univ western ontario|western university)(\\W|$))",
			Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT = "$2" + "UWO" + "$4";

	private static final Pattern STARTS_WITH_EXPRESS_TO = Pattern.compile("(^express to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^\\d+\\s?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, getIgnoredWords());
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(EMPTY); // 1st
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = STARTS_WITH_EXPRESS_TO.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = AREA.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = ARGYLE_.matcher(tripHeadsign).replaceAll(ARGYLE_REPLACEMENT);
		tripHeadsign = DEVERON_.matcher(tripHeadsign).replaceAll(DEVERON_REPLACEMENT);
		tripHeadsign = FIX_HOSPITAL_.matcher(tripHeadsign).replaceAll(FIX_HOSPITAL_REPLACEMENT);
		tripHeadsign = FIX_MASONVILLE_.matcher(tripHeadsign).replaceAll(FIX_MASONVILLE_REPLACEMENT);
		tripHeadsign = ONLY_.matcher(tripHeadsign).replaceAll(ONLY_REPLACEMENT);
		tripHeadsign = UNIVERSITY_OF_WESTERN_ONTARIO.matcher(tripHeadsign).replaceAll(UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private String[] getIgnoredWords() {
		return new String[]{
				"NB", "SB", "WB", "EB",
				"SE", "SW", "NW", "NE",
				"VMP", "YMCA",
		};
	}

	private static final Pattern ENDS_WITH_STOP_CODE = Pattern.compile("( - #\\d*\\w*'*$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredWords());
		gStopName = ENDS_WITH_STOP_CODE.matcher(gStopName).replaceAll(EMPTY);
		gStopName = FIX_HOSPITAL_.matcher(gStopName).replaceAll(FIX_HOSPITAL_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		final String stopId = gStop.getStopId();
		if (!CharUtils.isDigitsOnly(stopId)) {
			return Integer.parseInt(cleanStopCode(gStop));
		}
		return super.getStopId(gStop); // used (?) by real-time API https://realtime.londontransit.ca/ // TODO GTFS-RT
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		//noinspection deprecation
		return gStop.getStopId(); // using stop ID as stop code (useful to match with GTFS real-time)
	}

	@NotNull
	private String cleanStopCode(@NotNull GStop gStop) {
		String stopCode = gStop.getStopCode();
		if ("'".equals(stopCode)) {
			stopCode = EMPTY;
		}
		if (stopCode.isEmpty() || !CharUtils.isDigitsOnly(stopCode)) {
			//noinspection deprecation
			final String stopId = gStop.getStopId();
			// @formatter:off
			switch (stopId.toUpperCase(Locale.ENGLISH)) {
			case "DUFFWATS": return "2836";
			case "WELLBAS3": return "2434";
			case "SDALNIXO": return "69"; // Southdale at Nixon - # TODO ?
			case "MCMAWON2": return "2001";
			case "STACFANS": return "3838";
			case "WESTLAM1": return "2453";
			case "COLBCHEA": return "2796";
			case "COLBGRO1": return "2797";
			case "COLBSJM2": return "2801";
			case "COLBOXFO": return "2799";
			case "COLBSJM1": return "2800";
			case "COLBGRO2": return "2709";
			case "BARKMEL1": return "2788";
			case "BARKKIPP": return "2787";
			case "KIPPBEL1": return "2852";
			case "KIPPADE1": return "2850";
			case "BARKMELS": return "2789";
			case "BARKHUR1": return "2786";
			case "FANSWOND": return "2835";
			case "WELLSDA5": return "2887";
			case "SOUTMONT": return "2878";
			case "FANSRIC5": return "2832";
			case "FANSRIC4": return "2831";
			case "WELLSDA6": return "2888";
			case "WELLSDA4": return "2886";
			case "WHITBRA1": return "2898";
			case "WHITEXE1": return "2903";
			case "WHITEXE2": return "2904";
			case "WHITEXE3": return "2905";
			case "WHITEXET": return "2906";
			case "WHITDOW2": return "2901";
			case "WHITDOWE": return "2902";
			case "WHITDOW1": return "2900";
			case "PONDAIL2": return "2915";
			case "KINGWEL2": return "2739";
			case "YORKTHA1": return "2170";
			case "YORKTHA2": return "2171";
			case "YORKWAT1": return "2172";
			case "YORKCOL1": return "2157";
			case "YORKMAI1": return "2163";
			case "YORKWIL1": return "2175";
			case "YORKADE1": return "2154";
			case "YORKLYL1": return "2161";
			case "YORKLYL2": return "2162";
			case "YORKREC1": return "2165";
			case "YORKREC2": return "2166";
			case "YORKADE2": return "2155";
			case "YORKWIL2": return "2176";
			case "YORKMAI2": return "2164";
			case "YORKCOL2": return "2158";
			case "YORKWAT2": return "2173";
			case "WOODRIVE": return "2144";
			case "WOODEDI1": return "2142";
			case "WOODEDI2": return "2143";
			case "WOODTOZ2": return "2146";
			case "WHARMOR2": return "10001"; // TODO Wharncliffe at Morgan NS SB - # ???
			case "WATEDUN1": return "10002"; // FIXME
			case "YXUTERM" : return "2179";
			default:
				throw new MTLog.Fatal("Unexpected stop code for %s!", gStop.toStringPlus(true));
			}
			// @formatter:on
		}
		return super.getStopCode(gStop);
	}
}
