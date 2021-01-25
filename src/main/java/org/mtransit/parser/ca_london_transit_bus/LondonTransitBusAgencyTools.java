package org.mtransit.parser.ca_london_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// http://www.londontransit.ca/open-data/
// http://www.londontransit.ca/gtfsfeed/google_transit.zip
public class LondonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-london-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LondonTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating London Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating London Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return String.valueOf(Integer.parseInt(gRoute.getRouteShortName())); // remove leading 0
		}
		return super.getRouteShortName(gRoute);
	}

	private static final Pattern STARTS_WITH_ROUTE_RSN = Pattern.compile("(^route [\\d]+[\\s]?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, routeLongName, getIgnoredWords());
		routeLongName = STARTS_WITH_ROUTE_RSN.matcher(routeLongName).replaceAll(EMPTY);
		if (StringUtils.isEmpty(routeLongName)) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 1: return "Kipps Lane to Pond Mills Rd/King Edward";
			case 2: return "Natural Science – Trafalgar Heights / Bonaventure";
			case 3: return "Downtown – Argyle";
			case 4: return "Fanshawe College – White Oaks Mall";
			case 5: return "Byron – Argyle Mall";
			case 6: return "University Hospital – Parkwood Institute";
			case 7: return "Westmount Mall – Argyle Mall";
			case 9: return "Downtown – Whitehills";
			case 10: return "Natural Science to Barker at Huron";
			case 12: return "Downtown – Wharncliffe & Wonderland";
			case 13: return "White Oaks Mall – Masonville Place";
			case 15: return "Huron Heights – Westmount Mall";
			case 16: return "Masonville Mall – Pond Mills";
			case 17: return "Argyle Mall to Byron/Riverbend";
			case 19: return "Downtown – Stoney Creek";
			case 20: return "Fanshawe College to Beaverbrook";
			case 24: return "Talbot Village – Summerside";
			case 25: return "Fanshawe College to Masonville Place";
			case 27: return "Fanshawe College to Capulet";
			case 28: return "White Oaks Mall – Lambeth";
			case 30: return "White Oaks Mall to Cheese Factory Rd";
			case 31: return "Alumni Hall – Hyde Park Power Centre";
			case 33: return "Alumni Hall to Proudfoot";
			case 34: return "Masonville Place to Alumni Hall/Natural Science";
			case 35: return "Argyle Mall – Trafalgar Heights";
			case 36: return "Fanshawe College to London Airport";
			case 37: return "Argyle Mall to Neptune Crescent";
			case 90: return "Masonville Mall - White Oaks Mall";
			case 91: return "Fanshawe College - Oxford & Wonderland";
			case 93: return "White Oaks Mall to Masonville Place";
			case 94: return "Express: Natural Science – Argyle Mall";
			case 102: return "Downtown – Natural Science";
			case 104: return "Ridout @ Grand – Fanshawe College";
			case 106: return "Downtown to Natural Science";
			// @formatter:on
			}
			throw new MTLog.Fatal("Unexpected route long name for %s!", gRoute.toStringPlus());
		}
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "009F60"; // GREEN (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern STARTS_WITH_LETTER = Pattern.compile("(^[a-z] )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign) {
		if (!fromStopName) {
			if ("Masonville via Natural Science".equals(directionHeadSign)) { // route 34
				return "CW via Natural Science";
			} else if ("Masonville via Alumni Hall".equals(directionHeadSign)) { // route 34
				return "CWW via Alumni Hall";
			}
		}
		directionHeadSign = super.cleanDirectionHeadsign(fromStopName, directionHeadSign);
		directionHeadSign = STARTS_WITH_LETTER.matcher(directionHeadSign).replaceAll(EMPTY);
		return directionHeadSign;
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern AREA = Pattern.compile("((^|\\W)(area)(\\W|$))", Pattern.CASE_INSENSITIVE);

	private static final Pattern ARGYLE_ = Pattern.compile("((^|\\W)(agyle)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ARGYLE_REPLACEMENT = "$2" + "Argyle" + "$4";

	private static final Pattern DEVERON_ = Pattern.compile("((^|\\W)(deverion)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String DEVERON_REPLACEMENT = "$2" + "Deveron" + "$4";

	private static final String HOSPITAL_SHORT = "Hosp";
	private static final Pattern HOSPITAL_ = Pattern.compile("((^|\\W)(hosptial|hos)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String HOSPITAL_REPLACEMENT = "$2" + HOSPITAL_SHORT + "$4";

	private static final String MASONVILLE = "Masonville";
	private static final Pattern MASONVILLE_ = Pattern.compile("((^|\\W)(masonvile|masvonille|masonvillel|masonville)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String MASONVILLE_REPLACEMENT = "$2" + MASONVILLE + "$4";

	private static final Pattern ONLY_ = Pattern.compile("((^|\\W)(only)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ONLY_REPLACEMENT = "$2" + EMPTY + "$4";

	private static final String UWO = "UWO";
	private static final Pattern UNIVERSITY_OF_WESTERN_ONTARIO = Pattern.compile("((^|\\W)(univ western ontario|western university)(\\W|$))",
			Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT = "$2" + UWO + "$4";

	private static final Pattern STARTS_WITH_EXPRESS_TO = Pattern.compile("(^express to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+[\\s]?)", Pattern.CASE_INSENSITIVE);

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
		tripHeadsign = HOSPITAL_.matcher(tripHeadsign).replaceAll(HOSPITAL_REPLACEMENT);
		tripHeadsign = MASONVILLE_.matcher(tripHeadsign).replaceAll(MASONVILLE_REPLACEMENT);
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

	private static final Pattern ENDS_WITH_STOP_CODE = Pattern.compile("( - #[\\d]*[\\w]*[']*$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredWords());
		gStopName = ENDS_WITH_STOP_CODE.matcher(gStopName).replaceAll(EMPTY);
		gStopName = HOSPITAL_.matcher(gStopName).replaceAll(HOSPITAL_REPLACEMENT);
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
		if (!Utils.isDigitsOnly(stopId)) {
			if (!gStop.getStopCode().isEmpty()
					&& Utils.isDigitsOnly(gStop.getStopCode())) {
				return Integer.parseInt(gStop.getStopCode());
			}
			if ("DUFFWATS".equalsIgnoreCase(stopId)) {
				return 2836;
			}
			if ("WELLBAS3".equalsIgnoreCase(stopId)) {
				return 2434;
			}
			if ("SDALNIXO".equalsIgnoreCase(stopId)) {
				return 69;
			}
			if ("MCMAWON2".equalsIgnoreCase(stopId)) {
				return 2001;
			}
			if ("STACFANS".equalsIgnoreCase(stopId)) {
				return 3838;
			}
			if ("WESTLAM1".equalsIgnoreCase(stopId)) {
				return 2453;
			}
			throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop.toStringPlus());
		}
		return super.getStopId(gStop); // used by real-time API https://realtime.londontransit.ca/ // TODO GTFS-RT
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		if ("'".equals(gStop.getStopCode())) {
			return EMPTY;
		}
		return super.getStopCode(gStop);
	}
}
